package tetris.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.util.List;
import javax.swing.JPanel;
import tetris.board.Board;
import tetris.game.AttackLine;
import tetris.game.AttackQueue;

/**
 * 상대에게 받아 다음 블록 생성 전에 보드 아래에 추가될 줄을 넘어간 모양 그대로 보여준다.
 * 최대 10줄 칸을 아래부터 채운다.
 */
final class AttackPreview extends JPanel {
    private static final int CELL = 10;
    private List<AttackLine> lines = List.of();

    AttackPreview() {
        setOpaque(false);
        setFocusable(false);
        Dimension size = new Dimension(Board.COLUMNS * CELL + 4, AttackQueue.MAX_LINES * CELL + 4);
        setPreferredSize(size);
        setMinimumSize(size);
        getAccessibleContext().setAccessibleName("넘어올 줄 없음");
    }

    void setLines(List<AttackLine> next) {
        lines = List.copyOf(next);
        getAccessibleContext().setAccessibleName("넘어올 줄 " + lines.size() + "줄");
        repaint();
    }

    int lineCount() {
        return lines.size();
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int cell = Math.max(4, Math.min(CELL, Math.min((getWidth() - 4) / Board.COLUMNS,
                (getHeight() - 4) / AttackQueue.MAX_LINES)));
        int width = Board.COLUMNS * cell, height = AttackQueue.MAX_LINES * cell;
        int left = (getWidth() - width) / 2, top = (getHeight() - height) / 2;
        g.setColor(AppTheme.BORDER);
        g.drawRect(left - 2, top - 2, width + 3, height + 3);
        g.setColor(AppTheme.BOARD);
        g.fillRect(left, top, width, height);
        int firstRow = AttackQueue.MAX_LINES - lines.size();
        for (int index = 0; index < lines.size(); index++) {
            AttackLine line = lines.get(index);
            int y = top + (firstRow + index) * cell;
            for (int column = 0; column < Board.COLUMNS; column++) {
                if (line.isFilled(column)) {
                    BattleBoardView.paintGarbageCell(g, left + column * cell + 1, y + 1, cell - 2);
                }
            }
        }
    }
}
