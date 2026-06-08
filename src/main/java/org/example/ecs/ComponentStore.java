package org.example.ecs;

import java.util.Arrays;

/**
 * Sparse-set component storage: O(1) add/remove/lookup, cache-friendly iteration.
 * sparse[entityId] → dense index ; dense/denseToEntity are packed contiguously.
 */
@SuppressWarnings("unchecked")
public class ComponentStore<T> {

    private static final int EMPTY = -1;

    private Object[] dense;
    private int[]    denseToEntity;
    private final int[]    sparse;
    private int size;

    public ComponentStore(int maxEntities) {
        dense        = new Object[64];
        denseToEntity = new int[64];
        sparse       = new int[maxEntities];
        Arrays.fill(sparse, EMPTY);
    }

    public void set(int entityId, T component) {
        if (sparse[entityId] != EMPTY) {
            dense[sparse[entityId]] = component;
            return;
        }
        ensureCapacity(size + 1);
        sparse[entityId]    = size;
        denseToEntity[size] = entityId;
        dense[size]         = component;
        size++;
    }

    public void remove(int entityId) {
        int idx = sparse[entityId];
        if (idx == EMPTY) return;
        int last = size - 1;
        if (idx != last) {
            dense[idx]          = dense[last];
            int lastEntity      = denseToEntity[last];
            denseToEntity[idx]  = lastEntity;
            sparse[lastEntity]  = idx;
        }
        dense[last]      = null;
        sparse[entityId] = EMPTY;
        size--;
    }

    public T get(int entityId) {
        int idx = sparse[entityId];
        return idx == EMPTY ? null : (T) dense[idx];
    }

    public boolean has(int entityId) {
        return sparse[entityId] != EMPTY;
    }

    public int size() { return size; }

    public int entityAt(int denseIndex) { return denseToEntity[denseIndex]; }

    private void ensureCapacity(int needed) {
        if (needed <= dense.length) return;
        int cap = Math.max(dense.length * 2, needed);
        dense         = Arrays.copyOf(dense, cap);
        denseToEntity = Arrays.copyOf(denseToEntity, cap);
    }
}
