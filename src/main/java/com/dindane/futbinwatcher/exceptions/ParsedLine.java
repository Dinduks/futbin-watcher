package com.dindane.futbinwatcher.exceptions;

public class ParsedLine {
    private final String playerId;
    private final Action action;
    private final Long targetPrice;

    public ParsedLine(String playerId, Action action, Long targetPrice) {
        this.playerId = playerId;
        this.action = action;
        this.targetPrice = targetPrice;
    }

    public String getPlayerId() {
        return playerId;
    }

    public Action getAction() {
        return action;
    }

    public Long getTargetPrice() {
        return targetPrice;
    }
}