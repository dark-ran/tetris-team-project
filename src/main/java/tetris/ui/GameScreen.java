package tetris.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import tetris.game.GameState;

/**
 * 게임 상태 연결 전 보드와 화면 전환 구조를 제공하는 화면.
 *
 * <p>현재 사용 기능: 보드 크기 표시와 화면 전환 콜백. 임시 블록 조작은 제거했다.</p>
 * <p>후속 연결: GameState 렌더링과 GameEngine·GameLoop·InputHandler의 실제 게임 동작.</p>
 */
public class GameScreen extends JPanel {
    private final BoardPreview boardPreview = new BoardPreview();

    public GameScreen(Runnable onFinishPreview, Runnable onBack, Runnable onExit) {
        ScreenSupport.prepareScreen(this, "게임",
                "게임을 준비하고 있습니다.");
        JButton finishButton = ScreenSupport.primaryButton("종료 화면 보기", onFinishPreview);

        JPanel center = ScreenSupport.transparentPanel(new BorderLayout(28, 0));
        center.add(boardFrame(), BorderLayout.CENTER);
        center.add(statusSidebar(), BorderLayout.EAST);
        add(center, BorderLayout.CENTER);
        add(ScreenSupport.actionRow(finishButton,
                ScreenSupport.button("시작 메뉴", onBack),
                ScreenSupport.dangerButton("프로그램 종료", onExit)), BorderLayout.SOUTH);

        SwingKeyBindings.backOnEscape(this, onBack);
    }

    private JPanel boardFrame() {
        JPanel area = ScreenSupport.framedPanel(new BorderLayout(0, 12));
        JLabel title = ScreenSupport.sectionTitle("보드 20 × 10");
        area.add(title, BorderLayout.NORTH);
        area.add(boardPreview, BorderLayout.CENTER);
        JLabel notice = ScreenSupport.mutedLabel("게임 시작 후 현재 블록과 쌓인 블록을 표시합니다.");
        notice.setHorizontalAlignment(JLabel.CENTER);
        area.add(notice, BorderLayout.SOUTH);
        return area;
    }

    /** 점수와 다음 블록 표시 자리, 조작 안내를 구성한다. */
    private JPanel statusSidebar() {
        JPanel summary = ScreenSupport.framedPanel(new BorderLayout());
        summary.setPreferredSize(new Dimension(260, 0));
        JPanel sections = ScreenSupport.verticalPanel();
        sections.add(informationSection("점수", "미연결", "게임 엔진 연결 후 계산"));
        sections.add(sectionDivider());
        sections.add(informationSection("다음 블록", "미연결", "게임 상태 연결 후 표시"));
        sections.add(sectionDivider());
        sections.add(controlSection());
        sections.add(Box.createVerticalGlue());
        summary.add(sections, BorderLayout.CENTER);
        return summary;
    }

    private JPanel informationSection(String heading, String value, String detail) {
        JPanel section = ScreenSupport.transparentPanel(new BorderLayout(0, 8));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, 116));
        section.add(ScreenSupport.sectionTitle(heading), BorderLayout.NORTH);
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(AppTheme.font(Font.BOLD, 21f));
        section.add(valueLabel, BorderLayout.CENTER);
        section.add(ScreenSupport.mutedLabel(detail), BorderLayout.SOUTH);
        return section;
    }

    private JPanel controlSection() {
        JPanel controls = ScreenSupport.verticalPanel();
        controls.setAlignmentX(Component.LEFT_ALIGNMENT);
        controls.add(ScreenSupport.sectionTitle("조작"));
        controls.add(Box.createVerticalStrut(12));
        controls.add(new JLabel("[←] [A]  왼쪽"));
        controls.add(Box.createVerticalStrut(8));
        controls.add(new JLabel("[→] [D]  오른쪽"));
        controls.add(Box.createVerticalStrut(12));
        controls.add(ScreenSupport.mutedLabel("[↓] [↑] [Space]  연결 예정"));
        controls.add(Box.createVerticalStrut(8));
        controls.add(ScreenSupport.mutedLabel("[Esc]  시작 메뉴"));
        return controls;
    }

    private static JSeparator sectionDivider() {
        JSeparator separator = new JSeparator();
        separator.setForeground(AppTheme.BORDER);
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        separator.setAlignmentX(Component.LEFT_ALIGNMENT);
        return separator;
    }

    public void showScreen() {
        SwingKeyBindings.focus(boardPreview);
    }

    public void render(GameState state) {
        // TODO: GameState의 보드 사본, 현재 블록, 기준 행·열과 다음 블록 목록을 표시한다.
        throw new UnsupportedOperationException("TODO: 게임 상태를 화면에 표시");
    }
}
