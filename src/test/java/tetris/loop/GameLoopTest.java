package tetris.loop;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.junit.jupiter.api.Test;

/** 실제 Swing 타이머의 스레드·간격 변경·정지·재개를 검증한다. */
class GameLoopTest {
    @Test
    void ticksRunOnEventThreadAndStopDuringPauseAndAfterStop() throws Exception {
        GameLoop loop = new GameLoop();
        CountDownLatch firstTick = new CountDownLatch(1);
        AtomicBoolean eventThread = new AtomicBoolean();
        try {
            SwingUtilities.invokeAndWait(() -> {
                loop.setIntervalMillis(20);
                loop.setTickListener(() -> {
                    eventThread.set(SwingUtilities.isEventDispatchThread());
                    firstTick.countDown();
                });
                loop.stop();
                loop.start();
                loop.start();
            });
            assertTrue(firstTick.await(5, TimeUnit.SECONDS));
            assertTrue(eventThread.get());
            CountDownLatch resumedTick = new CountDownLatch(1);
            SwingUtilities.invokeAndWait(() -> {
                loop.pause();
                loop.pause();
                loop.setTickListener(resumedTick::countDown);
            });
            assertFalse(resumedTick.await(120, TimeUnit.MILLISECONDS), "일시정지 중 낙하하면 안 된다.");
            SwingUtilities.invokeAndWait(() -> {
                loop.resume();
                loop.resume();
            });
            assertTrue(resumedTick.await(5, TimeUnit.SECONDS));
            CountDownLatch stoppedTick = new CountDownLatch(1);
            SwingUtilities.invokeAndWait(() -> {
                loop.stop();
                loop.stop();
                loop.setTickListener(stoppedTick::countDown);
                loop.resume();
            });
            assertFalse(stoppedTick.await(120, TimeUnit.MILLISECONDS), "종료한 루프는 재개하면 안 된다.");
        } finally {
            SwingUtilities.invokeAndWait(loop::stop);
        }
    }

    @Test
    void intervalChangeReachesTheRunningTimer() throws Exception {
        GameLoop loop = new GameLoop();
        CountDownLatch tick = new CountDownLatch(1);
        try {
            SwingUtilities.invokeAndWait(() -> {
                loop.setTickListener(tick::countDown);
                loop.setIntervalMillis(Integer.MAX_VALUE);
                loop.start();
                loop.setIntervalMillis(20);
            });
            assertTrue(tick.await(5, TimeUnit.SECONDS), "실행 중 바꾼 간격으로 낙하해야 한다.");
        } finally {
            SwingUtilities.invokeAndWait(loop::stop);
        }
    }

    @Test
    void invalidIntervalAndControlOutsideEventThreadAreRejected() throws Exception {
        GameLoop loop = new GameLoop();
        assertThrows(IllegalStateException.class, loop::start);
        assertThrows(IllegalStateException.class, loop::stop);
        assertThrows(IllegalStateException.class, loop::pause);
        assertThrows(IllegalStateException.class, loop::resume);
        assertThrows(IllegalStateException.class, () -> loop.setIntervalMillis(20));
        SwingUtilities.invokeAndWait(() -> {
            assertThrows(IllegalArgumentException.class, () -> loop.setIntervalMillis(0));
            assertThrows(IllegalArgumentException.class,
                    () -> loop.setIntervalMillis((long) Integer.MAX_VALUE + 1));
            loop.stop();
        });
    }

    @Test void acceleratedTimerKeepsFallingDuringRepeatedUnchangedIntervalRequests() throws Exception {
        GameLoop loop = new GameLoop();
        CountDownLatch ticks = new CountDownLatch(3);
        AtomicInteger requests = new AtomicInteger();
        Timer input = new Timer(10, event -> {
            requests.incrementAndGet();
            loop.setIntervalMillis(100);
        });
        try {
            SwingUtilities.invokeAndWait(() -> {
                loop.setTickListener(ticks::countDown);
                loop.start();
                loop.setIntervalMillis(100); // 이미 실행 중인 1초 타이머에도 새 간격을 반영한다.
                input.start();
            });
            assertTrue(ticks.await(2, TimeUnit.SECONDS),
                    "100ms gravity must produce three ticks even while inputs repeatedly request the same interval");
            assertTrue(requests.get() >= 3);
        } finally {
            SwingUtilities.invokeAndWait(() -> { input.stop(); loop.stop(); });
        }
    }
}
