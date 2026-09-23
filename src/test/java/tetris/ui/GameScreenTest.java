package tetris.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

/** 게임 엔진 연결 전에도 좌우 입력 경로와 10칸 보드 경계를 검증한다. */
class GameScreenTest {
    @Test
    void arrowKeysMoveThePreviewOneColumnAtATime() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GameScreen screen = screen();
            screen.showScreen();
            int initialColumn = screen.previewColumn();

            invokeKey(screen, KeyEvent.VK_LEFT);
            assertEquals(initialColumn - 1, screen.previewColumn());
            invokeKey(screen, KeyEvent.VK_RIGHT);
            invokeKey(screen, KeyEvent.VK_RIGHT);
            assertEquals(initialColumn + 1, screen.previewColumn());
        });
    }

    @Test
    void letterKeysAndRepeatedInputUseTheSameMovementPath() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GameScreen screen = screen();
            screen.showScreen();

            invokeKey(screen, KeyEvent.VK_A);
            invokeKey(screen, KeyEvent.VK_A);
            invokeKey(screen, KeyEvent.VK_D);

            assertEquals(2, screen.previewColumn());
        });
    }

    @Test
    void previewCannotMoveOutsideTheTenColumnBoard() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GameScreen screen = screen();
            screen.showScreen();

            for (int press = 0; press < 20; press++) {
                invokeKey(screen, KeyEvent.VK_LEFT);
            }
            assertEquals(0, screen.previewColumn());

            for (int press = 0; press < 20; press++) {
                invokeKey(screen, KeyEvent.VK_RIGHT);
            }
            assertEquals(7, screen.previewColumn(), "폭이 3칸인 미리보기 블록은 8번째 열에서 멈춰야 한다");
        });
    }

    private static GameScreen screen() {
        return new GameScreen(() -> {}, () -> {}, () -> {});
    }

    private static void invokeKey(JComponent target, int keyCode) {
        int condition = JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
        Object actionName = target.getInputMap(condition).get(KeyStroke.getKeyStroke(keyCode, 0));
        assertNotNull(actionName, "키에 연결된 동작이 있어야 한다");
        target.getActionMap().get(actionName).actionPerformed(
                new ActionEvent(target, ActionEvent.ACTION_PERFORMED, actionName.toString()));
    }
}
