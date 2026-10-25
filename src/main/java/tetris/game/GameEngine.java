
package tetris.game;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

import tetris.board.Board;
import tetris.piece.PieceQueue;
import tetris.piece.Tetromino;
import tetris.piece.PieceType;

/**
 * 화면과 타이머에 의존하지 않고 게임 행동과 진행 상태를 처리한다.
 * 호출은 한 실행 흐름에서 한다.
 */
public final class GameEngine {
    private final Board board = new Board();

    private final Function<Difficulty, PieceQueue> pieceQueueFactory;
    private final PieceSpawnPolicy spawnPolicy;

    private PieceQueue pieceQueue;
    private Tetromino currentPiece;
    private int currentRow;
    private int currentColumn;

    private GamePhase phase = GamePhase.READY;
    private Difficulty difficulty = Difficulty.NORMAL;

    private long score;
    private int level = 1;
    private int spawnedPieces;
    private int totalClearedRows;

    /**
     * 기본 난수 생성기로 게임 엔진을 만든다.
     */
    public GameEngine() {
        this(RandomGenerator.getDefault());
    }

    /**
     * 테스트 등에서 재현 가능한 난수 생성기를 지정할 수 있다.
     */
    public GameEngine(RandomGenerator randomGenerator) {
        this(
                queueFactory(randomGenerator),
                new CenteredPieceSpawnPolicy());
    }

    /**
     * 기존 테스트와 외부 블록 공급 방식의 호환성을 유지한다.
     */
    public GameEngine(
            Supplier<PieceQueue> pieceQueueFactory,
            PieceSpawnPolicy spawnPolicy) {
        this(
                wrapSupplier(pieceQueueFactory),
                spawnPolicy);
    }

    private GameEngine(
            Function<Difficulty, PieceQueue> pieceQueueFactory,
            PieceSpawnPolicy spawnPolicy) {
        this.pieceQueueFactory = Objects.requireNonNull(
                pieceQueueFactory, "블록 목록 생성기가 필요합니다.");

        this.spawnPolicy = Objects.requireNonNull(
                spawnPolicy, "블록 시작 위치 규칙이 필요합니다.");
    }

    /**
     * 난이도 정보를 받아 생성기를 구성한다.
     */
    private static Function<Difficulty, PieceQueue> queueFactory(
            RandomGenerator randomGenerator) {

        Objects.requireNonNull(
                randomGenerator, "무작위 선택기가 필요합니다.");

        return difficulty -> new PieceQueue(
                randomGenerator,
                PieceQueue.DEFAULT_PREVIEW_COUNT,
                difficulty);
    }

    /**
     * 기존 Supplier 주입 방식은 난이도 인수를 사용하지 않는다.
     * 테스트에서 지정한 블록 순서를 그대로 유지한다.
     */
    private static Function<Difficulty, PieceQueue> wrapSupplier(
            Supplier<PieceQueue> supplier) {

        Objects.requireNonNull(
                supplier, "블록 목록 생성기가 필요합니다.");

        return difficulty -> supplier.get();
    }

    public void replaceLastPreviewPiece(PieceType type) {
        Objects.requireNonNull(type, "블록 종류가 필요합니다.");

        if (pieceQueue == null) {
            throw new IllegalStateException("게임이 시작되지 않았습니다.");
        }

        pieceQueue.replaceLastPreview(type);
    }

    public Difficulty difficulty() {
        return difficulty;
    }

    /**
     * 기존 호출 방식과의 호환을 위해 NORMAL로 시작한다.
     */
    public void newGame() {
        newGame(Difficulty.NORMAL);
    }

    /**
     * 선택한 난이도로 새 게임을 시작한다.
     */
    public void newGame(Difficulty selectedDifficulty) {
        Difficulty nextDifficulty = Objects.requireNonNull(
                selectedDifficulty, "난이도가 필요합니다.");

        PieceQueue newQueue = Objects.requireNonNull(
                pieceQueueFactory.apply(nextDifficulty),
                "생성한 블록 목록이 필요합니다.");

        board.reset();
        pieceQueue = newQueue;
        currentPiece = null;
        currentRow = 0;
        currentColumn = 0;

        score = 0;
        level = 1;
        spawnedPieces = 0;
        totalClearedRows = 0;

        difficulty = nextDifficulty;
        phase = GamePhase.RUNNING;

        spawnNextPiece();
    }

    /**
     * 게임 행동을 한 번 처리한다.
     * 무시된 행동도 GameActionResult를 반환한다.
     */
    public GameActionResult apply(GameAction action) {
        Objects.requireNonNull(
                action, "처리할 게임 행동이 필요합니다.");

        GamePhase previousPhase = phase;

        if (action == GameAction.TOGGLE_PAUSE) {
            if (phase == GamePhase.RUNNING) {
                phase = GamePhase.PAUSED;
            } else if (phase == GamePhase.PAUSED) {
                phase = GamePhase.RUNNING;
            }

            return result(
                    previousPhase != phase,
                    0, 0, false, false, previousPhase);
        }

        if (phase != GamePhase.RUNNING) {
            return result(
                    false, 0, 0, false, false, previousPhase);
        }

        return switch (action) {
            case LEFT -> result(
                    moveHorizontally(-1),
                    0, 0, false, false, previousPhase);

            case RIGHT -> result(
                    moveHorizontally(1),
                    0, 0, false, false, previousPhase);

            case ROTATE_CLOCKWISE -> result(
                    rotateClockwise(),
                    0, 0, false, false, previousPhase);

            case DOWN, TICK -> moveDown(previousPhase);

            case HARD_DROP -> hardDrop(previousPhase);

            case QUIT, TOGGLE_PAUSE -> result(
                    false, 0, 0, false, false, previousPhase);
        };
    }

    /**
     * 외부에서 수정할 수 없는 조회 시점의 게임 상태를 반환한다.
     */
    public GameState state() {
        return new GameState(
                board,
                currentPiece,
                currentRow,
                currentColumn,
                pieceQueue == null
                        ? List.of()
                        : pieceQueue.previewPieces(),
                phase,
                score,
                level,
                spawnedPieces,
                totalClearedRows,
                difficulty);
    }

    public boolean isGameOver() {
        return phase == GamePhase.GAME_OVER;
    }

    /**
     * 점수와 레벨은 앱에서 계산한 값을 받아 반영한다.
     */
    public void updateScoreAndLevel(long score, int level) {
        if (score < 0 || level < 1) {
            throw new IllegalArgumentException(
                    "점수는 0 이상, 레벨은 1 이상이어야 합니다.");
        }

        this.score = score;
        this.level = level;
    }

    /**
     * 좌우로 한 칸 이동을 시도한다.
     */
    private boolean moveHorizontally(int direction) {
        int candidateColumn = currentColumn + direction;

        if (!board.canPlace(
                currentPiece, currentRow, candidateColumn)) {
            return false;
        }

        currentColumn = candidateColumn;
        return true;
    }

    /**
     * 시계방향으로 90도 회전한다.
     * 1칸 블록은 회전해도 모양이 같으므로 변경하지 않는다.
     */
    private boolean rotateClockwise() {
        Tetromino candidatePiece =
                currentPiece.rotateClockwise();

        if (Arrays.deepEquals(
                candidatePiece.cells(), currentPiece.cells())) {
            return false;
        }

        if (!board.canPlace(
                candidatePiece, currentRow, currentColumn)) {
            return false;
        }

        currentPiece = candidatePiece;
        return true;
    }

    /**
     * 한 칸 하강하거나 바닥에 도착했을 때 고정한다.
     */
    private GameActionResult moveDown(GamePhase previousPhase) {
        if (board.canPlace(
                currentPiece, currentRow + 1, currentColumn)) {

            currentRow++;

            return result(
                    true, 1, 0, false, false, previousPhase);
        }

        return lockAndSpawn(0, previousPhase);
    }

    /**
     * 더 이상 내려갈 수 없을 때까지 하강한다.
     */
    private GameActionResult hardDrop(GamePhase previousPhase) {
        int droppedRows = 0;

        while (board.canPlace(
                currentPiece, currentRow + 1, currentColumn)) {

            currentRow++;
            droppedRows++;
        }

        return lockAndSpawn(droppedRows, previousPhase);
    }

    /**
     * 현재 블록을 고정하고 완성된 줄을 삭제한 다음
     * 다음 블록을 생성한다.
     */
    private GameActionResult lockAndSpawn(
            int droppedRows,
            GamePhase previousPhase) {

        board.lock(
                currentPiece,
                currentRow,
                currentColumn);

        int clearedRows = board.clearFullRows();

        totalClearedRows += clearedRows;

        boolean spawned = spawnNextPiece();

        return result(
                true,
                droppedRows,
                clearedRows,
                true,
                spawned,
                previousPhase);
    }

    /**
     * 다음 블록 객체를 그대로 생성한다.
     *
     * PieceQueue에서 Tetromino 전체를 받으므로
     * 1칸 블록이 일반 O 블록으로 바뀌지 않는다.
     */
    private boolean spawnNextPiece() {
        Tetromino candidatePiece = pieceQueue.nextPiece();

        currentRow =
                spawnPolicy.startingRow(candidatePiece);

        currentColumn =
                spawnPolicy.startingColumn(candidatePiece);

        if (!board.canPlace(
                candidatePiece,
                currentRow,
                currentColumn)) {

            currentPiece = null;
            phase = GamePhase.GAME_OVER;
            return false;
        }

        currentPiece = candidatePiece;
        spawnedPieces++;

        return true;
    }

    /**
     * 행동 처리 결과를 생성한다.
     */
    private GameActionResult result(
            boolean changed,
            int droppedRows,
            int clearedRows,
            boolean pieceLocked,
            boolean pieceSpawned,
            GamePhase previousPhase) {

        return new GameActionResult(
                changed,
                droppedRows,
                clearedRows,
                pieceLocked,
                pieceSpawned,
                previousPhase,
                phase);
    }
}
