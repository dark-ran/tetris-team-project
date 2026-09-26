package tetris.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JComponent;

/** 화면 제목 옆에 표시하는 작은 T 테트로미노 표식. */
final class TetrominoMark extends JComponent {
    private static final int CELL = 15;
    private static final int[][] CELLS = {{0, 1}, {1, 0}, {1, 1}, {1, 2}};

    TetrominoMark() {
        setPreferredSize(new Dimension(CELL * 3, CELL * 2));
        setMinimumSize(getPreferredSize());
        setFocusable(false);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        for (int[] cell : CELLS) {
            int x = cell[1] * CELL;
            int y = cell[0] * CELL;
            graphics.setColor(AppTheme.PURPLE);
            graphics.fillRect(x + 1, y + 1, CELL - 2, CELL - 2);
            graphics.setColor(AppTheme.TEXT);
            graphics.drawLine(x + 2, y + 2, x + CELL - 4, y + 2);
        }
    }
}
