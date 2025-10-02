package com.muhammaddaffa.nextcoinflip.users.models;

import com.muhammaddaffa.nextcoinflip.currencies.Currency;
import com.muhammaddaffa.nextcoinflip.currencies.repository.CurrencyRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CoinflipStatistic {

    private int wins;
    private int losses;
    private int loseStreak;

    private final Map<String, Integer> earningsMap = new HashMap<>();

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
            return 0.0; // Avoid division by zero
        }
        return (double) wins / (wins + losses) * 100;
    }

    public int getCurrencyEarnings(Currency currency) {
        return earningsMap.getOrDefault(currency.id(), 0);
    }

    public void setCurrencyEarnings(Currency currency, int amount) {
        earningsMap.put(currency.id(), amount);
    }

    public void addCurrencyEarnings(Currency currency, int amount) {
        earningsMap.put(currency.id(), getCurrencyEarnings(currency) + amount);
    }

    public String serializeEarnings() {

        List<String> data = new ArrayList<>();
        for (Currency currency : CurrencyRepository.getCurrencies()) {
            data.add(currency.id() + "=" + getCurrencyEarnings(currency));
        }
        return String.join(";", data);
    }

    public void deserializeEarnings(String serialized) {
        if (serialized == null || serialized.isEmpty()) return;
        if (serialized.contains(";")) {
            for (String each : serialized.split(";")) {
                setEarnings(each);
            }
        } else {
            setEarnings(serialized);
        }
    }

    private void setEarnings(String serialized) {
        String[] split = serialized.split("=");
        Currency currency = CurrencyRepository.getCurrency(split[0]);
        if (currency != null) {
            setCurrencyEarnings(currency, Integer.parseInt(split[1]));
        }
    }

}
