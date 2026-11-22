package tetris.game;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import tetris.board.Board;
import tetris.game.PlayerSnapshot.PieceView;
import tetris.piece.ItemType;

public final class LanSnapshotCodec {
    private static final ItemType[] ITEM_TYPES = ItemType.values();
    private LanSnapshotCodec() { }

    public static void write(DataOutput out, PlayerSnapshot snapshot) throws IOException {
        for (int row = 0; row < Board.ROWS; row++) {
            for (int col = 0; col < Board.COLUMNS; col++) {
                out.writeByte(snapshot.cellAt(row, col));
                out.writeByte(snapshot.itemAt(row, col).ordinal());
            }
        }
        writePiece(out, snapshot.currentPiece());
        out.writeByte(snapshot.landingRow());
        writePiece(out, snapshot.nextPiece());
        out.writeByte(snapshot.pendingAttackLines().size());
        for (AttackLine line : snapshot.pendingAttackLines()) {
            int mask = 0;
            for (int col = 0; col < Board.COLUMNS; col++) {
                if (line.isFilled(col)) mask |= 1 << col;
            }
            out.writeShort(mask);
        }
        out.writeByte(snapshot.phase().ordinal());
        out.writeLong(snapshot.score());
        out.writeInt(snapshot.level());
        out.writeInt(snapshot.totalClearedRows());
    }

    public static PlayerSnapshot read(DataInput in, BattleMode mode) throws IOException {
        int[][] cells = new int[Board.ROWS][Board.COLUMNS];
        ItemType[][] items = new ItemType[Board.ROWS][Board.COLUMNS];
        for (int row = 0; row < Board.ROWS; row++) {
            for (int col = 0; col < Board.COLUMNS; col++) {
                cells[row][col] = bounded(in.readUnsignedByte(), 0, Board.GARBAGE_CELL);
                items[row][col] = readEnum(in, ITEM_TYPES);
            }
        }
        PieceView current = readPiece(in);
        int landing = bounded(in.readByte(), -4, Board.ROWS);
        PieceView next = readPiece(in);
        int count = bounded(in.readUnsignedByte(), 0, 10);
        List<AttackLine> lines = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            int mask = bounded(in.readUnsignedShort(), 1, (1 << Board.COLUMNS) - 1);
            boolean[] filled = new boolean[Board.COLUMNS];
            for (int col = 0; col < filled.length; col++) filled[col] = (mask & (1 << col)) != 0;
            lines.add(new AttackLine(filled));
        }
        GamePhase phase = readEnum(in, GamePhase.values());
        long score = in.readLong();
        int level = in.readInt();
        int cleared = in.readInt();
        if (score < 0 || level < 0 || cleared < 0) throw new IOException("잘못된 점수 정보입니다.");
        return new PlayerSnapshot(cells, items, current, landing, next, lines, phase, mode, score, level, cleared);
    }

    private static void writePiece(DataOutput out, PieceView piece) throws IOException {
        out.writeBoolean(piece != null);
        if (piece == null) return;
        out.writeByte(piece.row());
        out.writeByte(piece.column());
        out.writeByte(piece.cellValue());
        out.writeByte(piece.itemType().ordinal());
        int[][] cells = piece.cells();
        boolean[] items = piece.itemCells();
        out.writeByte(cells.length);
        for (int index = 0; index < cells.length; index++) {
            out.writeByte(cells[index][0]);
            out.writeByte(cells[index][1]);
            out.writeBoolean(items[index]);
        }
    }

    private static PieceView readPiece(DataInput in) throws IOException {
        if (!in.readBoolean()) return null;
        int row = bounded(in.readByte(), -4, Board.ROWS);
        int col = bounded(in.readByte(), -4, Board.COLUMNS);
        int value = bounded(in.readUnsignedByte(), 1, Board.GARBAGE_CELL - 1);
        ItemType item = readEnum(in, ITEM_TYPES);
        int count = bounded(in.readUnsignedByte(), 1, 6);
        int[][] cells = new int[count][2];
        boolean[] itemCells = new boolean[count];
        for (int index = 0; index < count; index++) {
            cells[index][0] = bounded(in.readUnsignedByte(), 0, 3);
            cells[index][1] = bounded(in.readUnsignedByte(), 0, 3);
            itemCells[index] = in.readBoolean();
        }
        return new PieceView(row, col, value, item, cells, itemCells);
    }

    private static int bounded(int value, int min, int max) throws IOException {
        if (value < min || value > max) throw new IOException("잘못된 보드 데이터입니다.");
        return value;
    }

    private static <T> T readEnum(DataInput in, T[] values) throws IOException {
        return values[bounded(in.readUnsignedByte(), 0, values.length - 1)];
    }
}
