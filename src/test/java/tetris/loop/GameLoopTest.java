package tetris.loop;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.SwingUtilities;
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
}
