package tetris.ui;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tetris.app.AppController;
import tetris.game.GameAction;
import tetris.game.GameEngine;
import tetris.loop.GameLoop;
import tetris.scoreboard.ScoreBoardService;
import tetris.scoreboard.ScoreRepository;
import tetris.settings.ColorVisionMode;
import tetris.settings.GameSettings;

/** 별도 작업으로 실행하는 앱 처리·화면 그리기 측정. 최소 사양 검증과는 구분한다. */
class GameplayPerformanceTest {
    @TempDir Path directory;

    @ParameterizedTest
    @CsvSource({"0,NORMAL", "1,NORMAL", "2,NORMAL", "0,MONOCHROME", "1,MONOCHROME", "2,MONOCHROME"})
    void actionSnapshotAndPaintingFitTheFastestGravityBudget(int preset, ColorVisionMode mode) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AppController app = new AppController(() -> {}, new GameEngine(new Random(21)),
                    new GameLoop(), null, null, new ScoreBoardService(new ScoreRepository(directory.resolve("scores"))),
                    null, ignored -> {});
            try {
                app.startGame();
                GameScreen screen = find(app.view(), GameScreen.class);
                screen.applySettings(new GameSettings(preset, mode, mode.defaultPatternsEnabled(), GameSettings.DEFAULT_KEY_BINDINGS));
                app.view().setSize(GameSettings.presetDimension(preset)); layout(app.view());
                BufferedImage image = new BufferedImage(screen.getWidth(), screen.getHeight(), BufferedImage.TYPE_INT_RGB);
                int componentsBefore = count(screen);
                long[] samples = new long[300];
                for (int frame = -50; frame < samples.length; frame++) {
                    long start = System.nanoTime();
                    app.handleAction(frame % 2 == 0 ? GameAction.LEFT : GameAction.RIGHT);
                    // 큐 변경과 여러 게임의 초기화도 부하에 포함한다.
                    if (frame % 25 == 0) app.handleAction(GameAction.HARD_DROP);
                    if (app.snapshot().game().phase() == tetris.game.GamePhase.GAME_OVER) app.startGame();
                    assertTrue(screen.getWidth() > 0 && screen.getHeight() > 0);
                    var graphics = image.createGraphics();
                    try { screen.paint(graphics); } finally { graphics.dispose(); }
                    if (frame >= 0) samples[frame] = System.nanoTime() - start;
                }
                Arrays.sort(samples);
                double p95 = samples[(int) Math.ceil(samples.length * 0.95) - 1] / 1_000_000.0;
                long heapUsed = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
                assertEquals(componentsBefore, count(screen), "Repeated gameplay must not accumulate preview components");
                assertTrue(p95 < 100, "95% of input-to-paint cycles must finish within the fastest 100ms gravity interval: " + p95);
                assertTrue(heapUsed < 256L * 1024 * 1024, "The measurement runs with a 256 MiB heap limit");
                String result = String.format(Locale.ROOT,
                        "preset=%d mode=%s samples=%d p95_ms=%.3f max_ms=%.3f heap_used_mib=%.2f components=%d%n",
                        preset, mode, samples.length, p95, samples[samples.length - 1] / 1_000_000.0,
                        heapUsed / 1048576.0, componentsBefore);
                System.out.print(result);
                try {
                    Path folder = Path.of("build", "reports", "gameplay-performance"); Files.createDirectories(folder);
                    Files.writeString(folder.resolve("preset-" + preset + "-" + mode + ".txt"), result);
                } catch (java.io.IOException ex) { throw new java.io.UncheckedIOException(ex); }
            } finally { app.exit(); }
        });
    }

    private static <T> T find(Component component, Class<T> type) {
        if (type.isInstance(component)) return type.cast(component);
        if (component instanceof Container container) for (Component child : container.getComponents()) {
            T result = find(child, type); if (result != null) return result;
        }
        return null;
    }
    private static void layout(Component component) {
        if (component instanceof Container container) {
            container.doLayout(); for (Component child : container.getComponents()) layout(child);
        }
    }
    private static int count(Component component) {
        int result = 1;
        if (component instanceof Container container) for (Component child : container.getComponents()) result += count(child);
        return result;
    }
}
