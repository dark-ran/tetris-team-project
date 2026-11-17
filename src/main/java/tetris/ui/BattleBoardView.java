package tetris.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.Objects;
import javax.swing.JPanel;
import tetris.board.Board;
import tetris.game.PlayerSnapshot;
import tetris.piece.PieceType;
import tetris.settings.ColorVisionMode;

/** 대전 화면에서 한 플레이어의 보드를 {@link PlayerSnapshot}으로 그린다. 공격 줄 칸은 회색이다. */
final class BattleBoardView extends JPanel {
    static final Color GARBAGE = new Color(0x8C8C8C);
    private static final Color GARBAGE_EDGE = new Color(0x5E5E5E);
    private static final int MAX_CELL_SIZE = 26;
    private static final int MIN_CELL_SIZE = 12;
    private static final int PADDING = 6;
    private static final int BORDER_SIZE = 3;

    private PlayerSnapshot snapshot;
    private ColorVisionMode mode = ColorVisionMode.NORMAL;
    private boolean patterns;

    BattleBoardView() {
        setOpaque(false);
        setFocusable(true);
        setPreferredSize(new Dimension(Board.COLUMNS * MAX_CELL_SIZE + PADDING * 2,
                Board.ROWS * MAX_CELL_SIZE + PADDING * 2));
        setMinimumSize(new Dimension(Board.COLUMNS * MIN_CELL_SIZE + PADDING * 2,
                Board.ROWS * MIN_CELL_SIZE + PADDING * 2));
        getAccessibleContext().setAccessibleName("대전 보드");
    }

    void render(PlayerSnapshot next) {
        snapshot = next;
        repaint();
    }

    PlayerSnapshot snapshot() {
        return snapshot;
    }

    void setAppearance(ColorVisionMode nextMode, boolean nextPatterns) {
        mode = Objects.requireNonNull(nextMode);
        patterns = nextPatterns;
        repaint();
    }

    /** 현재 크기에서 그려지는 보드 바깥 테두리. */
    Rectangle boardBounds() {
        Geometry board = geometry();
        return new Rectangle(board.x - BORDER_SIZE, board.y - BORDER_SIZE,
                board.width() + BORDER_SIZE * 2, board.height() + BORDER_SIZE * 2);
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Geometry board = geometry();
            g.setColor(AppTheme.TEXT);
            g.fillRect(board.x - BORDER_SIZE, board.y - BORDER_SIZE,
                    board.width() + BORDER_SIZE * 2, board.height() + BORDER_SIZE * 2);
            g.setColor(AppTheme.BOARD);
            g.fillRect(board.x - 1, board.y - 1, board.width() + 2, board.height() + 2);
            g.setColor(AppTheme.GRID);
            for (int row = 0; row < Board.ROWS; row++) {
                for (int column = 0; column < Board.COLUMNS; column++) {
                    g.fillRect(board.cellX(column) + 2, board.cellY(row) + 2, board.cell - 4, board.cell - 4);
                }
            }
            if (snapshot != null) {
                paintFixedCells(g, board);
                paintCurrentPiece(g, board);
                if (snapshot.isGameOver()) paintGameOver(g, board);
            }
        } finally {
            g.dispose();
        }
    }

    private void paintFixedCells(Graphics2D g, Geometry board) {
        int size = board.cell - 4;
        for (int row = 0; row < Board.ROWS; row++) {
            for (int column = 0; column < Board.COLUMNS; column++) {
                int value = snapshot.cellAt(row, column);
                if (value == Board.EMPTY_CELL) continue;
                int x = board.cellX(column) + 2, y = board.cellY(row) + 2;
                if (value == Board.GARBAGE_CELL) {
                    paintGarbageCell(g, x, y, size);
                } else {
                    PieceStyle.paintCell(g, PieceType.fromCellValue(value), x, y, size, mode, patterns);
                    PieceStyle.paintItem(g, snapshot.itemAt(row, column), x, y, size);
                }
            }
        }
    }

    private void paintCurrentPiece(Graphics2D g, Geometry board) {
        PlayerSnapshot.PieceView piece = snapshot.currentPiece();
        if (piece == null) return;
        int size = board.cell - 4;
        int[][] cells = piece.cells();
        boolean[] items = piece.itemCells();
        if (snapshot.landingRow() != piece.row()) {
            for (int[] cell : cells) {
                int row = snapshot.landingRow() + cell[0], column = piece.column() + cell[1];
                if (onBoard(row, column)) {
                    PieceStyle.paintGhostCell(g, board.cellX(column) + 2, board.cellY(row) + 2, size);
                }
            }
        }
        PieceType type = PieceType.fromCellValue(piece.cellValue());
        for (int index = 0; index < cells.length; index++) {
            int row = piece.row() + cells[index][0], column = piece.column() + cells[index][1];
            if (!onBoard(row, column)) continue;
            int x = board.cellX(column) + 2, y = board.cellY(row) + 2;
            PieceStyle.paintCell(g, type, x, y, size, mode, patterns);
            if (items[index]) PieceStyle.paintItem(g, piece.itemType(), x, y, size);
        }
    }

    private static void paintGameOver(Graphics2D g, Geometry board) {
        g.setColor(new Color(0, 0, 0, 165));
        g.fillRect(board.x, board.y, board.width(), board.height());
        g.setColor(AppTheme.RED);
        g.setFont(AppTheme.font(Font.BOLD, Math.max(12f, board.cell * 0.8f)));
        FontMetrics metrics = g.getFontMetrics();
        String text = "GAME OVER";
        g.drawString(text, board.x + (board.width() - metrics.stringWidth(text)) / 2,
                board.y + board.height() / 2);
    }

    /** 상대에게 받은 공격 줄 칸. 블록 색과 섞이지 않도록 회색과 어두운 테두리로 그린다. */
    static void paintGarbageCell(Graphics g, int x, int y, int size) {
        g.setColor(GARBAGE);
        g.fillRect(x, y, size, size);
        g.setColor(GARBAGE_EDGE);
        g.drawRect(x, y, size - 1, size - 1);
    }

    private static boolean onBoard(int row, int column) {
        return row >= 0 && row < Board.ROWS && column >= 0 && column < Board.COLUMNS;
    }

    private Geometry geometry() {
        int width = Math.max(Board.COLUMNS, getWidth() - PADDING * 2);
        int height = Math.max(Board.ROWS, getHeight() - PADDING * 2);
        int cell = Math.max(1, Math.min(MAX_CELL_SIZE, Math.min(width / Board.COLUMNS, height / Board.ROWS)));
        return new Geometry(cell, (getWidth() - Board.COLUMNS * cell) / 2, (getHeight() - Board.ROWS * cell) / 2);
    }

    private record Geometry(int cell, int x, int y) {
        int width() { return Board.COLUMNS * cell; }
        int height() { return Board.ROWS * cell; }
        int cellX(int column) { return x + column * cell; }
        int cellY(int row) { return y + row * cell; }
    }
}
