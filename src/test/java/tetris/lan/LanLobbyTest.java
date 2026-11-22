package tetris.lan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tetris.app.AppController;
import tetris.app.AppState;
import tetris.game.BattleMode;
import tetris.game.GameEngine;
import tetris.loop.GameLoop;
import tetris.scoreboard.ScoreBoardService;
import tetris.scoreboard.ScoreRepository;

class LanLobbyTest {
    @TempDir Path directory;

    private record Peer(AppController app, LanLobby lobby) { }

    @Test
    void connectsStartsAfterBothReadyAndReplaysThenDetectsDisconnect() throws Exception {
        Peer host = onEdt(() -> openLobby("host", 0));
        Peer guest = null;
        try {
            onEdt(() -> {
                assertFalse(selectedMode(host, BattleMode.NORMAL));
                component(host.lobby, JComboBox.class).setSelectedItem("127.0.0.1");
                button(host.lobby, "서버 열기").doClick(0);
                return null;
            });
            await(() -> text(host.lobby).contains("서버 대기 중"));
            int port = onEdt(() -> {
                String label = text(host.lobby);
                return Integer.parseInt(label.substring(label.lastIndexOf("TCP ") + 4).trim());
            });
            Peer client = onEdt(() -> openLobby("guest", port));
            guest = client;
            connect(client, "127.0.0.1");
            await(() -> isGameStartAvailable(client));
            await(() -> Files.exists(directory.resolve("guest.txt")));
            assertEquals("127.0.0.1", Files.readString(directory.resolve("guest.txt")));
            onEdt(() -> {
                button(client.lobby, "게임 시작").doClick(0);
                assertEquals(AppState.P2P_LOBBY, client.app.state());
                return null;
            });
            await(() -> text(host.lobby).contains("준비 완료"));
            onEdt(() -> {
                button(host.lobby, BattleMode.ITEM.toString()).doClick(0);
                return null;
            });
            await(() -> selectedMode(client, BattleMode.ITEM)
                    && isGameStartAvailable(client));
            onEdt(() -> {
                assertFalse(button(client.lobby, BattleMode.ITEM.toString()).isEnabled());
                assertTrue(button(client.lobby, BattleMode.ITEM.toString()).isSelected());
                assertFalse(visibleTree(client.lobby).anyMatch(component -> component instanceof JTextField));
                button(host.lobby, BattleMode.NORMAL.toString()).doClick(0);
                return null;
            });
            await(() -> selectedMode(client, BattleMode.NORMAL));
            startBoth(host, client);
            onEdt(() -> {
                var screen = client.app.battleScreen();
                Object action = screen.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                        .get(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0));
                for (int i = 0; i < 30; i++) {
                    screen.getActionMap().get(action).actionPerformed(new ActionEvent(screen, 0, "drop"));
                }
                return null;
            });
            await(() -> host.app.battleScreen().isResultVisible() && client.app.battleScreen().isResultVisible());
            onEdt(() -> {
                button(host.app.battleScreen(), "대기실로").doClick(0);
                button(client.app.battleScreen(), "대기실로").doClick(0);
                return null;
            });
            await(() -> isGameStartAvailable(host) && isGameStartAvailable(client));
            startBoth(host, client);
            onEdt(() -> { host.lobby.close(); return null; });
            await(() -> client.app.state() == AppState.P2P_LOBBY && text(client.lobby).contains("끊겼습니다"));

            Peer restarted = onEdt(() -> openLobby("restarted", port));
            try {
                onEdt(() -> {
                    component(restarted.lobby, JComboBox.class).setSelectedItem("127.0.0.1");
                    button(restarted.lobby, "서버 열기").doClick(0);
                    return null;
                });
                await(() -> text(restarted.lobby).contains("서버 대기 중"));
                connect(client, "127.0.0.1");
                await(() -> isGameStartAvailable(client));
                onEdt(() -> { button(restarted.lobby, BattleMode.TIME_LIMIT.toString()).doClick(0); return null; });
                await(() -> selectedMode(client, BattleMode.TIME_LIMIT));
                startBoth(restarted, client);
            } finally {
                onEdt(() -> { restarted.lobby.close(); restarted.app.exit(); return null; });
            }
        } finally {
            Peer client = guest;
            onEdt(() -> {
                host.lobby.close();
                host.app.exit();
                if (client != null) { client.lobby.close(); client.app.exit(); }
                return null;
            });
        }
    }

    @Test
    void invalidAddressAndOccupiedPortLeaveLobbyAvailable() throws Exception {
        try (var occupied = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            Peer peer = onEdt(() -> openLobby("failed", occupied.getLocalPort()));
            try {
                connect(peer, "300.0.0.1");
                await(() -> text(peer.lobby).contains("IPv4 주소를 입력"));
                onEdt(() -> {
                    component(peer.lobby, JComboBox.class).setSelectedItem("127.0.0.1");
                    button(peer.lobby, "서버 열기").doClick(0);
                    return null;
                });
                await(() -> text(peer.lobby).contains("연결하지 못했습니다"));
                assertTrue(onEdt(() -> button(peer.lobby, "서버 열기").isEnabled()));
            } finally {
                onEdt(() -> { peer.lobby.close(); peer.app.exit(); return null; });
            }
        }
    }

    private Peer openLobby(String name, int port) {
        var scores = new ScoreBoardService(new ScoreRepository(directory.resolve(name + "-scores")));
        AppController app = new AppController(() -> {}, new GameEngine(), new GameLoop(),
                ignored -> {}, ignored -> {}, scores, null, ignored -> {});
        LanLobby panel = new LanLobby(app, directory.resolve(name + ".txt"), port);
        app.setP2PLobby(panel, panel::showLobby);
        app.showP2PLobby();
        return new Peer(app, panel);
    }

    private static void connect(Peer peer, String address) throws Exception {
        onEdt(() -> {
            component(peer.lobby, JTextField.class).setText(address);
            button(peer.lobby, "클라이언트 접속").doClick(0);
            return null;
        });
    }

    private static void startBoth(Peer host, Peer client) throws Exception {
        onEdt(() -> {
            button(host.lobby, "게임 시작").doClick(0);
            assertEquals(AppState.P2P_LOBBY, host.app.state());
            button(client.lobby, "게임 시작").doClick(0);
            return null;
        });
        await(() -> host.app.state() == AppState.BATTLE && client.app.state() == AppState.BATTLE);
    }

    private static boolean isGameStartAvailable(Peer peer) {
        return visibleTree(peer.lobby).filter(JButton.class::isInstance).map(JButton.class::cast)
                .anyMatch(button -> button.isEnabled() && "게임 시작".equals(button.getText()));
    }

    private static boolean selectedMode(Peer peer, BattleMode expected) {
        return visibleTree(peer.lobby).filter(JLabel.class::isInstance).map(JLabel.class::cast)
                .anyMatch(label -> ("선택된 모드: " + expected).equals(label.getText()));
    }

    private static Stream<Component> visibleTree(Component root) {
        if (!root.isVisible()) return Stream.empty();
        if (root instanceof Container container) {
            return Stream.concat(Stream.of(root),
                    Stream.of(container.getComponents()).flatMap(LanLobbyTest::visibleTree));
        }
        return Stream.of(root);
    }

    private static Stream<Component> tree(Component root) {
        if (root instanceof Container c)
            return Stream.concat(Stream.of(c), Stream.of(c.getComponents()).flatMap(LanLobbyTest::tree));
        return Stream.of(root);
    }

    private static JButton button(Component root, String label) {
        return tree(root).filter(JButton.class::isInstance).map(JButton.class::cast)
                .filter(button -> label.equals(button.getText())).findFirst().orElseThrow();
    }

    private static <T> T component(Component root, Class<T> type) {
        return tree(root).filter(type::isInstance).map(type::cast).findFirst().orElseThrow();
    }

    private static String text(Component root) {
        return tree(root).filter(JLabel.class::isInstance).map(JLabel.class::cast)
                .map(JLabel::getText).reduce("", (a, b) -> a + b);
    }

    private static void await(Callable<Boolean> condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (onEdt(condition)) return;
            Thread.sleep(15);
        }
        fail("LAN 상태가 5초 안에 전환되지 않았습니다.");
    }

    private static <T> T onEdt(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        SwingUtilities.invokeAndWait(task);
        return task.get();
    }
}
