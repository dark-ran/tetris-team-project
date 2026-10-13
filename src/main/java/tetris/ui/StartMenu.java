package tetris.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * 교체 가능한 시작 메뉴. 게임 규칙이나 저장소에 의존하지 않는다.
 *
 * <p>현재 사용 기능: 게임 시작·설정·스코어보드·종료 콜백과 메뉴 키보드 탐색.</p>
 * <p>후속 연결: 메뉴가 추가되면 버튼과 AppController 콜백을 함께 확장한다.</p>
 */
public class StartMenu extends JPanel {
    private final JButton startButton;

    public StartMenu(Runnable onStart, Runnable onSettings, Runnable onScoreboard, Runnable onExit) {
        setLayout(new BorderLayout(0, 20));
        setBackground(AppTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(26, 34, 24, 34));

        startButton = ScreenSupport.primaryButton("게임 시작", onStart);
        JButton settingsButton = ScreenSupport.button("설정", onSettings);
        JButton scoreboardButton = ScreenSupport.button("스코어보드", onScoreboard);
        JButton exitButton = ScreenSupport.dangerButton("프로그램 종료", onExit);
        List<JButton> buttons = List.of(startButton, settingsButton, scoreboardButton, exitButton);

        JPanel title = ScreenSupport.transparentPanel(new BorderLayout(0, 8));
        title.add(new TetrisWordmark(), BorderLayout.CENTER);

        JPanel buttonList = ScreenSupport.transparentPanel(new GridLayout(4, 1, 0, 8));
        buttons.forEach(buttonList::add);
        JPanel menu = new JPanel(new BorderLayout(0, 12));
        menu.setBackground(AppTheme.SURFACE);
        menu.setBorder(AppTheme.frameBorder(16, 18));
        menu.setPreferredSize(new Dimension(360, 244));
        JLabel menuLabel = new JLabel("[ 메뉴 ]");
        menuLabel.setFont(AppTheme.font(Font.BOLD, 13f));
        menuLabel.setForeground(AppTheme.YELLOW);
        menu.add(menuLabel, BorderLayout.NORTH);
        menu.add(buttonList, BorderLayout.CENTER);

        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        menu.setAlignmentX(Component.CENTER_ALIGNMENT);
        JPanel content = ScreenSupport.verticalPanel();
        content.add(title);
        content.add(Box.createVerticalStrut(36));
        content.add(menu);

        JPanel center = ScreenSupport.transparentPanel(new GridBagLayout());
        center.add(content);
        add(center, BorderLayout.CENTER);

        JLabel guide = ScreenSupport.mutedLabel("↑ ↓ / Tab : 이동       Enter / Space : 선택");
        guide.setHorizontalAlignment(JLabel.CENTER);
        add(guide, BorderLayout.SOUTH);
        SwingKeyBindings.menuNavigation(this, buttons);
    }

    public void showScreen() {
        SwingKeyBindings.focus(startButton);
    }
}
