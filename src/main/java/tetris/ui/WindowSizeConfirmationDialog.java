package tetris.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.Box;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;

/** Keep/cancel confirmation; close, Escape and timeout all mean cancel. */
public final class WindowSizeConfirmationDialog implements WindowSizeConfirmation {
    private final int seconds;
    public WindowSizeConfirmationDialog() { this(15); }
    public WindowSizeConfirmationDialog(int seconds) {
        if (seconds < 1) throw new IllegalArgumentException("Confirmation time must be positive");
        this.seconds = seconds;
    }

    @Override public boolean confirm(Component owner, Dimension previous, Dimension proposed) {
        JPanel message = ScreenSupport.verticalPanel();
        message.setBorder(BorderFactory.createEmptyBorder(8, 6, 8, 6));
        JLabel question = new JLabel("변경한 창 크기를 유지하시겠습니까?");
        question.setFont(AppTheme.font(Font.BOLD, 16f)); question.setForeground(AppTheme.TEXT);
        JLabel size = ScreenSupport.mutedLabel(proposed.width + " × " + proposed.height);
        JLabel countdown = ScreenSupport.mutedLabel(countdown(seconds));
        message.add(question); message.add(Box.createVerticalStrut(8)); message.add(size);
        message.add(Box.createVerticalStrut(8)); message.add(countdown);
        JOptionPane pane = new JOptionPane(message, JOptionPane.PLAIN_MESSAGE,
                JOptionPane.YES_NO_OPTION, null, new String[] {"유지", "취소"}, "취소");
        JDialog dialog = pane.createDialog(owner, "창 크기 확인");
        long deadline = System.nanoTime() + seconds * 1_000_000_000L;
        Timer timer = new Timer(200, event -> {
            long remaining = Math.max(0, (deadline - System.nanoTime() + 999_999_999L) / 1_000_000_000L);
            if (remaining == 0) { pane.setValue("취소"); dialog.dispose(); }
            else countdown.setText(countdown(remaining));
        });
        timer.start();
        try { dialog.setVisible(true); }
        finally { timer.stop(); dialog.dispose(); }
        return "유지".equals(pane.getValue());
    }

    private static String countdown(long remaining) {
        return remaining + "초 안에 선택하지 않으면 이전 크기로 돌아갑니다.";
    }
}
