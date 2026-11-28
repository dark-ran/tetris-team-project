package tetris.game;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import tetris.rule.PieceRules;
import tetris.rule.ScoreSystem;
import tetris.rule.SpeedSystem;

/**
 * 대전 모드에서 플레이어 한 명의 게임 진행 단위. 엔진 하나와 점수·레벨·낙하 간격 계산을 묶는다.
 *
 * <p>플레이어마다 별도 스레드에서 호출해도 되도록 상태 변경과 조회를 이 객체 기준으로 동기화한다.
 * 상대에게 공격을 전달하는 리스너는 잠금을 푼 뒤에 호출하므로, 두 세션이 서로 공격해도 교착되지 않는다.
 * 공격을 받는 {@link #receiveAttack(List)}는 세션 잠금 없이 엔진의 대기열만 사용한다.</p>
 */
public final class PlayerSession {
    private final GameEngine engine;
    private final ScoreSystem scoreSystem = new ScoreSystem();
    private final SpeedSystem speedSystem = new SpeedSystem();
    private volatile Consumer<List<AttackLine>> attackListener;
    private BattleMode mode = BattleMode.NORMAL;
    private long interval = SpeedSystem.INITIAL_INTERVAL_MILLIS;

    public PlayerSession() {
        this(new GameEngine());
    }

    /** 생성 순서를 고정한 엔진 등 미리 준비한 엔진을 사용한다. */
    public PlayerSession(GameEngine engine) {
        this.engine = Objects.requireNonNull(engine, "게임 엔진이 필요합니다.");
    }

    /** 내가 공격 줄을 만들었을 때 호출된다. 보통 상대 세션의 receiveAttack이나 네트워크 전송을 연결한다. */
    public void setAttackListener(Consumer<List<AttackLine>> listener) {
        attackListener = listener;
    }

    /** 새 대전을 시작한다. 보드·점수·공격 대기열을 초기화한다. */
    public synchronized void start(BattleMode mode, Difficulty difficulty) {
        this.mode = Objects.requireNonNull(mode, "대전 모드가 필요합니다.");
        engine.newGame(mode.gameMode(), Objects.requireNonNull(difficulty, "난이도가 필요합니다."));
        interval = SpeedSystem.INITIAL_INTERVAL_MILLIS;
        updateProgress(0);
    }

    /**
     * 행동 하나를 처리하고 점수·레벨·낙하 간격을 갱신한다. 계산 규칙은 일반 게임과 같다.
     * 공격 줄이 생기면 잠금을 푼 뒤 공격 리스너에 전달한다.
     */
    public GameActionResult apply(GameAction action) {
        Objects.requireNonNull(action, "처리할 게임 행동이 필요합니다.");
        GameActionResult result;
        synchronized (this) {
            GameState before = engine.state();
            result = engine.apply(action);
            boolean awardsScore = before.currentPiece() != null
                    && PieceRules.forPiece(before.currentPiece()).awardsScore();
            long gained = awardsScore
                    ? (long) scoreSystem.onDrop(result.droppedRows(),
                            interval < SpeedSystem.INITIAL_INTERVAL_MILLIS)
                      + scoreSystem.onLineClear(result.clearedRows(), result.lineScoreMultiplier())
                    : 0;
            updateProgress(Math.addExact(before.score(), gained));
        }
        Consumer<List<AttackLine>> listener = attackListener;
        if (!result.attackLines().isEmpty() && listener != null) {
            listener.accept(result.attackLines());
        }
        return result;
    }

    /** 상대가 보낸 공격 줄을 받는다. 다른 스레드에서 호출해도 된다. */
    public int receiveAttack(List<AttackLine> lines) {
        return engine.receiveAttack(lines);
    }

    public synchronized PlayerSnapshot snapshot() {
        return PlayerSnapshot.of(engine.state(), mode, engine.lineClear());
    }

    /** 현재 레벨의 자동 낙하 간격(ms). 플레이어별 낙하 스레드가 사용한다. */
    public synchronized long gravityIntervalMillis() {
        return interval;
    }

    public synchronized boolean isGameOver() {
        return engine.isGameOver();
    }

    public synchronized long score() {
        return engine.state().score();
    }

    public synchronized BattleMode mode() {
        return mode;
    }

    private void updateProgress(long score) {
        GameState current = engine.state();
        engine.updateScoreAndLevel(score,
                speedSystem.level(current.spawnedPieces(), current.totalClearedRows()));
        interval = speedSystem.gravityIntervalMillis(current.spawnedPieces(),
                current.totalClearedRows(), engine.difficulty());
    }
}
