package tetris.app;

import java.awt.event.KeyEvent;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.swing.Timer;
import tetris.game.BattleMode;
import tetris.game.BattleOutcome;
import tetris.game.BattleRules;
import tetris.game.GameAction;
import tetris.game.PlayerSession;
import tetris.game.PlayerSnapshot;
import tetris.input.KeyMapper;
import tetris.loop.PlayerGameThread;
import tetris.settings.GameSettings;
import tetris.settings.SettingsService;
import tetris.ui.BattleScreen;

/** 한 PC에서 두 플레이어의 대전 진행을 관리한다. */
public final class BattleController {
    private final AppController app;
    private final SettingsService settingsService;
    private final KeyMapper keyMapper = new KeyMapper();

    private PlayerSession p1;
    private PlayerSession p2;
    private PlayerGameThread thread1;
    private PlayerGameThread thread2;

    private BattleScreen screen;
    private BattleMode mode;

    private final Timer timer;
    private boolean running;
    private boolean paused;
    private boolean escapePressed;

    private long startedAt;
    private long pausedAt;

    public BattleController(AppController app, SettingsService settingsService) {
        this.app = Objects.requireNonNull(app);
        this.settingsService = Objects.requireNonNull(settingsService);

        timer = new Timer(50, event -> refresh());
        app.setLocalBattleStarter(this::start);
    }

    /** 새로운 로컬 대전을 시작한다. */
    public void start(BattleMode mode, BattleScreen screen) {
        stop();

        this.mode = Objects.requireNonNull(mode);
        this.screen = Objects.requireNonNull(screen);

        GameSettings settings = settingsService.current();

        p1 = new PlayerSession();
        p2 = new PlayerSession();

        // 플레이어별 게임 초기화
        p1.start(mode, settings.difficulty());
        p2.start(mode, settings.difficulty());

        // 상대방에게 공격 줄 전달
        p1.setAttackListener(p2::receiveAttack);
        p2.setAttackListener(p1::receiveAttack);

        // 플레이어별 게임 스레드 생성
        thread1 = new PlayerGameThread(p1, this::refresh);
        thread2 = new PlayerGameThread(p2, this::refresh);

        keyMapper.updateBattleBindings(
                settings.p1KeyBindings(), settings.p2KeyBindings());

        screen.hideResult();
        screen.clearKeys();
        screen.setStatusMessage(null, false);
        screen.setMode(mode);

        bindKeys(settings);

        screen.setControlsGuide(
                controlsGuide(settings.p1KeyBindings()),
                controlsGuide(settings.p2KeyBindings()));

        running = true;
        paused = false;
        escapePressed = false;
        startedAt = System.nanoTime();

        screen.render(p1.snapshot(), p2.snapshot());

        if (mode.isTimeLimited()) {
            screen.setRemainingMillis(BattleRules.DEFAULT_TIME_LIMIT_MILLIS);
        } else {
            screen.hideTimer();
        }

        thread1.start();
        thread2.start();
        timer.start();

        screen.showScreen();
    }

    /** P1/P2 조작 키를 연결한다. */
    private void bindKeys(GameSettings settings) {
        for (int code : settings.p1KeyBindings().values()) {
            bindKey(code);
        }

        for (int code : settings.p2KeyBindings().values()) {
            bindKey(code);
        }

        // Esc로 두 플레이어를 동시에 일시정지
        screen.bindKey(KeyEvent.VK_ESCAPE, () -> {
            if (!escapePressed) {
                escapePressed = true;
                togglePause();
            }
        }, () -> escapePressed = false);
    }

    private void bindKey(int code) {
        KeyMapper.BattleInput input = keyMapper.mapBattle(code);
        if (input == null) return;

        PlayerGameThread thread = input.player() == 1
                ? thread1 : thread2;

        screen.bindKey(code,
                () -> thread.press(input.action()),
                () -> thread.release(input.action()));
    }

    /** 현재 설정에 맞는 조작 안내 문구를 만든다. */
    private String controlsGuide(java.util.Map<String, Integer> keys) {
        return keyText(keys, GameAction.LEFT) + "/"
                + keyText(keys, GameAction.RIGHT) + " 이동 · "
                + keyText(keys, GameAction.DOWN) + " 아래 · "
                + keyText(keys, GameAction.ROTATE_CLOCKWISE) + " 회전 · "
                + keyText(keys, GameAction.HARD_DROP) + " 바로 내리기";
    }

    private String keyText(java.util.Map<String, Integer> keys, GameAction action) {
        return KeyEvent.getKeyText(keys.get(action.name()));
    }

    /** 화면 갱신과 승패 판정은 Swing EDT에서 처리한다. */
    private void refresh() {
        if (!running) return;

        // 대전 화면을 벗어나면 게임 스레드 정지
        if (app.state() != AppState.BATTLE) {
            stop();
            return;
        }

        PlayerSnapshot left = p1.snapshot();
        PlayerSnapshot right = p2.snapshot();

        screen.render(left, right);

        boolean timeUp = false;

        if (mode.isTimeLimited()) {
            long now = paused ? pausedAt : System.nanoTime();
            long elapsed = TimeUnit.NANOSECONDS.toMillis(now - startedAt);

            long remaining = BattleRules.remainingMillis(
                    BattleRules.DEFAULT_TIME_LIMIT_MILLIS, elapsed);

            screen.setRemainingMillis(remaining);
            timeUp = remaining == 0;
        }

        BattleOutcome outcome = BattleRules.decide(left, right, timeUp);

        if (outcome.isFinished()) {
            finish(outcome);
        }
    }

    /** 두 플레이어의 게임을 동시에 일시정지하거나 재개한다. */
    public void togglePause() {
        if (!running) return;

        if (!paused) {
            paused = true;
            pausedAt = System.nanoTime();

            thread1.pause();
            thread2.pause();

            screen.setStatusMessage("일시정지 · Esc로 재개", false);
        } else {
            startedAt += System.nanoTime() - pausedAt;
            paused = false;

            thread1.resume();
            thread2.resume();

            screen.setStatusMessage(null, false);
        }
    }

    /** 대전 종료 및 결과 표시 */
    private void finish(BattleOutcome outcome) {
        BattleMode finishedMode = mode;

        stop();

        screen.setStatusMessage(null, false);

        screen.showResult(outcome,
                "다시 하기", () -> app.startLocalBattle(finishedMode),
                "시작 메뉴", app::showStartMenu);
    }

    /** 타이머와 두 플레이어의 게임 스레드를 정지한다. */
    public void stop() {
        running = false;
        paused = false;
        escapePressed = false;

        timer.stop();

        if (thread1 != null) thread1.stop();
        if (thread2 != null) thread2.stop();

        if (screen != null) screen.clearKeys();
    }
}