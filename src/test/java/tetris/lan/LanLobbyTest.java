package tetris.lan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
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
import tetris.game.GameAction;
import tetris.game.GameEngine;
import tetris.game.PieceSpawnPolicy;
import tetris.game.PlayerSession;
import tetris.game.PlayerSnapshot;
import tetris.piece.PieceQueue;
import tetris.piece.PieceType;
import tetris.piece.Tetromino;
import tetris.loop.GameLoop;
import java.util.Map;
import tetris.scoreboard.ScoreBoardService;
import tetris.scoreboard.ScoreRepository;
import tetris.settings.ColorVisionMode;
import tetris.settings.GameSettings;
import tetris.settings.SettingsRepository;
import tetris.settings.SettingsService;

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
    void lineClearsReachBothBoardsEvenWhenDropsShareOneBroadcastInterval() throws Exception {
        Peer host = onEdt(() -> openLobby("clear-host", 0));
        Peer client = null;
        try {
            // Deterministic pieces let normal bound keys fill two rows in five drops.
            var field = LanLobby.class.getDeclaredField("players");
            field.setAccessible(true);
            PlayerSession[] players = (PlayerSession[]) field.get(host.lobby);
            for (int player = 0; player < players.length; player++) {
                AtomicInteger columns = new AtomicInteger();
                PieceSpawnPolicy spawn = new PieceSpawnPolicy() {
                    @Override public int startingRow(Tetromino piece) { return 0; }
                    @Override public int startingColumn(Tetromino piece) { return columns.getAndIncrement() % 5 * 2; }
                };
                players[player] = new PlayerSession(new GameEngine(
                        () -> PieceQueue.generatedBy(() -> new Tetromino(PieceType.O), 1), spawn));
            }
            onEdt(() -> {
                component(host.lobby, JComboBox.class).setSelectedItem("127.0.0.1");
                button(host.lobby, "서버 열기").doClick(0);
                return null;
            });
            await(() -> text(host.lobby).contains("서버 대기 중"));
            int port = onEdt(() -> {
                String label = text(host.lobby);
                return Integer.parseInt(label.substring(label.lastIndexOf("TCP ") + 4).trim());
            });
            Peer guest = onEdt(() -> openLobby("clear-guest", port));
            client = guest;
            connect(guest, "127.0.0.1");
            await(() -> isGameStartAvailable(host) && isGameStartAvailable(guest));
            startBoth(host, guest);
            onEdt(() -> {
                drop(host, 5);
                return null;
            });
            await(() -> boardSnapshot(guest, "상대 보드") != null
                    && boardSnapshot(guest, "상대 보드").totalClearedRows() == 2);
            onEdt(() -> {
                assertEquals(List.of(18, 19), boardSnapshot(guest, "상대 보드").lineClear().rows());
                assertEquals(boardPixel(host, "나 보드"), boardPixel(guest, "상대 보드"));
                // Rendering the highlight must differ from the same board after cancelling the timer.
                JComponent board = board(guest, "상대 보드");
                int highlighted = boardPixel(guest, "상대 보드");
                board.removeNotify();
                assertTrue(highlighted != boardPixel(guest, "상대 보드"));
                drop(guest, 5);
                return null;
            });
            await(() -> boardSnapshot(host, "상대 보드").totalClearedRows() == 2);
            onEdt(() -> {
                assertEquals(2, boardSnapshot(host, "상대 보드").lineClear().sequence());
                assertEquals(boardPixel(host, "상대 보드"), boardPixel(guest, "나 보드"));
                return null;
            });
        } finally {
            Peer guest = client;
            onEdt(() -> {
                host.lobby.close();
                host.app.exit();
                if (guest != null) { guest.lobby.close(); guest.app.exit(); }
                return null;
            });
        }
    }

    private static void drop(Peer peer, int count) {
        var screen = peer.app.battleScreen();
        Object name = screen.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .get(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0));
        for (int i = 0; i < count; i++) {
            screen.getActionMap().get(name).actionPerformed(new ActionEvent(screen, 0, "drop"));
        }
    }

    private static JComponent board(Peer peer, String name) {
        return tree(peer.app.battleScreen()).filter(JComponent.class::isInstance)
                .map(JComponent.class::cast).filter(part -> part.getAccessibleContext() != null
                        && name.equals(part.getAccessibleContext().getAccessibleName())).findFirst().orElseThrow();
    }

    private static PlayerSnapshot boardSnapshot(Peer peer, String name) throws Exception {
        JComponent board = board(peer, name);
        var method = board.getClass().getDeclaredMethod("snapshot");
        method.setAccessible(true);
        return (PlayerSnapshot) method.invoke(board);
    }

    private static int boardPixel(Peer peer, String name) {
        JComponent board = board(peer, name);
        board.setSize(272, 532);
        BufferedImage image = new BufferedImage(272, 532, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        try { board.paint(graphics); } finally { graphics.dispose(); }
        return image.getRGB(6 + 4 * 26 + 4, 6 + 19 * 26 + 4);
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

    @Test
    void pauseSynchronizesAcrossPeersAndAppliesUpdatedSettingsOnResume() throws Exception {
        Peer host = onEdt(() -> openLobby("pause-host", 0));
        Peer client = null;
        try {
            onEdt(() -> {
                component(host.lobby, JComboBox.class).setSelectedItem("127.0.0.1");
                button(host.lobby, "서버 열기").doClick(0);
                return null;
            });
            await(() -> text(host.lobby).contains("서버 대기 중"));
            int port = onEdt(() -> {
                String label = text(host.lobby);
                return Integer.parseInt(label.substring(label.lastIndexOf("TCP ") + 4).trim());
            });
            Peer guest = onEdt(() -> openLobby("pause-guest", port));
            client = guest;
            connect(guest, "127.0.0.1");
            await(() -> isGameStartAvailable(host) && isGameStartAvailable(guest));
            startBoth(host, guest);
            await(() -> boardSnapshot(guest, "나 보드") != null);

            long guestScoreBefore = onEdt(() -> boardSnapshot(guest, "나 보드").score());
            onEdt(() -> {
                pressKey(guest, KeyEvent.VK_ESCAPE);
                assertTrue(guest.app.battleScreen().isMenuVisible());
                return null;
            });
            await(() -> "상대 일시정지".equals( labelText(host)));
            onEdt(() -> {
                // While paused, neither peer's drop key advances the board.
                pressKey(guest, KeyEvent.VK_SPACE);
                pressKey(host, KeyEvent.VK_SPACE);
                assertEquals(guestScoreBefore, boardSnapshot(guest, "나 보드").score());
                // Host also opens pause menu while guest is in settings.
                pressKey(host, KeyEvent.VK_ESCAPE);
                assertTrue(host.app.battleScreen().isMenuVisible());
                button(guest.app.battleScreen(), "설정").doClick(0);
                assertEquals(AppState.SETTINGS, guest.app.state());
                GameSettings updated = guest.app.snapshot().settings()
                        .withColorVisionMode(ColorVisionMode.PROTAN)
                        .withKeyBindings(Map.of(GameAction.HARD_DROP.name(), KeyEvent.VK_F));
                guest.app.updateSettings(updated);
                guest.app.closeSettings();
                assertEquals(AppState.BATTLE, guest.app.state());
                assertTrue(guest.app.battleScreen().isMenuVisible());
                button(guest.app.battleScreen(), "게임으로 돌아가기").doClick(0);
                assertFalse(guest.app.battleScreen().isMenuVisible());
                return null;
            });
            // Host is still paused, so guest now sees that the peer is paused.
            await(() -> "상대 일시정지".equals(labelText(guest)));
            onEdt(() -> {
                pressKey(host, KeyEvent.VK_ESCAPE);
                assertFalse(host.app.battleScreen().isMenuVisible());
                return null;
            });
            await(() -> labelText(host).isEmpty() && labelText(guest).isEmpty());
            onEdt(() -> {
                pressKey(guest, KeyEvent.VK_F);
                return null;
            });
            await(() -> boardSnapshot(guest, "나 보드").score() > guestScoreBefore);
            onEdt(() -> {
                pressKey(host, KeyEvent.VK_ESCAPE);
                button(host.app.battleScreen(), "시작 메뉴로 이동").doClick(0);
                assertEquals(AppState.START_MENU, host.app.state());
                return null;
            });
            await(() -> guest.app.state() == AppState.P2P_LOBBY && text(guest.lobby).contains("끊겼습니다"));
        } finally {
            Peer guest = client;
            onEdt(() -> {
                host.lobby.close();
                host.app.exit();
                if (guest != null) { guest.lobby.close(); guest.app.exit(); }
                return null;
            });
        }
    }

    private static void pressKey(Peer peer, int keyCode) {
        var screen = peer.app.battleScreen();
        Object name = screen.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .get(KeyStroke.getKeyStroke(keyCode, 0, false));
        if (name != null) {
            screen.getActionMap().get(name).actionPerformed(new ActionEvent(screen, 0, "key"));
        }
    }

    private static String labelText(Peer peer) throws Exception {
        var method = peer.app.battleScreen().getClass().getDeclaredMethod("statusText");
        method.setAccessible(true);
        return (String) method.invoke(peer.app.battleScreen());
    }

    private Peer openLobby(String name, int port) {
        var scores = new ScoreBoardService(new ScoreRepository(directory.resolve(name + "-scores")));
        var settings = new SettingsService(new SettingsRepository(directory.resolve(name + "-settings.properties")));
        AppController app = new AppController(() -> {}, new GameEngine(), new GameLoop(),
                ignored -> {}, ignored -> {}, scores, settings, null);
        app.start();
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
