package com.muhammaddaffa.nextcoinflip.users.models;

import com.muhammaddaffa.nextcoinflip.currencies.Currency;
import com.muhammaddaffa.mdlib.utils.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CoinflipStatistic {

    private int wins;
    private int losses;
    private int loseStreak;

    private final Map<String, Long> earningsMap = new HashMap<>();

    public int getWins() {
        return wins;
    }

    public void setWins(int wins) {
        this.wins = wins;
    }

    public void addWins() {
        this.wins++;
    }

    public int getLosses() {
        return losses;
    }

    public void setLosses(int losses) {
        this.losses = losses;
    }

    public void addLosses() {
        this.losses++;
    }

    public int getLoseStreak() {
        return loseStreak;
    }

    public void setLoseStreak(int loseStreak) {
        this.loseStreak = loseStreak;
    }

    public void addLoseStreak() {
        this.loseStreak++;
    }

    public void resetLoseStreak() {
        this.loseStreak = 0;
    }

    public int getTotalGames() {
        return getWins() + getLosses();
    }

    public double getWinRate() {
        if (wins + losses == 0) {
            return 0.0;
        }
        return (double) wins / (wins + losses) * 100;
    }

    public long getCurrencyEarnings(Currency currency) {
        return earningsMap.getOrDefault(currency.id(), 0L);
    }

    public void setCurrencyEarnings(Currency currency, long amount) {
        earningsMap.put(currency.id(), amount);
    }

    public void addCurrencyEarnings(Currency currency, int amount) {
        earningsMap.put(currency.id(), getCurrencyEarnings(currency) + amount);
    }

    public String serializeEarnings() {

        List<String> data = new ArrayList<>();
        this.earningsMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> data.add(entry.getKey() + "=" + entry.getValue()));
        return String.join(";", data);
    }

    public void deserializeEarnings(String serialized) {
        if (serialized == null || serialized.isEmpty()) return;
        for (String entry : serialized.split(";")) {
            this.setEarnings(entry);
        }
    }

    private void setEarnings(String serialized) {
        String[] split = serialized.split("=", 2);
        if (split.length != 2 || split[0].isBlank()) {
            Logger.severe("Ignoring a malformed currency earnings entry");
            return;
        }
        try {
            this.earningsMap.put(split[0], Long.parseLong(split[1]));
        } catch (NumberFormatException ex) {
            Logger.severe("Ignoring a malformed currency earnings amount");
        }
    }

}
