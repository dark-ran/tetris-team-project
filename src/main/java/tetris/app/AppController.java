package tetris.app;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import tetris.game.GameAction;
import tetris.game.GameActionResult;
import tetris.game.GameEngine;
import tetris.game.GamePhase;
import tetris.game.GameState;
import tetris.loop.GameLoop;
import tetris.input.InputHandler;
import tetris.input.KeyMapper;
import tetris.rule.ScoreSystem;
import tetris.rule.SpeedSystem;
import tetris.scoreboard.ScoreBoardService;
import tetris.scoreboard.ScoreEntry;
import tetris.settings.GameSettings;
import tetris.settings.SettingsService;
import tetris.ui.AppTheme;
import tetris.ui.GameOverScreen;
import tetris.ui.GameScreen;
import tetris.ui.ScoreboardScreen;
import tetris.ui.SettingsScreen;
import tetris.ui.StartMenu;
import tetris.ui.WindowSizeConfirmation;
import tetris.ui.WindowSizeConfirmationDialog;

/**
 * 입력·자동 낙하·화면 전환을 연결하고, 엔진 결과에 점수·레벨과 게임오버 후처리를 반영한다.
 * Swing EDT에서 호출을 직렬 처리하므로 입력과 타이머가 동시에 엔진을 변경하지 않는다.
 */
public class AppController {
    static { AppTheme.install(); }
    private final CardLayout layout = new CardLayout();
    private final JPanel view = new JPanel(layout);
    private final Runnable onExit;
    private final StartMenu startMenu;
    private final GameScreen gameScreen;
    private final SettingsScreen settingsScreen;
    private final ScoreboardScreen scoreboardScreen;
    private final GameOverScreen gameOverScreen;
    private final GameEngine engine;
    private final GameLoop loop;
    private final KeyMapper keyMapper = new KeyMapper();
    private final InputHandler inputHandler = new InputHandler(keyMapper);
    private final LongConsumer intervalUpdater;
    private final Consumer<GameState> renderer;
    private final ScoreBoardService scores;
    private final SettingsService settingsService;
    private final Consumer<GameSettings> settingsApplier;
    private final ScoreSystem scoreSystem = new ScoreSystem();
    private final SpeedSystem speedSystem = new SpeedSystem();
    private AppState state = AppState.START_MENU;
    private long interval = SpeedSystem.INITIAL_INTERVAL_MILLIS;
    private boolean gameOverHandled;
    private boolean awaitingScoreName;
    private ScoreEntry pendingScore;
    private ScoreEntry lastRegisteredScore;
    private GameSettings settings;
    private String persistenceError;
    private boolean settingsFromGame;
    private final WindowSizeConfirmation windowSizeConfirmation;

    public AppController(Runnable onExit) {
        this(onExit, new GameEngine(), null, null, null, new ScoreBoardService(), new SettingsService(), null);
    }

    /**
     * 연결된 루프는 생성 직후 정지 상태여야 한다. Tick과 Input은 handleAction으로 전달한다.
     * renderer는 GameScreen::render, intervalUpdater는 루프의 간격 갱신 메서드를 전달한다.
     * 테스트 등에서 설정 연결이 필요하지 않으면 settingsService를 null로 생략할 수 있다.
     */
    public AppController(Runnable onExit, GameEngine engine, GameLoop loop,
            LongConsumer intervalUpdater, Consumer<GameState> renderer,
            ScoreBoardService scores, SettingsService settingsService,
            Consumer<GameSettings> settingsApplier) {
        this(onExit, engine, loop, intervalUpdater, renderer, scores, settingsService,
                settingsApplier, new WindowSizeConfirmationDialog());
    }

    public AppController(Runnable onExit, GameEngine engine, GameLoop loop,
            LongConsumer intervalUpdater, Consumer<GameState> renderer,
            ScoreBoardService scores, SettingsService settingsService,
            Consumer<GameSettings> settingsApplier, WindowSizeConfirmation confirmation) {
        requireEdt();
        windowSizeConfirmation = Objects.requireNonNull(confirmation);
        this.onExit = Objects.requireNonNull(onExit);
        this.engine = Objects.requireNonNull(engine);
        this.scores = Objects.requireNonNull(scores);
        this.settingsService = settingsService;
        this.settingsApplier = settingsApplier != null ? settingsApplier : this::applySettingsToModules;
        this.loop = loop != null ? loop : new GameLoop();
        this.intervalUpdater = intervalUpdater != null ? intervalUpdater : this.loop::setIntervalMillis;

        startMenu = register(AppState.START_MENU,
                new StartMenu(this::startGame, this::showSettings, this::showScoreboard, this::exit));
        gameScreen = register(AppState.GAME,
                new GameScreen(this::showGameMenu, this::showStartMenu, this::exit));

        settingsScreen = register(AppState.SETTINGS, new SettingsScreen(this::closeSettings));
        scoreboardScreen = register(AppState.SCOREBOARD, new ScoreboardScreen(this::showStartMenu));

        gameOverScreen = register(AppState.GAME_OVER,
                new GameOverScreen(this::startGame, this::showScoreboard, this::showStartMenu, this::exit));
        inputHandler.setActionListener(this::handleAction);
        this.loop.setTickListener(() -> handleAction(GameAction.TICK));
        gameScreen.setInputHandler(inputHandler);
        gameScreen.setMenuActions(this::resumeGame, this::showSettings, this::showStartMenu, this::exit);
        if (settingsService != null) {
            settingsScreen.setOnSaveValues(values -> updateSettings(
                    new GameSettings(values.preset(), values.colorVisionMode(), values.patternsEnabled(), values.keys())));
            settingsScreen.setOnWindowSizePreview(this::previewWindowSize);
            settingsScreen.setOnRestoreDefaults(this::restoreDefaultsFromSettingsScreen);
        }
        settingsScreen.setOnResetScores(this::resetScores);
        gameOverScreen.setOnRegister(this::registerScore);
        gameOverScreen.setOnSkip(this::skipScoreRegistration);
        gameOverScreen.setOnRetrySave(this::retryScoreSave);
        gameOverScreen.setSaveErrorSupplier(() -> persistenceError);
        this.renderer = renderer != null ? renderer : gameScreen::render;
        persistenceError = scores.loadWarning();
    }

    public JPanel view() { return view; }
    public AppState state() { return state; }
    public ScoreEntry pendingScore() { requireEdt(); return pendingScore; }
    /** 같은 조회 시점의 게임 상태와 앱 후처리 정보를 UI에 제공한다. */
    public AppState.Snapshot snapshot() {
        requireEdt();
        return new AppState.Snapshot(state, engine.state(), interval, scores.highScore(),
                awaitingScoreName, lastRegisteredScore, settings, persistenceError);
    }
    public void start() {
        requireEdt();
        if (state == AppState.EXIT) return;
        if (settingsService != null) applyCurrentSettings();
        showStartMenu();
    }
    public void showStartMenu() {
        settingsFromGame = false;
        activate(AppState.START_MENU, startMenu::showScreen);
    }
    /** 메뉴 복귀 전의 게임을 이어 하지 않고 점수·진행·랭킹 입력 대기를 초기화한다. */
    public void startGame() {
        requireEdt();
        if (state == AppState.EXIT) return;
        if (!resolvePendingScore()) return;
        loop.stop();
        gameScreen.hideMenu();
        settingsFromGame = false;
        engine.newGame();
        gameOverHandled = false;
        awaitingScoreName = false;
        lastRegisteredScore = null;
        updateProgress(0);
        updateLoopInterval();
        activate(AppState.GAME, gameScreen::showScreen);
        renderer.accept(engine.state());
        if (engine.isGameOver()) finishGame();
        else loop.start();
    }
    public void showSettings() {
        requireEdt();
        if (state == AppState.EXIT) return;
        if (state == AppState.GAME) showGameMenu();
        settingsFromGame = state == AppState.GAME_MENU;
        activate(AppState.SETTINGS, settingsScreen::showScreen);
    }
    public void closeSettings() {
        requireEdt();
        if (state != AppState.SETTINGS) return;
        if (settingsFromGame) {
            state = AppState.GAME_MENU;
            layout.show(view, AppState.GAME.name());
            gameScreen.showMenu();
        } else showStartMenu();
    }
    public void showGameMenu() {
        requireEdt();
        if (state != AppState.GAME && state != AppState.GAME_MENU) return;
        if (state == AppState.GAME) {
            pauseEngine();
            loop.pause();
            renderer.accept(engine.state());
        }
        state = AppState.GAME_MENU;
        layout.show(view, AppState.GAME.name());
        gameScreen.showMenu();
    }
    public void showScoreboard() {

        activate(AppState.SCOREBOARD, () -> scoreboardScreen.showScreen(scores.top(), lastRegisteredScore));
    }

    /** 기존 종료 화면 미리보기. 실제 게임오버 후처리는 finishGame에서만 수행한다. */
    public void showGameOver() { activate(AppState.GAME_OVER, gameOverScreen::showScreen); }
    public void showGameOver(long score) {
        if (score < 0) throw new IllegalArgumentException("score must be nonnegative");
        activate(AppState.GAME_OVER, () -> gameOverScreen.showScreen(score));
    }

    /** Input의 GameAction과 루프의 TICK을 동일한 순서로 엔진에 전달하는 진입점. */
    public void handleAction(GameAction action) {
        requireEdt();
        Objects.requireNonNull(action);
        if (state == AppState.EXIT) return;
        if (action == GameAction.QUIT) { exit(); return; }
        if (action == GameAction.TOGGLE_PAUSE) { if (state == AppState.GAME) showGameMenu(); else if (state == AppState.GAME_MENU) resumeGame(); return; }
        if (state != AppState.GAME) return;
        GameState before = engine.state();
        GameActionResult result = engine.apply(action);

        long gained = (long) scoreSystem.onDrop(result.droppedRows(),
                interval < SpeedSystem.INITIAL_INTERVAL_MILLIS) + scoreSystem.onLineClear(result.clearedRows());
        updateProgress(Math.addExact(before.score(), gained));

        if (result.currentPhase() == GamePhase.PAUSED && result.previousPhase() == GamePhase.RUNNING) loop.pause();
        if (result.currentPhase() == GamePhase.RUNNING && result.previousPhase() == GamePhase.PAUSED) loop.resume();
        renderer.accept(engine.state());
        if (result.becameGameOver()) finishGame();
    }
    public void pauseGame() { showGameMenu(); }
    public void resumeGame() {
        requireEdt();
        if (state != AppState.GAME_MENU || engine.state().phase() != GamePhase.PAUSED) return;
        engine.apply(GameAction.TOGGLE_PAUSE);
        state = AppState.GAME;
        gameScreen.hideMenu();
        layout.show(view, AppState.GAME.name());
        renderer.accept(engine.state());
        loop.resume();
    }
    /** 점수는 앱이 계산하고, 누적 생성·삭제 수와 실제 게임 상태는 엔진이 관리한다. */
    private void updateProgress(long score) {
        GameState current = engine.state();
        engine.updateScoreAndLevel(score, speedSystem.level(current.spawnedPieces(), current.totalClearedRows()));
        long nextInterval = speedSystem.gravityIntervalMillis(current.spawnedPieces(), current.totalClearedRows());
        // 입력마다 타이머를 재설정하면 낙하가 지연되므로 간격 변경 때만 적용
        if (nextInterval != interval) {
            interval = nextInterval;
            updateLoopInterval();
        }
    }
    private void updateLoopInterval() {

        intervalUpdater.accept(interval);
    }
    /** 한 게임당 한 번만 루프를 멈추고 최종 점수의 등록 여부를 결정한다. */
    private void finishGame() {
        if (gameOverHandled) return;
        gameOverHandled = true;
        loop.stop();
        awaitingScoreName = scores.qualifies(engine.state().score());

        activate(AppState.GAME_OVER,
                () -> gameOverScreen.showScreen(engine.state().score(), awaitingScoreName));
    }
    // 저장 실패한 기록이 있으면 이름을 다시 받지 않고 보관한 기록을 재시도한다.
    public void registerScore(String name) {
        requireEdt();
        if (state == AppState.EXIT || !awaitingScoreName) return;
        if (pendingScore == null) pendingScore = new ScoreEntry(name, engine.state().score());
        retryScoreSave();
    }
    // 성공하거나 저장 대기가 없으면 true. 실패 시 기록과 오류를 유지하고 false를 반환한다.
    public boolean retryScoreSave() {
        requireEdt();
        if (state == AppState.EXIT) return false;
        if (pendingScore == null) return true;
        try {
            ScoreEntry entry = pendingScore;
            scores.register(entry.name(), entry.score());
            // 영구 저장이 성공한 뒤에만 입력 대기를 해제하고 UI 강조용 기록을 확정한다.
            persistenceError = null;
            lastRegisteredScore = entry;
            pendingScore = null;
            awaitingScoreName = false;
            showScoreboard();
            return true;
        } catch (UncheckedIOException ex) {
            persistenceError = ex.getMessage();
            gameOverScreen.showSaveError(persistenceError);
            return false;
        }
    }
    private boolean resolvePendingScore() {
        return pendingScore == null || retryScoreSave();
    }
    public void skipScoreRegistration() {
        requireEdt();
        if (state == AppState.EXIT) return;
        if (pendingScore != null) return;
        awaitingScoreName = false;
        showScoreboard();
    }
    /** 기록만 초기화한다. */
    public void resetScores() {
        requireEdt();
        if (state == AppState.EXIT) return;
        if (!resolvePendingScore()) return;
        try {
            scores.reset();
            persistenceError = null;
            lastRegisteredScore = null;
            if (state == AppState.SCOREBOARD) showScoreboard();
        } catch (UncheckedIOException ex) { persistenceError = ex.getMessage(); }
    }
    /** 설정 서비스가 변경을 처리한 뒤 서비스의 현재 값을 관련 모듈에 전달 */
    public void updateSettings(GameSettings next) {
        requireEdt();
        if (state == AppState.EXIT) return;
        requireSettingsService().update(Objects.requireNonNull(next));
        applyCurrentSettings();
    }
    public void restoreDefaultSettings() {
        requireEdt();
        if (state == AppState.EXIT) return;
        requireSettingsService().restoreDefaults();
        applyCurrentSettings();
    }
    private SettingsService requireSettingsService() {
        if (settingsService == null) throw new IllegalStateException("SettingsService connection is required");
        return settingsService;
    }
    private void applyCurrentSettings() {
        applySettings(Objects.requireNonNull(requireSettingsService().current()));
    }
    private void applySettings(GameSettings next) {
        if (state == AppState.EXIT || settings == next) return;

        settingsApplier.accept(Objects.requireNonNull(next));
        settings = next;
    }
    private void applySettingsToModules(GameSettings next) {
        keyMapper.updateBindings(next.keyBindings());
        gameScreen.applySettings(next);
        settingsScreen.showSettings(next);
        gameScreen.render(engine.state());
        resizeWindow(next.windowDimension());
    }

    private boolean previewWindowSize(int preset) {
        GameSettings current = requireSettingsService().current();
        SettingsScreen.Values draft = settingsScreen.values();
        if (!confirmAndSaveWindowSize(current.withWindowSizePreset(preset))) return false;
        settingsScreen.showValues(draft);
        return true;
    }

    private void restoreDefaultsFromSettingsScreen() {
        GameSettings defaults = GameSettings.defaultSettings();
        if (requireSettingsService().current().windowSizePreset() == defaults.windowSizePreset()) {
            restoreDefaultSettings();
        } else if (!confirmAndSaveWindowSize(defaults)) {
            settingsScreen.showMessage("기본값 복구를 취소했습니다. 이전 창 크기를 유지합니다.");
        }
    }

    private boolean confirmAndSaveWindowSize(GameSettings next) {
        Window window = SwingUtilities.getWindowAncestor(view);
        Dimension previous = window != null ? window.getSize() : view.getPreferredSize();
        Dimension proposed = next.windowDimension();
        resizeWindow(proposed);
        try {
            if (!windowSizeConfirmation.confirm(view, new Dimension(previous), new Dimension(proposed))) {
                resizeWindow(previous); return false;
            }
            updateSettings(next);
            return true;
        } catch (RuntimeException ex) { resizeWindow(previous); throw ex; }
    }

    private void resizeWindow(Dimension dimension) {
        Window window = SwingUtilities.getWindowAncestor(view);
        if (window != null) window.setSize(dimension);
        else view.setPreferredSize(dimension);
        view.revalidate();
    }
    public void exit() {
        requireEdt();
        if (state == AppState.EXIT) return;
        // 저장 실패 상태에서 종료하거나 새 게임으로 넘어가 대기 기록을 잃지 않게 한다.
        if (!resolvePendingScore()) return;
        loop.stop();
        pauseEngine();
        state = AppState.EXIT;
        awaitingScoreName = false;
        onExit.run();
    }
    private void pauseEngine() {
        if (engine.state().phase() == GamePhase.RUNNING) engine.apply(GameAction.TOGGLE_PAUSE);
    }
    private <T extends JComponent> T register(AppState screen, T panel) {
        view.add(panel, screen.name());
        return panel;
    }
    private void activate(AppState next, Runnable onShow) {
        requireEdt();
        if (state == AppState.EXIT) return;
        if (next != AppState.GAME && !(next == AppState.SETTINGS && settingsFromGame)) {
            loop.stop();
            pauseEngine();
        }
        state = next;
        layout.show(view, next.name());
        onShow.run();
    }
    private static void requireEdt() {
        if (!SwingUtilities.isEventDispatchThread())
            throw new IllegalStateException("AppController must be used on the Swing EDT");
    }

}
