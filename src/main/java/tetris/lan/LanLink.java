package tetris.lan;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

final class LanLink implements AutoCloseable {
    static final int PORT = 50555;
    private static final int MAGIC = 0x544C414E;
    private static final int VERSION = 2;
    private static final int MAX_FRAME = 4096;
    private final Socket socket;
    private final DataInputStream input;
    private final DataOutputStream output;
    private final ArrayBlockingQueue<byte[]> outgoing = new ArrayBlockingQueue<>(64);
    private final AtomicBoolean closed = new AtomicBoolean();
    private volatile Thread writer;

    static LanLink connect(Socket socket, InetAddress address, int port) throws IOException {
        socket.connect(new InetSocketAddress(address, port), 3000);
        return new LanLink(socket);
    }

    static LanLink accept(ServerSocket server) throws IOException {
        return new LanLink(server.accept());
    }

    private LanLink(Socket socket) throws IOException {
        this.socket = socket;
        try {
            socket.setTcpNoDelay(true);
            socket.setSoTimeout(5000);
            output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            output.writeInt(MAGIC);
            output.writeByte(VERSION);
            output.flush();
            if (input.readInt() != MAGIC || input.readUnsignedByte() != VERSION) {
                throw new IOException("게임 버전이 다릅니다.");
            }
        } catch (IOException e) {
            socket.close();
            throw e;
        }
    }

    void start(Consumer<byte[]> receive, Consumer<IOException> failure) {
        writer = Thread.startVirtualThread(() -> {
            try {
                while (!closed.get()) {
                    byte[] frame = outgoing.take();
                    output.writeShort(frame.length);
                    output.write(frame);
                    output.flush();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                if (!closed.get()) failure.accept(e);
                close();
            }
        });
        Thread.startVirtualThread(() -> {
            try {
                while (!closed.get()) {
                    int length = input.readUnsignedShort();
                    if (length < 1 || length > MAX_FRAME) throw new IOException("잘못된 메시지 길이입니다.");
                    byte[] frame = input.readNBytes(length);
                    if (frame.length != length) throw new EOFException();
                    receive.accept(frame);
                }
            } catch (SocketTimeoutException e) {
                if (!closed.get()) failure.accept(new IOException("상대의 응답이 5초 동안 없습니다.", e));
            } catch (IOException e) {
                if (!closed.get()) failure.accept(e);
            } finally {
                close();
            }
        });
    }

    boolean send(byte[] frame) {
        return frame.length > 0 && frame.length <= MAX_FRAME && !closed.get() && outgoing.offer(frame);
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        try {
            socket.close();
        } catch (IOException ignored) {
            // The connection is already being discarded.
        }
        if (writer != null) writer.interrupt();
    }
}
