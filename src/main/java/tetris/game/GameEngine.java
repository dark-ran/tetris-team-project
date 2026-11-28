package tetris.game;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.random.RandomGenerator;
import tetris.board.Board;
import tetris.piece.PieceQueue;
import tetris.piece.PieceType;
import tetris.piece.ItemGenerator;
import tetris.piece.Tetromino;
import tetris.rule.PieceRules;
import tetris.rule.LineClearBonus;
import java.util.Set;

/**
 * 화면과 타이머에 의존하지 않고 행동과 진행 상태를 처리한다. 호출은 한 실행 흐름에서 한다.
 * 예외로 {@link #receiveAttack(List)}는 상대 플레이어의 스레드에서 호출해도 된다.
 */
public final class GameEngine {
    public static final int ROWS_PER_ITEM = 10;
    private final Board board = new Board();
    private final AttackQueue incomingAttacks = new AttackQueue();
    private final Function<Difficulty, PieceQueue> pieceQueueFactory;
    private final PieceSpawnPolicy spawnPolicy;
    private final UnaryOperator<Tetromino> itemGenerator;
    private PieceQueue pieceQueue;
    private Tetromino currentPiece;
    private PieceRules currentRules;
    private boolean contacted;
    private int currentRow;
    private int currentColumn;
    private GamePhase phase = GamePhase.READY;
    private GameMode mode = GameMode.NORMAL;
    private Difficulty difficulty = Difficulty.NORMAL;
    private long score;
    private int level = 1;
    private int spawnedPieces;
    private int totalClearedRows;
    private int pendingItems;
    private LineClearSnapshot lineClear;

    public GameEngine() {
        this(RandomGenerator.getDefault());
    }

    /** 재현 가능한 생성 순서가 필요할 때 무작위 선택기를 전달한다. */
    public GameEngine(RandomGenerator randomGenerator) {
        this(queueFactory(randomGenerator), new CenteredPieceSpawnPolicy(),
                new ItemGenerator(randomGenerator));
    }

    /** 새 게임의 블록 공급 방식과 시작 위치 규칙을 객체로 전달한다. */
    public GameEngine(Supplier<PieceQueue> pieceQueueFactory, PieceSpawnPolicy spawnPolicy) {
        this(pieceQueueFactory, spawnPolicy, new ItemGenerator(RandomGenerator.getDefault()));
    }

    /** 아이템 생성 방식을 바꾸어도 등장 시점과 진행 규칙은 유지된다. */
    public GameEngine(Supplier<PieceQueue> pieceQueueFactory, PieceSpawnPolicy spawnPolicy,
            UnaryOperator<Tetromino> itemGenerator) {
        this(wrapSupplier(pieceQueueFactory), spawnPolicy, itemGenerator);
    }

    private GameEngine(Function<Difficulty, PieceQueue> pieceQueueFactory, PieceSpawnPolicy spawnPolicy,
            UnaryOperator<Tetromino> itemGenerator) {
        this.pieceQueueFactory = Objects.requireNonNull(pieceQueueFactory, "블록 목록 생성기가 필요합니다.");
        this.spawnPolicy = Objects.requireNonNull(spawnPolicy, "블록 시작 위치 규칙이 필요합니다.");
        this.itemGenerator = Objects.requireNonNull(itemGenerator, "아이템 생성기가 필요합니다.");
    }

    private static Function<Difficulty, PieceQueue> queueFactory(RandomGenerator randomGenerator) {
        Objects.requireNonNull(randomGenerator, "무작위 선택기가 필요합니다.");
        return difficulty -> new PieceQueue(randomGenerator, PieceQueue.DEFAULT_PREVIEW_COUNT, difficulty);
    }

    private static Function<Difficulty, PieceQueue> wrapSupplier(Supplier<PieceQueue> supplier) {
        Objects.requireNonNull(supplier, "블록 목록 생성기가 필요합니다.");
        return difficulty -> supplier.get();
    }

    public Difficulty difficulty() { return difficulty; }

    public void replaceLastPreviewPiece(PieceType type) {
        Objects.requireNonNull(type, "블록 종류가 필요합니다.");
        if (pieceQueue == null) throw new IllegalStateException("게임이 시작되지 않았습니다.");
        pieceQueue.replaceLastPreview(type);
    }

    /** 보드·점수·누적 수치를 초기화하고 첫 블록을 생성한다. 생성 실패 시 게임오버이다. */
    public void newGame() {
        newGame(GameMode.NORMAL, Difficulty.NORMAL);
    }

    public void newGame(Difficulty difficulty) {
        newGame(GameMode.NORMAL, difficulty);
    }

    public void newGame(GameMode mode) {
        newGame(mode, Difficulty.NORMAL);
    }

    public void newGame(GameMode mode, Difficulty difficulty) {
        Objects.requireNonNull(mode);
        Objects.requireNonNull(difficulty);
        PieceQueue newQueue = Objects.requireNonNull(pieceQueueFactory.apply(difficulty), "생성한 블록 목록이 필요합니다.");
        board.reset();
        incomingAttacks.clear();
        pieceQueue = newQueue;
        currentPiece = null;
        score = 0;
        level = 1;
        spawnedPieces = 0;
        totalClearedRows = 0;
        pendingItems = 0;
        lineClear = null;
        this.mode = mode;
        this.difficulty = difficulty;
        contacted = false;
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
                pieceQueue == null ? List.of() : pieceQueue.previewPieces(), phase, score, level,
                spawnedPieces, totalClearedRows, currentPiece == null ? currentRow
                        : currentRules.movement().landingRow(board, currentPiece, currentRow, currentColumn), mode, difficulty,
                incomingAttacks.pending());
    }

    /**
     * 상대가 보낸 공격 줄을 대기열에 넣는다. 보드에는 현재 블록을 고정한 뒤 다음 블록 생성 전에 추가된다.
     * 대기열의 10줄 제한을 적용하며, 다른 스레드에서 호출해도 된다.
     *
     * @return 실제로 대기열에 들어간 줄 수
     */
    public int receiveAttack(List<AttackLine> lines) {
        return incomingAttacks.offer(lines);
    }

    LineClearSnapshot lineClear() { return lineClear; }

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
        if (!currentRules.movement().allowsHorizontal(contacted)) return false;
        int candidateColumn = currentColumn + direction;
        if (!board.canPlace(currentPiece, currentRow, candidateColumn)) {
            return false;
        }
        currentColumn = candidateColumn;
        updateContact();
        return true;
    }

    private boolean rotateClockwise() {
        if (!currentRules.movement().allowsRotation()) return false;
        Tetromino candidatePiece = currentPiece.rotateClockwise();
        if (candidatePiece.hasSameLayout(currentPiece)
                || !board.canPlace(candidatePiece, currentRow, currentColumn)) {
            return false;
        }
        currentPiece = candidatePiece;
        updateContact();
        return true;
    }

    private GameActionResult moveDown(GamePhase previousPhase) {
        if (currentRules.movement().canDescend(board, currentPiece, currentRow, currentColumn)) {
            descend();
            return result(true, 1, 0, false, false, previousPhase);
        }
        return lockAndSpawn(0, previousPhase);
    }

    private GameActionResult hardDrop(GamePhase previousPhase) {
        int droppedRows = 0;
        while (currentRules.movement().canDescend(board, currentPiece, currentRow, currentColumn)) {
            descend();
            droppedRows++;
        }
        return lockAndSpawn(droppedRows, previousPhase);
    }

    private GameActionResult lockAndSpawn(int droppedRows, GamePhase previousPhase) {
        if (currentRules.leavesPieceOnBoard()) board.lock(currentPiece, currentRow, currentColumn);
        currentRules.lockEffect().onLock(board, currentPiece, currentRow, currentColumn);
        Set<Integer> rows = currentRules.lockEffect().rowsToClear(board, currentPiece, currentRow, currentColumn);
        int lineScoreMultiplier = LineClearBonus.multiplier(board, rows);
        List<AttackLine> attackLines = BattleRules.sendsAttack(currentPiece.itemType(), rows.size())
                ? attackLinesFrom(rows) : List.of();
        if (!rows.isEmpty()) {
            lineClear = new LineClearSnapshot(totalClearedRows + rows.size(), board,
                    rows.stream().sorted().toList());
        }
        int clearedRows = board.clearRows(rows);
        int previousClearedRows = totalClearedRows;
        totalClearedRows += clearedRows;
        if (mode == GameMode.ITEM) {
            pendingItems += totalClearedRows / ROWS_PER_ITEM - previousClearedRows / ROWS_PER_ITEM;
        }
        boolean spawned = addIncomingAttacks() && spawnNextPiece();
        return new GameActionResult(true, droppedRows, clearedRows, true, spawned,
                previousPhase, phase, lineScoreMultiplier, attackLines);
    }

    /**
     * 삭제할 줄에서 방금 고정한 블록의 칸을 빼고 위에서 아래 순서로 공격 줄을 만든다.
     * 블록 칸을 빼면 남는 칸이 없는 줄(줄 삭제 아이템으로 지운 줄 등)은 넘기지 않는다.
     */
    private List<AttackLine> attackLinesFrom(Set<Integer> rows) {
        int[][] cells = board.snapshot();
        boolean[][] lockedCells = new boolean[Board.ROWS][Board.COLUMNS];
        if (currentRules.leavesPieceOnBoard()) {
            for (int[] cell : currentPiece.cells()) {
                lockedCells[currentRow + cell[0]][currentColumn + cell[1]] = true;
            }
        }
        return rows.stream().sorted().map(row -> {
            boolean[] filled = new boolean[Board.COLUMNS];
            for (int column = 0; column < Board.COLUMNS; column++) {
                filled[column] = cells[row][column] != Board.EMPTY_CELL && !lockedCells[row][column];
            }
            return new AttackLine(filled);
        }).filter(AttackLine::hasFilledCell).toList();
    }

    /** 대기 중인 공격 줄을 보드 아래에 추가한다. 고정 칸이 위로 넘치면 게임오버로 바꾸고 false를 반환한다. */
    private boolean addIncomingAttacks() {
        List<AttackLine> lines = incomingAttacks.drain();
        if (lines.isEmpty()) return true;
        boolean overflowed = board.addRowsAtBottom(lines.stream()
                .map(AttackLine::filled).toArray(boolean[][]::new));
        if (!overflowed) return true;
        currentPiece = null;
        phase = GamePhase.GAME_OVER;
        return false;
    }

    private boolean spawnNextPiece() {
        Tetromino candidatePiece;
        if (pendingItems > 0) {
            candidatePiece = pieceQueue.nextPiece(itemGenerator);
            pendingItems--;
        } else {
            candidatePiece = pieceQueue.nextPiece();
        }
        currentRow = spawnPolicy.startingRow(candidatePiece);
        currentColumn = spawnPolicy.startingColumn(candidatePiece);
        if (!board.canPlace(candidatePiece, currentRow, currentColumn)) {
            currentPiece = null;
            phase = GamePhase.GAME_OVER;
            return false;
        }
        currentPiece = candidatePiece;
        currentRules = PieceRules.forPiece(candidatePiece);
        contacted = false;
        updateContact();
        spawnedPieces++;
        return true;
    }

    private void descend() {
        currentRow++;
        currentRules.movement().afterDescent(board, currentPiece, currentRow, currentColumn);
        updateContact();
    }

    private void updateContact() {
        contacted |= !board.canPlace(currentPiece, currentRow + 1, currentColumn);
    }

    private GameActionResult result(boolean changed, int droppedRows, int clearedRows,
            boolean pieceLocked, boolean pieceSpawned, GamePhase previousPhase) {
        return new GameActionResult(changed, droppedRows, clearedRows, pieceLocked,
                pieceSpawned, previousPhase, phase);
    }
}
