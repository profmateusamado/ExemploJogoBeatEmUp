
package com.mycompany.exemplojogobeatemup;
 
/**
 * Representa um inimigo no Beat 'em up.
 * Cada inimigo tem posição, vida e um timer de ataque.
 */
public class Inimigo {
 
    private double x, y;
    private int    vida;
    private int    vidaMax;
    private boolean morto;
 
    // Timer: quantos segundos até o próximo ataque
    private double tempoAtaque;
 
    // Cada inimigo tem uma cor diferente para diferenciar visualmente
    private String cor;
 
    public Inimigo(double x, double y, int vida, String cor) {
        this.x           = x;
        this.y           = y;
        this.vida        = vida;
        this.vidaMax     = vida;
        this.morto       = false;
        this.tempoAtaque = 2.0;
        this.cor         = cor;
    }
 
    // ── Getters ───────────────────────────────────────────
    public double  getX()           { return x; }
    public double  getY()           { return y; }
    public int     getVida()        { return vida; }
    public int     getVidaMax()     { return vidaMax; }
    public boolean isMorto()        { return morto; }
    public double  getTempoAtaque() { return tempoAtaque; }
    public String  getCor()         { return cor; }
 
    // ── Setters ───────────────────────────────────────────
    public void setX(double x)                   { this.x = x; }
    public void setY(double y)                   { this.y = y; }
    public void setMorto(boolean morto)          { this.morto = morto; }
    public void setTempoAtaque(double t)         { this.tempoAtaque = t; }
 
    // ── Métodos de utilidade ──────────────────────────────
    public void levarDano(int dano) {
        this.vida -= dano;
        if (this.vida <= 0) {
            this.vida  = 0;
            this.morto = true;
        }
    }
 
    public void diminuirTempoAtaque(double delta) {
        this.tempoAtaque -= delta;
    }
 
    public void resetarTempoAtaque(double intervalo) {
        this.tempoAtaque = intervalo;
    }
}