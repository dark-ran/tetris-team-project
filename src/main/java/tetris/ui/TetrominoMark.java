package tetris.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JComponent;
import tetris.piece.PieceType;
import tetris.piece.Tetromino;

/** 제목과 색상표에 사용하는 블록 표식. 보드와 같은 일반 모드 표현을 사용한다. */
final class TetrominoMark extends JComponent {
    private static final int CELL_SIZE = 15;
    private final Tetromino piece;
    private final int firstRow;

    TetrominoMark() {
        this(PieceType.T);
    }

    TetrominoMark(PieceType type) {
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
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        for (int[] cell : piece.cells()) {
            int x = cell[1] * CELL_SIZE;
            int y = (cell[0] - firstRow) * CELL_SIZE;
            NormalPieceStyle.paintCell(graphics, piece.type(), x, y, CELL_SIZE);
        }
    }
}
