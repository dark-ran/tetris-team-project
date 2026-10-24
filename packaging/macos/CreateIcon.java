import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Draws the same T-shaped tile icon at every native macOS icon size. */
class CreateIcon {
    public static void main(String[] args) throws Exception {
        Path directory = Path.of(args[0]);
        Files.createDirectories(directory);
        for (int size : new int[] {16, 32, 128, 256, 512}) {
            draw(directory.resolve("icon_" + size + "x" + size + ".png"), size);
            draw(directory.resolve("icon_" + size + "x" + size + "@2x.png"), size * 2);
        }
    }

    private static void draw(Path target, int size) throws Exception {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.scale(size / 1024.0, size / 1024.0);
            graphics.setColor(new Color(20, 25, 40));
            graphics.fill(new RoundRectangle2D.Double(64, 64, 896, 896, 196, 196));
            for (int[] cell : new int[][] {{0, 0}, {0, 1}, {0, 2}, {1, 1}}) {
                int x = 200 + cell[1] * 212;
                int y = 300 + cell[0] * 212;
                graphics.setColor(new Color(75, 211, 220));
                graphics.fill(new RoundRectangle2D.Double(x, y, 200, 200, 22, 22));
                graphics.setColor(new Color(177, 247, 248));
                graphics.fill(new RoundRectangle2D.Double(x + 16, y + 16, 168, 20, 8, 8));
            }
        } finally {
            graphics.dispose();
        }
        ImageIO.write(image, "png", target.toFile());
    }
}
