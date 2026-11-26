package tetris.input;

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;
import tetris.game.GameAction;
import tetris.settings.GameSettings;

/** 사용자 키 코드를 게임 행동으로 변환한다. 미등록 키는 null을 반환한다. */
public class KeyMapper {
    private Map<Integer, GameAction> actionsByKey;

    // 대전 모드 키 매핑
    public record BattleInput(int player, GameAction action) {}

    private Map<Integer, BattleInput> battleActionsByKey;

    public KeyMapper() {
        actionsByKey = validateBindings(GameSettings.DEFAULT_KEY_BINDINGS);

        updateBattleBindings(GameSettings.DEFAULT_P1_KEY_BINDINGS,
                GameSettings.DEFAULT_P2_KEY_BINDINGS);
    }

    /** 전체 설정을 검증한 뒤 교체한다. 잘못된 설정은 기존 키 연결을 변경하지 않는다. */
    public void updateBindings(Map<String, Integer> bindings) {
        actionsByKey = validateBindings(bindings);
    }

    private static Map<Integer, GameAction> validateBindings(Map<String, Integer> bindings) {
        Map<Integer, GameAction> replacement = new HashMap<>();
        GameSettings.normalizeKeyBindings(bindings)
                .forEach((name, code) -> replacement.put(code, GameAction.valueOf(name)));
        replacement.put(KeyEvent.VK_ESCAPE, GameAction.TOGGLE_PAUSE);
        return Map.copyOf(replacement);
    }

    public GameAction map(int keyCode) {
        return actionsByKey.get(keyCode);
    }

    // P1/P2 대전 키 설정 변경
    public void updateBattleBindings(Map<String, Integer> p1, Map<String, Integer> p2) {
        p1 = GameSettings.normalizeBattleKeyBindings(p1, GameSettings.DEFAULT_P1_KEY_BINDINGS);
        p2 = GameSettings.normalizeBattleKeyBindings(p2, GameSettings.DEFAULT_P2_KEY_BINDINGS);

        GameSettings.validateBattleKeyBindings(p1, p2);

        Map<Integer, BattleInput> replacement = new HashMap<>();

        p1.forEach((name, code) ->
                replacement.put(code, new BattleInput(1, GameAction.valueOf(name))));

        p2.forEach((name, code) ->
                replacement.put(code, new BattleInput(2, GameAction.valueOf(name))));

        battleActionsByKey = Map.copyOf(replacement);
    }

    // 키 코드를 플레이어 번호와 게임 행동으로 변환
    public BattleInput mapBattle(int keyCode) {
        return battleActionsByKey.get(keyCode);
    }
}