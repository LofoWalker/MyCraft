package org.example.components;

// Smelting state for a furnace block entity. Carried by the block-entity entity alongside
// a 3-slot Inventory (slot 0 = input, slot 1 = fuel, slot 2 = output).
// cookTicks counts progress toward WorldConstants.FURNACE_COOK_TIME.
// fuelTicks is the remaining burn time for the currently-burning fuel item; when it reaches 0
// the furnace needs a new fuel item. fuelTicksMax records the original burn duration of the
// currently active fuel, used only for rendering the fuel gauge fraction.
public record Furnace(int cookTicks, int fuelTicks, int fuelTicksMax) {

    public static Furnace empty() {
        return new Furnace(0, 0, 0);
    }

    public boolean isBurning() {
        return fuelTicks > 0;
    }
}
