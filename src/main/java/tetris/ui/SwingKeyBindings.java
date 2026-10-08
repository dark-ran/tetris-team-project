package tetris.ui;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import tetris.input.InputHandler;

/** 메뉴 이동과 화면 전환용 Swing 키 바인딩을 한곳에서 관리한다. */
final class SwingKeyBindings {
    private SwingKeyBindings() {
    }

    // TODO(4번): 설정 변경 시 이전 바인딩 제거, Esc/메뉴 키 충돌과 포커스 정책을 구현한다.
    static void bindGameKeys(JComponent screen, InputHandler input, Set<Integer> keyCodes) {
        throw new UnsupportedOperationException("TODO: 게임 키를 InputHandler로 연결");
    }

    static void activateButtonWithEnter(JButton button) {
        bind(button, JComponent.WHEN_FOCUSED, KeyEvent.VK_ENTER,
                "activate-button", () -> button.doClick(0));
    }

    static void backOnEscape(JComponent screen, Runnable onBack) {
        bind(screen, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
                KeyEvent.VK_ESCAPE, "go-back", onBack);
    }

    static void menuNavigation(JComponent screen, List<JButton> buttons) {
        bind(screen, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
                KeyEvent.VK_UP, "previous-menu-item", () -> moveFocus(buttons, -1));
        bind(screen, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
                KeyEvent.VK_DOWN, "next-menu-item", () -> moveFocus(buttons, 1));
    }

    static void bindToScreen(JComponent screen, int keyCode, String actionName, Runnable callback) {
        bind(screen, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
                keyCode, actionName, callback);
    }

    static void focus(JComponent component) {
        SwingUtilities.invokeLater(component::requestFocusInWindow);
    }

    private static void moveFocus(List<JButton> buttons, int direction) {
        Component current = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        int currentIndex = buttons.indexOf(current);
        int nextIndex = currentIndex < 0 ? 0 : Math.floorMod(currentIndex + direction, buttons.size());
        buttons.get(nextIndex).requestFocusInWindow();
    }

    private static void bind(
            JComponent component,
            int condition,
            int keyCode,
            String actionName,
            Runnable callback) {
        component.getInputMap(condition).put(KeyStroke.getKeyStroke(keyCode, 0), actionName);
        component.getActionMap().put(actionName, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                callback.run();
            }
        });
    }
}
