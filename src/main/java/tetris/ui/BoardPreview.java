package tetris.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.HierarchyEvent;
import javax.swing.JPanel;
import javax.swing.Timer;
import tetris.board.Board;
import tetris.game.GameState;
import tetris.piece.Tetromino;
import tetris.piece.PieceType;
import tetris.rule.PieceRules;
import tetris.settings.ColorVisionMode;

/** Renders fixed and falling cells from an immutable engine snapshot. */
final class BoardPreview extends JPanel {
    private GameState state;
    private boolean flashing;
    private Board flashBoard;
    private boolean[] flashingRows;
    private final Timer flashTimer = new Timer(350, event -> clearFlash());
    private ColorVisionMode mode = ColorVisionMode.NORMAL;
    private boolean patterns;

    void setAppearance(ColorVisionMode mode, boolean patterns) {
        this.mode = java.util.Objects.requireNonNull(mode);
        this.patterns = patterns;
        repaint();
    }

    private static final int MAX_CELL_SIZE = 26;
    private static final int MIN_CELL_SIZE = 16;
    private static final int PADDING = 10;
    private static final int BORDER_SIZE = 3;

    BoardPreview() {
        flashTimer.setRepeats(false);
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && !isShowing()) clearFlash();
        });
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

    void render(GameState newState) {
        java.util.Objects.requireNonNull(newState);
        boolean newGame = state != null && (newState.totalClearedRows() < state.totalClearedRows()
                || newState.spawnedPieces() < state.spawnedPieces()
                || newState.mode() != state.mode() || newState.difficulty() != state.difficulty());
        if (newGame) clearFlash();
        // 줄 삭제가 발생한 경우 삭제 직전의 보드를 만들어 반짝임을 표시한다.
        if (!newGame && state != null
                && newState.totalClearedRows() > state.totalClearedRows()) {
            clearFlash();
            prepareLineClearFlash(state);
            flashing = flashBoard != null;
            if (flashing) flashTimer.restart();
        }

        this.state = newState;
        repaint();
    }

    private void clearFlash() {
        flashTimer.stop();
        flashing = false;
        flashBoard = null;
        flashingRows = null;
        repaint();
    }

    @Override public void removeNotify() {
        clearFlash();
        super.removeNotify();
    }

    private void prepareLineClearFlash(GameState oldState) {
        Tetromino piece = oldState.currentPiece();

        if (piece == null) {
            return;
        }

        Board beforeClearBoard = oldState.board().copy();

        int landingRow = oldState.landingRow();
        int column = oldState.currentColumn();

        PieceRules rules = PieceRules.forPiece(piece);

        // 실제 게임 엔진과 같은 방식으로 삭제 직전 상태를 만든다.
        if (rules.leavesPieceOnBoard()) {
            beforeClearBoard.lock(piece, landingRow, column);
        }

        rules.lockEffect().onLock(
                beforeClearBoard,
                piece,
                landingRow,
                column);

        java.util.Set<Integer> rowsToClear = rules.lockEffect().rowsToClear(
                beforeClearBoard,
                piece,
                landingRow,
                column);

        flashBoard = beforeClearBoard;
        flashingRows = new boolean[Board.ROWS];

        for (int row : rowsToClear) {
            if (row >= 0 && row < Board.ROWS) {
                flashingRows[row] = true;
            }
        }
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

            // 삭제되는 줄만 잠깐 밝게 표시한다.
            if (flashing && flashBoard != null && flashingRows != null) {
                paintFlashBoard(g, board);
            } else if (state != null) {
                paintGameState(g, board, state, mode, patterns);
            }
        } finally {
            g.dispose();
        }
    }

    private void paintFlashBoard(Graphics2D g, BoardGeometry board) {
        int cellSize = board.cellSize();
        int[][] cells = flashBoard.snapshot();

        for (int row = 0; row < Board.ROWS; row++) {
            for (int column = 0; column < Board.COLUMNS; column++) {
                int value = cells[row][column];

                if (value == Board.EMPTY_CELL) {
                    continue;
                }

                PieceType type = PieceType.fromCellValue(value);

                int x = board.x() + column * cellSize;
                int y = board.y() + row * cellSize;
                if (flashingRows[row]) {
                    java.awt.Color base = PieceStyle.colorFor(type, mode);

                    java.awt.Color highlight = new java.awt.Color(
                            Math.min(255, base.getRed() + 70),
                            Math.min(255, base.getGreen() + 70),
                            Math.min(255, base.getBlue() + 70));

                    g.setColor(highlight);
                    g.fillRect(
                            x + 2,
                            y + 2,
                            cellSize - 4,
                            cellSize - 4);
                } else {
                    PieceStyle.paintCell(
                            g,
                            type,
                            x + 2,
                            y + 2,
                            cellSize - 4,
                            mode,
                            patterns);
                }
                PieceStyle.paintItem(g, flashBoard.itemAt(row, column),
                        x + 2, y + 2, cellSize - 4);
            }
        }
    }

    // GameState의 보드 정보를 확인하고, 빈 칸을 제외한 블록을 화면에 그린다.
    private static void paintGameState(Graphics2D g, BoardGeometry board, GameState state,
            ColorVisionMode mode, boolean patterns) {
        // 고정된 블록을 보드에 그린다.
        Board fixedBoard = state.board();
        int[][] cells = fixedBoard.snapshot();
        int cellSize = board.cellSize();

        for (int row = 0; row < Board.ROWS; row++) {
            for (int column = 0; column < Board.COLUMNS; column++) {

                int value = cells[row][column];

                if (value == Board.EMPTY_CELL) {
                    continue;
                }

                int x = board.x() + column * cellSize;
                int y = board.y() + row * cellSize;

                // 저장된 값에 맞는 블록 종류로 표시한다.
                PieceType type = PieceType.fromCellValue(value);

                PieceStyle.paintCell(
                        g,
                        type,
                        x + 2,
                        y + 2,
                        cellSize - 4, mode, patterns);
                PieceStyle.paintItem(g, fixedBoard.itemAt(row, column),
                        x + 2, y + 2, cellSize - 4);
            }
        }

        // 현재 떨어지고 있는 블록을 GameState에서 가져와 보드에 표시한다.
        Tetromino currentPiece = state.currentPiece();

        if (currentPiece != null) {
            int ghostRow = state.landingRow();

            if (ghostRow != state.currentRow()) {
                for (int[] cell : currentPiece.cells()) {
                    int row = ghostRow + cell[0], column = state.currentColumn() + cell[1];
                    if (row >= 0 && row < Board.ROWS && column >= 0 && column < Board.COLUMNS)
                        PieceStyle.paintGhostCell(g, board.x() + column * cellSize + 2,
                                board.y() + row * cellSize + 2, cellSize - 4);
                }
            }
            for (int[] cell : currentPiece.cells()) {

                int row = state.currentRow() + cell[0];
                int column = state.currentColumn() + cell[1];
                if (row < 0 || row >= Board.ROWS || column < 0 || column >= Board.COLUMNS)
                    continue;

                int x = board.x() + column * cellSize;
                int y = board.y() + row * cellSize;

                PieceStyle.paintCell(
                        g,
                        currentPiece.type(),
                        x + 2,
                        y + 2,
                        cellSize - 4, mode, patterns);
                if (currentPiece.hasItemAt(cell[0], cell[1])) {
                    PieceStyle.paintItem(g, currentPiece.itemType(), x + 2, y + 2, cellSize - 4);
                }
            }
            PieceRules.forPiece(currentPiece).lockEffect()
                    .targetCell(fixedBoard, currentPiece, ghostRow, state.currentColumn())
                    .ifPresent(target -> PieceStyle.paintEffectTarget(g,
                            board.x() + target.column() * cellSize + 2,
                            board.y() + target.row() * cellSize + 2, cellSize - 4));
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
