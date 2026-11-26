package tetris.loop;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;

import tetris.game.GameAction;
import tetris.game.PlayerSession;

/**
 * 대전 모드에서 플레이어 한 명의 게임 진행을 담당한다.
 * 플레이어마다 독립적인 스레드를 사용한다.
 */
public final class PlayerGameThread {

    private final PlayerSession session;
    private final Runnable updateListener;

    private final BlockingQueue<GameAction> inputQueue =
            new LinkedBlockingQueue<>();

    private final Set<GameAction> pressedKeys =
            ConcurrentHashMap.newKeySet();

    private volatile boolean running;
    private volatile boolean paused;

    private Thread worker;

    private static final long REPEAT_INTERVAL = 100;

    public PlayerGameThread(
            PlayerSession session,
            Runnable updateListener) {

        this.session = Objects.requireNonNull(session);
        this.updateListener =
                Objects.requireNonNull(updateListener);
    }

    // 게임 스레드 시작
    public synchronized void start() {

        if (worker != null && worker.isAlive()) return;

        running = true;
        paused = false;

        inputQueue.clear();
        pressedKeys.clear();

        worker = new Thread(
                this::runLoop, "PlayerGameThread");

        worker.setDaemon(true);
        worker.start();
    }

    // 게임 스레드 정지
    public synchronized void stop() {

        running = false;
        paused = false;

        inputQueue.clear();
        pressedKeys.clear();

        if (worker != null) {
            worker.interrupt();
        }
    }

    // 일시정지
    public void pause() {
        paused = true;
        inputQueue.clear();
        pressedKeys.clear();
    }

    // 게임 재개
    public void resume() {
        paused = false;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isPaused() {
        return paused;
    }

    // 입력 행동을 큐에 추가
    public void submit(GameAction action) {

        Objects.requireNonNull(action);

        if (running && !paused) {
            inputQueue.offer(action);
        }
    }

    // 키를 눌렀을 때
    public void press(GameAction action) {

        if (!running || paused) return;

        if (pressedKeys.add(action)) {
            submit(action);
        }
    }

    // 키를 뗐을 때
    public void release(GameAction action) {
        pressedKeys.remove(action);
    }

    // 눌린 키 상태 초기화
    public void clearPressedKeys() {
        pressedKeys.clear();
    }

    // 플레이어별 게임 루프
    private void runLoop() {

        long lastGravity = System.nanoTime();
        long lastRepeat = System.nanoTime();

        try {

            while (running && !session.isGameOver()) {

                if (paused) {

                    Thread.sleep(20);

                    lastGravity = System.nanoTime();
                    lastRepeat = System.nanoTime();

                    continue;
                }

                GameAction action =
                        inputQueue.poll(
                                10, TimeUnit.MILLISECONDS);

                boolean changed = false;

                if (action != null && running && !paused) {

                    session.apply(action);
                    changed = true;
                }

                if (!running || paused) continue;

                long now = System.nanoTime();

                // 키를 누르고 있는 동안 반복 이동
                if (TimeUnit.NANOSECONDS.toMillis(
                        now - lastRepeat) >= REPEAT_INTERVAL) {

                    for (GameAction key : pressedKeys) {

                        if (!running || paused
                                || session.isGameOver()) {
                            break;
                        }

                        if (key == GameAction.LEFT
                                || key == GameAction.RIGHT
                                || key == GameAction.DOWN) {

                            session.apply(key);
                            changed = true;
                        }
                    }

                    lastRepeat = now;
                }

                // 현재 레벨에 따른 자동 낙하
                if (!session.isGameOver()
                        && TimeUnit.NANOSECONDS.toMillis(
                                now - lastGravity)
                        >= session.gravityIntervalMillis()) {

                    session.apply(GameAction.TICK);

                    lastGravity = now;
                    changed = true;
                }

                // 화면 갱신은 Swing EDT에서 실행
                if (changed && running) {
                    SwingUtilities.invokeLater(updateListener);
                }
            }

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

        } finally {

            running = false;
            inputQueue.clear();
            pressedKeys.clear();

            SwingUtilities.invokeLater(updateListener);
        }
    }
}