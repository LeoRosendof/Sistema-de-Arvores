package arvores;

import java.util.List;

/**
 * Contrato comum às duas árvores balanceadas do projeto (AVL e Rubro-Negra).
 *
 * <p>As chaves são inteiros que representam os identificadores das encomendas.
 * Ter uma interface única permite que o menu, os testes e o benchmark executem
 * exatamente a mesma sequência de operações nas duas estruturas.</p>
 *
 * <p>Decisões de projeto (válidas para as duas implementações):</p>
 * <ul>
 *   <li><b>Chaves repetidas</b> não são aceitas: {@link #inserir(int)} devolve
 *       {@code false} e a árvore permanece inalterada (um ID de encomenda é único).</li>
 *   <li><b>Remoção/busca de chave inexistente</b> não altera a árvore e devolve
 *       {@code false}.</li>
 *   <li><b>Remoção de nó com dois filhos</b> usa o sucessor em ordem
 *       (menor chave da subárvore direita).</li>
 *   <li><b>Altura</b> é medida em número de níveis: árvore vazia = 0,
 *       árvore com um único nó = 1.</li>
 * </ul>
 */
public interface ArvoreBalanceada {

    /** Nome legível da estrutura (ex.: "Árvore AVL"). */
    String nome();

    /**
     * Insere uma chave mantendo o balanceamento.
     *
     * @return {@code true} se inseriu; {@code false} se a chave já existia.
     */
    boolean inserir(int chave);

    /**
     * Remove uma chave mantendo o balanceamento.
     *
     * @return {@code true} se removeu; {@code false} se a chave não existia.
     */
    boolean remover(int chave);

    /** @return {@code true} se a chave está na árvore. */
    boolean buscar(int chave);

    /** Percurso em ordem (chaves em ordem crescente). */
    List<Integer> emOrdem();

    /** Representação textual da estrutura, deitada (raiz à esquerda). */
    String visualizar();

    /** Número de níveis da árvore (vazia = 0). */
    int altura();

    /** Quantidade de chaves armazenadas. */
    int tamanho();

    default boolean estaVazia() {
        return tamanho() == 0;
    }

    /**
     * Quantidade de filhos do nó que contém a chave.
     *
     * @return 0, 1 ou 2; ou -1 se a chave não existe.
     */
    int grauDoNo(int chave);

    /** Remove todas as chaves (os contadores não são zerados). */
    void limpar();

    /** Contadores de rotações, recolorações e comparações. */
    Estatisticas estatisticas();

    /**
     * Confere todas as propriedades da estrutura (ordem de busca, invariantes de
     * balanceamento, consistência dos campos internos).
     *
     * @return lista de violações encontradas; lista vazia = árvore válida.
     */
    List<String> verificarPropriedades();

    /**
     * Ativa/desativa o modo detalhado: cada rotação e ajuste de cor passa a ser
     * descrito em {@link #consumirRastro()}.
     */
    void setRastreamento(boolean ativo);

    boolean isRastreamento();

    /** Devolve e limpa as mensagens produzidas pelo modo detalhado. */
    List<String> consumirRastro();
}
