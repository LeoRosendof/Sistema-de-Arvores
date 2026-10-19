package arvores;

import java.util.ArrayList;
import java.util.List;

/**
 * Árvore AVL (Adelson-Velsky e Landis, 1962).
 *
 * <p><b>Estratégia de balanceamento:</b> cada nó guarda a sua altura. O fator de
 * balanceamento é {@code FB = altura(esquerda) - altura(direita)} e deve ficar
 * sempre em {-1, 0, +1}. Depois de cada inserção ou remoção, o caminho de volta
 * até a raiz é percorrido (pela própria recursão), as alturas são atualizadas e,
 * no primeiro nó com |FB| = 2, aplica-se uma das quatro correções:</p>
 *
 * <pre>
 *   FB = +2 e FB(esq) >= 0  -> caso Esquerda-Esquerda: rotação simples à direita
 *   FB = +2 e FB(esq) <  0  -> caso Esquerda-Direita : rotação à esquerda no filho + à direita no nó
 *   FB = -2 e FB(dir) <= 0  -> caso Direita-Direita  : rotação simples à esquerda
 *   FB = -2 e FB(dir) >  0  -> caso Direita-Esquerda : rotação à direita no filho + à esquerda no nó
 * </pre>
 *
 * <p>Na inserção, no máximo uma correção (simples ou dupla) é necessária. Na
 * remoção, a correção pode reduzir a altura da subárvore e propagar o
 * desbalanceamento, podendo haver uma correção por nível (O(log n)).</p>
 */
public final class ArvoreAVL extends ArvoreBase {

    private static final class No {
        final int chave;
        int altura = 1; // folha tem altura 1; filho nulo tem altura 0
        No esq;
        No dir;

        No(int chave) {
            this.chave = chave;
        }
    }

    private No raiz;
    private int tamanho;

    // Resultado das operações recursivas (evita devolver dois valores).
    private boolean alterou;

    @Override
    public String nome() {
        return "Árvore AVL";
    }

    // ------------------------------------------------------------------ inserção

    @Override
    public boolean inserir(int chave) {
        alterou = false;
        raiz = inserir(raiz, chave);
        if (alterou) {
            tamanho++;
        }
        return alterou;
    }

    private No inserir(No no, int chave) {
        if (no == null) {
            alterou = true;
            return new No(chave);
        }
        if (chave < no.chave) {
            no.esq = inserir(no.esq, chave);
        } else if (chave > no.chave) {
            no.dir = inserir(no.dir, chave);
        } else {
            return no; // duplicada: nada muda, nem as alturas
        }
        return balancear(no);
    }

    // ------------------------------------------------------------------ remoção

    @Override
    public boolean remover(int chave) {
        alterou = false;
        raiz = remover(raiz, chave);
        if (alterou) {
            tamanho--;
        }
        return alterou;
    }

    private No remover(No no, int chave) {
        if (no == null) {
            return null; // chave inexistente
        }
        if (chave < no.chave) {
            no.esq = remover(no.esq, chave);
        } else if (chave > no.chave) {
            no.dir = remover(no.dir, chave);
        } else {
            alterou = true;
            if (no.esq == null || no.dir == null) {
                // 0 ou 1 filho: o filho (ou null) ocupa o lugar do nó
                return no.esq != null ? no.esq : no.dir;
            }
            // 2 filhos: o sucessor em ordem (mínimo da subárvore direita) substitui o nó
            No sucessor = no.dir;
            while (sucessor.esq != null) {
                sucessor = sucessor.esq;
            }
            registrar("Nó %d tem dois filhos: será substituído pelo sucessor %d", no.chave, sucessor.chave);
            No novo = new No(sucessor.chave);
            novo.esq = no.esq;
            novo.dir = removerMinimo(no.dir);
            no = novo;
        }
        return balancear(no);
    }

    /** Remove o menor nó da subárvore, rebalanceando o caminho. */
    private No removerMinimo(No no) {
        if (no.esq == null) {
            return no.dir;
        }
        no.esq = removerMinimo(no.esq);
        return balancear(no);
    }

    // ------------------------------------------------------------------ balanceamento

    private static int altura(No no) {
        return no == null ? 0 : no.altura;
    }

    private static int fatorBalanceamento(No no) {
        return altura(no.esq) - altura(no.dir);
    }

    private static void atualizarAltura(No no) {
        no.altura = 1 + Math.max(altura(no.esq), altura(no.dir));
    }

    /** Atualiza a altura do nó e aplica a rotação adequada se |FB| = 2. */
    private No balancear(No no) {
        atualizarAltura(no);
        int fb = fatorBalanceamento(no);

        if (fb > 1) {
            if (fatorBalanceamento(no.esq) < 0) {
                registrar("Nó %d com FB=+2 e filho esquerdo %d com FB=%+d: caso Esquerda-Direita (rotação dupla)",
                        no.chave, no.esq.chave, fatorBalanceamento(no.esq));
                no.esq = rotacionarEsquerda(no.esq);
            } else {
                registrar("Nó %d com FB=+2: caso Esquerda-Esquerda (rotação simples à direita)", no.chave);
            }
            return rotacionarDireita(no);
        }
        if (fb < -1) {
            if (fatorBalanceamento(no.dir) > 0) {
                registrar("Nó %d com FB=-2 e filho direito %d com FB=%+d: caso Direita-Esquerda (rotação dupla)",
                        no.chave, no.dir.chave, fatorBalanceamento(no.dir));
                no.dir = rotacionarDireita(no.dir);
            } else {
                registrar("Nó %d com FB=-2: caso Direita-Direita (rotação simples à esquerda)", no.chave);
            }
            return rotacionarEsquerda(no);
        }
        return no;
    }

    /*
     *        y                x
     *       / \              / \
     *      x   C    -->     A   y
     *     / \                  / \
     *    A   B                B   C
     */
    private No rotacionarDireita(No y) {
        No x = y.esq;
        y.esq = x.dir;
        x.dir = y;
        atualizarAltura(y);
        atualizarAltura(x);
        estatisticas.contarRotacaoDireita();
        registrar("  rotação à direita em %d: %d sobe e vira raiz da subárvore", y.chave, x.chave);
        return x;
    }

    /*
     *      x                    y
     *     / \                  / \
     *    A   y      -->       x   C
     *       / \              / \
     *      B   C            A   B
     */
    private No rotacionarEsquerda(No x) {
        No y = x.dir;
        x.dir = y.esq;
        y.esq = x;
        atualizarAltura(x);
        atualizarAltura(y);
        estatisticas.contarRotacaoEsquerda();
        registrar("  rotação à esquerda em %d: %d sobe e vira raiz da subárvore", x.chave, y.chave);
        return y;
    }

    // ------------------------------------------------------------------ consultas

    @Override
    public boolean buscar(int chave) {
        No atual = raiz;
        while (atual != null) {
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
        while (atual != null && atual.chave != chave) {
            atual = chave < atual.chave ? atual.esq : atual.dir;
        }
        return atual;
    }

    @Override
    public int grauDoNo(int chave) {
        No no = encontrar(chave);
        if (no == null) {
            return -1;
        }
        return (no.esq != null ? 1 : 0) + (no.dir != null ? 1 : 0);
    }

    @Override
    public List<Integer> emOrdem() {
        List<Integer> saida = new ArrayList<>(tamanho);
        emOrdem(raiz, saida);
        return saida;
    }

    private static void emOrdem(No no, List<Integer> saida) {
        if (no == null) {
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

    @Override
    public int tamanho() {
        return tamanho;
    }

    @Override
    public void limpar() {
        raiz = null;
        tamanho = 0;
    }

    // ------------------------------------------------------------------ visualização

    @Override
    public String visualizar() {
        if (raiz == null) {
            return "(árvore vazia)\n";
        }
        StringBuilder sb = new StringBuilder();
        desenhar(raiz, sb, "", false, true);
        sb.append("Legenda: chave (fb = fator de balanceamento = altura esq - altura dir)\n");
        return sb.toString();
    }

    private static void desenhar(No no, StringBuilder sb, String prefixo, boolean ehDireito, boolean raiz) {
        if (no == null) {
            return;
        }
        desenhar(no.dir, sb, prefixoFilhos(prefixo, raiz, ehDireito, true), true, false);
        sb.append(prefixo).append(conector(raiz, ehDireito))
                .append(no.chave).append(" (fb=").append(formatarFb(fatorBalanceamento(no))).append(')')
                .append('\n');
        desenhar(no.esq, sb, prefixoFilhos(prefixo, raiz, ehDireito, false), false, false);
    }

    private static String formatarFb(int fb) {
        return fb == 0 ? "0" : String.format("%+d", fb);
    }

    // ------------------------------------------------------------------ verificação

    @Override
    public List<String> verificarPropriedades() {
        List<String> violacoes = new ArrayList<>();
        int contados = verificar(raiz, Long.MIN_VALUE, Long.MAX_VALUE, violacoes);
        if (contados != tamanho) {
            violacoes.add("Tamanho registrado (" + tamanho + ") difere do número de nós (" + contados + ")");
        }
        return violacoes;
    }

    /** Confere ordem de busca, altura armazenada e |FB| <= 1. Devolve o número de nós. */
    private static int verificar(No no, long min, long max, List<String> violacoes) {
        if (no == null) {
            return 0;
        }
        if (no.chave <= min || no.chave >= max) {
            violacoes.add("Ordem de busca violada no nó " + no.chave);
        }
        int qtd = 1 + verificar(no.esq, min, no.chave, violacoes)
                + verificar(no.dir, no.chave, max, violacoes);
        int alturaEsperada = 1 + Math.max(altura(no.esq), altura(no.dir));
        if (no.altura != alturaEsperada) {
            violacoes.add("Altura armazenada incorreta no nó " + no.chave);
        }
        int fb = fatorBalanceamento(no);
        if (fb < -1 || fb > 1) {
            violacoes.add("Nó " + no.chave + " desbalanceado (FB=" + fb + ")");
        }
        return qtd;
    }
}
