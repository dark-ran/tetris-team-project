package tetris.input;

import java.util.Objects;
import java.util.function.Consumer;
import tetris.game.GameAction;

/** 화면에서 받은 키를 게임 행동으로 변환하고 앱 제어에 전달한다. */
public class InputHandler {
    private final KeyMapper keyMapper;
    private Consumer<GameAction> actionListener;

    public InputHandler() { this(new KeyMapper()); }
    public InputHandler(KeyMapper keyMapper) { this.keyMapper = Objects.requireNonNull(keyMapper); }

    /** 앱의 행동 처리 메서드를 등록한다. */
    public void setActionListener(Consumer<GameAction> listener) {
        actionListener = Objects.requireNonNull(listener);
    }

    public void handleKey(int keyCode) {
        GameAction action = keyMapper.map(keyCode);
        if (action != null && actionListener != null) {
            actionListener.accept(action);
        }
    }
}
