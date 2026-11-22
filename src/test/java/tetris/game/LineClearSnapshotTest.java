package tetris.game;

import static org.junit.jupiter.api.Assertions.*;
import static tetris.game.BattleTestSupport.engine;
import static tetris.game.BattleTestSupport.line;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import org.junit.jupiter.api.Test;
import tetris.board.Board;
import tetris.piece.ItemType;
import tetris.piece.PieceType;
import tetris.piece.Tetromino;

class LineClearSnapshotTest {
    @Test
    void capturesTheActualBoardBeforeCompactionAndIncomingAttacks() throws Exception {
        PlayerSession session = new PlayerSession(engine(List.of(new Tetromino(PieceType.O)), 0, 2, 4, 6, 8));
        session.start(BattleMode.NORMAL, Difficulty.NORMAL);
        for (int i = 0; i < 4; i++) session.apply(GameAction.HARD_DROP);
        session.receiveAttack(List.of(line("#########.")));
        assertEquals(2, session.apply(GameAction.HARD_DROP).clearedRows());
        PlayerSnapshot snapshot = session.snapshot();
        LineClearSnapshot clear = snapshot.lineClear();
        assertEquals(List.of(18, 19), clear.rows());
        assertEquals(2, clear.sequence());
        for (int col = 0; col < Board.COLUMNS; col++) {
            assertEquals(PieceType.O.cellValue(), clear.cellAt(19, col));
        }
        assertEquals(Board.GARBAGE_CELL, snapshot.cellAt(19, 0));
        session.apply(GameAction.LEFT);
        assertSame(clear, session.snapshot().lineClear(), "Input and gravity must preserve the last event");
        session.start(BattleMode.NORMAL, Difficulty.NORMAL);
        assertNull(session.snapshot().lineClear());
        assertEquals(PieceType.O.cellValue(), clear.cellAt(19, 0), "Restart must not mutate old events");

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (var out = new ObjectOutputStream(bytes)) { out.writeObject(snapshot); }
        try (var in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            PlayerSnapshot copy = (PlayerSnapshot) in.readObject();
            assertEquals(clear.rows(), copy.lineClear().rows());
            assertEquals(clear.cellAt(19, 0), copy.lineClear().cellAt(19, 0));
        }
    }

    @Test
    void sparseItemClearSurvivesNetworkEncodingAndFitsBothBoardsInOneFrame() throws Exception {
        Tetromino marked = new Tetromino(PieceType.O).withItem(ItemType.LINE_CLEAR, 0);
        PlayerSession session = new PlayerSession(engine(List.of(marked), 0));
        session.start(BattleMode.ITEM, Difficulty.NORMAL);
        session.apply(GameAction.HARD_DROP);
        PlayerSnapshot original = session.snapshot();
        assertEquals(List.of(18), original.lineClear().rows());
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(bytes)) {
            out.writeByte(4); // BOARDS
            out.writeInt(1); // round
            out.writeLong(-1); // remaining time
            LanSnapshotCodec.write(out, original);
            LanSnapshotCodec.write(out, original);
        }
        assertTrue(bytes.size() <= 4096, "Two animation snapshots must fit the transport frame limit");
        try (var in = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            in.skipNBytes(13);
            for (int player = 0; player < 2; player++) {
                LineClearSnapshot copy = LanSnapshotCodec.read(in, BattleMode.ITEM).lineClear();
                assertEquals(original.lineClear().sequence(), copy.sequence());
                assertEquals(original.lineClear().rows(), copy.rows());
                for (int row = 0; row < Board.ROWS; row++) {
                    for (int col = 0; col < Board.COLUMNS; col++) {
                        assertEquals(original.lineClear().cellAt(row, col), copy.cellAt(row, col));
                        assertEquals(original.lineClear().itemAt(row, col), copy.itemAt(row, col));
                    }
                }
            }
            assertEquals(0, in.available());
        }
        assertEquals(ItemType.LINE_CLEAR, original.lineClear().itemAt(18, 0));
        assertEquals(Board.EMPTY_CELL, original.lineClear().cellAt(18, 5));
    }

    @Test
    void rejectsInvalidAnimationSequenceAndRowMask() throws Exception {
        PlayerSession session = new PlayerSession(engine(
                List.of(new Tetromino(PieceType.O).withItem(ItemType.LINE_CLEAR, 0)), 0));
        session.start(BattleMode.ITEM, Difficulty.NORMAL);
        session.apply(GameAction.HARD_DROP);
        var bytes = new ByteArrayOutputStream();
        LanSnapshotCodec.write(new DataOutputStream(bytes), session.snapshot());
        byte[] encoded = bytes.toByteArray();
        // The last section is sequence (4), row mask (4), then 200 cell/item pairs.
        int sequenceOffset = encoded.length - 408;
        for (int offset : new int[] {sequenceOffset, sequenceOffset + 4}) {
            byte[] invalid = encoded.clone();
            java.util.Arrays.fill(invalid, offset, offset + 4, (byte) 0);
            assertThrows(IOException.class, () -> LanSnapshotCodec.read(
                    new DataInputStream(new ByteArrayInputStream(invalid)), BattleMode.ITEM));
        }
        assertThrows(IOException.class, () -> LanSnapshotCodec.read(new DataInputStream(
                new ByteArrayInputStream(encoded, 0, encoded.length - 1)), BattleMode.ITEM));
    }
}
