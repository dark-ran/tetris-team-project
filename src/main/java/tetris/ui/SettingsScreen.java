package tetris.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import tetris.settings.GameSettings;

/**
 * 설정 항목의 자리. 저장과 실제 적용은 후속 담당 작업에서 연결한다.
 *
 * <p>현재 사용 기능: 요구 설정 항목 안내, 비활성 저장 버튼, 메뉴 복귀 콜백.</p>
 * <p>후속 연결: SettingsService를 통한 크기·키·색각이상 설정과 저장·복원·초기화.</p>
 */
public class SettingsScreen extends JPanel {
    private static final List<SettingItem> SETTINGS = List.of(
            new SettingItem("화면 크기", "3개 이상 프리셋 연결 예정", AppTheme.CYAN),
            new SettingItem("게임 조작 키", "키 변경·중복 정책 결정 예정", AppTheme.YELLOW),
            new SettingItem("색각이상 모드", "색상과 패턴 표현 검토 중", AppTheme.PURPLE),
            new SettingItem("초기화", "설정과 기록을 각각 처리할 예정", AppTheme.RED));

    private final JButton backButton;
    private Consumer<GameSettings> onSave;
    private Runnable onRestoreDefaults;
    private Runnable onResetScores;

    public void setOnSave(Consumer<GameSettings> callback) { onSave = Objects.requireNonNull(callback); }
    public void setOnRestoreDefaults(Runnable callback) { onRestoreDefaults = Objects.requireNonNull(callback); }
    public void setOnResetScores(Runnable callback) { onResetScores = Objects.requireNonNull(callback); }

    // TODO: UI 버튼에서 위 콜백을 호출한다. 입력 컨트롤과 버튼 동작은 아직 미구현이다.

    public SettingsScreen(Runnable onBack) {
        ScreenSupport.prepareScreen(this, "설정",
                "설정 항목의 위치를 확인하는 임시 화면입니다. 비활성 항목은 후속 작업에서 연결합니다.");
        backButton = ScreenSupport.button("시작 메뉴", onBack);

        JPanel center = ScreenSupport.transparentPanel(new GridBagLayout());
        center.add(settingsFrame());
        add(center, BorderLayout.CENTER);
        add(ScreenSupport.actionRow(disabledSaveButton(), backButton), BorderLayout.SOUTH);
        SwingKeyBindings.backOnEscape(this, onBack);
    }

    /** 요구사항의 설정 항목 목록. 컨트롤은 저장소 연결 전까지 비활성이다. */
    private static JPanel settingsFrame() {
        JPanel list = ScreenSupport.verticalPanel();
        for (int index = 0; index < SETTINGS.size(); index++) {
            list.add(settingRow(SETTINGS.get(index)));
            if (index < SETTINGS.size() - 1) {
                list.add(rowDivider());
            }
        }

        JPanel frame = ScreenSupport.framedPanel(new BorderLayout(0, 14));
        frame.setPreferredSize(new Dimension(800, 410));
        frame.add(settingsListHeader(), BorderLayout.NORTH);
        frame.add(list, BorderLayout.CENTER);
        return frame;
    }

    private static JPanel settingsListHeader() {
        JPanel header = ScreenSupport.transparentPanel(new BorderLayout());
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.BORDER),
                BorderFactory.createEmptyBorder(0, 0, 10, 0)));
        JLabel item = new JLabel("설정 항목");
        item.setFont(AppTheme.font(Font.BOLD, 13f));
        item.setForeground(AppTheme.MUTED);
        JLabel status = new JLabel("연결 상태");
        status.setFont(AppTheme.font(Font.BOLD, 13f));
        status.setForeground(AppTheme.MUTED);
        header.add(item, BorderLayout.WEST);
        header.add(status, BorderLayout.EAST);
        return header;
    }

    private static JPanel settingRow(SettingItem setting) {
        JPanel row = ScreenSupport.transparentPanel(new BorderLayout(24, 0));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setPreferredSize(new Dimension(0, 74));
        row.setMinimumSize(new Dimension(0, 74));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));
        row.setBorder(BorderFactory.createEmptyBorder(11, 0, 11, 0));

        JPanel copy = ScreenSupport.transparentPanel(new BorderLayout(0, 7));
        JLabel heading = new JLabel(setting.title());
        heading.setFont(AppTheme.font(Font.BOLD, 18f));
        copy.add(heading, BorderLayout.NORTH);
        JLabel description = ScreenSupport.mutedLabel(setting.description());
        description.setFont(AppTheme.font(Font.PLAIN, 14f));
        copy.add(description, BorderLayout.CENTER);
        row.add(copy, BorderLayout.CENTER);
        row.add(markerBar(setting.color()), BorderLayout.WEST);

        JLabel state = new JLabel("[ 준비 중 ]");
        state.setForeground(AppTheme.YELLOW);
        state.setFont(AppTheme.font(Font.BOLD, 13f));
        state.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.YELLOW),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)));
        row.add(state, BorderLayout.EAST);
        return row;
    }

    private static JPanel markerBar(Color color) {
        JPanel bar = new JPanel();
        bar.setBackground(color);
        bar.setPreferredSize(new Dimension(12, 0));
        bar.setBorder(BorderFactory.createLineBorder(color.brighter()));
        return bar;
    }

    private static JSeparator rowDivider() {
        JSeparator separator = new JSeparator();
        separator.setForeground(AppTheme.GRID);
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
        separator.setAlignmentX(Component.LEFT_ALIGNMENT);
        return separator;
    }

    private static JButton disabledSaveButton() {
        JButton saveButton = ScreenSupport.button("설정 저장 (준비 중)", () -> {});
        saveButton.setEnabled(false);
        return saveButton;
    }

    public void showScreen() {
        SwingKeyBindings.focus(backButton);
    }

    private record SettingItem(String title, String description, Color color) {
    }
}
