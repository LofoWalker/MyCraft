package org.example.components;

// Tag marking an entity as a dropped item lying in the world (spawned when a block is broken). It
// carries no data; the stack it represents is held in a separate ItemStack component, and its
// physical state in Position/Velocity/Gravity/ColliderAABB. Used to distinguish item drops from the
// player in physics and pickup queries.
public record ItemEntity() {}
