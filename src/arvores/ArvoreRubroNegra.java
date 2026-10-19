package arvores;

import java.util.ArrayList;
import java.util.List;

/**
 * Árvore Rubro-Negra (Bayer, 1972; Guibas e Sedgewick, 1978), implementada
 * conforme Cormen et al., "Algoritmos: Teoria e Prática", cap. 13.
 *
 * <p><b>Propriedades mantidas:</b></p>
 * <ol>
 *   <li>Todo nó é vermelho ou preto.</li>
 *   <li>A raiz é preta.</li>
 *   <li>Toda folha (NIL) é preta.</li>
 *   <li>Um nó vermelho não tem filho vermelho.</li>
 *   <li>Todos os caminhos de um nó até as folhas NIL descendentes têm o mesmo
 *       número de nós pretos (altura negra).</li>
 * </ol>
 *
 * <p>Consequência: o caminho mais longo tem no máximo o dobro do mais curto, logo
 * {@code altura <= 2·log2(n+1)}. O balanceamento é "mais frouxo" que o da AVL:
 * a árvore tolera mais desequilíbrio e, em troca, corrige-se com poucas
 * rotações — no máximo 2 por inserção e 3 por remoção. O restante do ajuste é
 * feito trocando cores.</p>
 *
 * <p>Implementação com <b>nó sentinela</b> {@code NIL} (preto) no lugar de
 * {@code null}, o que simplifica os casos de correção.</p>
 */
public final class ArvoreRubroNegra extends ArvoreBase {

    private static final boolean VERMELHO = true;
    private static final boolean PRETO = false;

    private static final String ANSI_VERMELHO = "\u001B[31;1m";
    private static final String ANSI_RESET = "\u001B[0m";

    private static final class No {
        int chave;
        boolean cor;
        No esq;
        No dir;
        No pai;

        No(int chave, boolean cor) {
            this.chave = chave;
            this.cor = cor;
        }
    }

    private final No nil;
    private No raiz;
    private int tamanho;
    private boolean coresAnsi;

    public ArvoreRubroNegra() {
        nil = new No(0, PRETO);
        nil.esq = nil;
        nil.dir = nil;
        nil.pai = nil;
        raiz = nil;
    }

    @Override
    public String nome() {
        return "Árvore Rubro-Negra";
    }

    /** Quando ativo, a visualização também pinta os nós vermelhos no terminal (ANSI). */
    public void setCoresAnsi(boolean ativo) {
        this.coresAnsi = ativo;
    }

    // ------------------------------------------------------------------ inserção

    @Override
    public boolean inserir(int chave) {
        No pai = nil;
        No atual = raiz;
        while (atual != nil) {
            pai = atual;
            if (chave < atual.chave) {
                atual = atual.esq;
            } else if (chave > atual.chave) {
                atual = atual.dir;
            } else {
                return false; // duplicada: árvore inalterada
            }
        }

        // Todo nó novo entra VERMELHO: não altera a altura negra (propriedade 5),
        // no máximo cria um conflito vermelho-vermelho (propriedade 4).
        No z = new No(chave, VERMELHO);
        z.esq = nil;
        z.dir = nil;
        z.pai = pai;
        if (pai == nil) {
            raiz = z;
        } else if (chave < pai.chave) {
            pai.esq = z;
        } else {
            pai.dir = z;
        }
        tamanho++;
        corrigirInsercao(z);
        return true;
    }

    private void corrigirInsercao(No z) {
        while (z.pai.cor == VERMELHO) {
            No avo = z.pai.pai;
            boolean paiEhEsquerdo = z.pai == avo.esq;
            No tio = paiEhEsquerdo ? avo.dir : avo.esq;

            if (tio.cor == VERMELHO) {
                // Caso 1: tio vermelho -> só recolorir e subir o problema para o avô
                registrar("Inserção caso 1 (tio %d vermelho): pai %d e tio ficam pretos, avô %d fica vermelho",
                        tio.chave, z.pai.chave, avo.chave);
                pintar(z.pai, PRETO);
                pintar(tio, PRETO);
                pintar(avo, VERMELHO);
                z = avo;
                continue;
            }

            boolean zEhInterno = paiEhEsquerdo ? z == z.pai.dir : z == z.pai.esq;
            if (zEhInterno) {
                // Caso 2: tio preto e z forma um "triângulo" -> rotação no pai vira caso 3
                registrar("Inserção caso 2 (tio preto, triângulo): rotação no pai %d", z.pai.chave);
                z = z.pai;
                if (paiEhEsquerdo) {
                    rotacionarEsquerda(z);
                } else {
                    rotacionarDireita(z);
                }
            }
            // Caso 3: tio preto e z forma uma "linha" -> recolorir e rotacionar o avô
            registrar("Inserção caso 3 (tio preto, linha): pai %d fica preto, avô %d fica vermelho, rotação no avô",
                    z.pai.chave, z.pai.pai.chave);
            pintar(z.pai, PRETO);
            pintar(z.pai.pai, VERMELHO);
            if (paiEhEsquerdo) {
                rotacionarDireita(z.pai.pai);
            } else {
                rotacionarEsquerda(z.pai.pai);
            }
        }
        if (raiz.cor == VERMELHO) {
            registrar("Raiz %d recolorida para preto", raiz.chave);
            pintar(raiz, PRETO);
        }
    }

    // ------------------------------------------------------------------ remoção

    @Override
    public boolean remover(int chave) {
        No z = encontrar(chave);
        if (z == nil) {
            return false; // inexistente: árvore inalterada
        }

        No y = z;                 // nó que efetivamente sai da sua posição
        boolean corOriginalY = y.cor;
        No x;                     // nó que ocupa a antiga posição de y

        if (z.esq == nil) {
            x = z.dir;
            transplantar(z, z.dir);
        } else if (z.dir == nil) {
            x = z.esq;
            transplantar(z, z.esq);
        } else {
            // Dois filhos: o sucessor em ordem ocupa o lugar de z e herda sua cor
            y = minimo(z.dir);
            registrar("Nó %d tem dois filhos: será substituído pelo sucessor %d", z.chave, y.chave);
            corOriginalY = y.cor;
            x = y.dir;
            if (y.pai == z) {
                x.pai = y; // necessário mesmo se x for o sentinela
            } else {
                transplantar(y, y.dir);
                y.dir = z.dir;
                y.dir.pai = y;
            }
            transplantar(z, y);
            y.esq = z.esq;
            y.esq.pai = y;
            y.cor = z.cor;
        }
        tamanho--;

        // Se saiu um nó preto, algum caminho perdeu um preto: x carrega um "preto extra".
        if (corOriginalY == PRETO) {
            corrigirRemocao(x);
        }
        return true;
    }

    private void corrigirRemocao(No x) {
        while (x != raiz && x.cor == PRETO) {
            boolean xEhEsquerdo = x == x.pai.esq;
            No w = xEhEsquerdo ? x.pai.dir : x.pai.esq; // irmão de x

            if (w.cor == VERMELHO) {
                // Caso 1: irmão vermelho -> recolorir e rotacionar para obter irmão preto
                registrar("Remoção caso 1 (irmão %d vermelho): recolorir e rotacionar no pai %d", w.chave, x.pai.chave);
                pintar(w, PRETO);
                pintar(x.pai, VERMELHO);
                if (xEhEsquerdo) {
                    rotacionarEsquerda(x.pai);
                    w = x.pai.dir;
                } else {
                    rotacionarDireita(x.pai);
                    w = x.pai.esq;
                }
            }

            No sobrinhoProximo = xEhEsquerdo ? w.esq : w.dir;
            No sobrinhoDistante = xEhEsquerdo ? w.dir : w.esq;

            if (sobrinhoProximo.cor == PRETO && sobrinhoDistante.cor == PRETO) {
                // Caso 2: irmão preto com filhos pretos -> irmão vira vermelho, problema sobe
                registrar("Remoção caso 2 (irmão %d preto, sobrinhos pretos): irmão fica vermelho, sobe para %d",
                        w.chave, x.pai.chave);
                pintar(w, VERMELHO);
                x = x.pai;
                continue;
            }

            if (sobrinhoDistante.cor == PRETO) {
                // Caso 3: sobrinho próximo vermelho -> rotação no irmão transforma no caso 4
                registrar("Remoção caso 3 (sobrinho próximo %d vermelho): recolorir e rotacionar no irmão %d",
                        sobrinhoProximo.chave, w.chave);
                pintar(sobrinhoProximo, PRETO);
                pintar(w, VERMELHO);
                if (xEhEsquerdo) {
                    rotacionarDireita(w);
                    w = x.pai.dir;
                } else {
                    rotacionarEsquerda(w);
                    w = x.pai.esq;
                }
                sobrinhoDistante = xEhEsquerdo ? w.dir : w.esq;
            }

            // Caso 4: sobrinho distante vermelho -> recolorir e rotacionar no pai; termina
            registrar("Remoção caso 4 (sobrinho distante %d vermelho): recolorir e rotacionar no pai %d",
                    sobrinhoDistante.chave, x.pai.chave);
            pintar(w, x.pai.cor);
            pintar(x.pai, PRETO);
            pintar(sobrinhoDistante, PRETO);
            if (xEhEsquerdo) {
                rotacionarEsquerda(x.pai);
            } else {
                rotacionarDireita(x.pai);
            }
            x = raiz;
        }
        if (x != nil && x.cor == VERMELHO) {
            // x vermelho absorve o "preto extra": basta pintá-lo de preto
            registrar("Nó %d (vermelho) recolorido para preto, repondo o preto removido do caminho", x.chave);
            pintar(x, PRETO);
        }
    }

    /** Coloca a subárvore v no lugar da subárvore u (ligação com o pai de u). */
    private void transplantar(No u, No v) {
        if (u.pai == nil) {
            raiz = v;
        } else if (u == u.pai.esq) {
            u.pai.esq = v;
        } else {
            u.pai.dir = v;
        }
        v.pai = u.pai;
    }

    private No minimo(No no) {
        while (no.esq != nil) {
            no = no.esq;
        }
        return no;
    }

    // ------------------------------------------------------------------ rotações e cores

    /** Troca a cor do nó, contabilizando apenas mudanças reais de cor. */
    private void pintar(No no, boolean cor) {
        if (no != nil && no.cor != cor) {
            no.cor = cor;
            estatisticas.contarRecoloracao();
        }
    }

    private void rotacionarEsquerda(No x) {
        No y = x.dir;
        x.dir = y.esq;
        if (y.esq != nil) {
            y.esq.pai = x;
        }
        y.pai = x.pai;
        if (x.pai == nil) {
            raiz = y;
        } else if (x == x.pai.esq) {
            x.pai.esq = y;
        } else {
            x.pai.dir = y;
        }
        y.esq = x;
        x.pai = y;
        estatisticas.contarRotacaoEsquerda();
        registrar("  rotação à esquerda em %d: %d sobe", x.chave, y.chave);
    }

    private void rotacionarDireita(No y) {
        No x = y.esq;
        y.esq = x.dir;
        if (x.dir != nil) {
            x.dir.pai = y;
        }
        x.pai = y.pai;
        if (y.pai == nil) {
            raiz = x;
        } else if (y == y.pai.dir) {
            y.pai.dir = x;
        } else {
            y.pai.esq = x;
        }
        x.dir = y;
        y.pai = x;
        estatisticas.contarRotacaoDireita();
        registrar("  rotação à direita em %d: %d sobe", y.chave, x.chave);
    }

    // ------------------------------------------------------------------ consultas

    @Override
    public boolean buscar(int chave) {
        No atual = raiz;
        while (atual != nil) {
            estatisticas.contarComparacao();
            if (chave == atual.chave) {
                return true;
            }
            atual = chave < atual.chave ? atual.esq : atual.dir;
        }
        return false;
    }

    private No encontrar(int chave) {
        No atual = raiz;
        while (atual != nil && atual.chave != chave) {
            atual = chave < atual.chave ? atual.esq : atual.dir;
        }
        return atual;
    }

    @Override
    public int grauDoNo(int chave) {
        No no = encontrar(chave);
        if (no == nil) {
            return -1;
        }
        return (no.esq != nil ? 1 : 0) + (no.dir != nil ? 1 : 0);
    }

    /** Cor do nó com a chave: "vermelho", "preto" ou {@code null} se não existe. */
    public String corDoNo(int chave) {
        No no = encontrar(chave);
        if (no == nil) {
            return null;
        }
        return no.cor == VERMELHO ? "vermelho" : "preto";
    }

    @Override
    public List<Integer> emOrdem() {
        List<Integer> saida = new ArrayList<>(tamanho);
        emOrdem(raiz, saida);
        return saida;
    }

    private void emOrdem(No no, List<Integer> saida) {
        if (no == nil) {
            return;
        }
        emOrdem(no.esq, saida);
        saida.add(no.chave);
        emOrdem(no.dir, saida);
    }

    @Override
    public int altura() {
        return altura(raiz);
    }

    private int altura(No no) {
        if (no == nil) {
            return 0;
        }
        return 1 + Math.max(altura(no.esq), altura(no.dir));
    }

    /** Número de nós pretos de qualquer caminho da raiz até uma folha NIL (sem contar o NIL). */
    public int alturaNegra() {
        int pretos = 0;
        for (No no = raiz; no != nil; no = no.esq) {
            if (no.cor == PRETO) {
                pretos++;
            }
        }
        return pretos;
    }

    @Override
    public int tamanho() {
        return tamanho;
    }

    @Override
    public void limpar() {
        raiz = nil;
        tamanho = 0;
    }

    // ------------------------------------------------------------------ visualização

    @Override
    public String visualizar() {
        if (raiz == nil) {
            return "(árvore vazia)\n";
        }
        StringBuilder sb = new StringBuilder();
        desenhar(raiz, sb, "", false, true);
        sb.append("Legenda: [P] = nó preto, [V] = nó vermelho. Altura negra = ").append(alturaNegra()).append('\n');
        return sb.toString();
    }

    private void desenhar(No no, StringBuilder sb, String prefixo, boolean ehDireito, boolean ehRaiz) {
        if (no == nil) {
            return;
        }
        desenhar(no.dir, sb, prefixoFilhos(prefixo, ehRaiz, ehDireito, true), true, false);
        sb.append(prefixo).append(conector(ehRaiz, ehDireito)).append(rotulo(no)).append('\n');
        desenhar(no.esq, sb, prefixoFilhos(prefixo, ehRaiz, ehDireito, false), false, false);
    }

    private String rotulo(No no) {
        String texto = no.chave + (no.cor == VERMELHO ? " [V]" : " [P]");
        if (coresAnsi && no.cor == VERMELHO) {
            return ANSI_VERMELHO + texto + ANSI_RESET;
        }
        return texto;
    }

    // ------------------------------------------------------------------ verificação

    @Override
    public List<String> verificarPropriedades() {
        List<String> violacoes = new ArrayList<>();
        if (nil.cor != PRETO) {
            violacoes.add("Sentinela NIL não é preto");
        }
        if (raiz != nil && raiz.cor != PRETO) {
            violacoes.add("Propriedade 2 violada: raiz " + raiz.chave + " é vermelha");
        }
        if (raiz != nil && raiz.pai != nil) {
            violacoes.add("Pai da raiz deveria ser NIL");
        }
        int[] contagem = new int[1];
        verificar(raiz, Long.MIN_VALUE, Long.MAX_VALUE, violacoes, contagem);
        if (contagem[0] != tamanho) {
            violacoes.add("Tamanho registrado (" + tamanho + ") difere do número de nós (" + contagem[0] + ")");
        }
        return violacoes;
    }

    /**
     * Confere ordem de busca, ponteiros de pai, ausência de vermelho-vermelho e
     * altura negra igual nos dois lados. Devolve a altura negra da subárvore.
     */
    private int verificar(No no, long min, long max, List<String> violacoes, int[] contagem) {
        if (no == nil) {
            return 1; // NIL conta como preto
        }
        contagem[0]++;
        if (no.chave <= min || no.chave >= max) {
            violacoes.add("Ordem de busca violada no nó " + no.chave);
        }
        if (no.esq != nil && no.esq.pai != no || no.dir != nil && no.dir.pai != no) {
            violacoes.add("Ponteiro de pai inconsistente abaixo do nó " + no.chave);
        }
        if (no.cor == VERMELHO && (no.esq.cor == VERMELHO || no.dir.cor == VERMELHO)) {
            violacoes.add("Propriedade 4 violada: nó vermelho " + no.chave + " tem filho vermelho");
        }
        int esq = verificar(no.esq, min, no.chave, violacoes, contagem);
        int dir = verificar(no.dir, no.chave, max, violacoes, contagem);
        if (esq != dir) {
            violacoes.add("Propriedade 5 violada no nó " + no.chave
                    + ": altura negra esquerda=" + esq + ", direita=" + dir);
        }
        return esq + (no.cor == PRETO ? 1 : 0);
    }
}
