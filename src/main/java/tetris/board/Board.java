package tetris.board;

import java.util.Arrays;
import java.util.Objects;
import tetris.piece.Tetromino;

/** 20행 10열의 고정된 블록을 보관하고 배치 검사, 블록 고정, 완성된 줄 삭제를 담당한다. */
public final class Board {
    public static final int ROWS = 20;
    public static final int COLUMNS = 10;
    public static final int EMPTY_CELL = 0;

    /** @deprecated 기존 코드와의 호환을 위한 이름이다. 새 코드에서는 {@link #COLUMNS}를 사용한다. */
    @Deprecated
    public static final int COLS = COLUMNS;

    private final int[][] fixedCells = new int[ROWS][COLUMNS];

    /** 모든 칸을 비워 새 게임을 준비한다. */
    public void reset() {
        for (int[] row : fixedCells) {
            Arrays.fill(row, EMPTY_CELL);
        }
    }

    /**
     * 회전 기준 정사각형의 왼쪽 위를 지정한 행·열에 놓을 수 있는지 확인한다.
     * 블록이 차지하는 네 칸만 검사하며, 한 칸이라도 보드 밖이거나 이미 채워져 있으면 false이다.
     */
    public boolean canPlace(Tetromino piece, int row, int column) {
        Objects.requireNonNull(piece, "배치할 블록이 필요합니다.");
        for (int[] cell : piece.cells()) {
            long boardRow = (long) row + cell[0];
            long boardColumn = (long) column + cell[1];
            if (boardRow < 0 || boardRow >= ROWS || boardColumn < 0 || boardColumn >= COLUMNS) {
                return false;
            }
            if (fixedCells[(int) boardRow][(int) boardColumn] != EMPTY_CELL) {
                return false;
            }
        }
        return true;
    }

    /** 배치 가능한 블록의 칸 저장값을 기록한다. 잘못된 위치이면 보드를 변경하지 않고 예외를 발생시킨다. */
    public void lock(Tetromino piece, int row, int column) {
        if (!canPlace(piece, row, column)) {
            throw new IllegalArgumentException("블록을 고정할 위치가 보드 밖이거나 이미 채워져 있습니다.");
        }
        for (int[] cell : piece.cells()) {
            fixedCells[row + cell[0]][column + cell[1]] = piece.type().cellValue();
        }
    }

    /** 완성된 줄을 모두 삭제하고 위쪽 줄을 내린 뒤, 삭제한 줄 수를 반환한다. */
    public int clearFullRows() {
        int destinationRow = ROWS - 1;
        int clearedRows = 0;
        for (int sourceRow = ROWS - 1; sourceRow >= 0; sourceRow--) {
            if (isFullRow(sourceRow)) {
                clearedRows++;
                continue;
            }
            if (sourceRow != destinationRow) {
                System.arraycopy(fixedCells[sourceRow], 0, fixedCells[destinationRow], 0, COLUMNS);
            }
            destinationRow--;
        }
        for (int row = destinationRow; row >= 0; row--) {
            Arrays.fill(fixedCells[row], EMPTY_CELL);
        }
        return clearedRows;
    }

    /** 빈 칸은 0, 고정된 블록은 1~7인 보드 사본을 반환한다. 현재 이동 중인 블록은 포함하지 않는다. */
    public int[][] snapshot() {
        int[][] copiedCells = new int[ROWS][];
        for (int row = 0; row < ROWS; row++) {
            copiedCells[row] = fixedCells[row].clone();
        }
        return copiedCells;
    }

    /** 고정된 블록을 보관한 독립 보드 사본을 반환한다. */
    public Board copy() {
        Board copiedBoard = new Board();
        for (int row = 0; row < ROWS; row++) {
            System.arraycopy(fixedCells[row], 0, copiedBoard.fixedCells[row], 0, COLUMNS);
        }
        return copiedBoard;
    }

    private boolean isFullRow(int row) {
        for (int cellValue : fixedCells[row]) {
            if (cellValue == EMPTY_CELL) {
                return false;
            }
        }
        return true;
    }
}
