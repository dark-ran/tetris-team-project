package tetris.game;

import java.util.List;
import tetris.board.Board;
import tetris.piece.PieceType;
import tetris.piece.Tetromino;

/** 조회 시점의 게임 상태 사본. 변경은 게임 엔진의 메서드를 통해서만 수행한다. */
public final class GameState {
    private final Board board;
    private final Tetromino currentPiece;
    private final int currentRow;
    private final int currentColumn;
    private final List<PieceType> nextPieces;
    private final GamePhase phase;
    private final long score;
    private final int level;
    private final int spawnedPieces;
    private final int totalClearedRows;

    GameState(Board board, Tetromino currentPiece, int currentRow, int currentColumn,
            List<PieceType> nextPieces, GamePhase phase, long score, int level,
            int spawnedPieces, int totalClearedRows) {
        this.board = board.copy();
        this.currentPiece = currentPiece;
        this.currentRow = currentRow;
        this.currentColumn = currentColumn;
        this.nextPieces = List.copyOf(nextPieces);
        this.phase = phase;
        this.score = score;
        this.level = level;
        this.spawnedPieces = spawnedPieces;
        this.totalClearedRows = totalClearedRows;
    }

    /** 고정된 블록만 보관한 독립 보드 사본을 반환한다. 사본 수정은 엔진에 영향을 주지 않는다. */
    public Board board() {
        return board.copy();
    }

    /** 시작 전과 게임오버에서는 현재 블록이 없어 null이다. */
    public Tetromino currentPiece() {
        return currentPiece;
    }

    /** 회전 기준 정사각형의 왼쪽 위가 놓이는 행. 현재 블록이 있을 때 사용한다. */
    public int currentRow() {
        return currentRow;
    }

    /** 회전 기준 정사각형의 왼쪽 위가 놓이는 열. 현재 블록이 있을 때 사용한다. */
    public int currentColumn() {
        return currentColumn;
    }

    /** 수정할 수 없는 다음 블록 목록. 첫 원소가 다음 생성 대상이다. */
    public List<PieceType> nextPieces() {
        return nextPieces;
    }

    /** 조회 시점의 게임 진행 상태. */
    public GamePhase phase() {
        return phase;
    }

    /** 앱 제어에서 계산하여 반영한 현재 점수. */
    public long score() {
        return score;
    }

    /** 앱 제어에서 계산하여 반영한 현재 레벨. */
    public int level() {
        return level;
    }

    /** 첫 블록을 포함하여 생성에 성공한 블록의 누적 개수. */
    public int spawnedPieces() {
        return spawnedPieces;
    }

    /** 현재 게임에서 삭제한 줄의 누적 개수. */
    public int totalClearedRows() {
        return totalClearedRows;
    }
}
