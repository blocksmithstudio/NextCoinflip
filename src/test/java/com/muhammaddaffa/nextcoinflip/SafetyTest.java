package com.muhammaddaffa.nextcoinflip;

import com.muhammaddaffa.nextcoinflip.coinflip.impl.CurrencyCoinflipGame;
import com.muhammaddaffa.nextcoinflip.coinflip.impl.ItemCoinflipGame;
import com.muhammaddaffa.nextcoinflip.coinflip.repository.CoinflipRepository;
import com.muhammaddaffa.nextcoinflip.currencies.Currency;
import com.muhammaddaffa.nextcoinflip.database.SQLDatabase;
import com.muhammaddaffa.nextcoinflip.users.User;
import com.muhammaddaffa.nextcoinflip.users.models.CoinflipStatistic;
import com.muhammaddaffa.nextcoinflip.users.models.WonItem;
import com.muhammaddaffa.nextcoinflip.users.repository.UserRepository;
import com.muhammaddaffa.nextcoinflip.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SafetyTest {
    private final UUID creator = UUID.randomUUID();
    private final UUID challenger = UUID.randomUUID();

    @AfterEach
    void clearGames() {
        CoinflipRepository.getGames().clear();
    }

    @Test
    void rejectsNegativeZeroAndOverflowingWagersAtTheGameBoundary() {
        Currency currency = currency();
        for (int amount : new int[]{Integer.MIN_VALUE, -100, 0, Integer.MAX_VALUE / 2 + 1, Integer.MAX_VALUE}) {
            assertFalse(CurrencyCoinflipGame.isValidWager(amount));
            assertThrows(IllegalArgumentException.class, () -> new CurrencyCoinflipGame(creator, currency, amount));
        }
        assertEquals(2_147_483_646, new CurrencyCoinflipGame(creator, currency, Integer.MAX_VALUE / 2).getWager() * 2);
    }

    @Test
    void appliesConfiguredMinimumAndMaximumAtTheGameBoundary() {
        Currency currency = new Currency("coins", true, 10, 100, "Coins", "%balance%", "take {amount}", "give {amount}");
        assertThrows(IllegalArgumentException.class, () -> new CurrencyCoinflipGame(creator, currency, 9));
        assertThrows(IllegalArgumentException.class, () -> new CurrencyCoinflipGame(creator, currency, 101));
        assertEquals(100, new CurrencyCoinflipGame(creator, currency, 100).getWager());
    }

    @Test
    void rejectsNonpositiveCurrencyCommands() {
        Currency currency = currency();
        assertThrows(IllegalArgumentException.class, () -> currency.giveBalance(creator, -100));
        assertThrows(IllegalArgumentException.class, () -> currency.takeBalance(creator, 0));
    }

    @Test
    void cancellationRefundsEachCurrencyStakeOnce() {
        Currency currency = mock(Currency.class);
        CurrencyCoinflipGame game = new CurrencyCoinflipGame(creator, currency, 100);
        game.setChallenger(challenger);
        CoinflipRepository.register(game);
        game.cancel(false);
        game.cancel(false);
        assertFalse(CoinflipRepository.contains(game));
        verify(currency).giveBalance(creator, 100);
        verify(currency).giveBalance(challenger, 100);
    }

    @Test
    void finishingThenCancellingCannotRefundOrPayTwice() {
        Currency currency = mock(Currency.class);
        User winner = spy(new User(creator));
        User loser = spy(new User(challenger));
        try (var users = mockStatic(UserRepository.class)) {
            users.when(() -> UserRepository.getUser(creator)).thenReturn(winner);
            users.when(() -> UserRepository.getUser(challenger)).thenReturn(loser);
            doNothing().when(winner).save(anyBoolean());
            doNothing().when(loser).save(anyBoolean());
            CurrencyCoinflipGame game = new CurrencyCoinflipGame(creator, currency, 100);
            CoinflipRepository.register(game);
            game.finish(creator, challenger);
            game.finish(creator, challenger);
            game.cancel(false);
            verify(currency).giveBalance(creator, 200);
            verify(currency, never()).giveBalance(creator, 100);
            verify(currency, never()).giveBalance(challenger, 100);
            assertEquals(1, winner.getStatistic().getWins());
            assertEquals(1, loser.getStatistic().getLosses());
        }
    }

    @Test
    void creatorCannotChallengeTheirOwnGame() {
        CurrencyCoinflipGame game = new CurrencyCoinflipGame(creator, currency(), 100);
        CoinflipRepository.register(game);
        assertFalse(game.canStart(creator));
    }

    @Test
    void staleItemConfirmationCannotAcceptAReplacementOffer() {
        ItemStack item = mock(ItemStack.class);
        Material material = mock(Material.class);
        when(item.getType()).thenReturn(material);
        when(item.getAmount()).thenReturn(1);
        when(item.clone()).thenReturn(item);
        ItemCoinflipGame game = new ItemCoinflipGame(creator, item);
        var oldOffer = new ItemCoinflipGame.ChallengerDetails(challenger, "Player", item, 1);
        var replacement = new ItemCoinflipGame.ChallengerDetails(challenger, "Player", item, 2);
        game.getChallengerMap().put("player", replacement);
        CoinflipRepository.register(game);
        assertFalse(game.acceptChallenge("Player", oldOffer));
        assertSame(replacement, game.getChallenger("Player"));
        assertNull(game.getChallenger());
        assertNull(game.getOffers()[1]);
    }

    @Test
    void offlineChallengerCannotStartOrChangeTheSelectedOffer() {
        ItemStack item = mock(ItemStack.class);
        Material material = mock(Material.class);
        when(item.getType()).thenReturn(material);
        when(item.getAmount()).thenReturn(1);
        when(item.clone()).thenReturn(item);
        ItemCoinflipGame game = new ItemCoinflipGame(creator, item);
        var offer = new ItemCoinflipGame.ChallengerDetails(challenger, "Player", item, 1);
        game.getChallengerMap().put("player", offer);
        CoinflipRepository.register(game);
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer(creator)).thenReturn(mock(Player.class));
            assertFalse(game.acceptChallenge("player", offer));
            assertNull(game.getChallenger());
            assertNull(game.getOffers()[1]);
        }
    }

    @Test
    void itemCancellationRefundsAcceptedAndPendingStakesOnce() {
        ItemStack item = mock(ItemStack.class);
        Material material = mock(Material.class);
        when(item.getType()).thenReturn(material);
        when(item.getAmount()).thenReturn(1);
        when(item.clone()).thenReturn(item);
        UUID pending = UUID.randomUUID();
        User owner = mock(User.class);
        User accepted = mock(User.class);
        User waiting = mock(User.class);
        ItemCoinflipGame game = new ItemCoinflipGame(creator, item);
        game.setChallenger(challenger);
        game.getOffers()[1] = item;
        game.getChallengerMap().put("waiting", new ItemCoinflipGame.ChallengerDetails(pending, "Waiting", item, 1));
        CoinflipRepository.register(game);
        try (var users = mockStatic(UserRepository.class); var bukkit = mockStatic(Bukkit.class)) {
            users.when(() -> UserRepository.getUser(creator)).thenReturn(owner);
            users.when(() -> UserRepository.getUser(challenger)).thenReturn(accepted);
            users.when(() -> UserRepository.getUser(pending)).thenReturn(waiting);
            game.cancel(true);
            game.cancel(true);
            verify(owner).addWonItem(item);
            verify(accepted).addWonItem(item);
            verify(waiting).addWonItem(item);
            assertTrue(game.getChallengerMap().isEmpty());
            assertFalse(CoinflipRepository.contains(game));
        }
    }

    @Test
    void changedHandCannotBeConsumedByAnOldConfirmation() {
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        ItemStack expected = mock(ItemStack.class);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getItemInMainHand()).thenReturn(mock(ItemStack.class));
        Material material = mock(Material.class);
        when(expected.getType()).thenReturn(material);
        when(expected.getAmount()).thenReturn(1);
        assertNull(Utils.takeWagerItem(player, expected));
        verify(inventory, never()).setItemInMainHand(any());
    }

    @Test
    void matchingHandIsConsumedOnce() {
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        ItemStack expected = mock(ItemStack.class);
        ItemStack clone = mock(ItemStack.class);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getItemInMainHand()).thenReturn(expected, (ItemStack) null);
        Material material = mock(Material.class);
        when(expected.getType()).thenReturn(material);
        when(expected.getAmount()).thenReturn(1);
        when(expected.clone()).thenReturn(clone);
        assertSame(clone, Utils.takeWagerItem(player, expected));
        assertNull(Utils.takeWagerItem(player, expected));
        verify(inventory).setItemInMainHand(null);
    }

    @Test
    void claimMustDeleteTheStoredItemBeforeItCanBeClaimedAgain() throws Exception {
        NextCoinflip plugin = mock(NextCoinflip.class);
        SQLDatabase db = mock(SQLDatabase.class);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(plugin.getDatabase()).thenReturn(db);
        when(db.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);
        WonItem item = mock(WonItem.class);
        UUID itemId = UUID.randomUUID();
        when(item.getUniqueId()).thenReturn(itemId);
        User user = new User(creator);
        user.addWonItem(item);
        try (var instance = mockStatic(NextCoinflip.class)) {
            instance.when(NextCoinflip::getInstance).thenReturn(plugin);
            assertTrue(user.removeWonItem(item));
            assertFalse(user.removeWonItem(item));
            assertTrue(user.getWonItems().isEmpty());
            verify(statement).setString(2, creator.toString());
            verify(statement).executeUpdate();
        }
    }

    @Test
    void failedClaimKeepsTheItemAvailableForRetry() throws Exception {
        NextCoinflip plugin = mock(NextCoinflip.class);
        SQLDatabase db = mock(SQLDatabase.class);
        when(plugin.getDatabase()).thenReturn(db);
        when(db.getConnection()).thenThrow(new SQLException("Synthetic test failure"));
        WonItem item = mock(WonItem.class);
        when(item.getUniqueId()).thenReturn(UUID.randomUUID());
        User user = new User(creator);
        user.addWonItem(item);
        try (var instance = mockStatic(NextCoinflip.class);
             var logger = mockStatic(com.muhammaddaffa.mdlib.utils.Logger.class)) {
            instance.when(NextCoinflip::getInstance).thenReturn(plugin);
            assertFalse(user.removeWonItem(item));
            assertEquals(1, user.getWonItems().size());
        }
    }

    @Test
    void offlineRefundUserLoadsExistingStatisticsBeforeAnyMutation() throws Exception {
        NextCoinflip plugin = mock(NextCoinflip.class);
        SQLDatabase db = mock(SQLDatabase.class);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet rows = mock(ResultSet.class);
        when(plugin.getDatabase()).thenReturn(db);
        when(db.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(rows);
        when(rows.next()).thenReturn(true, false);
        when(rows.getInt("wins")).thenReturn(17);
        when(rows.getInt("losses")).thenReturn(3);
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(creator);
        try (var instance = mockStatic(NextCoinflip.class)) {
            instance.when(NextCoinflip::getInstance).thenReturn(plugin);
            User user = UserRepository.getUser(creator);
            assertEquals(17, user.getStatistic().getWins());
            user.getStatistic().addWins();
            UserRepository.load(player);
            assertSame(user, UserRepository.getUser(creator));
            assertEquals(18, UserRepository.getUser(creator).getStatistic().getWins());
            verify(statement, times(2)).executeQuery();
        }
    }

    @Test
    void cumulativeEarningsDoNotOverflowAnInteger() {
        CoinflipStatistic statistic = new CoinflipStatistic();
        Currency currency = currency();
        statistic.setCurrencyEarnings(currency, Integer.MAX_VALUE);
        statistic.addCurrencyEarnings(currency, 100);
        assertEquals(2_147_483_747L, statistic.getCurrencyEarnings(currency));
    }

    private Currency currency() {
        return new Currency("coins", true, 1, 0, "Coins", "%balance%", "take {amount}", "give {amount}");
    }
}
