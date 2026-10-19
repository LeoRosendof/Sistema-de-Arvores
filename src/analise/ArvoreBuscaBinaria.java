package analise;

import java.util.ArrayDeque;

/**
 * Árvore binária de busca comum, SEM balanceamento.
 *
 * <p>Usada apenas como linha de base na análise, para mostrar o problema que as
 * árvores balanceadas resolvem: com IDs chegando em ordem crescente ela
 * degenera em uma lista encadeada (altura = n). Implementação iterativa para
 * não estourar a pilha nesse caso degenerado.</p>
 */
final class ArvoreBuscaBinaria {

    private static final class No {
        final int chave;
        No esq;
        No dir;

        No(int chave) {
            this.chave = chave;
        }
    }

    private No raiz;

    boolean inserir(int chave) {
        if (raiz == null) {
            raiz = new No(chave);
            return true;
        }
        No atual = raiz;
        while (true) {
            if (chave == atual.chave) {
                return false;
            }
            if (chave < atual.chave) {
                if (atual.esq == null) {
                    atual.esq = new No(chave);
                    return true;
                }
                atual = atual.esq;
            } else {
                if (atual.dir == null) {
                    atual.dir = new No(chave);
                    return true;
                }
                atual = atual.dir;
            }
        }
    }

    /** Altura em níveis, calculada por percurso em largura (sem recursão). */
    int altura() {
        if (raiz == null) {
            return 0;
        }
        int niveis = 0;
        ArrayDeque<No> fila = new ArrayDeque<>();
        fila.add(raiz);
        while (!fila.isEmpty()) {
            niveis++;
            for (int i = fila.size(); i > 0; i--) {
                No no = fila.poll();
                if (no.esq != null) {
                    fila.add(no.esq);
                }
                if (no.dir != null) {
                    fila.add(no.dir);
                }
            }
        }
        return niveis;
    }
}
