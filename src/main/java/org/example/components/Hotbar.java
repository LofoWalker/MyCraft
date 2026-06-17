package org.example.components;

// The currently selected hotbar slot, an index into the first WorldConstants.HOTBAR_SLOTS slots of
// the player's Inventory. Range 0..HOTBAR_SLOTS-1; HotbarSelectionSystem keeps it wrapped.
public record Hotbar(int selectedSlot) {}
