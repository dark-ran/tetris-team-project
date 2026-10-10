package tetris.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Objects;
import javax.swing.JPanel;
import tetris.piece.PieceType;
import tetris.piece.Tetromino;
import tetris.settings.ColorVisionMode;

/** 다음 생성 블록을 큰 칸으로 가운데 표시한다. 낙하·이동마다 컴포넌트를 다시 만들지 않는다. */
final class NextPiecePreview extends JPanel {
    static final int CELL_SIZE = 32;
    private PieceType type;
    private int[][] cells = new int[0][];
    private int firstRow, firstColumn, rows, columns;
    private ColorVisionMode mode = ColorVisionMode.NORMAL;
    private boolean patterns;

    NextPiecePreview() {
        setOpaque(false);
        setFocusable(false);
        setPreferredSize(new Dimension(144, 108));
        setMinimumSize(getPreferredSize());
        getAccessibleContext().setAccessibleName("다음 블록 없음");
    }

    void setPiece(PieceType next) {
        if (type == next) return;
        type = next;
        if (next == null) {
            cells = new int[0][];
        } else {
            cells = new Tetromino(next).cells();
            firstRow = firstColumn = Integer.MAX_VALUE;
            int lastRow = 0, lastColumn = 0;
            for (int[] cell : cells) {
                firstRow = Math.min(firstRow, cell[0]);
                firstColumn = Math.min(firstColumn, cell[1]);
                lastRow = Math.max(lastRow, cell[0]);
                lastColumn = Math.max(lastColumn, cell[1]);
            }
            rows = lastRow - firstRow + 1;
            columns = lastColumn - firstColumn + 1;
        }
        String description = next == null ? "다음 블록 없음" : "다음 블록: " + next.name();
        getAccessibleContext().setAccessibleName(description);
        setToolTipText(description);
        repaint();
    }

    void setAppearance(ColorVisionMode nextMode, boolean nextPatterns) {
        Objects.requireNonNull(nextMode);
        if (mode == nextMode && patterns == nextPatterns) return;
        mode = nextMode;
        patterns = nextPatterns;
        repaint();
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (type == null) return;
        int size = Math.max(1, Math.min(CELL_SIZE, Math.min(getWidth() / columns, getHeight() / rows)));
        Rectangle bounds = new Rectangle((getWidth() - columns * size) / 2,
                (getHeight() - rows * size) / 2, columns * size, rows * size);
        for (int[] cell : cells) {
            PieceStyle.paintCell(graphics, type, bounds.x + (cell[1] - firstColumn) * size,
                    bounds.y + (cell[0] - firstRow) * size, size, mode, patterns);
        }
    }
}
