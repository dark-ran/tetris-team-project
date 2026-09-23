package tetris.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.KeyEvent;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import tetris.game.GameState;

/** 게임 상태 연결 전 보드 크기와 좌우 입력을 확인하는 화면. */
public class GameScreen extends JPanel {
    private final BoardPreview boardPreview = new BoardPreview();
    private final JLabel positionLabel = ScreenSupport.mutedLabel("");

    public GameScreen(Runnable onFinishPreview, Runnable onBack, Runnable onExit) {
        ScreenSupport.prepareScreen(this, "게임",
                "← → 또는 A D로 입력을 확인할 수 있습니다. 낙하·충돌·회전·점수는 아직 연결되지 않았습니다.");
        JButton finishButton = ScreenSupport.primaryButton("종료 화면 보기", onFinishPreview);

        JPanel center = ScreenSupport.transparentPanel(new BorderLayout(28, 0));
        center.add(boardFrame(), BorderLayout.CENTER);
        center.add(statusSidebar(), BorderLayout.EAST);
        add(center, BorderLayout.CENTER);
        add(ScreenSupport.actionRow(finishButton,
                ScreenSupport.button("시작 메뉴", onBack),
                ScreenSupport.dangerButton("프로그램 종료", onExit)), BorderLayout.SOUTH);

        bindPreviewMovementKeys(KeyEvent.VK_LEFT, KeyEvent.VK_A, -1, "preview-left");
        bindPreviewMovementKeys(KeyEvent.VK_RIGHT, KeyEvent.VK_D, 1, "preview-right");
        SwingKeyBindings.backOnEscape(this, onBack);
    }

    private JPanel boardFrame() {
        JPanel area = ScreenSupport.framedPanel(new BorderLayout(0, 12));
        JLabel title = ScreenSupport.sectionTitle("보드 20 × 10    [입력 확인용]");
        area.add(title, BorderLayout.NORTH);
        area.add(boardPreview, BorderLayout.CENTER);
        JLabel notice = ScreenSupport.mutedLabel("보라색 T 블록은 입력 확인용이며 실제 게임 상태가 아닙니다.");
        notice.setHorizontalAlignment(JLabel.CENTER);
        area.add(notice, BorderLayout.SOUTH);
        return area;
    }

    /** 다음 블록·점수 등 게임 화면 요구 항목의 자리. 값은 연결 예정이다. */
    private JPanel statusSidebar() {
        JPanel summary = ScreenSupport.framedPanel(new BorderLayout());
        summary.setPreferredSize(new Dimension(260, 0));
        JPanel sections = ScreenSupport.verticalPanel();
        sections.add(informationSection("점수", "미연결", "게임 엔진 연결 후 계산"));
        sections.add(sectionDivider());
        sections.add(informationSection("다음 블록", "미연결", "블록 큐 연결 예정"));
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
        controls.add(Box.createVerticalStrut(18));
        controls.add(positionLabel);
        return controls;
    }

    private static JSeparator sectionDivider() {
        JSeparator separator = new JSeparator();
        separator.setForeground(AppTheme.BORDER);
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        separator.setAlignmentX(Component.LEFT_ALIGNMENT);
        return separator;
    }

    /** 실제 입력 계층 연결 전 좌우 입력 경로를 확인하기 위한 임시 바인딩. */
    private void bindPreviewMovementKeys(int arrowKey, int letterKey, int direction, String actionName) {
        Runnable move = () -> {
            boardPreview.moveHorizontally(direction);
            updatePreviewPositionLabel();
        };
        SwingKeyBindings.bindToScreen(this, arrowKey, actionName + "-arrow", move);
        SwingKeyBindings.bindToScreen(this, letterKey, actionName + "-letter", move);
    }

    private void updatePreviewPositionLabel() {
        positionLabel.setText("미리보기 위치: " + (boardPreview.pieceColumn() + 1) + "열");
    }

    public void showScreen() {
        boardPreview.reset();
        updatePreviewPositionLabel();
        SwingKeyBindings.focus(boardPreview);
    }

    int previewColumn() {
        return boardPreview.pieceColumn();
    }

    public void render(GameState state) {
        // TODO(Req1 / WS-03): WS-01/02에서 확정한 상태·좌표 계약으로 보드와 점수를 표시한다.
        throw new UnsupportedOperationException("TODO: 게임 상태를 화면에 표시");
    }
}
