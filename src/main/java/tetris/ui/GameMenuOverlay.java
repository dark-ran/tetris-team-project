package tetris.ui;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.swing.*;

/** Captures and blurs the frozen game, then displays keyboard-accessible menu choices. */
final class GameMenuOverlay extends JPanel {
    private final JComponent background;
    private final JButton resumeButton;
    private Runnable resume = () -> {}, settings = () -> {}, startMenu = () -> {}, exit = () -> {};
    private BufferedImage backdrop;

    GameMenuOverlay(JComponent background) {
        this.background = background;
        setOpaque(false); setLayout(new GridBagLayout()); setVisible(false);
        JPanel choices = ScreenSupport.verticalPanel();
        JLabel heading = new JLabel("게임 메뉴"); heading.setFont(AppTheme.font(Font.BOLD, 25f));
        heading.setAlignmentX(Component.CENTER_ALIGNMENT); choices.add(heading);
        choices.add(Box.createVerticalStrut(22));
        resumeButton = ScreenSupport.primaryButton("게임으로 돌아가기", () -> resume.run());
        JButton settingsButton = ScreenSupport.button("설정", () -> settings.run());
        JButton startButton = ScreenSupport.button("시작 메뉴로 이동", () -> startMenu.run());
        JButton exitButton = ScreenSupport.dangerButton("프로그램 종료", () -> exit.run());
        List<JButton> buttons = List.of(resumeButton, settingsButton, startButton, exitButton);
        for (JButton button : buttons) {
            button.setAlignmentX(Component.CENTER_ALIGNMENT);
            button.setMaximumSize(new Dimension(280, 44));
            choices.add(button); choices.add(Box.createVerticalStrut(12));
        }
        JPanel frame = ScreenSupport.framedPanel(new BorderLayout()); frame.add(choices);
        frame.setPreferredSize(new Dimension(340, 335)); add(frame);
        SwingKeyBindings.menuNavigation(this, buttons);
        SwingKeyBindings.backOnEscape(this, () -> resume.run());
    }
    void setActions(Runnable resume, Runnable settings, Runnable startMenu, Runnable exit) {
        this.resume = Objects.requireNonNull(resume); this.settings = Objects.requireNonNull(settings);
        this.startMenu = Objects.requireNonNull(startMenu); this.exit = Objects.requireNonNull(exit);
    }
    void open() {
        capture(); setVisible(true); revalidate(); repaint(); SwingKeyBindings.focus(resumeButton);
    }
    private void capture() {
        int width = Math.max(1, background.getWidth()), height = Math.max(1, background.getHeight());
        BufferedImage source = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = source.createGraphics();
        try { background.paint(graphics); } finally { graphics.dispose(); }
        if (width < 9 || height < 9) { backdrop = source; return; }
        float[] kernel = new float[81]; Arrays.fill(kernel, 1f / 81);
        backdrop = new ConvolveOp(new Kernel(9, 9, kernel), ConvolveOp.EDGE_NO_OP, null).filter(source, null);
    }
    @Override protected void paintComponent(Graphics graphics) {
        if (backdrop == null || backdrop.getWidth() != getWidth() || backdrop.getHeight() != getHeight()) capture();
        graphics.drawImage(backdrop, 0, 0, getWidth(), getHeight(), null);
        graphics.setColor(new Color(0, 0, 0, 150)); graphics.fillRect(0, 0, getWidth(), getHeight());
    }
}
