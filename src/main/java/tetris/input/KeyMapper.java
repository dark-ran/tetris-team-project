package tetris.input;

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import tetris.game.GameAction;
import tetris.settings.GameSettings;

/** 사용자 키 코드를 게임 행동으로 변환한다. 미등록 키는 null을 반환한다. */
public class KeyMapper {
    private Map<Integer, GameAction> actionsByKey;

    public KeyMapper() {
        actionsByKey = validateBindings(GameSettings.DEFAULT_KEY_BINDINGS);
    }

    /** 전체 설정을 검증한 뒤 교체한다. 잘못된 설정은 기존 키 연결을 변경하지 않는다. */
    public void updateBindings(Map<String, Integer> bindings) {
        actionsByKey = validateBindings(bindings);
    }

    private static Map<Integer, GameAction> validateBindings(Map<String, Integer> bindings) {
        Map<Integer, GameAction> replacement = new HashMap<>();
        GameSettings.normalizeKeyBindings(bindings).forEach((name, code) ->
                replacement.put(code, GameAction.valueOf(name)));
        replacement.put(KeyEvent.VK_ESCAPE, GameAction.TOGGLE_PAUSE);
        return Map.copyOf(replacement);
    }

    public GameAction map(int keyCode) {
        return actionsByKey.get(keyCode);
    }
}
