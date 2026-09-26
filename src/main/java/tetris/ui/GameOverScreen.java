package tetris.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** 최종 점수 표시와 재시작·메뉴 이동을 담당한다. */
public class GameOverScreen extends JPanel {
    private static final String SCORE_UNMEASURED = "최종 점수: 미측정 (게임 연결 전)";

    private final JLabel scoreLabel = new JLabel();
    private final JButton restartButton;

    public GameOverScreen(Runnable onRestart, Runnable onScoreboard, Runnable onBack, Runnable onExit) {
        ScreenSupport.prepareScreen(this, "게임 종료",
                "다시 시작하거나 다른 화면으로 이동할 수 있습니다. 이름 입력과 기록 저장은 연결 예정입니다.");
        restartButton = ScreenSupport.primaryButton("다시 시작", onRestart);

        JPanel result = ScreenSupport.framedPanel(new GridLayout(2, 1, 0, 14));
        result.setPreferredSize(new java.awt.Dimension(420, 150));
        scoreLabel.setFont(AppTheme.font(Font.BOLD, 24f));
        scoreLabel.setHorizontalAlignment(JLabel.CENTER);
        result.add(scoreLabel);
        JLabel guide = ScreenSupport.mutedLabel("Esc  시작 메뉴");
        guide.setHorizontalAlignment(JLabel.CENTER);
        result.add(guide);
        JPanel center = ScreenSupport.transparentPanel(new GridBagLayout());
        center.add(result);
        add(center, BorderLayout.CENTER);
        add(ScreenSupport.actionRow(restartButton,
                ScreenSupport.button("스코어보드", onScoreboard),
                ScreenSupport.button("시작 메뉴", onBack),
                ScreenSupport.dangerButton("프로그램 종료", onExit)), BorderLayout.SOUTH);
        SwingKeyBindings.backOnEscape(this, onBack);
    }

    /** 게임 미연결 상태의 미리보기. 실제 점수 대신 미측정을 표시한다. */
    public void showScreen() {
        scoreLabel.setText(SCORE_UNMEASURED);
        SwingKeyBindings.focus(restartButton);
    }

    public void showScreen(long score) {
        scoreLabel.setText("최종 점수: " + score);
        SwingKeyBindings.focus(restartButton);
    }
}
