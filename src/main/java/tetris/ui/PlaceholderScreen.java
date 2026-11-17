package tetris.ui;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.util.Objects;
import javax.swing.JButton;
import javax.swing.JPanel;

/** 아직 연결되지 않은 화면 자리. 실제 화면이 등록되기 전까지 안내 문구와 뒤로 가기만 제공한다. */
public class PlaceholderScreen extends JPanel {
    static { AppTheme.install(); }

    private final JButton backButton;

    public PlaceholderScreen(String title, String message, Runnable onBack) {
        Objects.requireNonNull(onBack);
        ScreenSupport.prepareScreen(this, Objects.requireNonNull(title), Objects.requireNonNull(message));
        JPanel center = ScreenSupport.transparentPanel(new GridBagLayout());
        center.add(ScreenSupport.mutedLabel("준비 중입니다."));
        add(center, BorderLayout.CENTER);
        backButton = ScreenSupport.button("뒤로 (Esc)", onBack);
        add(ScreenSupport.actionRow(backButton), BorderLayout.SOUTH);
        SwingKeyBindings.backOnEscape(this, onBack);
    }

    public void showScreen() {
        SwingKeyBindings.focus(backButton);
    }
}
