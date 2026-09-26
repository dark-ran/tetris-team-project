package tetris.ui;

import java.awt.BorderLayout;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import tetris.scoreboard.ScoreEntry;

/** 순위 표시용 임시 화면. 전달받은 기록만 표시한다. */
public class ScoreboardScreen extends JPanel {
    private final DefaultTableModel records = new DefaultTableModel(new String[] {"순위", "이름", "점수"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JButton backButton;

    public ScoreboardScreen(Runnable onBack) {
        ScreenSupport.prepareScreen(this, "스코어보드",
                "기록 저장소 연결 예정입니다. 현재는 저장된 기록을 읽지 않으며 예시 점수도 표시하지 않습니다.");
        backButton = ScreenSupport.button("시작 메뉴", onBack);

        JPanel tableFrame = ScreenSupport.framedPanel(new BorderLayout());
        tableFrame.add(recordTable(), BorderLayout.CENTER);
        add(tableFrame, BorderLayout.CENTER);
        add(ScreenSupport.actionRow(backButton), BorderLayout.SOUTH);
        SwingKeyBindings.backOnEscape(this, onBack);
    }

    private JScrollPane recordTable() {
        JTable table = new JTable(records);
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
        records.setRowCount(0);
        for (int index = 0; index < entries.size(); index++) {
            ScoreEntry entry = entries.get(index);
            records.addRow(new Object[] {index + 1, entry.name(), entry.score()});
        }
        SwingKeyBindings.focus(backButton);
    }
}
