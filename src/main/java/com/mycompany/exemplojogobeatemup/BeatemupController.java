package com.mycompany.exemplojogobeatemup;

 
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
 
import java.net.URL;
import java.util.*;
 
public class BeatemupController implements Initializable {
 
    // ── Injetados do FXML ──────────────────────────────────
    @FXML private Canvas canvas;
    @FXML private Label  lblVida;
    @FXML private Label  lblFase;
    @FXML private Label  lblInimigos;
    @FXML private Label  lblDica;
 
    // ── Configurações ─────────────────────────────────────
    static final double LARGURA  = 640;
    static final double ALTURA   = 420;
    static final double VEL_JOG  = 160;  // velocidade do jogador px/s
    static final double ALCANCE  = 65;   // raio do soco em pixels
    static final double TAM_JOG  = 36;   // tamanho do jogador
    static final double TAM_INI  = 32;   // tamanho do inimigo
    static final int    FASES    = 3;    // total de fases
 
    // Cores dos inimigos por fase
    static final String[] CORES_INI = {"#e17055", "#d63031", "#6c5ce7"};
 
    // ── Estado do jogador ─────────────────────────────────
    private double  jogX     = LARGURA / 2;
    private double  jogY     = ALTURA  / 2;
    private int     vidaJog  = 5;
    private int     fase     = 1;
    private boolean socando  = false;
    private double  tempoSoco = 0;       // duração visual do soco
 
    // ── Inimigos da fase atual ────────────────────────────
    private List<Inimigo> inimigos = new ArrayList<>();
 
    // ── Controle de teclado ───────────────────────────────
    private Set<KeyCode> teclas = new HashSet<>();
 
    // ── Estado geral ──────────────────────────────────────
    private boolean jogoAtivo = true;
    private boolean venceu    = false;
    private long    ultimoNano = 0;
 
    // ──────────────────────────────────────────────────────
    @Override
    public void initialize(URL u, ResourceBundle r) {
        gerarFase();
 
        // Registrar teclado quando o canvas entrar na scene
        canvas.sceneProperty().addListener((obs, ant, novo) -> {
            if (novo == null) return;
            novo.setOnKeyPressed(e -> {
                teclas.add(e.getCode());
                // ESPAÇO: socar
                if (e.getCode() == KeyCode.SPACE) socar();
            });
            novo.setOnKeyReleased(e -> teclas.remove(e.getCode()));
        });
 
        // Game loop
        new AnimationTimer() {
            @Override
            public void handle(long agora) {
                if (ultimoNano == 0) { ultimoNano = agora; return; }
                double delta = Math.min((agora - ultimoNano) / 1_000_000_000.0, 0.05);
                ultimoNano = agora;
                if (jogoAtivo) {
                    atualizar(delta);
                    desenhar();
                }
            }
        }.start();
    }
 
    // ──────────────────────────────────────────────────────
    // GERAR FASE
    // ──────────────────────────────────────────────────────
 
    private void gerarFase() {
        inimigos.clear();
 
        int qtdInimigos = 2 + fase;      // fase 1=3, fase 2=4, fase 3=5
        int vidaInimigo = 2 + fase;      // ficam mais resistentes
        String cor = CORES_INI[Math.min(fase - 1, CORES_INI.length - 1)];
 
        Random rand = new Random();
        for (int i = 0; i < qtdInimigos; i++) {
            double x, y;
            // Spawnar longe do jogador
            do {
                x = TAM_INI + rand.nextDouble() * (LARGURA - TAM_INI * 2);
                y = TAM_INI + rand.nextDouble() * (ALTURA  - TAM_INI * 2);
            } while (distancia(x, y, jogX, jogY) < 150);
 
            inimigos.add(new Inimigo(x, y, vidaInimigo, cor));
        }
 
        lblFase.setText("FASE " + fase);
        atualizarHUD();
    }
 
    // ──────────────────────────────────────────────────────
    // ATUALIZAR
    // ──────────────────────────────────────────────────────
 
    private void atualizar(double delta) {
 
        // ── Mover jogador ──────────────────────────────────
        double vx = 0, vy = 0;
        if (teclas.contains(KeyCode.A) || teclas.contains(KeyCode.LEFT))  vx -= VEL_JOG;
        if (teclas.contains(KeyCode.D) || teclas.contains(KeyCode.RIGHT)) vx += VEL_JOG;
        if (teclas.contains(KeyCode.W) || teclas.contains(KeyCode.UP))    vy -= VEL_JOG;
        if (teclas.contains(KeyCode.S) || teclas.contains(KeyCode.DOWN))  vy += VEL_JOG;
 
        // Normalizar diagonal (evita andar mais rápido na diagonal)
        if (vx != 0 && vy != 0) { vx *= 0.707; vy *= 0.707; }
 
        jogX = Math.max(0, Math.min(LARGURA - TAM_JOG, jogX + vx * delta));
        jogY = Math.max(0, Math.min(ALTURA  - TAM_JOG, jogY + vy * delta));
 
        // ── Animação do soco ───────────────────────────────
        if (socando) {
            tempoSoco -= delta;
            if (tempoSoco <= 0) socando = false;
        }
 
        // ── Inimigos: mover e atacar ───────────────────────
        for (Inimigo ini : inimigos) {
            if (ini.isMorto()) continue;
 
            // Mover em direção ao jogador
            double dx   = jogX - ini.getX();
            double dy   = jogY - ini.getY();
            double dist = distancia(ini.getX(), ini.getY(), jogX, jogY);
            double vel  = 55 + fase * 10;  // mais rápidos por fase
 
            if (dist > TAM_JOG) {
                ini.setX(ini.getX() + (dx / dist) * vel * delta);
                ini.setY(ini.getY() + (dy / dist) * vel * delta);
            }
 
            // Atacar jogador se estiver perto
            ini.diminuirTempoAtaque(delta);
            if (ini.getTempoAtaque() <= 0 && dist < TAM_JOG + 10) {
                vidaJog--;
                ini.resetarTempoAtaque(2.0);
                atualizarHUD();
                if (vidaJog <= 0) {
                    jogoAtivo = false;
                    desenhar();
                    return;
                }
            }
        }
 
        // ── Verificar se fase foi vencida ──────────────────
        boolean todosVivos = inimigos.stream().anyMatch(i -> !i.isMorto());
        if (!todosVivos) {
            fase++;
            if (fase > FASES) {
                venceu    = true;
                jogoAtivo = false;
                desenhar();
            } else {
                gerarFase();
            }
        }
 
        atualizarHUD();
    }
 
    // ──────────────────────────────────────────────────────
    // SOCO
    // ──────────────────────────────────────────────────────
 
    private void socar() {
        if (!jogoAtivo) return;
        socando   = true;
        tempoSoco = 0.25;  // duração da animação em segundos
 
        // Machuca todos os inimigos dentro do alcance
        for (Inimigo ini : inimigos) {
            if (ini.isMorto()) continue;
            if (distancia(jogX, jogY, ini.getX(), ini.getY()) < ALCANCE) {
                ini.levarDano(1);
            }
        }
    }
 
    // ──────────────────────────────────────────────────────
    // DESENHO
    // ──────────────────────────────────────────────────────
 
    private void desenhar() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
 
        // ── Fundo ──────────────────────────────────────────
        gc.setFill(Color.web("#2d3436"));
        gc.fillRect(0, 0, LARGURA, ALTURA);
 
        // Chão (faixa no fundo)
        gc.setFill(Color.web("#3d5a80"));
        gc.fillRect(0, ALTURA - 50, LARGURA, 50);
 
        // Grade no chão (dá sensação de perspectiva)
        gc.setStroke(Color.web("#3d5a80").darker());
        gc.setLineWidth(0.5);
        for (int x = 0; x < LARGURA; x += 60)
            gc.strokeLine(x, ALTURA - 50, x, ALTURA);
 
        // ── Inimigos ───────────────────────────────────────
        for (Inimigo ini : inimigos) {
            if (ini.isMorto()) continue;
 
            double ix = ini.getX();
            double iy = ini.getY();
 
            // Corpo do inimigo
            gc.setFill(Color.web(ini.getCor()));
            gc.fillOval(ix, iy, TAM_INI, TAM_INI);
 
            // Olhos simples
            gc.setFill(Color.WHITE);
            gc.fillOval(ix + 6,  iy + 8, 7, 7);
            gc.fillOval(ix + 18, iy + 8, 7, 7);
            gc.setFill(Color.web("#2d3436"));
            gc.fillOval(ix + 8,  iy + 10, 4, 4);
            gc.fillOval(ix + 20, iy + 10, 4, 4);
 
            // Barra de vida do inimigo
            double pctVida = (double) ini.getVida() / ini.getVidaMax();
            gc.setFill(Color.web("#c0392b"));
            gc.fillRect(ix, iy - 10, TAM_INI, 5);
            gc.setFill(Color.web("#2ecc71"));
            gc.fillRect(ix, iy - 10, TAM_INI * pctVida, 5);
        }
 
        // ── Jogador ────────────────────────────────────────
        // Muda de cor ao socar
        gc.setFill(socando ? Color.web("#fdcb6e") : Color.web("#00b894"));
        gc.fillRoundRect(jogX, jogY, TAM_JOG, TAM_JOG, 8, 8);
 
        // Olhos do jogador
        gc.setFill(Color.WHITE);
        gc.fillOval(jogX + 6,  jogY + 8, 8, 8);
        gc.fillOval(jogX + 22, jogY + 8, 8, 8);
        gc.setFill(Color.web("#2d3436"));
        gc.fillOval(jogX + 8,  jogY + 10, 4, 4);
        gc.fillOval(jogX + 24, jogY + 10, 4, 4);
 
        // Círculo do alcance do soco (aparece só ao socar)
        if (socando) {
            gc.setStroke(Color.web("#fdcb6e").deriveColor(0, 1, 1, 0.5));
            gc.setLineWidth(2);
            gc.strokeOval(
                jogX + TAM_JOG / 2 - ALCANCE,
                jogY + TAM_JOG / 2 - ALCANCE,
                ALCANCE * 2, ALCANCE * 2
            );
        }
 
        // ── Tela de fim ────────────────────────────────────
        if (!jogoAtivo) {
            gc.setFill(Color.color(0, 0, 0, 0.65));
            gc.fillRect(0, 0, LARGURA, ALTURA);
            gc.setTextAlign(TextAlignment.CENTER);
 
            if (venceu) {
                gc.setFill(Color.web("#fdcb6e"));
                gc.setFont(Font.font(34));
                gc.fillText("VOCÊ VENCEU!", LARGURA / 2, ALTURA / 2 - 10);
                gc.setFill(Color.WHITE);
                gc.setFont(Font.font(16));
                gc.fillText("Todas as " + FASES + " fases concluídas!", LARGURA / 2, ALTURA / 2 + 24);
            } else {
                gc.setFill(Color.web("#e17055"));
                gc.setFont(Font.font(34));
                gc.fillText("GAME OVER", LARGURA / 2, ALTURA / 2 - 10);
                gc.setFill(Color.WHITE);
                gc.setFont(Font.font(16));
                gc.fillText("Você foi derrubado na fase " + fase, LARGURA / 2, ALTURA / 2 + 24);
            }
        }
    }
 
    // ──────────────────────────────────────────────────────
    // HUD
    // ──────────────────────────────────────────────────────
 
    private void atualizarHUD() {
        // Vida como corações
        lblVida.setText("❤".repeat(Math.max(0, vidaJog)) +
                        "🖤".repeat(Math.max(0, 5 - vidaJog)));
 
        // Inimigos vivos restantes
        long vivos = inimigos.stream().filter(i -> !i.isMorto()).count();
        lblInimigos.setText("Inimigos: " + vivos);
    }
 
    // ──────────────────────────────────────────────────────
    // UTILITÁRIO
    // ──────────────────────────────────────────────────────
 
    private double distancia(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2, dy = y1 - y2;
        return Math.sqrt(dx * dx + dy * dy);
    }
}