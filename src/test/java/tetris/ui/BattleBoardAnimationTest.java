package tetris.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tetris.game.BattleMode;
import tetris.game.Difficulty;
import tetris.game.GameAction;
import tetris.game.GameEngine;
import tetris.game.LanSnapshotCodec;
import tetris.game.PieceSpawnPolicy;
import tetris.game.PlayerSession;
import tetris.game.PlayerSnapshot;
import tetris.piece.PieceQueue;
import tetris.piece.PieceType;
import tetris.piece.Tetromino;

class BattleBoardAnimationTest {
    @ParameterizedTest @EnumSource(BattleMode.class)
    void bothNetworkViewsHighlightExactlyTheClearedRows(BattleMode mode) throws Exception {
        PlayerSession session = session(mode);
        for (int i = 0; i < 4; i++) session.apply(GameAction.HARD_DROP);
        PlayerSnapshot before = session.snapshot();
        session.apply(GameAction.HARD_DROP);
        PlayerSnapshot after = session.snapshot();
        PlayerSnapshot remote = transmit(after);
        SwingUtilities.invokeAndWait(() -> {
            BattleScreen host = new BattleScreen("나", "상대");
            BattleScreen guest = new BattleScreen("나", "상대");
            host.render(before, null);
            guest.render(null, null); // The pre-lock movement snapshot may have been coalesced.
            host.render(after, null);
            guest.render(null, remote);
            BattleBoardView localBoard = host.leftPanel().board();
            BattleBoardView remoteBoard = guest.rightPanel().board();
            assertArrayEquals(pixels(localBoard), pixels(remoteBoard));
            var bounds = localBoard.boardBounds();
            int cell = (bounds.height - 6) / 20;
            Color base = PieceStyle.colorFor(PieceType.O, tetris.settings.ColorVisionMode.NORMAL);
            Color highlight = new Color(Math.min(255, base.getRed() + 70),
                    Math.min(255, base.getGreen() + 70), Math.min(255, base.getBlue() + 70));
            BufferedImage image = paint(localBoard);
            assertEquals(highlight.getRGB(), image.getRGB(bounds.x + 7 + 4 * cell, bounds.y + 7 + 19 * cell));
            localBoard.removeNotify();
            remoteBoard.removeNotify();
            assertArrayEquals(pixels(localBoard), pixels(remoteBoard));
            assertEquals(AppTheme.GRID.getRGB(), paint(localBoard).getRGB(bounds.x + 7 + 4 * cell, bounds.y + 7 + 19 * cell));
        });
    }

    @Test
    void repeatedUpdatesDoNotRestartTheFlashAndNewInputIsVisibleDuringIt() throws Exception {
        PlayerSession session = session(BattleMode.NORMAL);
        for (int i = 0; i < 5; i++) session.apply(GameAction.HARD_DROP);
        PlayerSnapshot after = transmit(session.snapshot());
        CountDownLatch finished = new CountDownLatch(1);
        Timer[] poll = new Timer[1];
        BattleBoardView[] view = new BattleBoardView[1];
        SwingUtilities.invokeAndWait(() -> {
            view[0] = new BattleBoardView();
            view[0].render(after);
            int[] beforeInput = pixels(view[0]);
            session.apply(GameAction.RIGHT);
            view[0].render(session.snapshot());
            assertFalse(Arrays.equals(beforeInput, pixels(view[0])), "Next-piece input must paint during the flash");
            BattleBoardView expected = new BattleBoardView();
            expected.render(session.snapshot());
            expected.removeNotify();
            int[] finalPixels = pixels(expected);
            poll[0] = new Timer(20, event -> {
                view[0].render(session.snapshot());
                if (Arrays.equals(finalPixels, pixels(view[0]))) {
                    ((Timer) event.getSource()).stop();
                    finished.countDown();
                }
            });
            poll[0].start();
        });
        try { assertTrue(finished.await(3, TimeUnit.SECONDS), "Repeated BOARDS updates must let the flash expire"); }
        finally { SwingUtilities.invokeAndWait(() -> { poll[0].stop(); view[0].removeNotify(); }); }
    }

    @Test
    void leavingAndRestartingCancelTheFlashAndAllowAnotherClear() throws Exception {
        PlayerSession session = session(BattleMode.NORMAL);
        for (int i = 0; i < 5; i++) session.apply(GameAction.HARD_DROP);
        PlayerSnapshot after = transmit(session.snapshot());
        SwingUtilities.invokeAndWait(() -> {
            BattleBoardView view = new BattleBoardView();
            view.render(after);
            int[] highlighted = pixels(view);
            view.removeNotify();
            assertFalse(Arrays.equals(highlighted, pixels(view)));
            view.render(after);
            assertFalse(Arrays.equals(highlighted, pixels(view)), "Returning must not replay an already seen event");
            view.render(null);
            view.render(after);
            assertArrayEquals(highlighted, pixels(view));
            session.start(BattleMode.NORMAL, Difficulty.NORMAL);
            view.render(session.snapshot());
            for (int i = 0; i < 5; i++) session.apply(GameAction.HARD_DROP);
            view.render(session.snapshot());
            assertArrayEquals(highlighted, pixels(view));
            view.removeNotify();
        });
    }

    private static PlayerSession session(BattleMode mode) {
        AtomicInteger pieces = new AtomicInteger();
        PieceSpawnPolicy spawn = new PieceSpawnPolicy() {
            @Override public int startingRow(Tetromino piece) { return 0; }
            @Override public int startingColumn(Tetromino piece) { return pieces.getAndIncrement() % 5 * 2; }
        };
        PlayerSession session = new PlayerSession(new GameEngine(
                () -> {
                    pieces.set(0);
                    return PieceQueue.generatedBy(() -> new Tetromino(PieceType.O), 1);
                }, spawn));
        session.start(mode, Difficulty.NORMAL);
        return session;
    }

    private static PlayerSnapshot transmit(PlayerSnapshot snapshot) throws Exception {
        var bytes = new ByteArrayOutputStream();
        LanSnapshotCodec.write(new DataOutputStream(bytes), snapshot);
        return LanSnapshotCodec.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())), snapshot.mode());
    }

    private static BufferedImage paint(BattleBoardView view) {
        view.setSize(272, 532);
        BufferedImage image = new BufferedImage(272, 532, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        try { view.paint(graphics); } finally { graphics.dispose(); }
        return image;
    }

    private static int[] pixels(BattleBoardView view) {
        return paint(view).getRGB(0, 0, 272, 532, null, 0, 272);
    }
}
