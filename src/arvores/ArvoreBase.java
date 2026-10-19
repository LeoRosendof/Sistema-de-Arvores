package arvores;

import java.util.ArrayList;
import java.util.List;

/**
 * Infraestrutura compartilhada pelas duas árvores: contadores e modo detalhado
 * (rastro das operações de balanceamento). Os algoritmos ficam nas subclasses.
 */
abstract class ArvoreBase implements ArvoreBalanceada {

    protected final Estatisticas estatisticas = new Estatisticas();
    private final List<String> rastro = new ArrayList<>();
    private boolean rastreamento;

    @Override
    public Estatisticas estatisticas() {
        return estatisticas;
    }

    @Override
    public void setRastreamento(boolean ativo) {
        this.rastreamento = ativo;
        if (!ativo) {
            rastro.clear();
        }
    }

    @Override
    public boolean isRastreamento() {
        return rastreamento;
    }

    @Override
    public List<String> consumirRastro() {
        List<String> copia = new ArrayList<>(rastro);
        rastro.clear();
        return copia;
    }

    /** Registra um passo do balanceamento (só quando o modo detalhado está ativo). */
    protected void registrar(String formato, Object... args) {
        if (rastreamento) {
            rastro.add(String.format(formato, args));
        }
    }

    /** Prefixos usados pelo desenho deitado da árvore (raiz à esquerda, direita em cima). */
    protected static String prefixoFilhos(String prefixo, boolean raiz, boolean ehDireito, boolean paraDireita) {
        if (raiz) {
            return prefixo;
        }
        // O traço vertical liga o pai ao filho; ele aparece apenas entre os dois.
        boolean precisaTraco = ehDireito != paraDireita;
        return prefixo + (precisaTraco ? "|   " : "    ");
    }

    protected static String conector(boolean raiz, boolean ehDireito) {
        if (raiz) {
            return "";
        }
        return ehDireito ? "/-- " : "\\-- ";
    }
}
