
package tetris.game;

import java.util.List;
import tetris.board.Board;
import tetris.piece.PieceType;
import tetris.piece.Tetromino;

/**
 * 조회 시점의 게임 상태 사본.
 * 변경은 GameEngine을 통해서만 수행한다.
 */
public final class GameState {
    private final Board board;
    private final Tetromino currentPiece;
    private final int currentRow;
    private final int currentColumn;
    private final List<Tetromino> upcomingPieces;
    private final GamePhase phase;
    private final long score;
    private final int level;
    private final int spawnedPieces;
    private final int totalClearedRows;
    private final Difficulty difficulty;

    GameState(
            Board board,
            Tetromino currentPiece,
            int currentRow,
            int currentColumn,
            List<Tetromino> nextPieces,
            GamePhase phase,
            long score,
            int level,
            int spawnedPieces,
            int totalClearedRows,
            Difficulty difficulty) {

        this.board = board.copy();
        this.currentPiece = currentPiece;
        this.currentRow = currentRow;
        this.currentColumn = currentColumn;
        this.upcomingPieces = List.copyOf(nextPieces);
        this.phase = phase;
        this.score = score;
        this.level = level;
        this.spawnedPieces = spawnedPieces;
        this.totalClearedRows = totalClearedRows;
        this.difficulty = java.util.Objects.requireNonNull(difficulty);
    }

    /**
     * 고정된 블록만 보관한 보드의 사본을 반환한다.
     */
    public Board board() {
        return board.copy();
    }

    /**
     * 현재 떨어지고 있는 블록.
     */
    public Tetromino currentPiece() {
        return currentPiece;
    }

    public int currentRow() {
        return currentRow;
    }

    public int currentColumn() {
        return currentColumn;
    }

    /**
     * 기존 UI 및 테스트와 호환되는 블록 종류 목록.
     */
    public List<PieceType> nextPieces() {
        return upcomingPieces.stream()
                .map(Tetromino::type)
                .toList();
    }

    /**
     * 블록의 실제 점유 모양까지 포함하는 미리보기 데이터.
     */
    public List<Tetromino> upcomingPieces() {
        return upcomingPieces;
    }

    public GamePhase phase() {
        return phase;
    }

    public long score() {
        return score;
    }

    public int level() {
        return level;
    }

    public int spawnedPieces() {
        return spawnedPieces;
    }

    public int totalClearedRows() {
        return totalClearedRows;
    }

    public Difficulty difficulty() {
        return difficulty;
    }
}
