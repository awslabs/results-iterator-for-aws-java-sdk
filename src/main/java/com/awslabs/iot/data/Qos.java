package com.awslabs.iot.data;

public enum Qos {
    ONE(1),
    ZERO(0);

    private final int level;

    Qos(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}
