package tetris.app;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tetris.game.*;
import tetris.loop.GameLoop;
import tetris.piece.*;
import tetris.scoreboard.*;
import tetris.settings.*;

/** 실제 엔진을 사용하고, 시간 대기 대신 루프 호출과 전달된 상태를 관찰하는 통합 테스트. */
class AppIntegrationTest {
    @TempDir Path directory;
    /** 타이머를 실행하지 않고 앱이 요청한 수명주기와 낙하 간격을 기록한다. */
    private static final class Loop extends GameLoop {
        int starts, stops, pauses, resumes;
        long interval;
        @Override public void start() { starts++; }
        @Override public void stop() { stops++; }
        @Override public void pause() { pauses++; }
        @Override public void resume() { resumes++; }
    }
    private AppController app(GameEngine engine, Loop loop, List<GameState> renders, ScoreRepository repository) {
        return new AppController(() -> {}, engine, loop, value -> loop.interval = value,
                renders::add, new ScoreBoardService(repository), null, ignored -> {});
    }
    @Test void forwardsInputScoresRendersAndPausesBothEngineAndLoop() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GameEngine engine = new GameEngine(new Random(21));
            Loop loop = new Loop();
            List<GameState> renders = new ArrayList<>();
            AppController app = app(engine, loop, renders, new ScoreRepository(directory.resolve("scores")));
            app.startGame();
            assertEquals(1, loop.stops);
            int row = engine.state().currentRow();
            assertEquals(1, loop.starts);
            assertEquals(1000, loop.interval);
            app.handleAction(GameAction.TICK);
            app.handleAction(GameAction.DOWN);
            assertEquals(row + 2, engine.state().currentRow());
            assertEquals(2, engine.state().score());
            assertEquals(2, renders.getLast().score());
            app.pauseGame();
            app.pauseGame();
            assertEquals(GamePhase.PAUSED, engine.state().phase());
            assertEquals(1, loop.pauses);
            app.handleAction(GameAction.TICK);
            assertEquals(row + 2, engine.state().currentRow());
            app.resumeGame();
            app.resumeGame();
            assertEquals(1, loop.resumes);
            app.handleAction(GameAction.TICK);
            assertEquals(3, engine.state().score());
            app.showSettings();
            assertEquals(1, loop.stops);
            assertEquals(GamePhase.PAUSED, engine.state().phase());
            app.handleAction(GameAction.DOWN);
            assertEquals(3, engine.state().score());
            app.startGame();
            assertEquals(0, engine.state().score());
            app.exit();
            assertEquals(3, loop.stops);
            int spawned = engine.state().spawnedPieces();
            app.startGame();
            assertEquals(AppState.EXIT, app.state());
            assertEquals(spawned, engine.state().spawnedPieces());
        });
    }
    @Test void realEngineGameOverRegistersFinalScoreOnceAndRetriesSaveFailure() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GameEngine engine = new GameEngine(new Random(21));
            Loop loop = new Loop();
            ScoreRepository repository = new ScoreRepository(directory.resolve("scores")) {
                // 첫 저장만 실패시켜 입력 대기를 유지한 뒤 같은 점수로 재시도하는지 확인한다.
                boolean fail = true;
                @Override public void save(List<ScoreEntry> entries) {
                    if (fail) { fail = false; throw new UncheckedIOException(new IOException("denied")); }
                    super.save(entries);
                }
            };
            AppController app = app(engine, loop, new ArrayList<>(), repository);
            app.startGame();
            for (int i = 0; i < 100 && !engine.isGameOver(); i++) app.handleAction(GameAction.HARD_DROP);
            assertTrue(engine.isGameOver());
            assertEquals(AppState.GAME_OVER, app.state());
            assertEquals(3, loop.stops);
            long score = engine.state().score();
            assertTrue(score > 0);
            assertTrue(app.snapshot().awaitingScoreName());
            app.registerScore("player");
            assertNotNull(app.snapshot().persistenceError());
            assertTrue(app.snapshot().awaitingScoreName());
            assertEquals(new ScoreEntry("player", score), app.pendingScore());
            assertTrue(app.retryScoreSave());
            assertNull(app.pendingScore());
            assertNull(app.snapshot().persistenceError());
            assertFalse(app.snapshot().awaitingScoreName());
            app.handleAction(GameAction.TICK);
            app.registerScore("duplicate");
            assertEquals(List.of(new ScoreEntry("player", score)), repository.load());
            assertEquals(score, app.snapshot().highScore());
            assertEquals("player", app.snapshot().lastRegisteredScore().name());
            app.resetScores();
            assertTrue(repository.load().isEmpty());
        });
    }
    @Test void pendingRecordSurvivesMenuAndBlocksRestartExitAndSkippingUntilSaveSucceeds() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            class FailingRepository extends ScoreRepository {
                boolean blocked = true;
                FailingRepository() { super(directory.resolve("pending-scores")); }
                @Override public void save(List<ScoreEntry> entries) {
                    if (blocked) throw new UncheckedIOException(new IOException("locked"));
                    super.save(entries);
                }
            }
            FailingRepository repository = new FailingRepository();
            GameEngine engine = new GameEngine(new Random(21));
            Loop loop = new Loop();
            AtomicInteger exits = new AtomicInteger();
            AppController app = new AppController(exits::incrementAndGet, engine, loop,
                    value -> loop.interval = value, ignored -> {}, new ScoreBoardService(repository),
                    null, ignored -> {});
            app.startGame();
            for (int i = 0; i < 100 && !engine.isGameOver(); i++) app.handleAction(GameAction.HARD_DROP);
            long finalScore = engine.state().score();
            app.registerScore("player");
            ScoreEntry pending = app.pendingScore();
            assertEquals(new ScoreEntry("player", finalScore), pending);
            app.showStartMenu();
            app.startGame();
            assertEquals(AppState.START_MENU, app.state());
            assertEquals(GamePhase.GAME_OVER, engine.state().phase());
            assertEquals(finalScore, engine.state().score());
            app.skipScoreRegistration();
            app.exit();
            assertEquals(0, exits.get());
            assertEquals(pending, app.pendingScore());
            assertTrue(app.snapshot().awaitingScoreName());
            // 재호출에 다른 이름이 들어와도 최초 저장 실패한 기록의 이름을 바꾸지 않는다.
            app.registerScore("different");
            assertEquals(pending, app.pendingScore());
            assertFalse(app.retryScoreSave());
            repository.blocked = false;
            assertTrue(app.retryScoreSave());
            assertTrue(app.retryScoreSave());
            assertEquals(List.of(pending), repository.load());
            assertNull(app.snapshot().persistenceError());
            app.exit();
            assertEquals(1, exits.get());
        });
    }
    @Test void firstSpawnFailureStopsAndOffersZeroScoreRegistration() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            PieceSpawnPolicy impossible = new PieceSpawnPolicy() {
                // 첫 생성부터 실패하는 경우를 재현해 루프를 시작하지 않는지 확인한다.
                public int startingRow(Tetromino piece) { return 100; }
                public int startingColumn(Tetromino piece) { return 0; }
            };
            GameEngine engine = new GameEngine(PieceQueue::new, impossible);
            Loop loop = new Loop();
            AppController app = app(engine, loop, new ArrayList<>(), new ScoreRepository(directory.resolve("scores")));
            app.startGame();
            assertEquals(0, loop.starts);
            assertEquals(AppState.GAME_OVER, app.state());
            assertEquals(3, loop.stops);
            assertTrue(app.snapshot().awaitingScoreName());
            app.skipScoreRegistration();
            assertFalse(app.snapshot().awaitingScoreName());
        });
    }
    @Test void lineClearThresholdUpdatesLevelIntervalAndNextDropBonus() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Random onlyO = new Random(0) {
                @Override public int nextInt(int bound) { return PieceType.O.ordinal(); }
            };
            GameEngine engine = new GameEngine(onlyO);
            Loop loop = new Loop();
            List<GameState> renders = new ArrayList<>();
            AppController app = app(engine, loop, renders, new ScoreRepository(directory.resolve("scores")));
            app.startGame();
            for (int piece = 0; piece < 25; piece++) {
                // O 블록 다섯 개를 두 칸씩 나란히 놓으면 두 줄이 동시에 삭제된다.
                int target = (piece % 5) * 2;
                int moves = Math.abs(target - engine.state().currentColumn());
                GameAction direction = target < engine.state().currentColumn() ? GameAction.LEFT : GameAction.RIGHT;
                for (int i = 0; i < moves; i++) app.handleAction(direction);
                app.handleAction(GameAction.HARD_DROP);
            }
            assertEquals(10, engine.state().totalClearedRows());
            assertEquals(26, engine.state().spawnedPieces());
            assertEquals(1950, engine.state().score(), "450 낙하 + 5회 두 줄 삭제 보너스 1500");
            assertEquals(2, engine.state().level());
            assertEquals(900, loop.interval);
            assertEquals(2, renders.getLast().level());
            app.handleAction(GameAction.DOWN);
            assertEquals(1952, engine.state().score());
            app.startGame();
            assertEquals(1000, loop.interval);
            assertEquals(1, engine.state().level());
            app.exit();
        });
    }
    @Test void settingsAreLoadedSavedAppliedAndResetWithoutClearingRankings() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GameSettings initial = new GameSettings();
            GameSettings replacement = new GameSettings();
            SettingsService service = new SettingsService() {
                GameSettings current = initial;
                @Override public GameSettings current() { return current; }
                @Override public void update(GameSettings next) { current = next; }
                @Override public void restoreDefaults() { current = initial; }
            };
            List<GameSettings> applied = new ArrayList<>();
            ScoreBoardService scores = new ScoreBoardService(new ScoreRepository(directory.resolve("scores")));
            scores.register("old", 50);
            Loop loop = new Loop();
            AppController app = new AppController(() -> {}, new GameEngine(), loop,
                    value -> loop.interval = value, ignored -> {}, scores, service, applied::add);
            app.start();
            assertSame(initial, app.snapshot().settings());
            app.updateSettings(replacement);
            assertSame(replacement, applied.getLast());
            app.restoreDefaultSettings();
            assertSame(initial, applied.getLast());
            assertEquals(50, app.snapshot().highScore());
            app.exit();
        });
    }
    @Test void rejectsOffEdtUseAndQuitIsIdempotent() throws Exception {
        assertThrows(IllegalStateException.class, () -> new AppController(() -> {}));
        SwingUtilities.invokeAndWait(() -> {
            AtomicInteger exits = new AtomicInteger();
            Loop loop = new Loop();
            AppController app = new AppController(exits::incrementAndGet, new GameEngine(), loop,
                    ignored -> {}, ignored -> {}, new ScoreBoardService(new ScoreRepository(directory.resolve("scores"))),
                    null, ignored -> {});
            app.handleAction(GameAction.QUIT);
            app.handleAction(GameAction.QUIT);
            assertEquals(1, exits.get());
        });
    }
}
