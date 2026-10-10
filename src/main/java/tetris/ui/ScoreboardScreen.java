package tetris.ui;

import java.awt.BorderLayout;
import java.util.List;
import java.util.Objects;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import tetris.scoreboard.ScoreEntry;

/**
 * Displays ranked records and highlights the newest matching entry.
 *
 * <p>현재 사용 기능: 전달받은 ScoreEntry 목록의 순위·이름·점수 표시와 메뉴 복귀.</p>
 */
public class ScoreboardScreen extends JPanel {
    private final DefaultTableModel records = new DefaultTableModel(new String[] {"순위", "이름", "점수"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JButton backButton;
    private final JButton resetButton;
    private final JLabel feedback = new JLabel(" ");
    private Runnable onReset;
    private JTable table;

    public ScoreboardScreen(Runnable onBack) {
        ScreenSupport.prepareScreen(this, "스코어보드",
                "상위 기록을 표시합니다. 방금 등록한 기록은 강조됩니다.");
        backButton = ScreenSupport.button("시작 메뉴", onBack);
        resetButton = ScreenSupport.dangerButton("기록 초기화", this::requestReset);
        resetButton.setEnabled(false);

        JPanel tableFrame = ScreenSupport.framedPanel(new BorderLayout());
        tableFrame.add(recordTable(), BorderLayout.CENTER);
        feedback.setForeground(AppTheme.YELLOW);
        feedback.setBorder(new EmptyBorder(10, 0, 0, 0));
        tableFrame.add(feedback, BorderLayout.SOUTH);
        add(tableFrame, BorderLayout.CENTER);
        add(ScreenSupport.actionRow(resetButton, backButton), BorderLayout.SOUTH);
        SwingKeyBindings.backOnEscape(this, onBack);
    }

    private JScrollPane recordTable() {
        table = new JTable(records);
        table.setRowHeight(42);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setIntercellSpacing(new java.awt.Dimension(0, 1));
        table.setBorder(new EmptyBorder(0, 0, 0, 0));
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new java.awt.Dimension(0, 38));
        table.setFillsViewportHeight(true);
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setHorizontalAlignment(SwingConstants.CENTER);
        renderer.setBorder(new EmptyBorder(0, 12, 0, 12));
        table.setDefaultRenderer(Object.class, renderer);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(null);
        scroll.setColumnHeaderView(table.getTableHeader());
        return scroll;
    }

    public void showScreen(List<ScoreEntry> entries) {
        clearRecords();
        for (int index = 0; index < entries.size(); index++) {
            ScoreEntry entry = entries.get(index);
            records.addRow(new Object[] {index + 1, entry.name(), entry.score()});
        }
        SwingKeyBindings.focus(backButton);
    }

    /** 화면의 기록·선택·메시지만 비운다. 영구 저장소 초기화는 앱 제어에서 처리한다. */
    public void clearRecords() {
        table.clearSelection(); records.setRowCount(0); showMessage(null);
    }

    public void setOnResetScores(Runnable callback) {
        onReset = Objects.requireNonNull(callback); resetButton.setEnabled(true);
    }

    public void showMessage(String message) {
        feedback.setText(message == null ? " " : message); feedback.setToolTipText(message);
    }

    private void requestReset() {
        if (onReset == null) return;
        try { onReset.run(); }
        catch (java.io.UncheckedIOException | IllegalArgumentException ex) { showMessage(ex.getMessage()); }
    }
    public void showScreen(List<ScoreEntry> entries, ScoreEntry highlighted) {
        showScreen(entries);
        if (highlighted == null) return;
        int selected = -1;
        for (int i = 0; i < entries.size(); i++) {
            ScoreEntry entry = entries.get(i);
            if (entry.name().equals(highlighted.name()) && entry.score() == highlighted.score()) selected = i;
        }
        if (selected >= 0) {
            table.setRowSelectionInterval(selected, selected);
            table.scrollRectToVisible(table.getCellRect(selected, 0, true));
        }
    }

}
