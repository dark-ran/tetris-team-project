package tetris;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import tetris.app.AppController;
import tetris.ui.AppTheme;

/** Swing 창의 생성과 종료를 연결하는 진입점. */
public class Main {
    public static void main(String[] args) {
        AppTheme.install();
        SwingUtilities.invokeLater(() -> {
            JFrame window = new JFrame("Tetris · Swing Skeleton");
            AppController app = new AppController(window::dispose);
            window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            window.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent event) {
                    app.exit();
                }
            });
            window.setContentPane(app.view());
            // 임시 확인용 크기. 팀이 정할 설정 프리셋과는 별개다.
            window.setMinimumSize(new Dimension(900, 760));
            window.setPreferredSize(new Dimension(1040, 860));
            window.pack();
            window.setLocationRelativeTo(null);
            window.setVisible(true);
            app.start();
        });
    }
}
