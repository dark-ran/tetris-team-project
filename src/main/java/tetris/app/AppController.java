package tetris.app;

import java.awt.CardLayout;
import java.util.List;
import java.util.Objects;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import tetris.ui.AppTheme;
import tetris.ui.GameOverScreen;
import tetris.ui.GameScreen;
import tetris.ui.ScoreboardScreen;
import tetris.ui.SettingsScreen;
import tetris.ui.StartMenu;

/**
 * 화면 전환의 단일 관리 지점. 생성과 호출은 Swing EDT에서 수행한다.
 * 창의 수명주기는 Main에 맡겨 디스플레이 없이도 화면 흐름을 테스트할 수 있다.
 */
public class AppController {
    static {
        AppTheme.install();
    }

    private final CardLayout layout = new CardLayout();
    private final JPanel view = new JPanel(layout);
    private final Runnable onExit;
    private final StartMenu startMenu;
    private final GameScreen gameScreen;
    private final SettingsScreen settingsScreen;
    private final ScoreboardScreen scoreboardScreen;
    private final GameOverScreen gameOverScreen;
    private AppState state = AppState.START_MENU;

    public AppController(Runnable onExit) {
        requireEdt();
        this.onExit = Objects.requireNonNull(onExit);
        startMenu = register(AppState.START_MENU,
                new StartMenu(this::startGame, this::showSettings, this::showScoreboard, this::exit));
        gameScreen = register(AppState.GAME,
                new GameScreen(this::showGameOver, this::showStartMenu, this::exit));
        settingsScreen = register(AppState.SETTINGS,
                new SettingsScreen(this::showStartMenu));
        scoreboardScreen = register(AppState.SCOREBOARD,
                new ScoreboardScreen(this::showStartMenu));
        gameOverScreen = register(AppState.GAME_OVER,
                new GameOverScreen(this::startGame, this::showScoreboard, this::showStartMenu, this::exit));
    }

    /** Main이 창에 연결하는 루트 패널. */
    public JPanel view() {
        return view;
    }

    public AppState state() {
        return state;
    }

    public void start() {
        showStartMenu();
    }

    public void showStartMenu() {
        activate(AppState.START_MENU, startMenu::showScreen);
    }

    public void startGame() {
        // TODO(Req1 / WS-02): GameEngine.newGame()과 GameLoop를 연결한다.
        activate(AppState.GAME, gameScreen::showScreen);
    }

    public void showSettings() {
        activate(AppState.SETTINGS, settingsScreen::showScreen);
    }

    public void showScoreboard() {
        // TODO(Req1 / WS-05): ScoreBoardService.top()을 연결한다.
        // 빈 목록은 미연결 화면용이며, 저장된 기록이 없다는 판정은 아니다.
        activate(AppState.SCOREBOARD, () -> scoreboardScreen.showScreen(List.of()));
    }

    /** 게임 연결 전의 종료 화면 미리보기. 임의의 점수를 전달하지 않는다. */
    public void showGameOver() {
        activate(AppState.GAME_OVER, gameOverScreen::showScreen);
    }

    /** 엔진 연결 후 실제 최종 점수를 전달할 진입점. */
    public void showGameOver(long score) {
        activate(AppState.GAME_OVER, () -> gameOverScreen.showScreen(score));
    }

    public void exit() {
        requireEdt();
        if (state != AppState.EXIT) {
            // TODO(Req1 / WS-02): 루프를 연결하면 여기서 중지한다.
            state = AppState.EXIT;
            onExit.run();
        }
    }

    /** 화면을 CardLayout에 등록하고 필드 대입을 위해 그대로 반환한다. */
    private <T extends JComponent> T register(AppState screen, T panel) {
        view.add(panel, screen.name());
        return panel;
    }

    private void activate(AppState next, Runnable onShow) {
        requireEdt();
        if (state == AppState.EXIT) {
            return;
        }
        state = next;
        layout.show(view, next.name());
        onShow.run();
    }

    private static void requireEdt() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("AppController must be used on the Swing EDT");
        }
    }
}
