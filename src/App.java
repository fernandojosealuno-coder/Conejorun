import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame janela = new JFrame("Conejo Run");
            ConejoGame jogo = new ConejoGame();

            janela.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            janela.setResizable(false);
            janela.add(jogo);
            janela.pack();
            janela.setLocationRelativeTo(null);
            janela.setVisible(true);

            jogo.requestFocusInWindow();
        });
    }
}
