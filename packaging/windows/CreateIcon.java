import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Generate a Windows .ico with a PNG-compressed 256x256 T-tetromino image. */
class CreateIcon {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected output .ico path");
        }
        int size = 256;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(20, 25, 40));
            graphics.fill(new RoundRectangle2D.Double(16, 16, 224, 224, 49, 49));
            for (int[] cell : new int[][] {{0, 0}, {0, 1}, {0, 2}, {1, 1}}) {
                int x = 50 + cell[1] * 52;
                int y = 76 + cell[0] * 52;
                graphics.setColor(new Color(75, 211, 220));
                graphics.fill(new RoundRectangle2D.Double(x, y, 48, 48, 6, 6));
                graphics.setColor(new Color(177, 247, 248));
                graphics.fill(new RoundRectangle2D.Double(x + 4, y + 4, 40, 5, 2, 2));
            }
        } finally {
            graphics.dispose();
        }

        ByteArrayOutputStream png = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "png", png)) {
            throw new IllegalStateException("PNG writer not available");
        }
        byte[] data = png.toByteArray();
        // ICO header (6 bytes) + image entry (16 bytes); dimension 0 denotes 256 px.
        ByteBuffer ico = ByteBuffer.allocate(22 + data.length).order(ByteOrder.LITTLE_ENDIAN);
        ico.putShort((short) 0); // reserved
        ico.putShort((short) 1); // icon
        ico.putShort((short) 1); // image count
        ico.put((byte) 0).put((byte) 0); // width, height: 256 each
        ico.put((byte) 0).put((byte) 0); // palette, reserved
        ico.putShort((short) 1); // color planes
        ico.putShort((short) 32); // bits per pixel
        ico.putInt(data.length);
        ico.putInt(22);
        ico.put(data);
        Path output = Path.of(args[0]);
        if (output.getParent() != null) {
            Files.createDirectories(output.getParent());
        }
        Files.write(output, ico.array());
    }
}
