package tetris.game;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;
import tetris.board.Board;
import tetris.piece.PieceQueue;
import tetris.piece.Tetromino;

/** 화면과 타이머에 의존하지 않고 행동과 진행 상태를 처리한다. 호출은 한 실행 흐름에서 한다. */
public final class GameEngine {
    private final Board board = new Board();
    private final Supplier<PieceQueue> pieceQueueFactory;
    private final PieceSpawnPolicy spawnPolicy;
    private PieceQueue pieceQueue;
    private Tetromino currentPiece;
    private int currentRow;
    private int currentColumn;
    private GamePhase phase = GamePhase.READY;
    private long score;
    private int level = 1;
    private int spawnedPieces;
    private int totalClearedRows;

    public GameEngine() {
        this(PieceQueue::new, new CenteredPieceSpawnPolicy());
    }

    /** 재현 가능한 생성 순서가 필요할 때 무작위 선택기를 전달한다. */
    public GameEngine(RandomGenerator randomGenerator) {
        this(queueFactory(randomGenerator), new CenteredPieceSpawnPolicy());
    }

    /** 새 게임의 블록 공급 방식과 시작 위치 규칙을 객체로 전달한다. */
    public GameEngine(Supplier<PieceQueue> pieceQueueFactory, PieceSpawnPolicy spawnPolicy) {
        this.pieceQueueFactory = Objects.requireNonNull(pieceQueueFactory, "블록 목록 생성기가 필요합니다.");
        this.spawnPolicy = Objects.requireNonNull(spawnPolicy, "블록 시작 위치 규칙이 필요합니다.");
    }

    private static Supplier<PieceQueue> queueFactory(RandomGenerator randomGenerator) {
        Objects.requireNonNull(randomGenerator, "무작위 선택기가 필요합니다.");
        return () -> new PieceQueue(randomGenerator);
    }

    /** 보드·점수·누적 수치를 초기화하고 첫 블록을 생성한다. 생성 실패 시 게임오버이다. */
    public void newGame() {
        PieceQueue newQueue = Objects.requireNonNull(pieceQueueFactory.get(), "생성한 블록 목록이 필요합니다.");
        board.reset();
        pieceQueue = newQueue;
        currentPiece = null;
        score = 0;
        level = 1;
        spawnedPieces = 0;
        totalClearedRows = 0;
        phase = GamePhase.RUNNING;
        spawnNextPiece();
    }

    /** 행동을 한 번 처리한다. 무시된 행동도 결과를 반환한다. 종료 요청은 앱 제어가 처리한다. */
    public GameActionResult apply(GameAction action) {
        Objects.requireNonNull(action, "처리할 게임 행동이 필요합니다.");
        GamePhase previousPhase = phase;
        if (action == GameAction.TOGGLE_PAUSE) {
            if (phase == GamePhase.RUNNING) {
                phase = GamePhase.PAUSED;
            } else if (phase == GamePhase.PAUSED) {
                phase = GamePhase.RUNNING;
            }
            return result(previousPhase != phase, 0, 0, false, false, previousPhase);
        }
        if (phase != GamePhase.RUNNING) {
            return result(false, 0, 0, false, false, previousPhase);
        }
        return switch (action) {
            case LEFT -> result(moveHorizontally(-1), 0, 0, false, false, previousPhase);
            case RIGHT -> result(moveHorizontally(1), 0, 0, false, false, previousPhase);
            case ROTATE_CLOCKWISE -> result(rotateClockwise(), 0, 0, false, false, previousPhase);
            case DOWN, TICK -> moveDown(previousPhase);
            case HARD_DROP -> hardDrop(previousPhase);
            case QUIT, TOGGLE_PAUSE -> result(false, 0, 0, false, false, previousPhase);
        };
    }

    /** 외부의 보드 수정과 이후 행동에 영향받지 않는 조회 시점의 상태를 반환한다. */
    public GameState state() {
        return new GameState(board, currentPiece, currentRow, currentColumn,
                pieceQueue == null ? List.of() : pieceQueue.preview(), phase, score, level,
                spawnedPieces, totalClearedRows);
    }

    public boolean isGameOver() {
        return phase == GamePhase.GAME_OVER;
    }

    /** 점수 계산은 하지 않는다. 앱에서 계산한 두 값을 검증 후 함께 반영한다. */
    public void updateScoreAndLevel(long score, int level) {
        if (score < 0 || level < 1) {
            throw new IllegalArgumentException("점수는 0 이상, 레벨은 1 이상이어야 합니다.");
        }
        this.score = score;
        this.level = level;
    }

    private boolean moveHorizontally(int direction) {
        int candidateColumn = currentColumn + direction;
        if (!board.canPlace(currentPiece, currentRow, candidateColumn)) {
            return false;
        }
        currentColumn = candidateColumn;
        return true;
    }

    private boolean rotateClockwise() {
        Tetromino candidatePiece = currentPiece.rotateClockwise();
        if (Arrays.deepEquals(candidatePiece.cells(), currentPiece.cells())
                || !board.canPlace(candidatePiece, currentRow, currentColumn)) {
            return false;
        }
        currentPiece = candidatePiece;
        return true;
    }

    private GameActionResult moveDown(GamePhase previousPhase) {
        if (board.canPlace(currentPiece, currentRow + 1, currentColumn)) {
            currentRow++;
            return result(true, 1, 0, false, false, previousPhase);
        }
        return lockAndSpawn(0, previousPhase);
    }

    private GameActionResult hardDrop(GamePhase previousPhase) {
        int droppedRows = 0;
        while (board.canPlace(currentPiece, currentRow + 1, currentColumn)) {
            currentRow++;
            droppedRows++;
        }
        return lockAndSpawn(droppedRows, previousPhase);
    }

    private GameActionResult lockAndSpawn(int droppedRows, GamePhase previousPhase) {
        board.lock(currentPiece, currentRow, currentColumn);
        int clearedRows = board.clearFullRows();
        totalClearedRows += clearedRows;
        boolean spawned = spawnNextPiece();
        return result(true, droppedRows, clearedRows, true, spawned, previousPhase);
    }

    private boolean spawnNextPiece() {
        Tetromino candidatePiece = new Tetromino(pieceQueue.next());
        currentRow = spawnPolicy.startingRow(candidatePiece);
        currentColumn = spawnPolicy.startingColumn(candidatePiece);
        if (!board.canPlace(candidatePiece, currentRow, currentColumn)) {
            currentPiece = null;
            phase = GamePhase.GAME_OVER;
            return false;
        }
        currentPiece = candidatePiece;
        spawnedPieces++;
        return true;
    }

    private GameActionResult result(boolean changed, int droppedRows, int clearedRows,
            boolean pieceLocked, boolean pieceSpawned, GamePhase previousPhase) {
        return new GameActionResult(changed, droppedRows, clearedRows, pieceLocked,
                pieceSpawned, previousPhase, phase);
    }
}
