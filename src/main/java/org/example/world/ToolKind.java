package org.example.world;

// The family of tool that a given item belongs to. NONE covers non-tool items (blocks, food).
// Each BlockType records which ToolKind is its "effective" tool (speeds up breaking and may be
// required to drop the block's loot).
public enum ToolKind {
    PICKAXE,
    AXE,
    SHOVEL,
    SWORD,
    NONE
}
