package tetris.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
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
        Window ownerWindow = owner instanceof Window window ? window
                : owner == null ? null : SwingUtilities.getWindowAncestor(owner);
        JDialog dialog = new JDialog(ownerWindow, "창 크기 확인", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setResizable(false);
        JPanel content = new JPanel(new BorderLayout(0, 18));
        content.setBackground(AppTheme.SURFACE);
        content.setBorder(AppTheme.frameBorder(22, 24));
        JLabel question = new JLabel("변경한 창 크기를 유지하시겠습니까?", JLabel.CENTER);
        question.setFont(AppTheme.font(Font.BOLD, 18f)); question.setForeground(AppTheme.TEXT);
        content.add(question, BorderLayout.NORTH);
        JPanel details = ScreenSupport.transparentPanel(new BorderLayout(0, 10));
        JLabel size = new JLabel(proposed.width + " × " + proposed.height, JLabel.CENTER);
        size.setFont(AppTheme.font(Font.BOLD, 18f)); size.setForeground(AppTheme.YELLOW);
        JLabel countdown = ScreenSupport.mutedLabel(countdown(seconds));
        countdown.setHorizontalAlignment(JLabel.CENTER);
        details.add(size, BorderLayout.NORTH); details.add(countdown, BorderLayout.CENTER);
        content.add(details, BorderLayout.CENTER);
        AtomicBoolean accepted = new AtomicBoolean();
        JButton cancel = ScreenSupport.button("취소", dialog::dispose);
        JButton keep = ScreenSupport.primaryButton("유지", () -> { accepted.set(true); dialog.dispose(); });
        JPanel actions = ScreenSupport.transparentPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        cancel.setPreferredSize(new Dimension(120, 42)); keep.setPreferredSize(new Dimension(120, 42));
        actions.add(cancel); actions.add(keep); content.add(actions, BorderLayout.SOUTH);
        Dimension preferred = content.getPreferredSize();
        content.setPreferredSize(new Dimension(Math.max(460, preferred.width), Math.max(220, preferred.height)));
        dialog.setContentPane(content);
        dialog.getRootPane().setDefaultButton(cancel);
        dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel-resize");
        dialog.getRootPane().getActionMap().put("cancel-resize", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) { dialog.dispose(); }
        });
        dialog.addWindowListener(new WindowAdapter() {
            @Override public void windowOpened(WindowEvent event) { cancel.requestFocusInWindow(); }
        });
        dialog.pack(); dialog.setLocationRelativeTo(owner);
        long deadline = System.nanoTime() + seconds * 1_000_000_000L;
        Timer timer = new Timer(200, event -> {
            long remaining = Math.max(0, (deadline - System.nanoTime() + 999_999_999L) / 1_000_000_000L);
            if (remaining == 0) dialog.dispose();
            else countdown.setText(countdown(remaining));
        });
        timer.start();
        try { dialog.setVisible(true); }
        finally { timer.stop(); dialog.dispose(); }
        return accepted.get();
    }

    private static String countdown(long remaining) {
        return remaining + "초 안에 선택하지 않으면 이전 크기로 돌아갑니다.";
    }
}
