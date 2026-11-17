package tetris.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.util.Objects;
import javax.swing.JPanel;
import tetris.game.PlayerSnapshot;
import tetris.piece.PieceType;
import tetris.settings.ColorVisionMode;

/** {@link PlayerSnapshot.PieceView}로 받은 다음 블록을 가운데 표시한다. */
final class SnapshotPiecePreview extends JPanel {
    private static final int CELL = 24;
    private PlayerSnapshot.PieceView piece;
    private ColorVisionMode mode = ColorVisionMode.NORMAL;
    private boolean patterns;

    SnapshotPiecePreview() {
        setOpaque(false);
        setFocusable(false);
        setPreferredSize(new Dimension(108, 60));
        setMinimumSize(new Dimension(80, 44));
        getAccessibleContext().setAccessibleName("다음 블록 없음");
    }

    void setPiece(PlayerSnapshot.PieceView next) {
        piece = next;
        getAccessibleContext().setAccessibleName(next == null ? "다음 블록 없음"
                : "다음 블록: " + PieceType.fromCellValue(next.cellValue()).name());
        repaint();
    }

    void setAppearance(ColorVisionMode nextMode, boolean nextPatterns) {
        mode = Objects.requireNonNull(nextMode);
        patterns = nextPatterns;
        repaint();
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (piece == null) return;
        int[][] cells = piece.cells();
        boolean[] items = piece.itemCells();
        int firstRow = Integer.MAX_VALUE, firstColumn = Integer.MAX_VALUE, lastRow = 0, lastColumn = 0;
        for (int[] cell : cells) {
            firstRow = Math.min(firstRow, cell[0]);
            firstColumn = Math.min(firstColumn, cell[1]);
            lastRow = Math.max(lastRow, cell[0]);
            lastColumn = Math.max(lastColumn, cell[1]);
        }
        int rows = lastRow - firstRow + 1, columns = lastColumn - firstColumn + 1;
        int size = Math.max(1, Math.min(CELL, Math.min(getWidth() / columns, getHeight() / rows)));
        int left = (getWidth() - columns * size) / 2, top = (getHeight() - rows * size) / 2;
        PieceType type = PieceType.fromCellValue(piece.cellValue());
        for (int index = 0; index < cells.length; index++) {
            int x = left + (cells[index][1] - firstColumn) * size;
            int y = top + (cells[index][0] - firstRow) * size;
            PieceStyle.paintCell(g, type, x, y, size, mode, patterns);
            if (items[index]) PieceStyle.paintItem(g, piece.itemType(), x, y, size);
        }
    }
}
