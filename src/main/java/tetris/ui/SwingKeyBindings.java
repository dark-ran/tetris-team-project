package tetris.ui;

import java.awt.Component;
import java.awt.Font;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/** 메뉴 이동과 화면 전환용 Swing 키 바인딩을 한곳에서 관리한다. */
public final class SwingKeyBindings {
    private SwingKeyBindings() {
    }

    /**
     * 화면에 표시할 키 이름. macOS는 Space와 Enter를 ␣, ⏎ 같은 기호로 돌려주는데
     * 번들 글꼴에는 없는 글자이므로 모든 운영체제에서 같은 영문 이름을 쓴다.
     * 그 밖에 글꼴이 그릴 수 없는 이름은 키 코드로 대신한다.
     */
    public static String keyText(int keyCode) {
        return switch (keyCode) {
            case KeyEvent.VK_LEFT -> "←";
            case KeyEvent.VK_RIGHT -> "→";
            case KeyEvent.VK_UP -> "↑";
            case KeyEvent.VK_DOWN -> "↓";
            case KeyEvent.VK_SPACE -> "Space";
            case KeyEvent.VK_ENTER -> "Enter";
            case KeyEvent.VK_BACK_SPACE -> "Backspace";
            case KeyEvent.VK_TAB -> "Tab";
            case KeyEvent.VK_DELETE -> "Delete";
            default -> displayable(KeyEvent.getKeyText(keyCode), keyCode);
        };
    }

    private static String displayable(String text, int keyCode) {
        return AppTheme.font(Font.PLAIN, 14f).canDisplayUpTo(text) < 0 ? text : "Key " + keyCode;
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
