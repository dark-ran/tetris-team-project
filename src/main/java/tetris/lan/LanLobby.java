package tetris.lan;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import tetris.app.AppController;
import tetris.game.BattleMode;
import tetris.game.BattleOutcome;
import tetris.game.BattleRules;
import tetris.game.GameAction;
import tetris.game.Difficulty;
import tetris.game.LanSnapshotCodec;
import tetris.game.PlayerSession;
import tetris.game.PlayerSnapshot;
import tetris.settings.GameSettings;
import tetris.settings.SettingsRepository;
import tetris.ui.BattleScreen;
import tetris.ui.LanLobbyScreen;
import tetris.ui.SwingKeyBindings;

/** LAN lobby and match state. All state transitions run on the Swing event thread. */
public final class LanLobby extends JPanel implements AutoCloseable {
    private enum Message { MODE, READY, START, INPUT, BOARDS, RESULT, BACK, PING, PONG, PAUSE, RESUME }
    private enum Phase { IDLE, CONNECTING, LOBBY, PLAYING, RESULT }
    private interface Payload { void write(DataOutputStream out) throws IOException; }

    private final AppController app;
    private final Path recentFile;
    private final int port;
    private final LanLobbyScreen screen;
    private final Timer timer = new Timer(20, event -> tick());
    private final PlayerSession[] players = {new PlayerSession(), new PlayerSession()};
    private final long[] nextDrop = new long[2];
    private Phase phase = Phase.IDLE;
    private BattleMode mode = BattleMode.NORMAL;
    private boolean host;
    private boolean hasMode;
    private boolean localReady;
    private boolean peerReady;
    private boolean peerBack = true;
    private boolean dirty;
    private int generation;
    private int modeVersion;
    private int round;
    private long deadline;
    private long nextReport;
    private long nextPing;
    private long lastPong;
    private int slowPings;
    private boolean lagging;
    private boolean localPaused;
    private boolean peerPaused;
    private long pausedAt;
    private ServerSocket listener;
    private Socket connectingSocket;
    private LanLink link;

    public LanLobby(AppController app) {
        this(app, SettingsRepository.defaultPath().resolveSibling("recent-lan-ip.txt"), LanLink.PORT);
    }

    LanLobby(AppController app, Path recentFile, int port) {
        super(new BorderLayout());
        this.app = app;
        this.recentFile = recentFile;
        this.port = port;
        screen = new LanLobbyScreen(() -> open(true), () -> open(false), this::changeMode,
                this::ready, () -> reset("연결을 닫았습니다."), this::leaveLobby);
        add(screen, BorderLayout.CENTER);
        refreshAddresses();
        try {
            if (Files.isRegularFile(recentFile)) screen.setRemoteAddress(Files.readString(recentFile).trim());
        } catch (IOException ignored) {
            // A missing recent address does not prevent an IP from being entered.
        }
        syncControls();
    }

    private void leaveLobby() {
        close();
        screen.showStatus("연결 방식을 선택하세요.", false);
        app.showStartMenu();
    }

    public void showLobby() {
        syncControls();
        if (phase == Phase.IDLE) screen.focusConnection();
        if (phase == Phase.LOBBY) screen.focusRoom(host && peerBack);
    }

    private void refreshAddresses() {
        List<String> candidates = new ArrayList<>();
        try {
            for (NetworkInterface network : NetworkInterface.networkInterfaces().toList()) {
                if (!network.isUp() || network.isLoopback() || network.isVirtual()) continue;
                network.inetAddresses().filter(Inet4Address.class::isInstance)
                        .filter(ip -> !ip.isLinkLocalAddress())
                        .map(InetAddress::getHostAddress).forEach(candidates::add);
            }
        } catch (IOException ignored) {
            // Loopback remains useful to run two instances for development.
        }
        candidates.add("127.0.0.1");
        screen.setHostAddresses(candidates.stream().distinct().toList());
    }

    private static InetAddress ipv4(String raw) throws IOException {
        String[] parts = raw.trim().split("\\.", -1);
        byte[] bytes = new byte[4];
        if (parts.length != bytes.length) throw new IOException("IPv4 주소를 입력하세요.");
        for (int i = 0; i < bytes.length; i++) {
            if (!parts[i].matches("[0-9]{1,3}")) throw new IOException("IPv4 주소를 입력하세요.");
            int part = Integer.parseInt(parts[i]);
            if (part > 255) throw new IOException("IPv4 주소를 입력하세요.");
            bytes[i] = (byte) part;
        }
        return InetAddress.getByAddress(bytes);
    }

    private void open(boolean bind) {
        if (phase != Phase.IDLE) return;
        InetAddress address;
        try {
            address = ipv4(bind ? screen.hostAddress() : screen.remoteAddress());
            if (bind) listener = new ServerSocket();
            else connectingSocket = new Socket();
        } catch (IOException e) {
            screen.showStatus(e.getMessage(), true);
            return;
        }
        int attempt = ++generation;
        ServerSocket server = listener;
        Socket socket = connectingSocket;
        phase = Phase.CONNECTING;
        host = bind;
        screen.showStatus(bind ? "서버 여는 중…" : address.getHostAddress() + "에 연결 중…", false);
        syncControls();
        screen.focusDisconnect();
        Thread.startVirtualThread(() -> {
            try {
                LanLink opened;
                if (bind) {
                    server.setReuseAddress(true);
                    server.bind(new InetSocketAddress(address, port));
                    onEvent(attempt, () -> screen.showStatus("서버 대기 중 · " + address.getHostAddress()
                            + " · TCP " + server.getLocalPort(), false));
                    try (server) {
                        opened = LanLink.accept(server);
                    }
                } else {
                    opened = LanLink.connect(socket, address, port);
                }
                SwingUtilities.invokeLater(() -> {
                    if (attempt != generation) opened.close();
                    else connected(opened, address, attempt);
                });
            } catch (IOException e) {
                onEvent(attempt, () -> reset("연결하지 못했습니다. " + reason(e)));
            }
        });
    }

    private void connected(LanLink opened, InetAddress address, int attempt) {
        link = opened;
        connectingSocket = null;
        listener = null;
        phase = Phase.LOBBY;
        modeVersion = 0;
        hasMode = host;
        round = 0;
        if (!host) mode = BattleMode.NORMAL;
        screen.setMode(host ? mode : null);
        syncControls();
        if (host) screen.focusRoom(true);
        if (!host) {
            Thread.startVirtualThread(() -> {
                try {
                    Files.createDirectories(recentFile.toAbsolutePath().getParent());
                    Files.writeString(recentFile, address.getHostAddress());
                } catch (IOException ignored) {
                    // Saving this convenience setting does not affect the connection.
                }
            });
        }
        opened.start(data -> onEvent(attempt, () -> receive(data)),
                error -> onEvent(attempt, () -> reset("연결이 끊겼습니다. " + reason(error))));
        lastPong = System.nanoTime();
        nextPing = lastPong;
        timer.start();
        if (host) sendMode();
    }

    private void onEvent(int attempt, Runnable action) {
        SwingUtilities.invokeLater(() -> { if (attempt == generation) action.run(); });
    }

    private void changeMode(BattleMode selected) {
        if (!host || phase != Phase.LOBBY || mode == selected) return;
        mode = selected;
        modeVersion++;
        localReady = peerReady = false;
        sendMode();
        syncControls();
    }

    private void sendMode() {
        send(Message.MODE, out -> { out.writeInt(modeVersion); out.writeByte(mode.ordinal()); });
    }

    private void ready() {
        if (phase != Phase.LOBBY || localReady || !peerBack || !hasMode) return;
        localReady = true;
        send(Message.READY, out -> out.writeInt(modeVersion));
        syncControls();
        startWhenReady();
    }

    private void startWhenReady() {
        if (!host || phase != Phase.LOBBY || !localReady || !peerReady || !peerBack) return;
        round++;
        send(Message.START, out -> out.writeInt(round));
        if (link == null) return;
        startRound();
        var settings = app.snapshot().settings();
        Difficulty difficulty = settings == null ? Difficulty.NORMAL : settings.difficulty();
        players[0].setAttackListener(players[1]::receiveAttack);
        players[1].setAttackListener(players[0]::receiveAttack);
        long now = System.nanoTime();
        for (int i = 0; i < players.length; i++) {
            players[i].start(mode, difficulty);
            nextDrop[i] = now + TimeUnit.MILLISECONDS.toNanos(players[i].gravityIntervalMillis());
        }
        deadline = now + TimeUnit.MILLISECONDS.toNanos(BattleRules.DEFAULT_TIME_LIMIT_MILLIS);
        nextReport = now;
        dirty = true;
        publishBoards(now);
    }

    private void startRound() {
        phase = Phase.PLAYING;
        peerBack = false;
        localReady = peerReady = false;
        localPaused = peerPaused = false;
        pausedAt = 0;
        app.showBattleScreen(mode, "나", "상대");
        configureBattleControls();
    }

    private void configureBattleControls() {
        BattleScreen battle = app.battleScreen();
        battle.clearKeys();
        var settings = app.snapshot().settings();
        var keys = settings == null ? GameSettings.DEFAULT_KEY_BINDINGS : settings.keyBindings();
        List<String> guide = new ArrayList<>();
        GameAction[] actions = {GameAction.LEFT, GameAction.RIGHT, GameAction.DOWN,
                GameAction.ROTATE_CLOCKWISE, GameAction.HARD_DROP};
        String[] labels = {"왼쪽", "오른쪽", "내리기", "회전", "떨어뜨리기"};
        for (int i = 0; i < actions.length; i++) {
            int key = keys.get(actions[i].name());
            bind(battle, key, actions[i]);
            guide.add(SwingKeyBindings.keyText(key) + " " + labels[i]);
        }
        guide.add("Esc 메뉴");
        battle.setControlsGuide(String.join(" · ", guide), "");
        battle.bindKey(KeyEvent.VK_ESCAPE, this::pauseLocal, null);
        battle.bindKey(keys.get(GameAction.QUIT.name()), () -> reset("대전에서 나왔습니다."), null);
        battle.setMenuActions(this::resumeLocal, app::showSettings,
                this::leaveLobby, () -> { close(); app.exit(); });
        updateBattleStatus();
    }

    private boolean paused() {
        return localPaused || peerPaused;
    }

    private void pauseLocal() {
        if (phase != Phase.PLAYING || localPaused) return;
        if (!paused()) pausedAt = System.nanoTime();
        localPaused = true;
        app.battleScreen().showMenu();
        updateBattleStatus();
        send(Message.PAUSE, out -> out.writeInt(round));
    }

    private void resumeLocal() {
        if (phase != Phase.PLAYING || !localPaused) return;
        localPaused = false;
        configureBattleControls();
        app.battleScreen().hideMenu();
        finishPauseIfClear();
        updateBattleStatus();
        send(Message.RESUME, out -> out.writeInt(round));
    }

    private void finishPauseIfClear() {
        if (paused() || pausedAt == 0) return;
        long elapsed = Math.max(0, System.nanoTime() - pausedAt);
        pausedAt = 0;
        deadline += elapsed;
        for (int i = 0; i < nextDrop.length; i++) nextDrop[i] += elapsed;
        if (host && phase == Phase.PLAYING) publishBoards(System.nanoTime());
    }

    private void updateBattleStatus() {
        if (phase != Phase.PLAYING) return;
        String message = peerPaused ? "상대 일시정지"
                : localPaused ? "일시정지"
                : lagging ? "네트워크 지연" : null;
        app.battleScreen().setStatusMessage(message, peerPaused || localPaused || lagging);
    }

    private void bind(BattleScreen screen, int key, GameAction action) {
        screen.bindKey(key, () -> {
            if (phase != Phase.PLAYING || paused()) return;
            if (host) apply(0, action);
            else send(Message.INPUT, out -> { out.writeInt(round); out.writeByte(action.ordinal()); });
        }, null);
    }

    private void apply(int player, GameAction action) {
        long now = System.nanoTime();
        if (mode.isTimeLimited() && now >= deadline) {
            publishBoards(now);
            return;
        }
        var result = players[player].apply(action);
        dirty = true;
        if (result.clearedRows() > 0 || now >= nextReport || players[player].isGameOver()) publishBoards(now);
    }

    private void publishBoards(long now) {
        if (phase != Phase.PLAYING) return;
        PlayerSnapshot first = players[0].snapshot();
        PlayerSnapshot second = players[1].snapshot();
        long remaining = mode.isTimeLimited()
                ? Math.max(0, TimeUnit.NANOSECONDS.toMillis(deadline - now)) : -1;
        render(first, second, remaining);
        send(Message.BOARDS, out -> {
            out.writeInt(round);
            out.writeLong(remaining);
            LanSnapshotCodec.write(out, first);
            LanSnapshotCodec.write(out, second);
        });
        if (link == null) return;
        dirty = false;
        nextReport = now + TimeUnit.MILLISECONDS.toNanos(mode.isTimeLimited() ? 80 : 33);
        BattleOutcome result = BattleRules.decide(first, second, mode.isTimeLimited() && now >= deadline);
        if (result.isFinished()) {
            send(Message.RESULT, out -> { out.writeInt(round); out.writeByte(result.ordinal()); });
            if (link != null) result(result);
        }
    }

    private void render(PlayerSnapshot first, PlayerSnapshot second, long remaining) {
        BattleScreen screen = app.battleScreen();
        screen.render(host ? first : second, host ? second : first);
        if (remaining >= 0) screen.setRemainingMillis(remaining);
        else screen.hideTimer();
    }

    private void result(BattleOutcome outcome) {
        phase = Phase.RESULT;
        localPaused = peerPaused = false;
        pausedAt = 0;
        BattleOutcome relative = outcome;
        if (!host && outcome == BattleOutcome.PLAYER1_WIN) relative = BattleOutcome.PLAYER2_WIN;
        if (!host && outcome == BattleOutcome.PLAYER2_WIN) relative = BattleOutcome.PLAYER1_WIN;
        BattleScreen screen = app.battleScreen();
        screen.clearKeys();
        screen.showResult(relative, "대기실로", this::back, "연결 닫기", () -> reset("연결을 닫았습니다."));
    }

    private void back() {
        phase = Phase.LOBBY;
        send(Message.BACK, out -> out.writeInt(round));
        app.showP2PLobby();
    }

    private void tick() {
        if (link == null) return;
        long now = System.nanoTime();
        if (now - lastPong > TimeUnit.SECONDS.toNanos(5)) {
            reset("연결이 끊겼습니다. 상대의 응답이 없습니다.");
            return;
        }
        if (now >= nextPing) {
            nextPing = now + TimeUnit.MILLISECONDS.toNanos(500);
            send(Message.PING, out -> out.writeLong(now));
        }
        if (!host || phase != Phase.PLAYING || paused()) return;
        if (mode.isTimeLimited() && now >= deadline) {
            publishBoards(now);
            return;
        }
        for (int i = 0; i < players.length && phase == Phase.PLAYING; i++) {
            if (now >= nextDrop[i]) {
                apply(i, GameAction.TICK);
                nextDrop[i] = now + TimeUnit.MILLISECONDS.toNanos(players[i].gravityIntervalMillis());
            }
        }
        if (phase == Phase.PLAYING && now >= nextReport && (dirty || mode.isTimeLimited())) publishBoards(now);
    }

    private void receive(byte[] payload) {
        try (var in = new DataInputStream(new ByteArrayInputStream(payload))) {
            Message kind = readEnum(in, Message.values());
            switch (kind) {
                case MODE -> {
                    require(!host && phase == Phase.LOBBY);
                    modeVersion = in.readInt();
                    boolean firstMode = !hasMode;
                    hasMode = true;
                    mode = readEnum(in, BattleMode.values());
                    localReady = peerReady = false;
                    screen.setMode(mode);
                    syncControls();
                    if (firstMode) screen.focusRoom(false);
                }
                case READY -> {
                    int version = in.readInt();
                    if (phase != Phase.LOBBY || version != modeVersion) return;
                    peerReady = true;
                    syncControls();
                    startWhenReady();
                }
                case START -> {
                    require(!host && phase == Phase.LOBBY && localReady && peerReady);
                    int announced = in.readInt();
                    require(announced == round + 1);
                    round = announced;
                    startRound();
                }
                case INPUT -> {
                    require(host);
                    int inputRound = in.readInt();
                    GameAction action = readEnum(in, GameAction.values());
                    require(action.ordinal() < GameAction.TICK.ordinal());
                    if (inputRound != round || phase != Phase.PLAYING || paused()) return;
                    apply(1, action);
                }
                case BOARDS -> {
                    require(!host);
                    if (in.readInt() != round || phase != Phase.PLAYING) return;
                    long remaining = in.readLong();
                    render(LanSnapshotCodec.read(in, mode), LanSnapshotCodec.read(in, mode), remaining);
                }
                case RESULT -> {
                    require(!host);
                    if (in.readInt() != round || phase != Phase.PLAYING) return;
                    BattleOutcome outcome = readEnum(in, BattleOutcome.values());
                    require(outcome.isFinished());
                    result(outcome);
                }
                case BACK -> {
                    if (in.readInt() != round || (phase != Phase.RESULT && phase != Phase.LOBBY)) return;
                    peerBack = true;
                    syncControls();
                }
                case PING -> {
                    long stamp = in.readLong();
                    send(Message.PONG, out -> out.writeLong(stamp));
                }
                case PONG -> pong(in.readLong());
                case PAUSE -> {
                    if (in.readInt() != round || phase != Phase.PLAYING) return;
                    if (!paused()) pausedAt = System.nanoTime();
                    peerPaused = true;
                    updateBattleStatus();
                }
                case RESUME -> {
                    if (in.readInt() != round || phase != Phase.PLAYING) return;
                    peerPaused = false;
                    finishPauseIfClear();
                    updateBattleStatus();
                }
            }
            require(in.available() == 0);
        } catch (IOException e) {
            reset("연결을 닫았습니다. " + reason(e));
        }
    }

    private void pong(long stamp) throws IOException {
        long now = System.nanoTime();
        require(now - stamp >= 0);
        lastPong = now;
        slowPings = now - stamp > TimeUnit.MILLISECONDS.toNanos(200) ? slowPings + 1 : 0;
        lagging = slowPings >= 3;
        updateBattleStatus();
    }

    private void send(Message kind, Payload payload) {
        if (link == null) return;
        try {
            var bytes = new ByteArrayOutputStream();
            try (var out = new DataOutputStream(bytes)) {
                out.writeByte(kind.ordinal());
                payload.write(out);
            }
            if (!link.send(bytes.toByteArray())) reset("연결이 끊겼거나 전송이 지연되었습니다.");
        } catch (IOException error) {
            reset("메시지를 보내지 못했습니다.");
        }
    }

    private static <T> T readEnum(DataInputStream in, T[] values) throws IOException {
        int index = in.readUnsignedByte();
        require(index < values.length);
        return values[index];
    }

    private static void require(boolean valid) throws IOException {
        if (!valid) throw new IOException("지원하지 않는 대전 메시지입니다.");
    }

    private static String reason(IOException e) {
        String message = e.getMessage();
        return message == null ? "상대와의 연결이 종료되었습니다." : message;
    }

    private void syncControls() {
        if (phase == Phase.LOBBY) {
            screen.showRoom(host, host && peerBack, peerBack && hasMode && !localReady, localReady);
            screen.showStatus(peerBack ? "접속 완료 · 상대 " + (peerReady ? "준비 완료" : "대기 중")
                    : "상대가 대기실로 돌아오기를 기다리는 중입니다.", false);
        } else {
            screen.showConnection(phase == Phase.CONNECTING);
        }
    }

    private void reset(String message) {
        boolean inGame = phase == Phase.PLAYING || phase == Phase.RESULT;
        close();
        screen.showStatus(message, true);
        if (inGame) app.showP2PLobby();
        else screen.focusConnection();
    }

    @Override public void close() {
        generation++;
        phase = Phase.IDLE;
        localReady = peerReady = lagging = localPaused = peerPaused = false;
        pausedAt = 0;
        if (app.battleScreen().isMenuVisible()) app.battleScreen().hideMenu();
        peerBack = true;
        slowPings = 0;
        timer.stop();
        if (link != null) app.battleScreen().clearKeys();
        if (link != null) link.close();
        closeSocket(listener);
        closeSocket(connectingSocket);
        listener = null;
        connectingSocket = null;
        link = null;
        syncControls();
    }

    private static void closeSocket(AutoCloseable socket) {
        if (socket == null) return;
        try {
            socket.close();
        } catch (Exception ignored) {
            // State is idle after reset even if the socket was already closed.
        }
    }
}
