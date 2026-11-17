package tetris.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tetris.game.AttackLine;
import tetris.game.BattleMode;
import tetris.game.BattleOutcome;
import tetris.game.Difficulty;
import tetris.game.GameAction;
import tetris.game.PlayerSession;
import tetris.game.PlayerSnapshot;
import tetris.settings.ColorVisionMode;

@DisplayName("로컬·P2P 공통 대전 화면")
class BattleScreenTest {
    private static AttackLine line(int hole) {
        boolean[] filled = new boolean[10];
        Arrays.fill(filled, true);
        filled[hole] = false;
        return new AttackLine(filled);
    }

    private static PlayerSnapshot playing(int attacks) {
        PlayerSession session = new PlayerSession();
        session.start(BattleMode.ITEM, Difficulty.NORMAL);
        session.apply(GameAction.HARD_DROP);
        if (attacks > 0) {
            session.receiveAttack(List.of(line(0), line(1)));
            session.apply(GameAction.HARD_DROP);
            session.receiveAttack(java.util.Collections.nCopies(attacks, line(2)));
        }
        return session.snapshot();
    }

    private static PlayerSnapshot gameOver() {
        PlayerSession session = new PlayerSession();
        session.start(BattleMode.NORMAL, Difficulty.NORMAL);
        for (int count = 0; count < 200 && !session.isGameOver(); count++) session.apply(GameAction.HARD_DROP);
        return session.snapshot();
    }

    private static void paint(Component component, int width, int height) {
        component.setSize(width, height);
        layout(component);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try { component.paint(graphics); } finally { graphics.dispose(); }
    }

    private static void layout(Component component) {
        component.invalidate();
        if (component instanceof Container container) {
            container.doLayout();
            for (Component child : container.getComponents()) layout(child);
        }
    }

    private static void press(JComponent screen, KeyStroke stroke) {
        Object name = screen.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(stroke);
        screen.getActionMap().get(name).actionPerformed(new ActionEvent(screen, 0, "key"));
    }

    @Test
    @DisplayName("두 플레이어의 점수·넘어올 줄을 표시하고 회색 공격 줄과 게임오버까지 그린다")
    void rendersBothPlayers() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BattleScreen screen = new BattleScreen();
            PlayerSnapshot left = playing(3);
            PlayerSnapshot right = gameOver();
            screen.setAppearance(ColorVisionMode.DEUTAN, true);
            screen.render(left, right);
            assertEquals(Long.toString(left.score()), screen.leftPanel().scoreText());
            assertEquals("3 / 10", screen.leftPanel().attackCountText());
            assertEquals("0 / 10", screen.rightPanel().attackCountText());
            paint(screen, 884, 700);
            screen.render(null, null);
            assertEquals("0", screen.leftPanel().scoreText());
            paint(screen, 884, 700);
        });
    }

    @Test
    @DisplayName("가장 작은 창 크기에서도 두 보드가 잘리지 않는다")
    void boardsFitInTheSmallestWindow() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BattleScreen screen = new BattleScreen();
            screen.setSize(884, 700);
            layout(screen);
            for (BattlePlayerPanel panel : List.of(screen.leftPanel(), screen.rightPanel())) {
                BattleBoardView board = panel.board();
                Rectangle bounds = board.boardBounds();
                assertTrue(bounds.x >= 0 && bounds.y >= 0);
                assertTrue(bounds.x + bounds.width <= board.getWidth());
                assertTrue(bounds.y + bounds.height <= board.getHeight());
                assertTrue(board.getHeight() >= board.getMinimumSize().height);
            }
            assertTrue(screen.getPreferredSize().width <= 900);
        });
    }

    @Test
    @DisplayName("모드·남은 시간·상태 문구·이름을 표시한다")
    void showsHeaderInformation() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BattleScreen screen = new BattleScreen("나", "상대");
            screen.setMode(BattleMode.TIME_LIMIT);
            screen.setRemainingMillis(83_000);
            assertEquals("1:23", screen.timerText());
            screen.setRemainingMillis(9_001);
            assertEquals("0:10", screen.timerText());
            screen.setRemainingMillis(-5);
            assertEquals("0:00", screen.timerText());
            screen.setMode(BattleMode.NORMAL);
            assertEquals("", screen.timerText());
            screen.setStatusMessage("랙 걸린 상태", true);
            assertEquals("랙 걸린 상태", screen.statusText());
            screen.setStatusMessage(null, false);
            assertEquals("", screen.statusText());
            screen.setControlsGuide("A/D 이동", null);
            screen.setPlayerNames("P1", "P2");
            assertEquals("P1", screen.leftPanel().playerName());
            assertThrows(NullPointerException.class, () -> screen.setPlayerNames(null, "P2"));
        });
    }

    @Test
    @DisplayName("결과 화면은 승자·점수·버튼을 보여주고 떠 있는 동안 게임 키를 막는다")
    void resultOverlayShowsWinnerAndBlocksGameKeys() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BattleScreen screen = new BattleScreen();
            AtomicInteger presses = new AtomicInteger();
            AtomicInteger releases = new AtomicInteger();
            AtomicReference<String> clicked = new AtomicReference<>();
            screen.bindKey(KeyEvent.VK_ENTER, presses::incrementAndGet, releases::incrementAndGet);
            screen.bindKey(KeyEvent.VK_A, presses::incrementAndGet, null);
            press(screen, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false));
            press(screen, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, true));
            assertEquals(1, presses.get());
            assertEquals(1, releases.get());

            screen.showResult(BattleOutcome.PLAYER2_WIN, "다시 하기", () -> clicked.set("again"),
                    "시작 메뉴", () -> clicked.set("menu"));
            assertTrue(screen.isResultVisible());
            assertEquals("P2 승리", screen.resultHeading());
            assertTrue(screen.resultDetail().contains("P1 0점"));
            assertEquals(2, screen.resultButtons().size());
            press(screen, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false));
            assertEquals(1, presses.get());
            screen.resultButtons().get(1).doClick(0);
            assertEquals("menu", clicked.get());
            paint(screen, 884, 700);

            screen.showResult(BattleOutcome.DRAW, "대기실로", () -> clicked.set("lobby"), null, null);
            assertEquals("무승부", screen.resultHeading());
            assertEquals(1, screen.resultButtons().size());
            screen.showResult(BattleOutcome.PLAYER1_WIN, "확인", () -> { }, null, null);
            assertEquals("P1 승리", screen.resultHeading());
            assertThrows(IllegalArgumentException.class,
                    () -> screen.showResult(BattleOutcome.ONGOING, "확인", () -> { }, null, null));
            assertEquals("", screen.resultTitle(BattleOutcome.ONGOING));

            screen.hideResult();
            assertFalse(screen.isResultVisible());
            screen.clearKeys();
            assertNull(screen.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .get(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0, false)));
        });
    }

    @Test
    @DisplayName("모드 선택 패널은 고른 모드를 알리고, 고를 수 없는 쪽은 표시만 한다")
    void modeSelectPanel() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BattleModeSelectPanel panel = new BattleModeSelectPanel();
            AtomicReference<BattleMode> chosen = new AtomicReference<>();
            panel.setOnSelect(chosen::set);
            panel.button(BattleMode.TIME_LIMIT).doClick(0);
            assertEquals(BattleMode.TIME_LIMIT, chosen.get());
            assertEquals(BattleMode.TIME_LIMIT, panel.selected());

            panel.setSelectable(false);
            assertFalse(panel.button(BattleMode.NORMAL).isEnabled());
            panel.setSelected(BattleMode.ITEM);
            assertEquals(BattleMode.ITEM, panel.selected());
            assertEquals(BattleMode.TIME_LIMIT, chosen.get());
            panel.setSelected(null);
            panel.focusFirst();
            assertThrows(NullPointerException.class, () -> panel.setOnSelect(null));
            paint(panel, 520, 260);
        });
    }

    @Test
    @DisplayName("작은 부품은 정보가 없거나 크기가 작아도 그릴 수 있다")
    void partsPaintWithoutData() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            SnapshotPiecePreview preview = new SnapshotPiecePreview();
            paint(preview, 108, 60);
            preview.setPiece(playing(0).nextPiece());
            preview.setAppearance(ColorVisionMode.MONOCHROME, false);
            paint(preview, 30, 20);
            AttackPreview attacks = new AttackPreview();
            attacks.setLines(List.of(line(4)));
            assertEquals(1, attacks.lineCount());
            paint(attacks, 50, 40);
            BattleBoardView board = new BattleBoardView();
            assertNull(board.snapshot());
            paint(board, 140, 260);
            assertTrue(board.getPreferredSize().height > board.getMinimumSize().height);
            Dimension minimum = board.getMinimumSize();
            board.setSize(minimum);
            Rectangle bounds = board.boardBounds();
            assertTrue(bounds.y + bounds.height <= minimum.height);
        });
    }
}
