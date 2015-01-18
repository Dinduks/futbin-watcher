package com.dindane.futbinwatcher;

import com.dindane.futbinwatcher.exceptions.Action;

public class Player {
    private final String name;
    private final String url;
    private final Long targetPrice;
    private final Long lowestBIN;
    private final Long lowestBIN2;
    private final Long lowestBIN3;
    private final Action action;

    public Player(String name, String url, Long targetPrice, Long lowestBIN, Long lowestBIN2, Long lowestBIN3, Action action) {
        this.name = name;
        this.url = url;
        this.targetPrice = targetPrice;
        this.lowestBIN = lowestBIN;
        this.lowestBIN2 = lowestBIN2;
        this.lowestBIN3 = lowestBIN3;
        this.action = action;
    }

    public Long priceDifference() {
        return (lowestBIN == null) ? null : targetPrice - lowestBIN;
    }

    public String getName() {
        return name;
    }

    public Long getTargetPrice() {
        return targetPrice;
    }

    public Long getLowestBIN() {
        return lowestBIN;
    }

    public Long getLowestBIN2() {
        return lowestBIN2;
    }

    public Long getLowestBIN3() {
        return lowestBIN3;
    }

    public Action getAction() {
        return action;
    }

    public String getUrl() {
        return url;
    }
}
