package tetris.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.JPanel;
import tetris.game.BattleMode;

/** 시작 메뉴의 "대전 모드"에서 여는 한 PC 2인 대전 모드 선택 화면. 고른 모드는 콜백으로 전달한다. */
public class BattleSelectScreen extends JPanel {
    static { AppTheme.install(); }

    private final BattleModeSelectPanel modes = new BattleModeSelectPanel();

    public BattleSelectScreen(Consumer<BattleMode> onSelect, Runnable onBack) {
        Objects.requireNonNull(onSelect);
        Objects.requireNonNull(onBack);
        ScreenSupport.prepareScreen(this, "한 PC 대전", "한 PC에서 2명이 대결합니다. 모드를 고르면 바로 시작합니다.");
        modes.setOnSelect(onSelect);
        JPanel frame = ScreenSupport.framedPanel(new BorderLayout());
        frame.add(modes);
        frame.setPreferredSize(new Dimension(460, 240));
        JPanel center = ScreenSupport.transparentPanel(new GridBagLayout());
        center.add(frame);
        add(center, BorderLayout.CENTER);
        add(ScreenSupport.actionRow(ScreenSupport.button("뒤로 (Esc)", onBack)), BorderLayout.SOUTH);
        SwingKeyBindings.backOnEscape(this, onBack);
    }

    public void showScreen() {
        modes.setSelected(null);
        modes.focusFirst();
    }
}
