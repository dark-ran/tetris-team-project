package tetris.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tetris.game.BattleMode;
import tetris.game.GameEngine;
import tetris.loop.GameLoop;
import tetris.scoreboard.ScoreBoardService;
import tetris.scoreboard.ScoreRepository;
import tetris.ui.BattleScreen;

@DisplayName("시작 메뉴에서 대전 화면으로의 이동")
class BattleNavigationTest {
    @TempDir Path directory;

    private static final class TestLoop extends GameLoop {
        @Override public void start() { }
        @Override public void stop() { }
        @Override public void pause() { }
        @Override public void resume() { }
    }

    private AppController newApp() {
        return new AppController(() -> { }, new GameEngine(), new TestLoop(), ignored -> { }, ignored -> { },
                new ScoreBoardService(new ScoreRepository(directory.resolve("scores.tsv"))),
                null, ignored -> { });
    }

    private static Stream<Component> visibleTree(Component component) {
        if (!component.isVisible()) return Stream.empty();
        Stream<Component> self = Stream.of(component);
        if (component instanceof Container container) {
            return Stream.concat(self, Stream.of(container.getComponents()).flatMap(BattleNavigationTest::visibleTree));
        }
        return self;
    }

    private static JButton button(AppController app, String text) {
        return visibleTree(app.view()).filter(JButton.class::isInstance).map(JButton.class::cast)
                .filter(button -> text.equals(button.getText())).findFirst().orElseThrow();
    }

    private static boolean hasLabel(AppController app, String text) {
        return visibleTree(app.view()).filter(JLabel.class::isInstance).map(JLabel.class::cast)
                .anyMatch(label -> text.equals(label.getText()));
    }

    @Test
    @DisplayName("대전 모드 버튼 → 모드 선택 → 대전 화면, 고른 모드와 화면이 시작 동작에 전달된다")
    void localBattleFlow() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AppController app = newApp();
            AtomicReference<BattleMode> started = new AtomicReference<>();
            AtomicReference<BattleScreen> screen = new AtomicReference<>();
            app.setLocalBattleStarter((mode, battle) -> { started.set(mode); screen.set(battle); });
            app.start();

            button(app, "대전 모드").doClick(0);
            assertEquals(AppState.BATTLE_SELECT, app.state());
            button(app, "뒤로 (Esc)").doClick(0);
            assertEquals(AppState.START_MENU, app.state());

            button(app, "대전 모드").doClick(0);
            button(app, "아이템 대전").doClick(0);
            assertEquals(AppState.BATTLE, app.state());
            assertEquals(BattleMode.ITEM, started.get());
            assertSame(app.battleScreen(), screen.get());
            assertTrue(hasLabel(app, "아이템 대전"));
            assertTrue(hasLabel(app, "P1"));
            assertThrows(NullPointerException.class, () -> app.startLocalBattle(null));
            assertThrows(NullPointerException.class, () -> app.setLocalBattleStarter(null));
        });
    }

    @Test
    @DisplayName("다시 대전 화면에 들어오면 이전 결과·키·상태 문구를 지운다")
    void battleScreenIsResetOnEntry() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AppController app = newApp();
            app.start();
            BattleScreen battle = app.battleScreen();
            AtomicInteger presses = new AtomicInteger();
            battle.bindKey(java.awt.event.KeyEvent.VK_A, presses::incrementAndGet, null);
            battle.setStatusMessage("랙 걸린 상태", true);
            battle.showResult(tetris.game.BattleOutcome.DRAW, "확인", () -> { }, null, null);
            app.showBattleScreen(BattleMode.NORMAL, "나", "상대");
            assertEquals(AppState.BATTLE, app.state());
            assertFalse(battle.isResultVisible());
            assertNull(battle.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .get(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, 0, false)));
            assertTrue(hasLabel(app, "나"));
            app.showStartMenu();
            assertEquals(AppState.START_MENU, app.state());
        });
    }

    @Test
    @DisplayName("P2P 대전 버튼은 준비 중 화면을 열고, 등록한 P2P 화면이 있으면 그 화면을 연다")
    void p2pLobbyFlow() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AppController app = newApp();
            app.start();
            button(app, "P2P 대전").doClick(0);
            assertEquals(AppState.P2P_LOBBY, app.state());
            assertTrue(hasLabel(app, "준비 중입니다."));
            button(app, "뒤로 (Esc)").doClick(0);
            assertEquals(AppState.START_MENU, app.state());

            AtomicInteger shown = new AtomicInteger();
            javax.swing.JPanel lobby = new javax.swing.JPanel();
            lobby.add(new JLabel("대기실"));
            app.setP2PLobby(lobby, shown::incrementAndGet);
            button(app, "P2P 대전").doClick(0);
            assertEquals(1, shown.get());
            assertTrue(hasLabel(app, "대기실"));
            assertFalse(hasLabel(app, "준비 중입니다."));
            app.setP2PLobby(lobby, null);
            app.showP2PLobby();
            assertEquals(1, shown.get());
            assertThrows(NullPointerException.class, () -> app.setP2PLobby(null, null));
        });
    }
}
