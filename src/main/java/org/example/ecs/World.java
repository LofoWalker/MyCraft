package org.example.ecs;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class World {

    private static final int MAX_ENTITIES = 65_536;

    private int nextId = 0;
    private final int[] freeIds = new int[MAX_ENTITIES];
    private int freeCount = 0;

    private final Map<Class<?>, ComponentStore<?>> stores = new HashMap<>();

    public Entity create() {
        int id = freeCount > 0 ? freeIds[--freeCount] : nextId++;
        return new Entity(id);
    }

    public void destroy(Entity entity) {
        for (ComponentStore<?> s : stores.values()) s.remove(entity.id());
        freeIds[freeCount++] = entity.id();
    }

    @SuppressWarnings("unchecked")
    public <T> void add(Entity entity, T component) {
        store((Class<T>) component.getClass()).set(entity.id(), component);
    }

    public <T> void remove(Entity entity, Class<T> type) {
        ComponentStore<T> s = storeOrNull(type);
        if (s != null) s.remove(entity.id());
    }

    public <T> Optional<T> get(Entity entity, Class<T> type) {
        ComponentStore<T> s = storeOrNull(type);
        return s != null ? Optional.ofNullable(s.get(entity.id())) : Optional.empty();
    }

    public <T> boolean has(Entity entity, Class<T> type) {
        ComponentStore<T> s = storeOrNull(type);
        return s != null && s.has(entity.id());
    }

    /**
     * Stores are pre-resolved to avoid HashMap lookups in the hot inner loop.
     */
    public int[] query(Class<?>... types) {
        ComponentStore<?>[] resolved = resolveStores(types);
        if (resolved == null) return new int[0];

        ComponentStore<?> pivot = Arrays.stream(resolved)
                .min(Comparator.comparingInt(ComponentStore::size))
                .orElseThrow();

        int[] result = new int[pivot.size()];
        int count = 0;
        outer:
        for (int i = 0, n = pivot.size(); i < n; i++) {
            int eid = pivot.entityAt(i);
            for (ComponentStore<?> s : resolved) {
                if (!s.has(eid)) continue outer;
            }
            result[count++] = eid;
        }
        return count == result.length ? result : Arrays.copyOf(result, count);
    }

    private ComponentStore<?>[] resolveStores(Class<?>... types) {
        ComponentStore<?>[] resolved = new ComponentStore<?>[types.length];
        for (int i = 0; i < types.length; i++) {
            resolved[i] = stores.get(types[i]);
            if (resolved[i] == null) return null;
        }
        return resolved;
    }

    @SuppressWarnings("unchecked")
    private <T> ComponentStore<T> storeOrNull(Class<T> type) {
        return (ComponentStore<T>) stores.get(type);
    }

    @SuppressWarnings("unchecked")
    private <T> ComponentStore<T> store(Class<T> type) {
        return (ComponentStore<T>) stores.computeIfAbsent(
                type, k -> new ComponentStore<>(MAX_ENTITIES));
    }
}
