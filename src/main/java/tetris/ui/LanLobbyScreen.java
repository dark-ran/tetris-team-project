package tetris.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.plaf.basic.BasicComboBoxUI;
import tetris.game.BattleMode;

/** P2P 접속과 대기실을 기존 대전 메뉴와 같은 구성으로 표시한다. */
public final class LanLobbyScreen extends JPanel {
    private static final String CONNECTION = "connection";
    private static final String ROOM = "room";

    private final CardLayout layout = new CardLayout();
    private final JPanel cards = ScreenSupport.transparentPanel(layout);
    private final JPanel stack = ScreenSupport.transparentPanel(new BorderLayout(0, 14));
    private final JComboBox<String> hostAddress = new JComboBox<>();
    private final JTextField remoteAddress = new JTextField();
    private final BattleModeSelectPanel modes = new BattleModeSelectPanel();
    private final JLabel status = ScreenSupport.mutedLabel("연결 방식을 선택하세요.");
    private final JLabel role = ScreenSupport.sectionTitle("대기실");
    private final JButton host;
    private final JButton join;
    private final JButton ready;
    private final JButton disconnect;

    public LanLobbyScreen(Runnable onHost, Runnable onJoin, Consumer<BattleMode> onMode,
            Runnable onReady, Runnable onDisconnect, Runnable onBack) {
        ScreenSupport.prepareScreen(this, "P2P 대전",
                "같은 네트워크에 연결된 두 PC에서 대전합니다.");
        host = ScreenSupport.primaryButton("서버 열기", onHost);
        join = ScreenSupport.primaryButton("클라이언트 접속", onJoin);
        ready = ScreenSupport.primaryButton("게임 시작", onReady);
        disconnect = ScreenSupport.button("연결 닫기", onDisconnect);
        JButton back = ScreenSupport.button("시작 메뉴 (Esc)", onBack);
        SwingKeyBindings.backOnEscape(this, onBack);
        remoteAddress.addActionListener(event -> { if (join.isEnabled()) onJoin.run(); });
        modes.setOnSelect(onMode);

        JPanel connections = ScreenSupport.transparentPanel(new GridLayout(1, 2, 16, 0));
        connections.add(connectionCard("서버", "내 PC에서 방을 엽니다.", "서버 주소", hostAddress, host));
        connections.add(connectionCard("클라이언트", "방장에게 받은 주소로 접속합니다.", "상대 IP", remoteAddress, join));
        cards.add(connections, CONNECTION);

        JPanel room = ScreenSupport.framedPanel(new BorderLayout(0, 18));
        room.add(role, BorderLayout.NORTH);
        room.add(modes, BorderLayout.CENTER);
        room.add(ScreenSupport.actionRow(ready), BorderLayout.SOUTH);
        cards.add(room, ROOM);

        status.setFont(AppTheme.font(Font.PLAIN, 14f));
        JPanel feedback = ScreenSupport.framedPanel(new BorderLayout());
        feedback.setBorder(AppTheme.frameBorder(12, 20));
        feedback.add(status, BorderLayout.CENTER);

        stack.add(cards, BorderLayout.CENTER);
        stack.add(feedback, BorderLayout.SOUTH);
        JPanel centered = ScreenSupport.transparentPanel(new GridBagLayout());
        centered.add(stack);
        add(centered, BorderLayout.CENTER);
        add(ScreenSupport.actionRow(disconnect, back), BorderLayout.SOUTH);

        styleAddressFields();
        showConnection(false);
    }

    private static JPanel connectionCard(String title, String description, String fieldName,
            JComponent field, JButton action) {
        JPanel card = ScreenSupport.framedPanel(new BorderLayout(0, 20));
        JPanel introduction = ScreenSupport.transparentPanel(new BorderLayout(0, 8));
        JLabel heading = ScreenSupport.sectionTitle(title);
        heading.setFont(AppTheme.font(Font.BOLD, 19f));
        introduction.add(heading, BorderLayout.NORTH);
        introduction.add(ScreenSupport.mutedLabel(description), BorderLayout.CENTER);
        card.add(introduction, BorderLayout.NORTH);
        JPanel entry = ScreenSupport.transparentPanel(new BorderLayout(0, 8));
        JLabel label = new JLabel(fieldName);
        label.setLabelFor(field);
        entry.add(label, BorderLayout.NORTH);
        entry.add(field, BorderLayout.CENTER);
        JPanel fieldRow = ScreenSupport.transparentPanel(new BorderLayout());
        fieldRow.add(entry, BorderLayout.NORTH);
        card.add(fieldRow, BorderLayout.CENTER);
        card.add(action, BorderLayout.SOUTH);
        return card;
    }

    private void styleAddressFields() {
        hostAddress.setUI(new BasicComboBoxUI() {
            @Override
            public void paintCurrentValueBackground(Graphics graphics, Rectangle bounds, boolean focused) {
                graphics.setColor(AppTheme.SURFACE_RAISED);
                graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }

            @Override
            public void paintCurrentValue(Graphics graphics, Rectangle bounds, boolean focused) {
                Component value = comboBox.getRenderer().getListCellRendererComponent(
                        listBox, hostAddress.getSelectedItem(), -1, false, false);
                currentValuePane.paintComponent(graphics, value, hostAddress, bounds.x, bounds.y,
                        bounds.width, bounds.height, false);
            }

            @Override
            protected JButton createArrowButton() {
                JButton arrow = new JButton("▾");
                arrow.setFont(AppTheme.font(Font.PLAIN, 17f));
                arrow.setForeground(AppTheme.TEXT);
                arrow.setContentAreaFilled(false);
                arrow.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 1, 0, 0, AppTheme.BORDER),
                        BorderFactory.createEmptyBorder(0, 9, 0, 9)));
                return arrow;
            }
        });
        hostAddress.setBackground(AppTheme.SURFACE_RAISED);
        hostAddress.setForeground(AppTheme.TEXT);
        hostAddress.setFont(AppTheme.font(Font.PLAIN, 14f));
        hostAddress.setPreferredSize(new Dimension(210, 40));
        hostAddress.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER));
        hostAddress.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean selected, boolean focused) {
                super.getListCellRendererComponent(list, value, index, selected, focused);
                setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 8));
                setBackground(selected ? AppTheme.YELLOW : AppTheme.SURFACE_RAISED);
                setForeground(selected ? AppTheme.BACKGROUND
                        : hostAddress.isEnabled() ? AppTheme.TEXT : AppTheme.MUTED);
                return this;
            }
        });
        remoteAddress.setBackground(AppTheme.SURFACE_RAISED);
        remoteAddress.setForeground(AppTheme.TEXT);
        remoteAddress.setDisabledTextColor(AppTheme.MUTED);
        remoteAddress.setCaretColor(AppTheme.YELLOW);
        remoteAddress.setSelectionColor(AppTheme.YELLOW);
        remoteAddress.setSelectedTextColor(AppTheme.BACKGROUND);
        remoteAddress.setFont(AppTheme.font(Font.PLAIN, 14f));
        remoteAddress.setPreferredSize(new Dimension(210, 40));
        remoteAddress.setBorder(AppTheme.frameBorder(8, 10));
    }

    public void setHostAddresses(List<String> addresses) {
        addresses.forEach(hostAddress::addItem);
    }

    public String hostAddress() {
        return (String) hostAddress.getSelectedItem();
    }

    public String remoteAddress() {
        return remoteAddress.getText();
    }

    public void setRemoteAddress(String address) {
        remoteAddress.setText(address);
    }

    public void setMode(BattleMode mode) {
        modes.setSelected(mode);
    }

    public void showConnection(boolean connecting) {
        host.setEnabled(!connecting);
        join.setEnabled(!connecting);
        hostAddress.setEnabled(!connecting);
        remoteAddress.setEnabled(!connecting);
        disconnect.setVisible(connecting);
        disconnect.setText("접속 취소");
        showCard(CONNECTION, 296);
    }

    public void showRoom(boolean hosting, boolean canSelect, boolean canReady, boolean localReady) {
        role.setText(hosting ? "대기실 · 서버" : "대기실 · 클라이언트");
        modes.setSelectable(canSelect);
        ready.setEnabled(canReady);
        ready.setText(localReady ? "상대 대기 중" : "게임 시작");
        disconnect.setText("연결 닫기");
        disconnect.setVisible(true);
        showCard(ROOM, 352);
    }

    private void showCard(String card, int height) {
        stack.setPreferredSize(new Dimension(700, height));
        layout.show(cards, card);
    }

    public void showStatus(String message, boolean warning) {
        status.setText(message);
        status.setForeground(warning ? AppTheme.ORANGE : AppTheme.TEXT);
    }

    public void focusConnection() {
        SwingKeyBindings.focus(remoteAddress.getText().isBlank() ? host : remoteAddress);
    }

    public void focusRoom(boolean hosting) {
        if (hosting) modes.focusFirst();
        else SwingKeyBindings.focus(ready);
    }

    public void focusDisconnect() {
        SwingKeyBindings.focus(disconnect);
    }
}
