package tetris.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import javax.swing.JPanel;
import tetris.board.Board;

/** 실제 게임 상태 연결 전 보드 크기와 테두리만 표시하는 화면 구성 요소. */
final class BoardPreview extends JPanel {
    private static final int MAX_CELL_SIZE = 26;
    private static final int MIN_CELL_SIZE = 16;
    private static final int PADDING = 10;
    private static final int BORDER_SIZE = 3;

    BoardPreview() {
        setOpaque(false);
        setFocusable(true);
        setPreferredSize(new Dimension(
                Board.COLUMNS * MAX_CELL_SIZE + PADDING * 2,
                Board.ROWS * MAX_CELL_SIZE + PADDING * 2));
        setMinimumSize(new Dimension(
                Board.COLUMNS * MIN_CELL_SIZE + PADDING * 2,
                Board.ROWS * MIN_CELL_SIZE + PADDING * 2));
        getAccessibleContext().setAccessibleName("20행 10열 보드");
    }

    /** 현재 컴포넌트 크기에서 실제로 그려지는 바깥 테두리 영역. */
    Rectangle boardBounds() {
        return geometry().outerBounds();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            BoardGeometry board = geometry();
            paintBoardFrame(g, board);
            paintEmptyCells(g, board);
        } finally {
            g.dispose();
        }
    }

    private static void paintBoardFrame(Graphics2D g, BoardGeometry board) {
        g.setColor(AppTheme.TEXT);
        g.fillRect(board.x() - BORDER_SIZE, board.y() - BORDER_SIZE,
                board.width() + BORDER_SIZE * 2, board.height() + BORDER_SIZE * 2);
        g.setColor(AppTheme.BOARD);
        g.fillRect(board.x() - 1, board.y() - 1, board.width() + 2, board.height() + 2);
    }

    private static void paintEmptyCells(Graphics2D g, BoardGeometry board) {
        int cellSize = board.cellSize();
        g.setColor(AppTheme.GRID);
        for (int row = 0; row < Board.ROWS; row++) {
            for (int column = 0; column < Board.COLUMNS; column++) {
                int x = board.x() + column * cellSize;
                int y = board.y() + row * cellSize;
                g.fillRect(x + 2, y + 2, cellSize - 4, cellSize - 4);
            }
        }
    }

    private BoardGeometry geometry() {
        int availableWidth = Math.max(Board.COLUMNS, getWidth() - PADDING * 2);
        int availableHeight = Math.max(Board.ROWS, getHeight() - PADDING * 2);
        int cellSize = Math.max(1, Math.min(MAX_CELL_SIZE,
                Math.min(availableWidth / Board.COLUMNS, availableHeight / Board.ROWS)));
        int boardWidth = Board.COLUMNS * cellSize;
        int boardHeight = Board.ROWS * cellSize;
        return new BoardGeometry(
                cellSize,
                (getWidth() - boardWidth) / 2,
                (getHeight() - boardHeight) / 2,
                boardWidth,
                boardHeight);
    }

    private record BoardGeometry(int cellSize, int x, int y, int width, int height) {
        Rectangle outerBounds() {
            return new Rectangle(
                    x - BORDER_SIZE,
                    y - BORDER_SIZE,
                    width + BORDER_SIZE * 2,
                    height + BORDER_SIZE * 2);
        }
    }
}
