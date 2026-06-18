package org.example.systems;

import org.example.components.PlayerInput;
import org.example.ecs.Entity;
import org.example.ecs.World;
import org.example.world.WorldConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.glfw.GLFW.*;

class InputSystemTest {

    private World world;
    private Entity player;

    @BeforeEach
    void setUp() {
        world  = new World();
        player = world.create();
        world.add(player, new PlayerInput(false, false, false, false, false, false, 0f, 0f, false, false,
                false, 0, WorldConstants.NO_HOTBAR_SELECT, false));
    }

    /** Build an InputSystem where only the given GLFW key codes are considered pressed. */
    private InputSystem buildSystem(int... pressedKeys) {
        Set<Integer> pressed = new HashSet<>();
        for (int k : pressedKeys) pressed.add(k);
        return new InputSystem(
                (win, key) -> pressed.contains(key) ? GLFW_PRESS : GLFW_RELEASE,
                () -> {});
    }

    @Test
    void noKeysPressedAllFlagsFalse() {
        InputSystem sys = buildSystem();
        sys.update(world, 0.016f);
        PlayerInput input = world.get(player, PlayerInput.class).orElseThrow();
        assertFalse(input.forward());
        assertFalse(input.backward());
        assertFalse(input.strafeLeft());
        assertFalse(input.strafeRight());
        assertFalse(input.jump());
        assertFalse(input.descend());
    }

    @Test
    void wKeySetsForwardFlag() {
        buildSystem(GLFW_KEY_W).update(world, 0.016f);
        assertTrue(world.get(player, PlayerInput.class).orElseThrow().forward());
    }

    @Test
    void sKeySetsBackwardFlag() {
        buildSystem(GLFW_KEY_S).update(world, 0.016f);
        assertTrue(world.get(player, PlayerInput.class).orElseThrow().backward());
    }

    @Test
    void aKeySetsStrafeLeftFlag() {
        buildSystem(GLFW_KEY_A).update(world, 0.016f);
        assertTrue(world.get(player, PlayerInput.class).orElseThrow().strafeLeft());
    }

    @Test
    void dKeySetsStrafeRightFlag() {
        buildSystem(GLFW_KEY_D).update(world, 0.016f);
        assertTrue(world.get(player, PlayerInput.class).orElseThrow().strafeRight());
    }

    @Test
    void spaceKeySetsJumpFlag() {
        buildSystem(GLFW_KEY_SPACE).update(world, 0.016f);
        assertTrue(world.get(player, PlayerInput.class).orElseThrow().jump());
    }

    @Test
    void leftControlSetsDescendFlag() {
        buildSystem(GLFW_KEY_LEFT_CONTROL).update(world, 0.016f);
        assertTrue(world.get(player, PlayerInput.class).orElseThrow().descend());
    }

    @Test
    void fKeySetsEatFlag() {
        buildSystem(GLFW_KEY_F).update(world, 0.016f);
        assertTrue(world.get(player, PlayerInput.class).orElseThrow().eat());
    }

    @Test
    void noKeyLeavesEatFalse() {
        buildSystem().update(world, 0.016f);
        assertFalse(world.get(player, PlayerInput.class).orElseThrow().eat());
    }

    @Test
    void multiplePressedKeysCombineCorrectly() {
        buildSystem(GLFW_KEY_W, GLFW_KEY_D, GLFW_KEY_SPACE).update(world, 0.016f);
        PlayerInput input = world.get(player, PlayerInput.class).orElseThrow();
        assertTrue(input.forward());
        assertTrue(input.strafeRight());
        assertTrue(input.jump());
        assertFalse(input.backward());
        assertFalse(input.strafeLeft());
    }

    @Test
    void mouseDeltaPopulatesPlayerInput() {
        InputSystem sys = buildSystem();
        sys.accumulateDelta(8f, -3f);
        sys.update(world, 0.016f);
        PlayerInput input = world.get(player, PlayerInput.class).orElseThrow();
        assertEquals(8f,  input.mouseDeltaX(), 1e-5f);
        assertEquals(-3f, input.mouseDeltaY(), 1e-5f);
    }

    @Test
    void mouseDeltaResetAfterFirstRead() {
        InputSystem sys = buildSystem();
        sys.accumulateDelta(5f, 2f);
        sys.update(world, 0.016f);
        sys.update(world, 0.016f);
        PlayerInput input = world.get(player, PlayerInput.class).orElseThrow();
        assertEquals(0f, input.mouseDeltaX(), 1e-5f);
        assertEquals(0f, input.mouseDeltaY(), 1e-5f);
    }

    @Test
    void scrollAccumulatesIntoScrollDelta() {
        InputSystem sys = buildSystem();
        sys.accumulateScroll(2);
        sys.update(world, 0.016f);
        assertEquals(2, world.get(player, PlayerInput.class).orElseThrow().scrollDelta());
    }

    @Test
    void scrollResetAfterFirstRead() {
        InputSystem sys = buildSystem();
        sys.accumulateScroll(3);
        sys.update(world, 0.016f);
        sys.update(world, 0.016f);
        assertEquals(0, world.get(player, PlayerInput.class).orElseThrow().scrollDelta());
    }

    @Test
    void noNumberKeyLeavesHotbarSelectAtSentinel() {
        buildSystem().update(world, 0.016f);
        assertEquals(WorldConstants.NO_HOTBAR_SELECT,
                world.get(player, PlayerInput.class).orElseThrow().hotbarSelect());
    }

    @Test
    void numberKeyOneSelectsSlotZero() {
        buildSystem(GLFW_KEY_1).update(world, 0.016f);
        assertEquals(0, world.get(player, PlayerInput.class).orElseThrow().hotbarSelect());
    }

    @Test
    void numberKeyNineSelectsSlotEight() {
        buildSystem(GLFW_KEY_9).update(world, 0.016f);
        assertEquals(8, world.get(player, PlayerInput.class).orElseThrow().hotbarSelect());
    }

    @Test
    void escapeKeyTriggersCloseAction() {
        AtomicBoolean closed = new AtomicBoolean(false);
        InputSystem sys = new InputSystem(
                (win, key) -> key == GLFW_KEY_ESCAPE ? GLFW_PRESS : GLFW_RELEASE,
                () -> closed.set(true));
        sys.update(world, 0.016f);
        assertTrue(closed.get());
    }

    @Test
    void escapeKeySkipsPlayerInputUpdate() {
        InputSystem sys = new InputSystem(
                (win, key) -> key == GLFW_KEY_ESCAPE ? GLFW_PRESS : GLFW_RELEASE,
                () -> {});
        PlayerInput before = world.get(player, PlayerInput.class).orElseThrow();
        sys.update(world, 0.016f);
        PlayerInput after = world.get(player, PlayerInput.class).orElseThrow();
        assertSame(before, after);
    }
}
