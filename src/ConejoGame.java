import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.*;

public class ConejoGame extends JPanel implements ActionListener, KeyListener {
    private static final int LARGURA = 360;
    private static final int ALTURA = 640;
    private static final int CHAO_Y = 535;

    private static final int COELHO_X = 118;
    private static final int COELHO_LARGURA = 78;
    private static final int COELHO_ALTURA = 110;

    private static final int RAPOSA_LARGURA = 82;
    private static final int RAPOSA_ALTURA = 99;

    private static final int TRONCO_LARGURA = 88;
    private static final int TRONCO_ALTURA = 45;

    private static final int CENOURA_LARGURA = 30;
    private static final int CENOURA_ALTURA = 41;

    private final Timer gameLoop = new Timer(1000 / 60, this);
    private final Random random = new Random();
    private final ArrayList<Obstaculo> obstaculos = new ArrayList<>();
    private final ArrayList<Cenoura> cenouras = new ArrayList<>();

    private BufferedImage cenarioImage;
    private BufferedImage coelhoImage;
    private BufferedImage raposaImage;
    private BufferedImage cenouraImage;
    private BufferedImage troncoImage;

    private int coelhoY = CHAO_Y - COELHO_ALTURA;
    private double velocidadeY = 0;
    private final double gravidade = 0.85;
    private boolean noChao = true;

    private double raposaX = 16;
    private boolean gameOver = false;
    private boolean capturado = false;

    private double velocidadeCenario = 4.8;
    private double pontuacao = 0;
    private int cenourasColetadas = 0;
    private int tempoDesdeSpawn = 0;
    private int proximoSpawn = 1100;
    private double animacaoPasso = 0;

    private static class Obstaculo {
        double x;
        final int y;
        final int largura;
        final int altura;

        Obstaculo(double x, int y, int largura, int altura) {
            this.x = x;
            this.y = y;
            this.largura = largura;
            this.altura = altura;
        }

        Rectangle getBounds() {
            return new Rectangle((int) x + 8, y + 8, largura - 16, altura - 12);
        }
    }

    private static class Cenoura {
        double x;
        final int y;
        boolean coletada;

        Cenoura(double x, int y) {
            this.x = x;
            this.y = y;
        }

        Rectangle getBounds() {
            return new Rectangle((int) x + 4, y + 3, CENOURA_LARGURA - 8, CENOURA_ALTURA - 6);
        }
    }

    public ConejoGame() {
        setPreferredSize(new Dimension(LARGURA, ALTURA));
        setFocusable(true);
        addKeyListener(this);
        carregarAssets();
        gameLoop.start();
    }

    private void carregarAssets() {
        cenarioImage = carregarImagem("/assets/cenario.png");
        coelhoImage = carregarImagem("/assets/conejo.png");
        raposaImage = carregarImagem("/assets/raposa.png");
        cenouraImage = carregarImagem("/assets/cenoura.png");
        troncoImage = carregarImagem("/assets/tronco.png");
    }

    private BufferedImage carregarImagem(String caminho) {
        try {
            URL recurso = getClass().getResource(caminho);
            if (recurso != null) {
                return ImageIO.read(recurso);
            }

            String caminhoSemBarra = caminho.startsWith("/") ? caminho.substring(1) : caminho;
            File[] alternativas = {
                new File("src", caminhoSemBarra),
                new File(caminhoSemBarra),
                new File(".." + File.separator + caminhoSemBarra)
            };

            for (File arquivo : alternativas) {
                if (arquivo.exists()) {
                    return ImageIO.read(arquivo);
                }
            }

            BufferedImage imagemCodificada = carregarImagemCodificada(caminhoSemBarra);
            if (imagemCodificada != null) {
                return imagemCodificada;
            }
        } catch (Exception e) {
            System.err.println("Erro ao carregar " + caminho + ": " + e.getMessage());
        }

        System.err.println("Imagem não encontrada: " + caminho);
        return null;
    }

    private BufferedImage carregarImagemCodificada(String caminhoSemBarra) throws Exception {
        int ponto = caminhoSemBarra.lastIndexOf('.');
        String caminhoBase = ponto >= 0 ? caminhoSemBarra.substring(0, ponto) : caminhoSemBarra;

        String textoBase64 = lerTextoAsset("/" + caminhoBase + ".b64");
        byte[] dados;

        if (textoBase64 != null) {
            dados = Base64.getDecoder().decode(textoBase64.replaceAll("\\s", ""));
        } else {
            String textoHex = lerTextoAsset("/" + caminhoBase + ".hex");
            if (textoHex == null) {
                return null;
            }
            dados = converterHexParaBytes(textoHex.replaceAll("\\s", ""));
        }

        return ImageIO.read(new ByteArrayInputStream(dados));
    }

    private String lerTextoAsset(String caminho) throws Exception {
        try (InputStream entrada = getClass().getResourceAsStream(caminho)) {
            if (entrada != null) {
                return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        String caminhoSemBarra = caminho.startsWith("/") ? caminho.substring(1) : caminho;
        File[] alternativas = {
            new File("src", caminhoSemBarra),
            new File(caminhoSemBarra),
            new File(".." + File.separator + caminhoSemBarra)
        };

        for (File arquivo : alternativas) {
            if (arquivo.exists()) {
                return Files.readString(arquivo.toPath(), StandardCharsets.UTF_8);
            }
        }

        return null;
    }

    private byte[] converterHexParaBytes(String hexadecimal) {
        if (hexadecimal.length() % 2 != 0) {
            throw new IllegalArgumentException("Arquivo hexadecimal inválido.");
        }

        byte[] dados = new byte[hexadecimal.length() / 2];
        for (int i = 0; i < hexadecimal.length(); i += 2) {
            dados[i / 2] = (byte) Integer.parseInt(hexadecimal.substring(i, i + 2), 16);
        }
        return dados;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        desenharCenario(g2);
        desenharPoeira(g2);
        desenharRaposa(g2);
        desenharCenouras(g2);
        desenharObstaculos(g2);
        desenharCoelho(g2);
        desenharInterface(g2);

        g2.dispose();
    }

    private void desenharCenario(Graphics2D g2) {
        if (cenarioImage != null) {
            g2.drawImage(cenarioImage, 0, 0, LARGURA, ALTURA, null);
            return;
        }

        GradientPaint ceu = new GradientPaint(
                0, 0, new Color(116, 205, 255),
                0, ALTURA, new Color(225, 247, 255)
        );
        g2.setPaint(ceu);
        g2.fillRect(0, 0, LARGURA, ALTURA);
        g2.setColor(new Color(92, 172, 78));
        g2.fillRect(0, CHAO_Y - 8, LARGURA, 16);
        g2.setColor(new Color(181, 132, 76));
        g2.fillRect(0, CHAO_Y + 8, LARGURA, ALTURA - CHAO_Y);
    }

    private void desenharPoeira(Graphics2D g2) {
        if (gameOver || !noChao) {
            return;
        }

        int deslocamento = (int) ((animacaoPasso * 18) % 24);
        g2.setColor(new Color(255, 244, 211, 125));
        g2.fillOval(COELHO_X - 12 - deslocamento / 2, CHAO_Y - 16, 18, 9);
        g2.fillOval((int) raposaX - 5 - deslocamento, CHAO_Y - 13, 14, 7);
    }

    private void desenharRaposa(Graphics2D g2) {
        int y = CHAO_Y - RAPOSA_ALTURA;
        if (!capturado) {
            y += (int) (Math.sin(animacaoPasso * 1.12) * 3);
        }

        if (raposaImage != null) {
            g2.drawImage(raposaImage, (int) raposaX, y, RAPOSA_LARGURA, RAPOSA_ALTURA, null);
        } else {
            g2.setColor(new Color(240, 116, 24));
            g2.fillOval((int) raposaX, y, RAPOSA_LARGURA, RAPOSA_ALTURA);
        }
    }

    private void desenharCoelho(Graphics2D g2) {
        int y = coelhoY;
        if (noChao && !gameOver) {
            y += (int) (Math.sin(animacaoPasso) * 3);
        }

        if (coelhoImage != null) {
            g2.drawImage(coelhoImage, COELHO_X, y, COELHO_LARGURA, COELHO_ALTURA, null);
        } else {
            g2.setColor(Color.WHITE);
            g2.fillOval(COELHO_X, y, COELHO_LARGURA, COELHO_ALTURA);
        }
    }

    private void desenharObstaculos(Graphics2D g2) {
        for (Obstaculo obstaculo : obstaculos) {
            int x = (int) obstaculo.x;
            if (troncoImage != null) {
                g2.drawImage(troncoImage, x, obstaculo.y, obstaculo.largura, obstaculo.altura, null);
            } else {
                g2.setColor(new Color(117, 73, 40));
                g2.fillRoundRect(x, obstaculo.y, obstaculo.largura, obstaculo.altura, 10, 10);
            }
        }
    }

    private void desenharCenouras(Graphics2D g2) {
        for (Cenoura cenoura : cenouras) {
            if (cenoura.coletada) {
                continue;
            }

            int x = (int) cenoura.x;
            int y = cenoura.y + (int) (Math.sin(animacaoPasso + x * 0.03) * 3);

            if (cenouraImage != null) {
                g2.drawImage(cenouraImage, x, y, CENOURA_LARGURA, CENOURA_ALTURA, null);
            } else {
                g2.setColor(new Color(245, 132, 31));
                g2.fillOval(x, y, CENOURA_LARGURA, CENOURA_ALTURA);
            }
        }
    }

    private void desenharInterface(Graphics2D g2) {
        g2.setColor(new Color(255, 255, 255, 205));
        g2.fillRoundRect(8, 8, 150, 55, 16, 16);

        g2.setColor(new Color(40, 69, 50));
        g2.setFont(new Font("Arial", Font.BOLD, 18));
        g2.drawString("Pontos: " + (int) pontuacao, 17, 31);
        g2.drawString("Cenouras: " + cenourasColetadas, 17, 54);

        if (pontuacao < 2 && !gameOver) {
            g2.setColor(new Color(0, 0, 0, 125));
            g2.fillRoundRect(66, 86, 228, 37, 18, 18);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 14));
            g2.drawString("ESPAÇO ou ↑ para pular", 88, 110);
        }

        if (gameOver && !capturado) {
            g2.setColor(new Color(0, 0, 0, 135));
            g2.fillRoundRect(55, 190, 250, 54, 18, 18);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 17));
            g2.drawString("A raposa está chegando!", 76, 224);
        }

        if (capturado) {
            g2.setColor(new Color(0, 0, 0, 165));
            g2.fillRoundRect(28, 183, 304, 202, 24, 24);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 28));
            g2.drawString("FIM DE JOGO", 78, 230);

            g2.setFont(new Font("Arial", Font.BOLD, 17));
            g2.drawString("A raposa alcançou o conejo!", 55, 270);
            g2.drawString("Pontuação: " + (int) pontuacao, 105, 306);
            g2.drawString("Cenouras: " + cenourasColetadas, 105, 333);

            g2.setFont(new Font("Arial", Font.PLAIN, 14));
            g2.drawString("Pressione ESPAÇO para reiniciar", 72, 365);
        }
    }

    private void atualizarJogo() {
        animacaoPasso += 0.22;

        if (gameOver) {
            atualizarCapturaDaRaposa();
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

        velocidadeCenario = Math.min(10.0, 4.8 + pontuacao / 190.0);
        pontuacao += velocidadeCenario / 70.0;

        raposaX = 16 + Math.sin(animacaoPasso * 0.45) * 3;

        tempoDesdeSpawn += 1000 / 60;
        if (tempoDesdeSpawn >= proximoSpawn) {
            criarElementos();
            tempoDesdeSpawn = 0;
            proximoSpawn = 950 + random.nextInt(550);
        }

        Rectangle areaCoelho = new Rectangle(
                COELHO_X + 17,
                coelhoY + 12,
                COELHO_LARGURA - 31,
                COELHO_ALTURA - 19
        );

        Iterator<Obstaculo> itObstaculos = obstaculos.iterator();
        while (itObstaculos.hasNext()) {
            Obstaculo obstaculo = itObstaculos.next();
            obstaculo.x -= velocidadeCenario;

            if (areaCoelho.intersects(obstaculo.getBounds())) {
                iniciarCaptura();
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

            if (cenoura.x + CENOURA_LARGURA < 0 || cenoura.coletada) {
                itCenouras.remove();
            }
        }
    }

    private void atualizarCapturaDaRaposa() {
        if (capturado) {
            return;
        }

        raposaX += 4.6;
        if (raposaX + RAPOSA_LARGURA >= COELHO_X + 27) {
            raposaX = COELHO_X + 27 - RAPOSA_LARGURA;
            capturado = true;
        }
    }

    private void iniciarCaptura() {
        if (!gameOver) {
            gameOver = true;
            velocidadeY = 0;
        }
    }

    private void criarElementos() {
        int alturaTronco = TRONCO_ALTURA + random.nextInt(7);
        obstaculos.add(new Obstaculo(
                LARGURA + 15,
                CHAO_Y - alturaTronco + 6,
                TRONCO_LARGURA,
                alturaTronco
        ));

        if (random.nextDouble() < 0.78) {
            int yCenoura = CHAO_Y - alturaTronco - 52 - random.nextInt(32);
            cenouras.add(new Cenoura(LARGURA + 44, Math.max(210, yCenoura)));
        }
    }

    private void pular() {
        if (capturado) {
            reiniciar();
            return;
        }

        if (!gameOver && noChao) {
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
        raposaX = 16;
        gameOver = false;
        capturado = false;
        velocidadeCenario = 4.8;
        pontuacao = 0;
        cenourasColetadas = 0;
        tempoDesdeSpawn = 0;
        proximoSpawn = 1100;
        animacaoPasso = 0;
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
