package tetris.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Objects;
import javax.swing.JPanel;
import tetris.piece.PieceType;
import tetris.piece.ItemType;
import tetris.piece.Tetromino;
import tetris.settings.ColorVisionMode;

/** 다음 생성 블록을 큰 칸으로 가운데 표시한다. 낙하·이동마다 컴포넌트를 다시 만들지 않는다. */
final class NextPiecePreview extends JPanel {
    static final int CELL_SIZE = 32;
    private Tetromino piece;
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
        if (piece != null && piece.type() == next && piece.itemType() == ItemType.NONE) return;
        setBlock(next == null ? null : new Tetromino(next));
    }

    void setBlock(Tetromino next) {
        if (piece == next) return;
        piece = next;
        if (next == null) {
            cells = new int[0][];
        } else {
            cells = next.cells();
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
        String description = next == null ? "다음 블록 없음" : "다음 블록: " + next.type().name();
        if (next != null && next.itemType() != ItemType.NONE) {
            description += " / " + next.itemType().displayName();
        }
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
        if (piece == null) return;
        int size = Math.max(1, Math.min(CELL_SIZE, Math.min(getWidth() / columns, getHeight() / rows)));
        Rectangle bounds = new Rectangle((getWidth() - columns * size) / 2,
                (getHeight() - rows * size) / 2, columns * size, rows * size);
        for (int[] cell : cells) {
            int x = bounds.x + (cell[1] - firstColumn) * size;
            int y = bounds.y + (cell[0] - firstRow) * size;
            PieceStyle.paintCell(graphics, piece.type(), x, y, size, mode, patterns);
            if (piece.hasItemAt(cell[0], cell[1])) {
                PieceStyle.paintItem(graphics, piece.itemType(), x, y, size);
            }
        }
    }
}
