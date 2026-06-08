package org.example.ecs;

public record Entity(int id) {
    public static final Entity NULL = new Entity(-1);
}
