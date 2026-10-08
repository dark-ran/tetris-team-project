package tetris.loop;

import java.util.Objects;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Swing 이벤트 처리 스레드에서 자동 낙하와 입력을 순서대로 처리하도록 타이머를 제어한다. */
public class GameLoop {
    private enum Phase { STOPPED, RUNNING, PAUSED }

    private final Timer timer = new Timer(1000, event -> tick());
    private Runnable tickListener;
    private Phase phase = Phase.STOPPED;

    /** 자동 낙하가 발생할 때 호출할 앱의 처리 메서드를 등록한다. */
    public void setTickListener(Runnable listener) {
        tickListener = Objects.requireNonNull(listener);
    }

    /** 간격 변경과 재개 후에는 한 번의 전체 간격이 지난 뒤 다음 낙하가 발생한다. */
    public void setIntervalMillis(long millis) {
        requireEventThread();
        if (millis < 1 || millis > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("낙하 간격은 1 이상인 정수 밀리초 범위여야 합니다.");
        }
        int delay = (int) millis;
        if (timer.getDelay() == delay) return;
        timer.setDelay(delay);
        timer.setInitialDelay(delay);
        if (phase == Phase.RUNNING) timer.restart();
    }

    public void start() {
        requireEventThread();
        if (phase == Phase.RUNNING) return;
        phase = Phase.RUNNING;
        timer.restart();
    }

    public void stop() {
        requireEventThread();
        timer.stop();
        phase = Phase.STOPPED;
    }

    public void pause() {
        requireEventThread();
        if (phase != Phase.RUNNING) return;
        timer.stop();
        phase = Phase.PAUSED;
    }

    public void resume() {
        requireEventThread();
        if (phase != Phase.PAUSED) return;
        phase = Phase.RUNNING;
        timer.restart();
    }

    private void tick() {
        if (phase == Phase.RUNNING && tickListener != null) tickListener.run();
    }

    private static void requireEventThread() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("게임 루프는 Swing 이벤트 처리 스레드에서 제어해야 합니다.");
        }
    }
}
