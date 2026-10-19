package testes;

import arvores.ArvoreAVL;
import arvores.ArvoreBalanceada;
import arvores.ArvoreRubroNegra;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Bateria de testes automatizados, sem dependências externas.
 *
 * <p>Cada cenário é executado com as DUAS árvores, com os mesmos dados e a mesma
 * sequência de operações. Depois de toda inserção ou remoção relevante, as
 * propriedades de balanceamento são conferidas com
 * {@link ArvoreBalanceada#verificarPropriedades()}.</p>
 *
 * <p>Execução: {@code java -jar dist/arvores-balanceadas.jar --testes}</p>
 */
public final class TestesArvores {

    private record Cenario(String nome, Consumer<Supplier<ArvoreBalanceada>> corpo, boolean somenteRubroNegra) {
        Cenario(String nome, Consumer<Supplier<ArvoreBalanceada>> corpo) {
            this(nome, corpo, false);
        }
    }

    private static final class FalhaTeste extends RuntimeException {
        FalhaTeste(String mensagem) {
            super(mensagem);
        }
    }

    private static final List<Supplier<ArvoreBalanceada>> ARVORES =
            List.of(ArvoreAVL::new, ArvoreRubroNegra::new);

    public static void main(String[] args) {
        System.exit(executar() ? 0 : 1);
    }

    /** Executa todos os cenários; devolve {@code true} se todos passaram. */
    public static boolean executar() {
        List<Cenario> cenarios = List.of(
                new Cenario("Operações em árvore vazia", TestesArvores::arvoreVazia),
                new Cenario("Árvore com apenas um elemento", TestesArvores::umElemento),
                new Cenario("Inserção em ordem crescente (1..2000)", a -> insercaoEmOrdem(a, crescente(2000))),
                new Cenario("Inserção em ordem decrescente (2000..1)", a -> insercaoEmOrdem(a, decrescente(2000))),
                new Cenario("Inserção em ordem não ordenada (aleatória)", a -> insercaoEmOrdem(a, aleatoria(2000, 7))),
                new Cenario("Inserção de valores repetidos é rejeitada", TestesArvores::insercaoRepetidos),
                new Cenario("Busca de elementos existentes e inexistentes", TestesArvores::buscaExistentesEInexistentes),
                new Cenario("Remoção de folha (nó sem filhos)", TestesArvores::remocaoFolha),
                new Cenario("Remoção de nó com um filho", TestesArvores::remocaoUmFilho),
                new Cenario("Remoção de nó com dois filhos (sucessor)", TestesArvores::remocaoDoisFilhos),
                new Cenario("Remoção de todos os nós com cada configuração de filhos", TestesArvores::remocaoPorGrau),
                new Cenario("Remoção de elemento inexistente", TestesArvores::remocaoInexistente),
                new Cenario("Esvaziar a árvore removendo em várias ordens", TestesArvores::esvaziar),
                new Cenario("Rotação simples (inserir 1, 2, 3)", TestesArvores::rotacaoSimples),
                new Cenario("Rotação dupla (inserir 30, 10, 20)", TestesArvores::rotacaoDupla),
                new Cenario("Altura dentro do limite teórico (100.000 IDs)", TestesArvores::limiteAltura),
                new Cenario("Operações aleatórias comparadas com java.util.TreeSet", TestesArvores::comparacaoComOraculo),
                new Cenario("Limpar a árvore", TestesArvores::limpar),
                new Cenario("Cores esperadas após inserir 10, 20, 30, 40", TestesArvores::coresRubroNegra, true));

        int aprovados = 0;
        int total = 0;
        List<String> falhas = new ArrayList<>();
        System.out.println("=== Testes automatizados: Árvore AVL e Árvore Rubro-Negra ===");
        for (Cenario cenario : cenarios) {
            for (Supplier<ArvoreBalanceada> fabrica : ARVORES) {
                ArvoreBalanceada exemplo = fabrica.get();
                if (cenario.somenteRubroNegra && !(exemplo instanceof ArvoreRubroNegra)) {
                    continue;
                }
                total++;
                String rotulo = String.format("%-60s [%s]", cenario.nome, exemplo.nome());
                try {
                    cenario.corpo.accept(fabrica);
                    aprovados++;
                    System.out.println("  OK     " + rotulo);
                } catch (RuntimeException e) {
                    System.out.println("  FALHOU " + rotulo + " -> " + e.getMessage());
                    falhas.add(rotulo + ": " + e);
                }
            }
        }
        System.out.printf("%nResultado: %d de %d testes aprovados.%n", aprovados, total);
        if (!falhas.isEmpty()) {
            System.out.println("Falhas:");
            falhas.forEach(f -> System.out.println("  - " + f));
        }
        return falhas.isEmpty();
    }

    // ================================================================ cenários

    private static void arvoreVazia(Supplier<ArvoreBalanceada> fabrica) {
        ArvoreBalanceada a = fabrica.get();
        verificar(a.estaVazia(), "árvore nova deveria estar vazia");
        verificar(!a.buscar(10), "busca em árvore vazia deveria falhar");
        verificar(!a.remover(10), "remoção em árvore vazia deveria devolver false");
        verificar(a.emOrdem().isEmpty(), "percurso em ordem deveria ser vazio");
        verificar(a.altura() == 0, "altura da árvore vazia deveria ser 0");
        verificar(a.grauDoNo(10) == -1, "grau de chave inexistente deveria ser -1");
        verificar(a.visualizar().contains("vazia"), "visualização deveria indicar árvore vazia");
        verificarValida(a, "árvore vazia");
    }

    private static void umElemento(Supplier<ArvoreBalanceada> fabrica) {
        ArvoreBalanceada a = fabrica.get();
        verificar(a.inserir(42), "inserção de 42 deveria ser aceita");
        verificarValida(a, "após inserir 42");
        verificar(a.tamanho() == 1 && a.altura() == 1, "árvore com um nó deveria ter tamanho 1 e altura 1");
        verificar(a.buscar(42), "42 deveria ser encontrado");
        verificar(!a.buscar(7), "7 não deveria ser encontrado");
        verificar(a.grauDoNo(42) == 0, "único nó deveria ser folha");
        verificar(a.emOrdem().equals(List.of(42)), "percurso deveria ser [42]");
        verificar(!a.inserir(42), "inserir 42 de novo deveria ser rejeitado");
        verificar(!a.remover(7), "remover 7 deveria devolver false");
        verificar(a.tamanho() == 1, "tamanho deveria continuar 1");
        verificar(a.remover(42), "remover 42 deveria funcionar");
        verificar(a.estaVazia() && a.altura() == 0, "árvore deveria ficar vazia");
        verificarValida(a, "após remover o único elemento");
    }

    private static void insercaoEmOrdem(Supplier<ArvoreBalanceada> fabrica, int[] dados) {
        ArvoreBalanceada a = fabrica.get();
        for (int chave : dados) {
            verificar(a.inserir(chave), "inserção de " + chave + " deveria ser aceita");
            verificarValida(a, "após inserir " + chave);
        }
        verificar(a.tamanho() == dados.length, "tamanho incorreto");
        verificarOrdenado(a.emOrdem(), dados.length);
        verificarLimiteAltura(a);
    }

    private static void insercaoRepetidos(Supplier<ArvoreBalanceada> fabrica) {
        ArvoreBalanceada a = fabrica.get();
        int[] dados = aleatoria(300, 11);
        for (int chave : dados) {
            a.inserir(chave);
        }
        List<Integer> antes = a.emOrdem();
        String desenhoAntes = a.visualizar();
        long rotacoesAntes = a.estatisticas().rotacoes();
        long coresAntes = a.estatisticas().recoloracoes();
        for (int chave : dados) {
            verificar(!a.inserir(chave), "valor repetido " + chave + " deveria ser rejeitado");
        }
        verificar(a.tamanho() == dados.length, "tamanho mudou após inserir repetidos");
        verificar(a.emOrdem().equals(antes), "conteúdo mudou após inserir repetidos");
        verificar(a.visualizar().equals(desenhoAntes), "estrutura mudou após inserir repetidos");
        verificar(a.estatisticas().rotacoes() == rotacoesAntes
                && a.estatisticas().recoloracoes() == coresAntes, "repetidos não deveriam causar balanceamento");
        verificarValida(a, "após tentativas de repetidos");
    }

    private static void buscaExistentesEInexistentes(Supplier<ArvoreBalanceada> fabrica) {
        ArvoreBalanceada a = fabrica.get();
        for (int i = 2; i <= 400; i += 2) {
            a.inserir(i);
        }
        for (int i = 2; i <= 400; i += 2) {
            verificar(a.buscar(i), "par " + i + " deveria ser encontrado");
            verificar(!a.buscar(i - 1), "ímpar " + (i - 1) + " não deveria ser encontrado");
        }
        verificar(!a.buscar(0) && !a.buscar(-5) && !a.buscar(401) && !a.buscar(Integer.MAX_VALUE)
                && !a.buscar(Integer.MIN_VALUE), "valores fora do intervalo não deveriam ser encontrados");
        verificar(a.tamanho() == 200, "buscas não deveriam alterar a árvore");
        verificarValida(a, "após buscas");
    }

    private static void remocaoFolha(Supplier<ArvoreBalanceada> fabrica) {
        ArvoreBalanceada a = construir(fabrica, 50, 30, 70, 20, 40, 60, 80);
        verificar(a.grauDoNo(20) == 0, "pré-condição: 20 deveria ser folha");
        removerEConferir(a, 20);
    }

    private static void remocaoUmFilho(Supplier<ArvoreBalanceada> fabrica) {
        // Nas duas árvores, 5 fica com um único filho (3)
        ArvoreBalanceada a = construir(fabrica, 10, 5, 15, 3);
        verificar(a.grauDoNo(5) == 1, "pré-condição: 5 deveria ter exatamente um filho");
        removerEConferir(a, 5);
        verificar(a.buscar(3), "o filho (3) deveria continuar na árvore");
    }

    private static void remocaoDoisFilhos(Supplier<ArvoreBalanceada> fabrica) {
        ArvoreBalanceada a = construir(fabrica, 50, 30, 70, 20, 40, 60, 80);
        verificar(a.grauDoNo(30) == 2, "pré-condição: 30 deveria ter dois filhos");
        removerEConferir(a, 30);
        verificar(a.grauDoNo(50) == 2, "pré-condição: 50 deveria ter dois filhos");
        removerEConferir(a, 50); // raiz com dois filhos
        verificar(a.emOrdem().equals(List.of(20, 40, 60, 70, 80)), "conteúdo após remoções incorreto");
    }

    /** Remove repetidamente nós de grau 0, 1 e 2 até esvaziar, conferindo tudo a cada passo. */
    private static void remocaoPorGrau(Supplier<ArvoreBalanceada> fabrica) {
        ArvoreBalanceada a = fabrica.get();
        for (int chave : aleatoria(500, 23)) {
            a.inserir(chave);
        }
        int[] removidosPorGrau = new int[3];
        int grauAlvo = 0;
        while (!a.estaVazia()) {
            Integer escolhido = null;
            for (int tentativa = 0; tentativa < 3 && escolhido == null; tentativa++) {
                int grau = (grauAlvo + tentativa) % 3;
                for (int chave : a.emOrdem()) {
                    if (a.grauDoNo(chave) == grau) {
                        escolhido = chave;
                        removidosPorGrau[grau]++;
                        break;
                    }
                }
            }
            removerEConferir(a, escolhido);
            grauAlvo = (grauAlvo + 1) % 3;
        }
        verificar(removidosPorGrau[0] > 0 && removidosPorGrau[1] > 0 && removidosPorGrau[2] > 0,
                "deveria ter removido nós com 0, 1 e 2 filhos");
    }

    private static void remocaoInexistente(Supplier<ArvoreBalanceada> fabrica) {
        ArvoreBalanceada a = construir(fabrica, 50, 30, 70, 20, 40, 60, 80);
        String antes = a.visualizar();
        long rotacoes = a.estatisticas().rotacoes();
        for (int chave : new int[] {0, 25, 45, 100, -1}) {
            verificar(!a.remover(chave), "remover " + chave + " (inexistente) deveria devolver false");
        }
        verificar(a.visualizar().equals(antes), "a estrutura não deveria mudar");
        verificar(a.estatisticas().rotacoes() == rotacoes, "não deveria haver rotações");
        verificar(a.tamanho() == 7, "tamanho não deveria mudar");
        verificarValida(a, "após remoções inexistentes");
        verificar(a.remover(30), "remover 30 (existente) deveria funcionar");
        verificar(!a.remover(30), "remover 30 pela segunda vez deveria devolver false");
        verificarValida(a, "após remover 30 duas vezes");
    }

    private static void esvaziar(Supplier<ArvoreBalanceada> fabrica) {
        List<int[]> ordensRemocao = List.of(crescente(1000), decrescente(1000), aleatoria(1000, 99));
        for (int[] ordem : ordensRemocao) {
            ArvoreBalanceada a = fabrica.get();
            for (int chave : aleatoria(1000, 5)) {
                a.inserir(chave);
            }
            for (int chave : ordem) {
                verificar(a.remover(chave), "remoção de " + chave + " deveria funcionar");
                verificarValida(a, "após remover " + chave);
            }
            verificar(a.estaVazia() && a.altura() == 0, "árvore deveria ficar vazia");
        }
    }

    private static void rotacaoSimples(Supplier<ArvoreBalanceada> fabrica) {
        ArvoreBalanceada a = construir(fabrica, 1, 2, 3);
        verificar(a.estatisticas().rotacoesEsquerda() == 1 && a.estatisticas().rotacoesDireita() == 0,
                "inserir 1, 2, 3 deveria causar exatamente uma rotação à esquerda, houve: " + a.estatisticas());
        verificar(a.altura() == 2 && a.grauDoNo(2) == 2, "2 deveria virar a raiz com filhos 1 e 3");
        verificarValida(a, "após rotação simples");
    }

    private static void rotacaoDupla(Supplier<ArvoreBalanceada> fabrica) {
        ArvoreBalanceada a = construir(fabrica, 30, 10, 20);
        verificar(a.estatisticas().rotacoesEsquerda() == 1 && a.estatisticas().rotacoesDireita() == 1,
                "inserir 30, 10, 20 deveria causar rotação dupla (esq. + dir.), houve: " + a.estatisticas());
        verificar(a.altura() == 2 && a.grauDoNo(20) == 2, "20 deveria virar a raiz com filhos 10 e 30");
        verificarValida(a, "após rotação dupla");
    }

    private static void limiteAltura(Supplier<ArvoreBalanceada> fabrica) {
        for (int[] dados : List.of(crescente(100_000), aleatoria(100_000, 3))) {
            ArvoreBalanceada a = fabrica.get();
            for (int chave : dados) {
                a.inserir(chave);
            }
            verificarValida(a, "após 100.000 inserções");
            verificarLimiteAltura(a);
        }
    }

    /** Sequência aleatória de inserções, buscas e remoções; o TreeSet do Java serve de referência. */
    private static void comparacaoComOraculo(Supplier<ArvoreBalanceada> fabrica) {
        ArvoreBalanceada a = fabrica.get();
        TreeSet<Integer> referencia = new TreeSet<>();
        Random aleatorio = new Random(2025);
        for (int i = 0; i < 30_000; i++) {
            int chave = aleatorio.nextInt(1500);
            int operacao = aleatorio.nextInt(10);
            if (operacao < 4) {
                verificar(a.inserir(chave) == referencia.add(chave), "resultado de inserir(" + chave + ") divergiu");
            } else if (operacao < 7) {
                verificar(a.remover(chave) == referencia.remove(chave), "resultado de remover(" + chave + ") divergiu");
            } else {
                verificar(a.buscar(chave) == referencia.contains(chave), "resultado de buscar(" + chave + ") divergiu");
            }
            verificar(a.tamanho() == referencia.size(), "tamanho divergiu na operação " + i);
            if (operacao < 7) {
                verificarValida(a, "na operação " + i);
            }
        }
        verificar(a.emOrdem().equals(new ArrayList<>(referencia)), "conteúdo final divergiu do TreeSet");
    }

    private static void limpar(Supplier<ArvoreBalanceada> fabrica) {
        ArvoreBalanceada a = construir(fabrica, 5, 3, 8, 1, 4);
        a.limpar();
        verificar(a.estaVazia() && a.altura() == 0 && a.emOrdem().isEmpty(), "árvore deveria ficar vazia");
        verificar(!a.buscar(5), "nada deveria ser encontrado");
        verificar(a.inserir(5), "deveria aceitar novas inserções após limpar");
        verificarValida(a, "após limpar e reinserir");
    }

    /** Específico da Rubro-Negra: cores esperadas após inserções conhecidas. */
    private static void coresRubroNegra(Supplier<ArvoreBalanceada> fabrica) {
        ArvoreRubroNegra rn = (ArvoreRubroNegra) construir(fabrica, 10, 20, 30);
        verificar("preto".equals(rn.corDoNo(20)) && "vermelho".equals(rn.corDoNo(10))
                && "vermelho".equals(rn.corDoNo(30)) && rn.corDoNo(99) == null,
                "após 10, 20, 30 esperava raiz 20 preta e filhos vermelhos");
        long rotacoes = rn.estatisticas().rotacoes();
        rn.inserir(40); // tio (10) vermelho: caso 1, só recoloração
        verificar("preto".equals(rn.corDoNo(10)) && "preto".equals(rn.corDoNo(30))
                && "preto".equals(rn.corDoNo(20)) && "vermelho".equals(rn.corDoNo(40)),
                "após inserir 40 esperava 10 e 30 pretos e 40 vermelho");
        verificar(rn.estatisticas().rotacoes() == rotacoes, "o caso 1 não deveria rotacionar");
        verificarValida(rn, "após inserir 40");
    }

    // ================================================================ auxiliares

    private static ArvoreBalanceada construir(Supplier<ArvoreBalanceada> fabrica, int... chaves) {
        ArvoreBalanceada a = fabrica.get();
        for (int chave : chaves) {
            verificar(a.inserir(chave), "inserção de " + chave + " deveria ser aceita");
            verificarValida(a, "após inserir " + chave);
        }
        return a;
    }

    private static void removerEConferir(ArvoreBalanceada a, int chave) {
        List<Integer> esperado = new ArrayList<>(a.emOrdem());
        esperado.remove(Integer.valueOf(chave));
        verificar(a.remover(chave), "remoção de " + chave + " deveria devolver true");
        verificar(!a.buscar(chave), chave + " não deveria mais ser encontrado");
        verificar(a.emOrdem().equals(esperado), "demais elementos deveriam ser preservados após remover " + chave);
        verificar(a.tamanho() == esperado.size(), "tamanho incorreto após remover " + chave);
        verificarValida(a, "após remover " + chave);
    }

    private static void verificarValida(ArvoreBalanceada a, String contexto) {
        List<String> violacoes = a.verificarPropriedades();
        if (!violacoes.isEmpty()) {
            throw new FalhaTeste("propriedade violada " + contexto + ": " + violacoes);
        }
    }

    private static void verificarLimiteAltura(ArvoreBalanceada a) {
        int n = a.tamanho();
        double log2 = Math.log(n + 2) / Math.log(2);
        double limite = a instanceof ArvoreAVL
                ? 1.4405 * log2 - 0.3277
                : 2 * Math.log(n + 1) / Math.log(2);
        verificar(a.altura() <= limite, "altura " + a.altura() + " acima do limite teórico " + limite);
    }

    private static void verificarOrdenado(List<Integer> lista, int n) {
        verificar(lista.size() == n, "percurso deveria ter " + n + " elementos");
        for (int i = 0; i < n; i++) {
            verificar(lista.get(i) == i + 1, "percurso em ordem incorreto na posição " + i);
        }
    }

    private static void verificar(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new FalhaTeste(mensagem);
        }
    }

    private static int[] crescente(int n) {
        int[] v = new int[n];
        for (int i = 0; i < n; i++) {
            v[i] = i + 1;
        }
        return v;
    }

    private static int[] decrescente(int n) {
        int[] v = new int[n];
        for (int i = 0; i < n; i++) {
            v[i] = n - i;
        }
        return v;
    }

    private static int[] aleatoria(int n, long semente) {
        int[] v = crescente(n);
        Random aleatorio = new Random(semente);
        for (int i = n - 1; i > 0; i--) {
            int j = aleatorio.nextInt(i + 1);
            int tmp = v[i];
            v[i] = v[j];
            v[j] = tmp;
        }
        return v;
    }
}
