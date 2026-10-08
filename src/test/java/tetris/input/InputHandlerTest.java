package tetris.input;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import tetris.game.GameAction;

/** 반복 키 입력과 미등록 키, 처리 메서드의 실패를 입력 전달 경계에서 확인한다. */
class InputHandlerTest {
    @Test
    void eachRepeatedKeyIsDeliveredAndUnknownKeysAreIgnored() {
        InputHandler handler = new InputHandler();
        List<GameAction> received = new ArrayList<>();
        handler.setActionListener(received::add);
        handler.handleKey(KeyEvent.VK_LEFT);
        handler.handleKey(KeyEvent.VK_LEFT);
        handler.handleKey(KeyEvent.VK_F12);
        assertEquals(List.of(GameAction.LEFT, GameAction.LEFT), received);
    }

    @Test
    void inputBeforeListenerRegistrationIsIgnored() {
        assertDoesNotThrow(() -> new InputHandler().handleKey(KeyEvent.VK_LEFT));
    }

    @Test
    void listenerFailureIsNotHidden() {
        InputHandler handler = new InputHandler();
        IllegalStateException failure = new IllegalStateException("행동 처리 실패");
        handler.setActionListener(action -> { throw failure; });
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> handler.handleKey(KeyEvent.VK_LEFT)));
    }
}
