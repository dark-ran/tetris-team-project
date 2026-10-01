package tetris.settings;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tetris.game.GameAction;

class GameSettingsTest {

    @Test
    void defaultSettingsHasExpectedValues() {
        GameSettings settings = GameSettings.defaultSettings();

        assertEquals(GameSettings.PRESET_MEDIUM, settings.windowSizePreset());
        assertFalse(settings.colorBlindMode());
        assertEquals(new Dimension(1040, 860), settings.windowDimension());

        Map<String, Integer> keys = settings.keyBindings();
        assertEquals(KeyEvent.VK_LEFT, keys.get(GameAction.LEFT.name()));
        assertEquals(KeyEvent.VK_RIGHT, keys.get(GameAction.RIGHT.name()));
        assertEquals(KeyEvent.VK_DOWN, keys.get(GameAction.DOWN.name()));
        assertEquals(KeyEvent.VK_UP, keys.get(GameAction.ROTATE_CLOCKWISE.name()));
        assertEquals(KeyEvent.VK_SPACE, keys.get(GameAction.HARD_DROP.name()));
        assertEquals(KeyEvent.VK_P, keys.get(GameAction.TOGGLE_PAUSE.name()));
        assertEquals(KeyEvent.VK_Q, keys.get(GameAction.QUIT.name()));
    }

    @Test
    void keyBindingsIsUnmodifiable() {
        GameSettings settings = GameSettings.defaultSettings();
        assertThrows(UnsupportedOperationException.class, () ->
                settings.keyBindings().put("NEW_ACTION", KeyEvent.VK_A));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void validPresetsProduceCorrectDimensions(int preset) {
        GameSettings settings = new GameSettings().withWindowSizePreset(preset);
        assertEquals(preset, settings.windowSizePreset());
        assertTrue(GameSettings.isValidPreset(preset));

        Dimension dim = settings.windowDimension();
        assertNotNull(dim);
        assertTrue(dim.width > 0);
        assertTrue(dim.height > 0);

        String label = settings.windowSizePresetLabel();
        assertNotNull(label);
        assertFalse(label.isBlank());
    }

    @Test
    void presetLabelsAndListAreAccurate() {
        assertEquals(3, GameSettings.PRESET_COUNT);
        assertEquals(List.of(0, 1, 2), GameSettings.PRESETS);
        assertEquals("Small (900×760)", GameSettings.presetLabel(GameSettings.PRESET_SMALL));
        assertEquals("Medium (1040×860)", GameSettings.presetLabel(GameSettings.PRESET_MEDIUM));
        assertEquals("Large (1280×1024)", GameSettings.presetLabel(GameSettings.PRESET_LARGE));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 3, 99})
    void invalidPresetsThrowException(int invalidPreset) {
        assertFalse(GameSettings.isValidPreset(invalidPreset));
        assertThrows(IllegalArgumentException.class, () ->
                new GameSettings(invalidPreset, false, Map.of()));
        assertThrows(IllegalArgumentException.class, () ->
                GameSettings.presetDimension(invalidPreset));
        assertThrows(IllegalArgumentException.class, () ->
                GameSettings.presetLabel(invalidPreset));
    }

    @Test
    void witherMethodsCreateModifiedCopies() {
        GameSettings initial = GameSettings.defaultSettings();

        GameSettings withPreset = initial.withWindowSizePreset(GameSettings.PRESET_LARGE);
        assertEquals(GameSettings.PRESET_LARGE, withPreset.windowSizePreset());
        assertEquals(initial.colorBlindMode(), withPreset.colorBlindMode());

        GameSettings withColorBlind = initial.withColorBlindMode(true);
        assertTrue(withColorBlind.colorBlindMode());
        assertEquals(initial.windowSizePreset(), withColorBlind.windowSizePreset());

        Map<String, Integer> customKeys = new HashMap<>(initial.keyBindings());
        customKeys.put(GameAction.HARD_DROP.name(), KeyEvent.VK_ENTER);
        GameSettings withKeys = initial.withKeyBindings(customKeys);
        assertEquals(KeyEvent.VK_ENTER, withKeys.keyBindings().get(GameAction.HARD_DROP.name()));
    }

    @Test
    void equalsAndHashCodeContract() {
        GameSettings a = new GameSettings();
        GameSettings b = GameSettings.defaultSettings();
        GameSettings c = a.withColorBlindMode(true);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotNull(a.toString());
    }

    @Test
    void nullKeyBindingsThrowsException() {
        assertThrows(NullPointerException.class, () ->
                new GameSettings(GameSettings.PRESET_MEDIUM, false, null));
    }
}
