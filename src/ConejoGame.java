import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import javax.swing.*;

public class ConejoGame extends JPanel implements ActionListener, KeyListener {
    private static final int LARGURA = 360;
    private static final int ALTURA = 640;
    private static final int CHAO_Y = 535;

    private static final int COELHO_X = 65;
    private static final int COELHO_LARGURA = 48;
    private static final int COELHO_ALTURA = 58;

    private final Timer gameLoop = new Timer(1000 / 60, this);
    private final Random random = new Random();
    private final ArrayList<Obstaculo> obstaculos = new ArrayList<>();
    private final ArrayList<Cenoura> cenouras = new ArrayList<>();

    private int coelhoY = CHAO_Y - COELHO_ALTURA;
    private double velocidadeY = 0;
    private final double gravidade = 0.85;
    private boolean noChao = true;
    private boolean gameOver = false;

    private double velocidadeCenario = 4.2;
    private double pontuacao = 0;
    private int cenourasColetadas = 0;
    private int tempoDesdeSpawn = 0;
    private int proximoSpawn = 1050;

    private static class Obstaculo {
        double x;
        int y;
        int largura;
        int altura;

        Obstaculo(double x, int y, int largura, int altura) {
            this.x = x;
            this.y = y;
            this.largura = largura;
            this.altura = altura;
        }

        Rectangle getBounds() {
            return new Rectangle((int) x + 3, y + 3, largura - 6, altura - 3);
        }
    }

    private static class Cenoura {
        double x;
        int y;
        boolean coletada;

        Cenoura(double x, int y) {
            this.x = x;
            this.y = y;
        }

        Rectangle getBounds() {
            return new Rectangle((int) x, y, 22, 34);
        }
    }

    public ConejoGame() {
        setPreferredSize(new Dimension(LARGURA, ALTURA));
        setFocusable(true);
        addKeyListener(this);
        gameLoop.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        desenharCenario(g2);
        desenharCenouras(g2);
        desenharObstaculos(g2);
        desenharCoelho(g2);
        desenharInterface(g2);

        g2.dispose();
    }

    private void desenharCenario(Graphics2D g2) {
        GradientPaint ceu = new GradientPaint(0, 0, new Color(116, 205, 255), 0, CHAO_Y, new Color(225, 247, 255));
        g2.setPaint(ceu);
        g2.fillRect(0, 0, LARGURA, CHAO_Y);

        g2.setColor(new Color(255, 246, 170));
        g2.fillOval(265, 55, 62, 62);

        desenharNuvem(g2, 35, 95);
        desenharNuvem(g2, 225, 170);

        g2.setColor(new Color(122, 196, 106));
        g2.fillOval(-75, 390, 260, 210);
        g2.fillOval(130, 405, 310, 205);

        g2.setColor(new Color(92, 172, 78));
        g2.fillRect(0, CHAO_Y - 8, LARGURA, 16);

        g2.setColor(new Color(181, 132, 76));
        g2.fillRect(0, CHAO_Y + 8, LARGURA, ALTURA - CHAO_Y);

        g2.setColor(new Color(211, 164, 101));
        for (int x = -20; x < LARGURA; x += 48) {
            int deslocamento = (int) ((pontuacao * 3) % 48);
            g2.fillOval(x - deslocamento, CHAO_Y + 34, 28, 8);
        }
    }

    private void desenharNuvem(Graphics2D g2, int x, int y) {
        g2.setColor(new Color(255, 255, 255, 220));
        g2.fillOval(x, y + 12, 54, 27);
        g2.fillOval(x + 15, y, 38, 38);
        g2.fillOval(x + 35, y + 9, 44, 30);
    }

    private void desenharCoelho(Graphics2D g2) {
        int x = COELHO_X;
        int y = coelhoY;

        g2.setColor(new Color(244, 244, 244));
        g2.fillOval(x + 4, y + 23, 42, 33);
        g2.fillOval(x + 12, y + 10, 31, 32);
        g2.fillRoundRect(x + 14, y - 7, 10, 29, 10, 10);
        g2.fillRoundRect(x + 30, y - 8, 10, 30, 10, 10);

        g2.setColor(new Color(250, 174, 190));
        g2.fillRoundRect(x + 17, y - 3, 4, 20, 5, 5);
        g2.fillRoundRect(x + 33, y - 4, 4, 21, 5, 5);

        g2.setColor(Color.WHITE);
        g2.fillOval(x - 4, y + 31, 18, 18);

        g2.setColor(new Color(45, 45, 45));
        g2.fillOval(x + 32, y + 19, 5, 6);

        g2.setColor(new Color(239, 132, 150));
        g2.fillOval(x + 41, y + 26, 6, 5);

        g2.setColor(new Color(215, 215, 215));
        g2.fillOval(x + 10, y + 48, 18, 9);
        g2.fillOval(x + 30, y + 48, 18, 9);
    }

    private void desenharObstaculos(Graphics2D g2) {
        for (Obstaculo obstaculo : obstaculos) {
            int x = (int) obstaculo.x;
            g2.setColor(new Color(117, 73, 40));
            g2.fillRoundRect(x, obstaculo.y, obstaculo.largura, obstaculo.altura, 10, 10);

            g2.setColor(new Color(157, 103, 57));
            g2.fillOval(x - 2, obstaculo.y - 3, obstaculo.largura + 4, 12);

            g2.setColor(new Color(88, 56, 34));
            g2.drawOval(x + 5, obstaculo.y, Math.max(8, obstaculo.largura - 10), 7);
            g2.drawLine(x + obstaculo.largura / 2, obstaculo.y + 13,
                    x + obstaculo.largura / 2, obstaculo.y + obstaculo.altura - 5);
        }
    }

    private void desenharCenouras(Graphics2D g2) {
        for (Cenoura cenoura : cenouras) {
            if (cenoura.coletada) {
                continue;
            }

            int x = (int) cenoura.x;
            int y = cenoura.y;

            g2.setColor(new Color(58, 153, 73));
            g2.fillOval(x + 4, y - 6, 8, 15);
            g2.fillOval(x + 11, y - 7, 8, 16);

            Polygon corpo = new Polygon();
            corpo.addPoint(x, y + 4);
            corpo.addPoint(x + 22, y + 4);
            corpo.addPoint(x + 11, y + 34);
            g2.setColor(new Color(245, 132, 31));
            g2.fillPolygon(corpo);

            g2.setColor(new Color(212, 96, 20));
            g2.drawLine(x + 5, y + 13, x + 15, y + 13);
            g2.drawLine(x + 7, y + 21, x + 14, y + 21);
        }
    }

    private void desenharInterface(Graphics2D g2) {
        g2.setFont(new Font("Arial", Font.BOLD, 20));
        g2.setColor(new Color(40, 69, 50));
        g2.drawString("Pontos: " + (int) pontuacao, 12, 28);
        g2.drawString("Cenouras: " + cenourasColetadas, 12, 53);

        if (pontuacao < 2 && !gameOver) {
            g2.setFont(new Font("Arial", Font.BOLD, 16));
            g2.drawString("ESPAÇO ou ↑ para pular", 78, 110);
        }

        if (gameOver) {
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRoundRect(35, 205, 290, 170, 22, 22);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 31));
            g2.drawString("FIM DE JOGO", 73, 255);
            g2.setFont(new Font("Arial", Font.BOLD, 18));
            g2.drawString("Pontuação: " + (int) pontuacao, 108, 294);
            g2.drawString("Cenouras: " + cenourasColetadas, 108, 322);
            g2.setFont(new Font("Arial", Font.PLAIN, 15));
            g2.drawString("Pressione ESPAÇO para reiniciar", 64, 354);
        }
    }

    private void atualizarJogo() {
        if (gameOver) {
            return;
        }

        velocidadeY += gravidade;
        coelhoY += (int) velocidadeY;

        int posicaoChao = CHAO_Y - COELHO_ALTURA;
        if (coelhoY >= posicaoChao) {
            coelhoY = posicaoChao;
            velocidadeY = 0;
            noChao = true;
        } else {
            noChao = false;
        }

        velocidadeCenario = Math.min(9.2, 4.2 + pontuacao / 180.0);
        pontuacao += velocidadeCenario / 70.0;

        tempoDesdeSpawn += 1000 / 60;
        if (tempoDesdeSpawn >= proximoSpawn) {
            criarElementos();
            tempoDesdeSpawn = 0;
            proximoSpawn = 900 + random.nextInt(650);
        }

        Rectangle areaCoelho = new Rectangle(
                COELHO_X + 7,
                coelhoY + 9,
                COELHO_LARGURA - 14,
                COELHO_ALTURA - 12
        );

        Iterator<Obstaculo> itObstaculos = obstaculos.iterator();
        while (itObstaculos.hasNext()) {
            Obstaculo obstaculo = itObstaculos.next();
            obstaculo.x -= velocidadeCenario;

            if (areaCoelho.intersects(obstaculo.getBounds())) {
                gameOver = true;
            }

            if (obstaculo.x + obstaculo.largura < 0) {
                itObstaculos.remove();
            }
        }

        Iterator<Cenoura> itCenouras = cenouras.iterator();
        while (itCenouras.hasNext()) {
            Cenoura cenoura = itCenouras.next();
            cenoura.x -= velocidadeCenario;

            if (!cenoura.coletada && areaCoelho.intersects(cenoura.getBounds())) {
                cenoura.coletada = true;
                cenourasColetadas++;
                pontuacao += 10;
            }

            if (cenoura.x + 25 < 0 || cenoura.coletada) {
                itCenouras.remove();
            }
        }
    }

    private void criarElementos() {
        int altura = 30 + random.nextInt(28);
        int largura = 28 + random.nextInt(20);
        obstaculos.add(new Obstaculo(LARGURA + 15, CHAO_Y - altura, largura, altura));

        if (random.nextDouble() < 0.65) {
            int alturaCenoura = CHAO_Y - altura - 65 - random.nextInt(45);
            cenouras.add(new Cenoura(LARGURA + 25, Math.max(210, alturaCenoura)));
        }
    }

    private void pular() {
        if (gameOver) {
            reiniciar();
            return;
        }

        if (noChao) {
            velocidadeY = -14.5;
            noChao = false;
        }
    }

    private void reiniciar() {
        obstaculos.clear();
        cenouras.clear();
        coelhoY = CHAO_Y - COELHO_ALTURA;
        velocidadeY = 0;
        noChao = true;
        gameOver = false;
        velocidadeCenario = 4.2;
        pontuacao = 0;
        cenourasColetadas = 0;
        tempoDesdeSpawn = 0;
        proximoSpawn = 1050;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        atualizarJogo();
        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_UP) {
            pular();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }
}
