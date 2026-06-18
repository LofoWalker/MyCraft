package org.example.components;

/**
 * Identifies which kind of mob an entity is. Width/height sizes and max health come
 * from the enum so that all callers share a single source of truth without putting
 * logic into the component itself.
 */
public record MobType(Kind kind) {

    public enum Kind {
        COW(0.9f, 1.4f, 10),
        PIG(0.9f, 0.9f, 10),
        SHEEP(0.9f, 1.3f, 8),
        CHICKEN(0.4f, 0.7f, 4),
        ZOMBIE(0.6f, 1.95f, 20);

        private final float width;
        private final float height;
        private final int   maxHealth;

        Kind(float width, float height, int maxHealth) {
            this.width     = width;
            this.height    = height;
            this.maxHealth = maxHealth;
        }

        public float width()     { return width;     }
        public float height()    { return height;    }
        public int   maxHealth() { return maxHealth; }
    }
}
