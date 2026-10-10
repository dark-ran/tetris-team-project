package tetris.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JComponent;
import tetris.piece.PieceType;
import tetris.piece.Tetromino;
import tetris.settings.ColorVisionMode;

/** 제목과 미리보기에 사용하는 블록 표식. 보드와 같은 팔레트·패턴을 사용한다. */
final class TetrominoMark extends JComponent {
    private static final int CELL_SIZE = 15;
    private final Tetromino piece;
    private final int firstRow;
    private final ColorVisionMode mode;
    private final boolean patterns;

    TetrominoMark() {
        this(PieceType.T);
    }

    TetrominoMark(PieceType type) { this(type, false); }

    TetrominoMark(PieceType type, boolean accessible) {
        this(type, accessible ? ColorVisionMode.DEUTAN : ColorVisionMode.NORMAL, accessible);
    }

    TetrominoMark(PieceType type, ColorVisionMode mode, boolean patterns) {
        this.mode = java.util.Objects.requireNonNull(mode);
        this.patterns = patterns;
        piece = new Tetromino(type);
        int minimumRow = Integer.MAX_VALUE;
        int maximumRow = 0;
        int maximumColumn = 0;
        for (int[] cell : piece.cells()) {
            minimumRow = Math.min(minimumRow, cell[0]);
            maximumRow = Math.max(maximumRow, cell[0]);
            maximumColumn = Math.max(maximumColumn, cell[1]);
        }
        firstRow = minimumRow;
        setPreferredSize(new Dimension(CELL_SIZE * (maximumColumn + 1),
                CELL_SIZE * (maximumRow - firstRow + 1)));
        setMinimumSize(getPreferredSize());
        setFocusable(false);
        setToolTipText(type.name() + " 블록 미리보기");
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        for (int[] cell : piece.cells()) {
            int x = cell[1] * CELL_SIZE;
            int y = (cell[0] - firstRow) * CELL_SIZE;
            PieceStyle.paintCell(graphics, piece.type(), x, y, CELL_SIZE, mode, patterns);
        }
    }
}
