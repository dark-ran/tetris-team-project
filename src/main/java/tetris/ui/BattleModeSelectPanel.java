package tetris.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import tetris.game.BattleMode;

/**
 * 일반 / 아이템 / 시간제한 대전 모드 선택 패널. 로컬 대전 시작 화면과 P2P 대기실에서 함께 쓴다.
 * P2P 클라이언트처럼 고를 수 없는 쪽은 {@link #setSelectable(boolean)}로 막고 선택된 모드만 보여준다.
 */
public class BattleModeSelectPanel extends JPanel {
    static { AppTheme.install(); }

    private static final Map<BattleMode, String> DESCRIPTIONS = Map.of(
            BattleMode.NORMAL, "줄을 지워 상대를 공격",
            BattleMode.ITEM, "일반 대전 + 아이템",
            BattleMode.TIME_LIMIT, "2분 뒤 점수가 높은 쪽 승리");

    private final Map<BattleMode, JButton> buttons = new EnumMap<>(BattleMode.class);
    private final JLabel selectedLabel = ScreenSupport.mutedLabel("모드를 선택하세요.");
    private Consumer<BattleMode> listener = mode -> { };
    private BattleMode selected;

    public BattleModeSelectPanel() {
        super(new BorderLayout(0, 10));
        setOpaque(false);
        JLabel title = ScreenSupport.sectionTitle("대전 모드");
        title.setFont(AppTheme.font(Font.BOLD, 16f));
        add(title, BorderLayout.NORTH);
        JPanel list = ScreenSupport.transparentPanel(new GridLayout(BattleMode.values().length, 1, 0, 8));
        for (BattleMode mode : BattleMode.values()) {
            JButton button = ScreenSupport.button(mode.toString(), () -> choose(mode));
            button.setPreferredSize(new Dimension(170, 40));
            buttons.put(mode, button);
            JPanel row = ScreenSupport.transparentPanel(new BorderLayout(14, 0));
            row.add(button, BorderLayout.WEST);
            row.add(ScreenSupport.mutedLabel(DESCRIPTIONS.get(mode)), BorderLayout.CENTER);
            list.add(row);
        }
        add(list, BorderLayout.CENTER);
        add(selectedLabel, BorderLayout.SOUTH);
        SwingKeyBindings.menuNavigation(this, List.copyOf(buttons.values()));
    }

    /** 모드를 고르면 호출된다. */
    public void setOnSelect(Consumer<BattleMode> onSelect) {
        listener = Objects.requireNonNull(onSelect);
    }

    /** 고를 수 있는지. P2P 클라이언트는 false로 두고 서버가 고른 모드를 setSelected로 표시한다. */
    public void setSelectable(boolean selectable) {
        buttons.values().forEach(button -> button.setEnabled(selectable));
    }

    /** 선택된 모드를 표시만 한다. 리스너는 호출하지 않는다. */
    public void setSelected(BattleMode mode) {
        selected = mode;
        buttons.forEach((choice, button) -> button.setSelected(choice == mode));
        selectedLabel.setText(mode == null ? "모드를 선택하세요." : "선택된 모드: " + mode);
    }

    public BattleMode selected() {
        return selected;
    }

    /** 첫 번째 버튼에 포커스를 둔다. */
    public void focusFirst() {
        SwingKeyBindings.focus(buttons.get(BattleMode.NORMAL));
    }

    private void choose(BattleMode mode) {
        setSelected(mode);
        listener.accept(mode);
    }

    JButton button(BattleMode mode) {
        return buttons.get(mode);
    }
}
