package org.example.components;

// The block the player is currently looking at, within reach. Lives on the player entity; written
// each tick from the view raycast and removed when nothing is targeted. faceX/Y/Z is the integer
// normal of the face being looked at (used by block placement in a later step).
public record TargetedBlock(int x, int y, int z, int faceX, int faceY, int faceZ) {}
