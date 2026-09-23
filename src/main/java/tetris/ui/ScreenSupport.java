package tetris.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.LayoutManager;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** 화면 프레임, 버튼, 레이블 등 반복되는 Swing 컴포넌트를 만든다. */
final class ScreenSupport {
    static {
        AppTheme.install();
    }

    private ScreenSupport() {
    }

    /** 화면 공통 여백과 제목·설명 헤더를 구성한다. */
    static void prepareScreen(JPanel screen, String title, String description) {
        screen.setLayout(new BorderLayout(20, 20));
        screen.setBackground(AppTheme.BACKGROUND);
        screen.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JLabel heading = new JLabel(title);
        heading.setFont(AppTheme.font(Font.BOLD, 27f));
        JLabel details = new JLabel("<html>" + description + "</html>");
        details.setFont(AppTheme.font(Font.PLAIN, 13f));
        details.setForeground(AppTheme.MUTED);

        JPanel copy = transparentPanel(new BorderLayout(0, 6));
        copy.add(heading, BorderLayout.NORTH);
        copy.add(details, BorderLayout.CENTER);
        JPanel header = transparentPanel(new BorderLayout(14, 0));
        header.add(new TetrominoMark(), BorderLayout.WEST);
        header.add(copy, BorderLayout.CENTER);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.BORDER),
                BorderFactory.createEmptyBorder(0, 0, 16, 0)));
        screen.add(header, BorderLayout.NORTH);
    }

    /** Enter로도 눌리는 버튼을 만든다. */
    static JButton button(String text, Runnable callback) {
        return button(text, callback, ThemedButton.Style.SECONDARY);
    }

    static JButton primaryButton(String text, Runnable callback) {
        return button(text, callback, ThemedButton.Style.PRIMARY);
    }

    static JButton dangerButton(String text, Runnable callback) {
        return button(text, callback, ThemedButton.Style.DANGER);
    }

    private static JButton button(String text, Runnable callback, ThemedButton.Style style) {
        JButton button = new ThemedButton(text, style);
        button.addActionListener(event -> callback.run());
        SwingKeyBindings.activateButtonWithEnter(button);
        return button;
    }

    /** 화면 하단에 버튼을 나란히 배치한 줄을 만든다. */
    static JPanel actionRow(JButton... buttons) {
        JPanel row = transparentPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, AppTheme.BORDER),
                BorderFactory.createEmptyBorder(12, 0, 0, 0)));
        for (JButton button : buttons) {
            row.add(button);
        }
        return row;
    }

    /** 테두리와 안쪽 여백이 적용된 콘텐츠 패널. */
    static JPanel framedPanel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(AppTheme.SURFACE);
        panel.setBorder(AppTheme.frameBorder(18, 20));
        return panel;
    }

    static JLabel mutedLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(AppTheme.MUTED);
        return label;
    }

    static JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(AppTheme.font(Font.BOLD, 14f));
        label.setForeground(AppTheme.YELLOW);
        return label;
    }

    static JPanel transparentPanel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    static JPanel verticalPanel() {
        JPanel panel = transparentPanel(null);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }
}
