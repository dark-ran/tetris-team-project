package tetris.app;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** 실제 Swing 패널과 버튼 콜백으로 화면 이동을 검증한다. JFrame이나 가짜 게임 엔진은 만들지 않는다. */
class AppControllerTest {
    @Test
    void startGameFinishRestartAndReturnToMenu() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AppController app = new AppController(() -> {});
            app.start();
            assertScreen(app, AppState.START_MENU);
            click(app, "게임 시작");
            assertScreen(app, AppState.GAME);
            click(app, "종료 화면 보기");
            assertScreen(app, AppState.GAME_OVER);
            assertLabel(app, "최종 점수: 미측정 (게임 연결 전)");
            click(app, "다시 시작");
            assertScreen(app, AppState.GAME);
            click(app, "시작 메뉴");
            assertScreen(app, AppState.START_MENU);
        });
    }

    @Test
    void settingsAndScoreboardCanBeVisitedRepeatedlyWithoutCallingRepositories() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AppController app = new AppController(() -> {});
            app.start();
            for (int visit = 0; visit < 2; visit++) {
                click(app, "설정");
                assertScreen(app, AppState.SETTINGS);
                assertFalse(button(app, "설정 저장 (준비 중)").isEnabled());
                click(app, "시작 메뉴");
                click(app, "스코어보드");
                assertScreen(app, AppState.SCOREBOARD);
                JTable table = visibleTree(app.view()).filter(JTable.class::isInstance)
                        .map(JTable.class::cast).findFirst().orElseThrow();
                assertEquals(0, table.getRowCount(), "미연결 상태에 예시 기록을 넣으면 안 된다");
                click(app, "시작 메뉴");
                assertScreen(app, AppState.START_MENU);
            }
        });
    }

    @Test
    void enterActivatesTheSelectedMenuButton() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AppController app = new AppController(() -> {});
            app.start();
            invokeKey(button(app, "설정"), JComponent.WHEN_FOCUSED, KeyEvent.VK_ENTER);
            assertScreen(app, AppState.SETTINGS);
        });
    }

    @ParameterizedTest
    @EnumSource(value = AppState.class, names = {"GAME", "SETTINGS", "SCOREBOARD", "GAME_OVER"})
    void escapeReturnsToMenuFromEverySecondaryScreen(AppState screen) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AppController app = new AppController(() -> {});
            open(app, screen);
            JComponent activeCard = Arrays.stream(app.view().getComponents())
                    .filter(Component::isVisible).map(JComponent.class::cast).findFirst().orElseThrow();
            invokeKey(activeCard, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, KeyEvent.VK_ESCAPE);
            assertScreen(app, AppState.START_MENU);
        });
    }

    @ParameterizedTest
    @EnumSource(value = AppState.class, names = {"EXIT"}, mode = EnumSource.Mode.EXCLUDE)
    void windowClosePathIsIdempotentAndCannotReopenScreens(AppState screen) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AtomicInteger closeCount = new AtomicInteger();
            AppController app = new AppController(closeCount::incrementAndGet);
            open(app, screen);
            app.exit();
            app.exit();
            app.showStartMenu();
            assertEquals(AppState.EXIT, app.state());
            assertEquals(1, closeCount.get());
        });
    }

    @ParameterizedTest
    @EnumSource(value = AppState.class, names = {"START_MENU", "GAME", "GAME_OVER"})
    void exitButtonClosesTheApplication(AppState screen) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AtomicInteger closeCount = new AtomicInteger();
            AppController app = new AppController(closeCount::incrementAndGet);
            open(app, screen);
            click(app, "프로그램 종료");
            assertEquals(AppState.EXIT, app.state());
            assertEquals(1, closeCount.get());
        });
    }

    @Test
    void realLongScoreIsDisplayedAndClearedForTheNextPreview() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AppController app = new AppController(() -> {});
            app.start();
            app.showGameOver(6_000_000_000L);
            assertLabel(app, "최종 점수: 6000000000");
            click(app, "다시 시작");
            click(app, "종료 화면 보기");
            assertLabel(app, "최종 점수: 미측정 (게임 연결 전)");
        });
    }

    private static void open(AppController app, AppState screen) {
        app.start();
        switch (screen) {
            case START_MENU -> { }
            case GAME -> app.startGame();
            case SETTINGS -> app.showSettings();
            case SCOREBOARD -> app.showScoreboard();
            case GAME_OVER -> app.showGameOver();
            case EXIT -> throw new IllegalArgumentException("EXIT is not a screen");
        }
        assertScreen(app, screen);
    }

    private static void assertScreen(AppController app, AppState expected) {
        assertEquals(expected, app.state());
        assertEquals(1, Arrays.stream(app.view().getComponents()).filter(Component::isVisible).count(),
                "한 번에 한 화면만 보여야 한다");
    }

    private static void click(AppController app, String text) {
        JButton button = button(app, text);
        assertTrue(button.isEnabled());
        button.doClick(0);
    }

    private static JButton button(AppController app, String text) {
        return visibleTree(app.view()).filter(JButton.class::isInstance).map(JButton.class::cast)
                .filter(button -> text.equals(button.getText())).findFirst().orElseThrow();
    }

    private static void assertLabel(AppController app, String text) {
        assertTrue(visibleTree(app.view()).filter(JLabel.class::isInstance).map(JLabel.class::cast)
                .anyMatch(label -> text.equals(label.getText())), text);
    }

    private static void invokeKey(JComponent target, int condition, int keyCode) {
        Object actionName = target.getInputMap(condition).get(KeyStroke.getKeyStroke(keyCode, 0));
        assertNotNull(actionName, "키에 연결된 동작이 있어야 한다");
        target.getActionMap().get(actionName).actionPerformed(
                new ActionEvent(target, ActionEvent.ACTION_PERFORMED, actionName.toString()));
    }

    private static Stream<Component> visibleTree(Component component) {
        if (!component.isVisible()) {
            return Stream.empty();
        }
        Stream<Component> self = Stream.of(component);
        return component instanceof Container container
                ? Stream.concat(self, Arrays.stream(container.getComponents()).flatMap(AppControllerTest::visibleTree))
                : self;
    }
}
