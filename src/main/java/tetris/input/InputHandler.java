package tetris.input;

import java.util.Objects;
import java.util.function.Consumer;
import tetris.game.GameAction;

/** 입력 전달 경계. Swing 이벤트 연결은 후속 UI 구현에서 담당한다. */
public class InputHandler {
    private final KeyMapper keyMapper;
    private Consumer<GameAction> actionListener;

    public InputHandler() { this(new KeyMapper()); }
    public InputHandler(KeyMapper keyMapper) { this.keyMapper = Objects.requireNonNull(keyMapper); }

    // 연결 계약 제안: UI 키를 변환한 뒤 앱의 handleAction으로 전달한다.
    public void setActionListener(Consumer<GameAction> listener) {
        actionListener = Objects.requireNonNull(listener);
    }

    public void handleKey(int keyCode) {
        if (actionListener == null) throw new IllegalStateException("GameAction listener is required");
        GameAction action = keyMapper.map(keyCode);
        // KeyMapper는 미등록 키를 null로 반환하도록 계약을 제안한다.
        if (action != null) actionListener.accept(action);
    }
}
