package tetris.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import tetris.game.AttackQueue;
import tetris.game.PlayerSnapshot;
import tetris.settings.ColorVisionMode;

/** 대전 화면의 한 플레이어 영역: 이름, 보드, 다음 블록, 점수, 레벨, 넘어올 줄. */
final class BattlePlayerPanel extends JPanel {
    private final JLabel nameLabel = new JLabel();
    private final BattleBoardView board = new BattleBoardView();
    private final SnapshotPiecePreview nextPiece = new SnapshotPiecePreview();
    private final JLabel scoreLabel = valueLabel();
    private final JLabel levelLabel = valueLabel();
    private final JLabel attackCountLabel = ScreenSupport.mutedLabel("0 / " + AttackQueue.MAX_LINES);
    private final AttackPreview attackPreview = new AttackPreview();

    BattlePlayerPanel(String name) {
        super(new BorderLayout(10, 8));
        setOpaque(false);
        nameLabel.setFont(AppTheme.font(Font.BOLD, 17f));
        nameLabel.setForeground(AppTheme.YELLOW);
        setPlayerName(name);
        add(nameLabel, BorderLayout.NORTH);
        add(board, BorderLayout.CENTER);
        add(sidebar(), BorderLayout.EAST);
    }

    private JPanel sidebar() {
        JPanel side = ScreenSupport.verticalPanel();
        side.setPreferredSize(new Dimension(128, 0));
        side.add(section("다음 블록", nextPiece));
        side.add(Box.createVerticalStrut(10));
        side.add(section("점수", scoreLabel));
        side.add(Box.createVerticalStrut(6));
        side.add(section("레벨", levelLabel));
        side.add(Box.createVerticalStrut(10));
        JPanel attack = section("넘어올 줄", attackPreview);
        attack.add(attackCountLabel);
        side.add(attack);
        side.add(Box.createVerticalGlue());
        return side;
    }

    private static JPanel section(String title, Component content) {
        JPanel section = ScreenSupport.verticalPanel();
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel heading = ScreenSupport.sectionTitle(title);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(heading);
        section.add(Box.createVerticalStrut(4));
        if (content instanceof JPanel panel) panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (content instanceof JLabel label) label.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(content);
        return section;
    }

    private static JLabel valueLabel() {
        JLabel label = new JLabel("0");
        label.setFont(AppTheme.font(Font.BOLD, 19f));
        label.setForeground(AppTheme.TEXT);
        return label;
    }

    void setPlayerName(String name) {
        nameLabel.setText(name);
        board.getAccessibleContext().setAccessibleName(name + " 보드");
    }

    String playerName() {
        return nameLabel.getText();
    }

    void render(PlayerSnapshot snapshot) {
        board.render(snapshot);
        if (snapshot == null) {
            nextPiece.setPiece(null);
            attackPreview.setLines(java.util.List.of());
            scoreLabel.setText("0");
            levelLabel.setText("1");
        } else {
            nextPiece.setPiece(snapshot.nextPiece());
            attackPreview.setLines(snapshot.pendingAttackLines());
            scoreLabel.setText(Long.toString(snapshot.score()));
            levelLabel.setText(Integer.toString(snapshot.level()));
        }
        attackCountLabel.setText(attackPreview.lineCount() + " / " + AttackQueue.MAX_LINES);
    }

    void setAppearance(ColorVisionMode mode, boolean patterns) {
        board.setAppearance(mode, patterns);
        nextPiece.setAppearance(mode, patterns);
    }

    BattleBoardView board() { return board; }
    String scoreText() { return scoreLabel.getText(); }
    String attackCountText() { return attackCountLabel.getText(); }
}
