package tetris.game;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import tetris.board.Board;
import tetris.piece.ItemType;
import tetris.piece.Tetromino;

/**
 * 대전 화면에서 한 플레이어를 그리는 데 필요한 정보만 담은 사본.
 * 로컬 대전 화면이 그대로 그리고, LAN 대전에서는 필요한 필드를 전용 메시지로 전송한다.
 *
 * <p>칸 좌표는 모두 {행, 열}이다. 보드 칸 값은 {@link Board#EMPTY_CELL}, 블록 종류 값(1~8),
 * 공격 줄 {@link Board#GARBAGE_CELL} 중 하나이다.</p>
 */
public final class PlayerSnapshot implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    private final int[][] boardCells;
    private final ItemType[][] boardItems;
    private final PieceView currentPiece;
    private final int landingRow;
    private final PieceView nextPiece;
    private final List<AttackLine> pendingAttackLines;
    private final GamePhase phase;
    private final BattleMode mode;
    private final long score;
    private final int level;
    private final int totalClearedRows;

    PlayerSnapshot(int[][] boardCells, ItemType[][] boardItems, PieceView currentPiece,
            int landingRow, PieceView nextPiece, List<AttackLine> pendingAttackLines, GamePhase phase,
            BattleMode mode, long score, int level, int totalClearedRows) {
        this.boardCells = boardCells;
        this.boardItems = boardItems;
        this.currentPiece = currentPiece;
        this.landingRow = landingRow;
        this.nextPiece = nextPiece;
        this.pendingAttackLines = List.copyOf(pendingAttackLines);
        this.phase = Objects.requireNonNull(phase);
        this.mode = Objects.requireNonNull(mode);
        this.score = score;
        this.level = level;
        this.totalClearedRows = totalClearedRows;
    }

    /** 엔진의 상태 사본으로 만든다. */
    public static PlayerSnapshot of(GameState state, BattleMode mode) {
        Objects.requireNonNull(state, "게임 상태가 필요합니다.");
        Objects.requireNonNull(mode, "대전 모드가 필요합니다.");
        Board board = state.board();
        ItemType[][] items = new ItemType[Board.ROWS][Board.COLUMNS];
        for (int row = 0; row < Board.ROWS; row++) {
            for (int column = 0; column < Board.COLUMNS; column++) {
                items[row][column] = board.itemAt(row, column);
            }
        }
        Tetromino current = state.currentPiece();
        PieceView currentView = current == null ? null
                : PieceView.of(current, state.currentRow(), state.currentColumn());
        List<Tetromino> upcoming = state.upcomingPieces();
        PieceView nextView = upcoming.isEmpty() ? null : PieceView.of(upcoming.getFirst(), 0, 0);
        return new PlayerSnapshot(board.snapshot(), items, currentView, state.landingRow(), nextView,
                state.pendingAttackLines(), state.phase(), mode, state.score(), state.level(),
                state.totalClearedRows());
    }

    /** 고정된 칸 값의 사본. [행][열]. */
    public int[][] boardCells() {
        return copy(boardCells);
    }

    public int cellAt(int row, int column) {
        return boardCells[Objects.checkIndex(row, Board.ROWS)][Objects.checkIndex(column, Board.COLUMNS)];
    }

    public ItemType itemAt(int row, int column) {
        return boardItems[Objects.checkIndex(row, Board.ROWS)][Objects.checkIndex(column, Board.COLUMNS)];
    }

    /** 조작 중인 블록. 보드 기준 위치가 반영되어 있고, 블록이 없으면 null이다. */
    public PieceView currentPiece() { return currentPiece; }

    /** 조작 중인 블록이 떨어질 행(회전 기준 정사각형의 왼쪽 위). */
    public int landingRow() { return landingRow; }

    /** 다음 블록. 위치는 {0, 0} 기준이며 없으면 null이다. */
    public PieceView nextPiece() { return nextPiece; }

    /** 다음 블록 생성 전에 보드 아래에 추가될 공격 줄. 위에서 아래 순서이다. */
    public List<AttackLine> pendingAttackLines() { return pendingAttackLines; }

    public GamePhase phase() { return phase; }
    public BattleMode mode() { return mode; }
    public long score() { return score; }
    public int level() { return level; }
    public int totalClearedRows() { return totalClearedRows; }

    /** 더 이상 블록을 쌓을 수 없는 상태인지. */
    public boolean isGameOver() {
        return phase == GamePhase.GAME_OVER;
    }

    private static int[][] copy(int[][] source) {
        int[][] copied = new int[source.length][];
        for (int index = 0; index < source.length; index++) copied[index] = source[index].clone();
        return copied;
    }

    /**
     * 블록 하나의 표시 정보.
     *
     * @param cellValue 블록 종류 값 (PieceType의 칸 저장값)
     * @param itemType 블록에 붙은 아이템. 없으면 NONE
     */
    public record PieceView(int row, int column, int cellValue, ItemType itemType,
            int[][] cells, boolean[] itemCells) implements Serializable {
        @Serial private static final long serialVersionUID = 1L;

        public PieceView {
            Objects.requireNonNull(itemType);
            cells = copy(cells);
            itemCells = itemCells.clone();
            if (cells.length != itemCells.length) {
                throw new IllegalArgumentException("칸 수와 아이템 표시 수가 같아야 합니다.");
            }
        }

        static PieceView of(Tetromino piece, int row, int column) {
            int[][] cells = piece.cells();
            boolean[] itemCells = new boolean[cells.length];
            for (int index = 0; index < cells.length; index++) {
                itemCells[index] = piece.hasItemAt(cells[index][0], cells[index][1]);
            }
            return new PieceView(row, column, piece.type().cellValue(), piece.itemType(), cells, itemCells);
        }

        /** 회전 기준 정사각형 안의 칸 좌표 사본. 보드 위치는 row·column을 더한다. */
        @Override public int[][] cells() {
            return copy(cells);
        }

        /** cells와 같은 순서로, 아이템 표시가 붙은 칸이면 true. */
        @Override public boolean[] itemCells() {
            return itemCells.clone();
        }
    }
}
