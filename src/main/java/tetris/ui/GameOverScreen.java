package tetris.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

/** 최종 점수·이름 등록·저장 재시도와 재시작·메뉴 이동을 제공한다. */
public class GameOverScreen extends JPanel {
    static { AppTheme.install(); }
    private static final String SCORE_UNMEASURED = "최종 점수: 미측정 (게임 연결 전)";

    private final JLabel scoreLabel = new JLabel();
    private final JButton restartButton;
    private final JPanel registration = ScreenSupport.transparentPanel(new BorderLayout(0, 12));
    private final JTextField nameField = new JTextField();
    private final JLabel errorLabel = new JLabel(" ", JLabel.CENTER);
    private Consumer<String> onRegister;
    private Runnable onSkip;
    private BooleanSupplier onRetry;
    private Supplier<String> saveError = () -> null;

    public GameOverScreen(Runnable onRestart, Runnable onScoreboard, Runnable onBack, Runnable onExit) {
        ScreenSupport.prepareScreen(this, "게임 종료",
                "Space로 새 게임을 시작하거나 기록을 등록할 수 있습니다.");
        restartButton = ScreenSupport.primaryButton("다시 시작", onRestart);
        configureNameField();
        JPanel center = ScreenSupport.transparentPanel(new GridBagLayout());
        center.add(createResultCard());
        add(center, BorderLayout.CENTER);
        add(createNavigation(onScoreboard, onBack, onExit), BorderLayout.SOUTH);
        configureShortcuts(onRestart, onBack);
    }

    private void configureNameField() {
        nameField.setFont(AppTheme.font(Font.PLAIN, 20f));
        nameField.setBackground(AppTheme.SURFACE_RAISED);
        nameField.setForeground(AppTheme.TEXT);
        nameField.setCaretColor(AppTheme.YELLOW);
        nameField.setSelectionColor(AppTheme.YELLOW);
        nameField.setSelectedTextColor(AppTheme.BACKGROUND);
        nameField.setOpaque(true);
        nameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        nameField.setPreferredSize(new Dimension(440, Math.max(44, nameField.getPreferredSize().height)));
        nameField.getAccessibleContext().setAccessibleName("랭킹에 등록할 이름");
    }

    private JPanel createResultCard() {
        JPanel result = ScreenSupport.framedPanel(new BorderLayout(0, 16));
        scoreLabel.setFont(AppTheme.font(Font.BOLD, 24f));
        scoreLabel.setHorizontalAlignment(JLabel.CENTER);
        result.add(scoreLabel, BorderLayout.NORTH);
        result.add(createRegistrationForm(), BorderLayout.CENTER);
        JLabel guide = ScreenSupport.mutedLabel("Space  다시 시작     ·     Esc  시작 메뉴");
        guide.setHorizontalAlignment(JLabel.CENTER);
        result.add(guide, BorderLayout.SOUTH);
        return result;
    }

    private JPanel createRegistrationForm() {
        registration.add(new JLabel("이름을 입력해 기록을 등록하세요 (1~20자).", JLabel.CENTER), BorderLayout.NORTH);
        // The field keeps its preferred height even when the result card has extra space.
        JPanel editor = ScreenSupport.transparentPanel(new GridBagLayout());
        editor.add(nameField); registration.add(editor, BorderLayout.CENTER);
        JPanel feedback = ScreenSupport.transparentPanel(new BorderLayout(0, 10));
        feedback.add(createRegistrationActions(), BorderLayout.NORTH);
        errorLabel.setForeground(AppTheme.YELLOW); feedback.add(errorLabel, BorderLayout.SOUTH);
        registration.add(feedback, BorderLayout.SOUTH);
        registration.setVisible(false);
        return registration;
    }

    private JPanel createRegistrationActions() {
        return ScreenSupport.actionRow(
                ScreenSupport.primaryButton("기록 등록", this::registerName),
                ScreenSupport.button("등록 건너뛰기", this::skipRegistration),
                ScreenSupport.button("저장 재시도", this::retryRegistration));
    }

    private JPanel createNavigation(Runnable onScoreboard, Runnable onBack, Runnable onExit) {
        return ScreenSupport.actionRow(restartButton,
                ScreenSupport.button("스코어보드", onScoreboard),
                ScreenSupport.button("시작 메뉴", onBack),
                ScreenSupport.dangerButton("프로그램 종료", onExit));
    }

    private void registerName() {
        try {
            if (onRegister != null) onRegister.accept(nameField.getText());
            showSaveError(saveError.get());
        } catch (IllegalArgumentException ex) { showSaveError(ex.getMessage()); }
    }

    private void skipRegistration() {
        if (onSkip != null) onSkip.run();
    }

    private void retryRegistration() {
        if (onRetry != null) onRetry.getAsBoolean();
        showSaveError(saveError.get());
    }

    private void configureShortcuts(Runnable onRestart, Runnable onBack) {
        SwingKeyBindings.backOnEscape(this, onBack);
        SwingKeyBindings.bindToScreen(this, KeyEvent.VK_SPACE, "restart-game", onRestart);
        // Consume the pressed event; the typed Space still goes to the name editor.
        nameField.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "name-space");
        nameField.getActionMap().put("name-space", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) { }
        });
        bindSpaceToButtons(this, onRestart);
    }

    private static void bindSpaceToButtons(Component component, Runnable restart) {
        if (component instanceof JButton button) {
            button.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "restart-game");
            button.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, true), "ignore-space-release");
            button.getActionMap().put("restart-game", new AbstractAction() {
                @Override public void actionPerformed(ActionEvent event) { restart.run(); }
            });
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) bindSpaceToButtons(child, restart);
        }
    }

    public void setOnRegister(Consumer<String> callback) { onRegister = Objects.requireNonNull(callback); }
    public void setOnSkip(Runnable callback) { onSkip = Objects.requireNonNull(callback); }
    public void setOnRetrySave(BooleanSupplier callback) { onRetry = Objects.requireNonNull(callback); }
    public void setSaveErrorSupplier(Supplier<String> supplier) { saveError = Objects.requireNonNull(supplier); }
    public void showSaveError(String message) {
        errorLabel.setText(message == null ? " " : message);
        errorLabel.setToolTipText(message);
    }

    /** 게임 미연결 상태의 미리보기. 실제 점수 대신 미측정을 표시한다. */
    public void showScreen() { showResult(SCORE_UNMEASURED, false); }
    public void showScreen(long score) { showScreen(score, false); }
    public void showScreen(long score, boolean canRegister) { showResult("최종 점수: " + score, canRegister); }

    private void showResult(String score, boolean canRegister) {
        scoreLabel.setText(score);
        registration.setVisible(canRegister);
        nameField.setText(""); showSaveError(saveError.get());
        revalidate(); repaint();
        SwingKeyBindings.focus(restartButton);
    }
}
