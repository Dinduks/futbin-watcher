package com.dindane.futbinwatcher;

public class Player {
    private final String name;
    private final String url;
    private final Long targetPrice;
    private final Long lowestBIN;
    private final Long lowestBIN2;
    private final Long lowestBIN3;

    public Player(String name, String url, Long targetPrice, Long lowestBIN, Long lowestBIN2, Long lowestBIN3) {
        this.name = name;
        this.url = url;
        this.targetPrice = targetPrice;
        this.lowestBIN = lowestBIN;
        this.lowestBIN2 = lowestBIN2;
        this.lowestBIN3 = lowestBIN3;
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
}
