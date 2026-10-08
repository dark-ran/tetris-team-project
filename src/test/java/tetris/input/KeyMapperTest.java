package tetris.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.event.KeyEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tetris.game.GameAction;
import tetris.settings.GameSettings;

/** 기본 설정과 변경한 키가 같은 행동으로 전달되며 잘못된 설정은 기존 연결을 보존하는지 확인한다. */
class KeyMapperTest {
    @ParameterizedTest
    @EnumSource(value = GameAction.class, names = {"TICK", "TOGGLE_PAUSE"}, mode = EnumSource.Mode.EXCLUDE)
    void defaultKeyMapsToItsAction(GameAction action) {
        KeyMapper mapper = new KeyMapper();
        assertEquals(action, mapper.map(GameSettings.DEFAULT_KEY_BINDINGS.get(action.name())));
    }

    @Test
    void replacementRemovesOldKeysAndDoesNotRetainMutableSettings() {
        KeyMapper mapper = new KeyMapper();
        Map<String, Integer> bindings = new LinkedHashMap<>();
        bindings.put(GameAction.LEFT.name(), KeyEvent.VK_A);
        mapper.updateBindings(bindings);
        bindings.put(GameAction.LEFT.name(), KeyEvent.VK_B);
        assertEquals(GameAction.LEFT, mapper.map(KeyEvent.VK_A));
        assertNull(mapper.map(KeyEvent.VK_LEFT));
        assertNull(mapper.map(KeyEvent.VK_B));
    }

    @Test
    void duplicateKeysDoNotReplaceExistingBindings() {
        KeyMapper mapper = new KeyMapper();
        assertThrows(IllegalArgumentException.class, () -> mapper.updateBindings(Map.of(
                GameAction.LEFT.name(), KeyEvent.VK_A, GameAction.RIGHT.name(), KeyEvent.VK_A)));
        assertEquals(GameAction.LEFT, mapper.map(KeyEvent.VK_LEFT));
        assertEquals(GameAction.RIGHT, mapper.map(KeyEvent.VK_RIGHT));
    }

    @Test
    void invalidActionAndKeyAreRejected() {
        KeyMapper mapper = new KeyMapper();
        assertThrows(IllegalArgumentException.class,
                () -> mapper.updateBindings(Map.of("UNKNOWN_ACTION", KeyEvent.VK_A)));
        assertThrows(IllegalArgumentException.class,
                () -> mapper.updateBindings(Map.of(GameAction.TICK.name(), KeyEvent.VK_A)));
        assertThrows(IllegalArgumentException.class,
                () -> mapper.updateBindings(Map.of(GameAction.LEFT.name(), KeyEvent.VK_UNDEFINED)));
        assertThrows(NullPointerException.class, () -> mapper.updateBindings(null));
        assertEquals(GameAction.LEFT, mapper.map(KeyEvent.VK_LEFT));
    }

    @Test
    void unknownKeysAreIgnored() {
        assertNull(new KeyMapper().map(KeyEvent.VK_F12));
        assertNull(new KeyMapper().map(KeyEvent.VK_P));
        assertEquals(GameAction.TOGGLE_PAUSE, new KeyMapper().map(KeyEvent.VK_ESCAPE));
        assertThrows(IllegalArgumentException.class, () -> new KeyMapper().updateBindings(
                Map.of(GameAction.LEFT.name(), KeyEvent.VK_ESCAPE)));
    }
}
