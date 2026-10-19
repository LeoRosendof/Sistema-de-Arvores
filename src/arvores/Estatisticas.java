package arvores;

/**
 * Contadores de trabalho de balanceamento, usados na análise comparativa.
 *
 * <p>Convenção: uma rotação dupla (AVL: casos Esquerda-Direita e
 * Direita-Esquerda; Rubro-Negra: caso "triângulo") é contabilizada como
 * <b>duas</b> rotações simples, porque é exatamente isso que o código executa.
 * Assim, os números das duas árvores são diretamente comparáveis.</p>
 */
public final class Estatisticas {

    private long rotacoesEsquerda;
    private long rotacoesDireita;
    private long recoloracoes;
    private long comparacoes;

    void contarRotacaoEsquerda() {
        rotacoesEsquerda++;
    }

    void contarRotacaoDireita() {
        rotacoesDireita++;
    }

    void contarRecoloracao() {
        recoloracoes++;
    }

    void contarComparacao() {
        comparacoes++;
    }

    public long rotacoesEsquerda() {
        return rotacoesEsquerda;
    }

    public long rotacoesDireita() {
        return rotacoesDireita;
    }

    public long rotacoes() {
        return rotacoesEsquerda + rotacoesDireita;
    }

    /** Número de nós que trocaram de cor (sempre 0 na AVL). */
    public long recoloracoes() {
        return recoloracoes;
    }

    /** Comparações de chave feitas durante as buscas ({@code buscar}). */
    public long comparacoes() {
        return comparacoes;
    }

    public void zerar() {
        rotacoesEsquerda = 0;
        rotacoesDireita = 0;
        recoloracoes = 0;
        comparacoes = 0;
    }

    @Override
    public String toString() {
        return String.format("rotações=%d (esq=%d, dir=%d), recolorações=%d, comparações em buscas=%d",
                rotacoes(), rotacoesEsquerda, rotacoesDireita, recoloracoes, comparacoes);
    }
}
