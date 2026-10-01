package tetris.game;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tetris.board.Board;
import tetris.piece.PieceQueue;
import tetris.piece.PieceType;
import tetris.piece.Tetromino;

@DisplayName("게임 엔진의 행동과 진행 상태")
class GameEngineTest {
    @Test
    @DisplayName("기본 생성기로 새 게임을 시작할 수 있음")
    void defaultEngineCanStartANewGame() {
        GameEngine engine = new GameEngine();
        engine.newGame();
        assertEquals(GamePhase.RUNNING, engine.state().phase());
        assertNotNull(engine.state().currentPiece());
        assertEquals(1, engine.state().nextPieces().size());
    }

    @Test
    @DisplayName("게임오버 전환과 줄 삭제가 함께 일어나도 마지막 점수 계산 자료를 제공")
    void finalDropAndLineClearAreRetainedWhenTheNextSpawnFails() {
        PieceSpawnPolicy policy = new PieceSpawnPolicy() {
            @Override
            public int startingRow(Tetromino piece) {
                return piece.type() == PieceType.T ? 20 : 0;
            }

            @Override
            public int startingColumn(Tetromino piece) {
                return (10 - piece.rotationSize()) / 2;
            }
        };
        GameEngine engine = new GameEngine(() -> new PieceQueue(new SequenceRandomGenerator(
                PieceType.I, PieceType.I, PieceType.O, PieceType.T)), policy);
        engine.newGame();
        moveToColumn(engine, 0);
        engine.apply(GameAction.HARD_DROP);
        moveToColumn(engine, 4);
        engine.apply(GameAction.HARD_DROP);
        moveToColumn(engine, 8);
        GameActionResult result = engine.apply(GameAction.HARD_DROP);
        assertTrue(result.becameGameOver());
        assertEquals(18, result.droppedRows());
        assertEquals(1, result.clearedRows());
        assertTrue(result.pieceLocked());
        assertFalse(result.pieceSpawned());
        assertEquals(1, engine.state().totalClearedRows());
        assertEquals(3, engine.state().spawnedPieces());
        engine.updateScoreAndLevel(456, 3);
        assertEquals(456, engine.state().score());
        engine.newGame();
        assertEquals(0, engine.state().totalClearedRows());
        assertEquals(1, engine.state().spawnedPieces());
        assertEquals(0, engine.state().score());
    }
    @Test
    @DisplayName("생성 직후에는 시작 전 상태이며 현재 블록과 다음 목록이 없음")
    void initialStateIsReady() {
        GameState state = new GameEngine().state();
        assertEquals(GamePhase.READY, state.phase());
        assertNull(state.currentPiece());
        assertEquals(List.of(), state.nextPieces());
        assertEquals(0, state.score());
        assertEquals(1, state.level());
        assertEquals(0, state.spawnedPieces());
        assertEquals(0, state.totalClearedRows());
        assertArrayEquals(new int[20][10], state.board().snapshot());
    }

    @ParameterizedTest(name = "{0}은 보드 위쪽 가운데에서 생성")
    @EnumSource(PieceType.class)
    void everyPieceStartsAtTheAgreedPosition(PieceType type) {
        GameEngine engine = engine(type);
        engine.newGame();
        GameState state = engine.state();
        assertEquals(GamePhase.RUNNING, state.phase());
        assertEquals(type, state.currentPiece().type());
        assertEquals(0, state.currentRow());
        int squareSize = type == PieceType.I ? 4 : type == PieceType.O ? 2 : 3;
        assertEquals((10 - squareSize) / 2, state.currentColumn());
        assertEquals(List.of(type), state.nextPieces());
        assertEquals(1, state.spawnedPieces());
        assertFalse(engine.isGameOver());
    }

    @ParameterizedTest(name = "시작 전에는 {0}을 무시")
    @EnumSource(GameAction.class)
    void actionsBeforeStartingDoNotChangeState(GameAction action) {
        GameEngine engine = engine(PieceType.T);
        assertUnchanged(engine.apply(action), GamePhase.READY);
    }

    @Test
    @DisplayName("좌우 이동은 한 칸이며 양쪽 보드 경계에서 상태를 유지함")
    void horizontalMovementStopsAtBothEdges() {
        GameEngine engine = engine(PieceType.O);
        engine.newGame();
        assertTrue(engine.apply(GameAction.LEFT).changed());
        assertEquals(3, engine.state().currentColumn());
        assertTrue(engine.apply(GameAction.RIGHT).changed());
        assertEquals(4, engine.state().currentColumn());
        for (int count = 0; count < 20; count++) {
            engine.apply(GameAction.LEFT);
        }
        assertEquals(0, engine.state().currentColumn());
        assertUnchanged(engine.apply(GameAction.LEFT), GamePhase.RUNNING);
        for (int count = 0; count < 20; count++) {
            engine.apply(GameAction.RIGHT);
        }
        assertEquals(8, engine.state().currentColumn());
        assertUnchanged(engine.apply(GameAction.RIGHT), GamePhase.RUNNING);
    }

    @Test
    @DisplayName("고정된 블록과 겹치는 좌우 이동은 거부")
    void horizontalMovementCannotCrossFixedPieces() {
        GameEngine engine = engine(PieceType.O);
        engine.newGame();
        engine.apply(GameAction.HARD_DROP);
        moveToColumn(engine, 2);
        for (int count = 0; count < 17; count++) {
            engine.apply(GameAction.DOWN);
        }
        assertEquals(17, engine.state().currentRow());
        assertUnchanged(engine.apply(GameAction.RIGHT), GamePhase.RUNNING);
        assertEquals(2, engine.state().currentColumn());
    }

    @ParameterizedTest(name = "{0}은 실제 하강만 한 칸으로 계산하고 다음 행동에서 고정")
    @EnumSource(value = GameAction.class, names = {"DOWN", "TICK"})
    void automaticAndManualDropReportOnlyActualMovement(GameAction action) {
        GameEngine engine = engine(PieceType.O);
        engine.newGame();
        for (int count = 1; count <= 18; count++) {
            GameActionResult result = engine.apply(action);
            assertTrue(result.changed());
            assertEquals(1, result.droppedRows());
            assertFalse(result.pieceLocked());
            assertEquals(count, engine.state().currentRow());
        }
        assertEquals(0, occupiedCells(engine.state()));
        GameActionResult result = engine.apply(action);
        assertEquals(0, result.droppedRows());
        assertTrue(result.pieceLocked());
        assertTrue(result.pieceSpawned());
        assertEquals(4, occupiedCells(engine.state()));
        assertEquals(0, engine.state().currentRow());
        assertEquals(2, engine.state().spawnedPieces());
        assertEquals(0, engine.state().score());
    }

    @Test
    @DisplayName("즉시 낙하는 이동한 전체 칸 수와 고정·다음 생성 결과를 반환")
    void hardDropReportsItsDistanceAndAdvancesTheQueue() {
        GameEngine engine = engine(PieceType.O, PieceType.T, PieceType.L);
        engine.newGame();
        assertEquals(List.of(PieceType.T), engine.state().nextPieces());
        GameActionResult result = engine.apply(GameAction.HARD_DROP);
        assertEquals(18, result.droppedRows());
        assertEquals(0, result.clearedRows());
        assertTrue(result.pieceLocked());
        assertTrue(result.pieceSpawned());
        assertFalse(result.becameGameOver());
        assertEquals(PieceType.T, engine.state().currentPiece().type());
        assertEquals(List.of(PieceType.L), engine.state().nextPieces());
        assertEquals(2, engine.state().board().snapshot()[19][4]);
    }

    @Test
    @DisplayName("회전은 위치를 유지하며 네 번 회전하면 원래 모양")
    void rotationChangesTheShapeWithoutMovingTheOrigin() {
        GameEngine engine = engine(PieceType.T);
        engine.newGame();
        int[][] original = engine.state().currentPiece().cells();
        for (int count = 0; count < 4; count++) {
            assertTrue(engine.apply(GameAction.ROTATE_CLOCKWISE).changed());
            assertEquals(0, engine.state().currentRow());
            assertEquals(3, engine.state().currentColumn());
        }
        assertArrayEquals(original, engine.state().currentPiece().cells());
    }

    @Test
    @DisplayName("O 회전은 모양이 같아 상태 변경으로 보고하지 않음")
    void squareRotationIsAnUnchangedAction() {
        GameEngine engine = engine(PieceType.O);
        engine.newGame();
        assertUnchanged(engine.apply(GameAction.ROTATE_CLOCKWISE), GamePhase.RUNNING);
    }

    @Test
    @DisplayName("벽과 바닥을 넘는 회전은 원래 모양을 유지")
    void rotationCannotCrossTheWallOrFloor() {
        GameEngine engine = engine(PieceType.I);
        engine.newGame();
        engine.apply(GameAction.ROTATE_CLOCKWISE);
        moveToColumn(engine, 7);
        int[][] original = engine.state().currentPiece().cells();
        assertUnchanged(engine.apply(GameAction.ROTATE_CLOCKWISE), GamePhase.RUNNING);
        assertArrayEquals(original, engine.state().currentPiece().cells());
        engine = engine(PieceType.T);
        engine.newGame();
        for (int count = 0; count < 18; count++) {
            engine.apply(GameAction.DOWN);
        }
        assertUnchanged(engine.apply(GameAction.ROTATE_CLOCKWISE), GamePhase.RUNNING);
        assertEquals(18, engine.state().currentRow());
    }

    @Test
    @DisplayName("고정된 블록을 통과하는 회전을 거부")
    void rotationCannotCrossFixedPieces() {
        GameEngine engine = engine(PieceType.O, PieceType.O, PieceType.O, PieceType.O,
                PieceType.O, PieceType.O, PieceType.O, PieceType.O, PieceType.O, PieceType.T);
        engine.newGame();
        for (int count = 0; count < 9; count++) {
            engine.apply(GameAction.HARD_DROP);
        }
        assertEquals(PieceType.T, engine.state().currentPiece().type());
        assertUnchanged(engine.apply(GameAction.ROTATE_CLOCKWISE), GamePhase.RUNNING);
    }

    @Test
    @DisplayName("하강 중 삭제한 한 줄의 결과와 누적 수치를 제공")
    void lockingClearsAFullRowAndPreservesTheCellsAboveIt() {
        GameEngine engine = engine(PieceType.I, PieceType.I, PieceType.O, PieceType.T);
        engine.newGame();
        moveToColumn(engine, 0);
        engine.apply(GameAction.HARD_DROP);
        moveToColumn(engine, 4);
        engine.apply(GameAction.HARD_DROP);
        moveToColumn(engine, 8);
        GameActionResult result = engine.apply(GameAction.HARD_DROP);
        assertEquals(18, result.droppedRows());
        assertEquals(1, result.clearedRows());
        assertEquals(1, engine.state().totalClearedRows());
        assertEquals(4, engine.state().spawnedPieces());
        assertEquals(2, occupiedCells(engine.state()));
        assertEquals(2, engine.state().board().snapshot()[19][8]);
        assertEquals(0, engine.state().score());
    }

    @Test
    @DisplayName("네 줄 삭제 결과를 한 행동으로 제공하고 누적 수치를 유지")
    void clearsFourRowsAtOnce() {
        GameEngine engine = engine(PieceType.I);
        engine.newGame();
        GameActionResult result = null;
        for (int column = 0; column < 10; column++) {
            engine.apply(GameAction.ROTATE_CLOCKWISE);
            moveToColumn(engine, column - 2);
            result = engine.apply(GameAction.HARD_DROP);
        }
        assertNotNull(result);
        assertEquals(16, result.droppedRows());
        assertEquals(4, result.clearedRows());
        assertEquals(4, engine.state().totalClearedRows());
        assertEquals(11, engine.state().spawnedPieces());
        assertEquals(0, occupiedCells(engine.state()));
    }

    @ParameterizedTest(name = "일시정지 중에는 {0}을 무시")
    @EnumSource(value = GameAction.class, mode = EnumSource.Mode.EXCLUDE, names = "TOGGLE_PAUSE")
    void pausedStateIgnoresGameplayActions(GameAction action) {
        GameEngine engine = engine(PieceType.T);
        engine.newGame();
        engine.apply(GameAction.DOWN);
        GameActionResult pause = engine.apply(GameAction.TOGGLE_PAUSE);
        assertTrue(pause.changed());
        assertEquals(GamePhase.RUNNING, pause.previousPhase());
        assertEquals(GamePhase.PAUSED, pause.currentPhase());
        GameState before = engine.state();
        assertUnchanged(engine.apply(action), GamePhase.PAUSED);
        assertEquals(before.currentRow(), engine.state().currentRow());
        assertEquals(before.currentColumn(), engine.state().currentColumn());
        assertArrayEquals(before.currentPiece().cells(), engine.state().currentPiece().cells());
        GameActionResult resume = engine.apply(GameAction.TOGGLE_PAUSE);
        assertEquals(GamePhase.RUNNING, resume.currentPhase());
        assertTrue(engine.apply(GameAction.TICK).changed());
        assertEquals(2, engine.state().currentRow());
    }

    @ParameterizedTest(name = "게임오버 이후에는 {0}을 무시")
    @EnumSource(GameAction.class)
    void gameOverIsReportedOnceAndSubsequentActionsAreIgnored(GameAction action) {
        GameEngine engine = engine(PieceType.O);
        engine.newGame();
        GameActionResult last = null;
        for (int count = 0; count < 10; count++) {
            last = engine.apply(GameAction.HARD_DROP);
        }
        assertNotNull(last);
        assertTrue(last.becameGameOver());
        assertTrue(last.pieceLocked());
        assertFalse(last.pieceSpawned());
        assertEquals(0, last.droppedRows());
        assertTrue(engine.isGameOver());
        assertNull(engine.state().currentPiece());
        assertEquals(10, engine.state().spawnedPieces());
        assertEquals(40, occupiedCells(engine.state()));
        assertUnchanged(engine.apply(action), GamePhase.GAME_OVER);
        engine.updateScoreAndLevel(123, 2);
        assertEquals(123, engine.state().score());
    }

    @Test
    @DisplayName("재시작은 보드·점수·레벨·진행 상태·누적 수치를 초기화")
    void restartCreatesAFreshQueueAndResetsTheWholeGame() {
        AtomicInteger createdQueues = new AtomicInteger();
        GameEngine engine = new GameEngine(() -> {
            createdQueues.incrementAndGet();
            return new PieceQueue(new SequenceRandomGenerator(PieceType.O));
        }, new CenteredPieceSpawnPolicy());
        engine.newGame();
        for (int count = 0; count < 10; count++) {
            engine.apply(GameAction.HARD_DROP);
        }
        engine.updateScoreAndLevel(50, 3);
        engine.newGame();
        assertEquals(2, createdQueues.get());
        assertEquals(GamePhase.RUNNING, engine.state().phase());
        assertEquals(0, occupiedCells(engine.state()));
        assertEquals(0, engine.state().score());
        assertEquals(1, engine.state().level());
        assertEquals(1, engine.state().spawnedPieces());
        assertEquals(0, engine.state().totalClearedRows());
    }

    @Test
    @DisplayName("상태 사본·보드·다음 목록의 외부 수정은 엔진에 영향을 주지 않음")
    void stateSnapshotsCannotModifyTheEngineOrChangeLater() {
        GameEngine engine = engine(PieceType.O, PieceType.T);
        engine.newGame();
        engine.apply(GameAction.HARD_DROP);
        GameState previous = engine.state();
        Board modifiedBoard = previous.board();
        modifiedBoard.reset();
        int[][] modifiedArray = previous.board().snapshot();
        modifiedArray[19][4] = 7;
        assertThrows(UnsupportedOperationException.class, () -> previous.nextPieces().clear());
        assertEquals(4, occupiedCells(engine.state()));
        engine.apply(GameAction.HARD_DROP);
        engine.updateScoreAndLevel(70, 2);
        assertEquals(4, occupiedCells(previous));
        assertEquals(8, occupiedCells(engine.state()));
        assertEquals(0, previous.score());
        assertEquals(1, previous.level());
        assertEquals(70, engine.state().score());
        assertEquals(2, engine.state().level());
    }

    @Test
    @DisplayName("잘못된 점수·레벨은 두 값을 함께 유지")
    void invalidScoreAndLevelDoNotPartiallyUpdateState() {
        GameEngine engine = engine(PieceType.T);
        engine.newGame();
        engine.updateScoreAndLevel(25, 2);
        assertThrows(IllegalArgumentException.class, () -> engine.updateScoreAndLevel(-1, 4));
        assertThrows(IllegalArgumentException.class, () -> engine.updateScoreAndLevel(90, 0));
        assertEquals(25, engine.state().score());
        assertEquals(2, engine.state().level());
        engine.updateScoreAndLevel(Long.MAX_VALUE, Integer.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, engine.state().score());
    }

    @Test
    @DisplayName("생성 규칙을 교체하면 엔진 수정 없이 다른 시작 위치 사용")
    void spawnPolicyCanBeReplacedWithoutChangingTheEngine() {
        PieceSpawnPolicy policy = new PieceSpawnPolicy() {
            public int startingRow(Tetromino piece) { return 5; }
            public int startingColumn(Tetromino piece) { return 1; }
        };
        GameEngine engine = new GameEngine(() -> new PieceQueue(new SequenceRandomGenerator(PieceType.O)), policy);
        engine.newGame();
        assertEquals(5, engine.state().currentRow());
        assertEquals(1, engine.state().currentColumn());
        assertEquals(13, engine.apply(GameAction.HARD_DROP).droppedRows());
    }

    @Test
    @DisplayName("첫 블록을 생성할 수 없는 시작 규칙이면 즉시 게임오버")
    void impossibleInitialSpawnEndsTheGame() {
        PieceSpawnPolicy policy = new PieceSpawnPolicy() {
            public int startingRow(Tetromino piece) { return -1; }
            public int startingColumn(Tetromino piece) { return 0; }
        };
        GameEngine engine = new GameEngine(() -> new PieceQueue(new SequenceRandomGenerator(PieceType.O)), policy);
        engine.newGame();
        assertTrue(engine.isGameOver());
        assertEquals(0, engine.state().spawnedPieces());
        assertNull(engine.state().currentPiece());
    }

    @Test
    @DisplayName("종료 요청은 앱 제어 책임이며 엔진 상태를 변경하지 않음")
    void quitIsHandledByTheApplication() {
        GameEngine engine = engine(PieceType.T);
        engine.newGame();
        assertUnchanged(engine.apply(GameAction.QUIT), GamePhase.RUNNING);
    }

    @Test
    @DisplayName("없는 행동과 의존 객체는 거부")
    void missingArgumentsAreRejected() {
        assertThrows(NullPointerException.class, () -> new GameEngine((RandomGenerator) null));
        assertThrows(NullPointerException.class, () -> new GameEngine(null, new CenteredPieceSpawnPolicy()));
        assertThrows(NullPointerException.class, () -> new GameEngine(PieceQueue::new, null));
        GameEngine engine = engine(PieceType.T);
        assertThrows(NullPointerException.class, () -> engine.apply(null));
        assertThrows(NullPointerException.class, () -> new GameEngine(() -> null,
                new CenteredPieceSpawnPolicy()).newGame());
    }

    private static GameEngine engine(PieceType... types) {
        return new GameEngine(new SequenceRandomGenerator(types));
    }

    private static void moveToColumn(GameEngine engine, int column) {
        while (engine.state().currentColumn() != column) {
            GameAction action = engine.state().currentColumn() < column ? GameAction.RIGHT : GameAction.LEFT;
            assertTrue(engine.apply(action).changed(), "준비할 열까지 이동할 수 있어야 한다");
        }
    }

    private static long occupiedCells(GameState state) {
        return Arrays.stream(state.board().snapshot()).flatMapToInt(Arrays::stream)
                .filter(value -> value != 0).count();
    }

    private static void assertUnchanged(GameActionResult result, GamePhase phase) {
        assertEquals(new GameActionResult(false, 0, 0, false, false, phase, phase), result);
        assertFalse(result.becameGameOver());
    }

    private static final class SequenceRandomGenerator implements RandomGenerator {
        private final PieceType[] types;
        private int selectionCount;

        private SequenceRandomGenerator(PieceType... types) { this.types = types.clone(); }

        @Override
        public int nextInt(int bound) {
            assertEquals(7, bound);
            return types[selectionCount++ % types.length].cellValue() - 1;
        }

        @Override
        public long nextLong() { throw new AssertionError("범위를 지정한 정수 선택을 사용해야 한다"); }
    }
}
