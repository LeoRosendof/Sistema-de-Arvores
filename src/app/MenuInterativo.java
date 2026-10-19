package app;

import arvores.ArvoreAVL;
import arvores.ArvoreBalanceada;
import arvores.ArvoreRubroNegra;
import arvores.Estatisticas;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

/**
 * Interface de linha de comando do protótipo. O usuário escolhe a estrutura
 * (AVL ou Rubro-Negra) e executa as operações sobre IDs de encomendas.
 *
 * <p>Vários IDs podem ser informados de uma vez, separados por espaço ou
 * vírgula (ex.: {@code 50 30 70, 20}). Entradas inválidas são ignoradas com
 * aviso, sem encerrar o programa.</p>
 */
final class MenuInterativo {

    /** Acima deste tamanho a árvore não é redesenhada automaticamente no modo detalhado. */
    private static final int LIMITE_DESENHO_AUTOMATICO = 40;

    private final Scanner entrada;
    private final PrintStream out;
    private final boolean coresAnsi;
    private final boolean ecoarEntrada;
    private ArvoreBalanceada arvore;
    private boolean encerrar;

    MenuInterativo(InputStream in, PrintStream out, boolean coresAnsi, boolean ecoarEntrada) {
        this.entrada = new Scanner(in);
        this.out = out;
        this.coresAnsi = coresAnsi;
        this.ecoarEntrada = ecoarEntrada;
    }

    void iniciar(String estruturaInicial) {
        out.println("==================================================================");
        out.println("  Amazon Logistics - Black Friday");
        out.println("  Protótipo de indexação de encomendas com árvores balanceadas");
        out.println("==================================================================");

        arvore = estruturaInicial == null ? escolherEstrutura() : criar(estruturaInicial.equals("avl") ? 1 : 2);
        while (arvore != null && !encerrar) {
            exibirMenu();
            String opcao = lerLinha("Opção: ");
            if (opcao == null) {
                break;
            }
            executar(opcao.trim());
        }
        out.println("Encerrando. Até logo!");
    }

    private ArvoreBalanceada escolherEstrutura() {
        while (true) {
            out.println();
            out.println("Escolha a estrutura de dados:");
            out.println("  1) Árvore AVL");
            out.println("  2) Árvore Rubro-Negra");
            out.println("  0) Sair");
            String opcao = lerLinha("Opção: ");
            if (opcao == null || opcao.trim().equals("0")) {
                return null;
            }
            switch (opcao.trim()) {
                case "1", "2" -> {
                    return criar(Integer.parseInt(opcao.trim()));
                }
                default -> out.println("Opção inválida. Digite 1, 2 ou 0.");
            }
        }
    }

    private ArvoreBalanceada criar(int tipo) {
        ArvoreBalanceada nova;
        if (tipo == 1) {
            nova = new ArvoreAVL();
        } else {
            ArvoreRubroNegra rn = new ArvoreRubroNegra();
            rn.setCoresAnsi(coresAnsi);
            nova = rn;
        }
        out.println("Estrutura selecionada: " + nova.nome());
        return nova;
    }

    private void exibirMenu() {
        out.println();
        out.printf("--- %s | %d encomenda(s) | altura %d | modo detalhado: %s ---%n",
                arvore.nome(), arvore.tamanho(), arvore.altura(), arvore.isRastreamento() ? "LIGADO" : "desligado");
        out.println("  1) Inserir ID(s) de encomenda");
        out.println("  2) Remover ID(s)");
        out.println("  3) Buscar ID(s)");
        out.println("  4) Percurso em ordem (IDs em ordem crescente)");
        out.println("  5) Visualizar a estrutura da árvore");
        out.println("  6) Estatísticas e verificação das propriedades");
        out.println("  7) Ligar/desligar modo detalhado (rotações e cores passo a passo)");
        out.println("  8) Carregar sequência de IDs (crescente, decrescente ou aleatória)");
        out.println("  9) Limpar a árvore");
        out.println(" 10) Trocar de estrutura (AVL <-> Rubro-Negra)");
        out.println(" 11) Comparar AVL x Rubro-Negra com os mesmos dados");
        out.println("  0) Sair");
    }

    private void executar(String opcao) {
        switch (opcao) {
            case "1" -> inserir();
            case "2" -> remover();
            case "3" -> buscar();
            case "4" -> percursoEmOrdem();
            case "5" -> out.print(arvore.visualizar());
            case "6" -> estatisticas();
            case "7" -> alternarModoDetalhado();
            case "8" -> carregarSequencia();
            case "9" -> limpar();
            case "10" -> trocarEstrutura();
            case "11" -> comparar();
            case "0" -> encerrar = true;
            case "" -> { }
            default -> out.println("Opção inválida.");
        }
    }

    // ------------------------------------------------------------------ operações

    private void inserir() {
        for (int id : lerIds("ID(s) a inserir: ")) {
            if (arvore.inserir(id)) {
                out.println("ID " + id + " inserido.");
            } else {
                out.println("ID " + id + " já existe: inserção ignorada (IDs de encomenda são únicos).");
            }
            mostrarPassoAPasso();
        }
    }

    private void remover() {
        if (arvore.estaVazia()) {
            out.println("Aviso: a árvore está vazia.");
        }
        for (int id : lerIds("ID(s) a remover: ")) {
            if (arvore.remover(id)) {
                out.println("ID " + id + " removido.");
            } else {
                out.println("ID " + id + " não existe: nada foi removido.");
            }
            mostrarPassoAPasso();
        }
    }

    private void buscar() {
        for (int id : lerIds("ID(s) a buscar: ")) {
            long antes = arvore.estatisticas().comparacoes();
            boolean achou = arvore.buscar(id);
            long comparacoes = arvore.estatisticas().comparacoes() - antes;
            if (achou) {
                StringBuilder detalhe = new StringBuilder("filhos: " + arvore.grauDoNo(id));
                if (arvore instanceof ArvoreRubroNegra rn) {
                    detalhe.append(", cor: ").append(rn.corDoNo(id));
                }
                out.printf("ID %d encontrado (%s) após %d comparação(ões).%n", id, detalhe, comparacoes);
            } else {
                out.printf("ID %d não encontrado (%d comparação(ões)).%n", id, comparacoes);
            }
        }
    }

    private void percursoEmOrdem() {
        List<Integer> ids = arvore.emOrdem();
        if (ids.isEmpty()) {
            out.println("(árvore vazia)");
            return;
        }
        out.println("Percurso em ordem (" + ids.size() + " IDs):");
        StringBuilder linha = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            linha.append(ids.get(i));
            if (i < ids.size() - 1) {
                linha.append(", ");
            }
            if (linha.length() > 70) {
                out.println("  " + linha);
                linha.setLength(0);
            }
        }
        if (linha.length() > 0) {
            out.println("  " + linha);
        }
    }

    private void estatisticas() {
        int n = arvore.tamanho();
        Estatisticas est = arvore.estatisticas();
        out.println("Estrutura ............ " + arvore.nome());
        out.println("Encomendas (nós) ..... " + n);
        out.println("Altura ............... " + arvore.altura());
        if (n > 0) {
            double log2 = Math.log(n + 1) / Math.log(2);
            out.printf("Altura mínima possível %d   (ceil(log2(n+1)))%n", (int) Math.ceil(log2));
            if (arvore instanceof ArvoreRubroNegra rn) {
                out.printf("Limite teórico ....... %.2f (2*log2(n+1))%n", 2 * log2);
                out.println("Altura negra ......... " + rn.alturaNegra());
            } else {
                out.printf("Limite teórico ....... %.2f (1,44*log2(n+2) - 0,328)%n",
                        1.4405 * Math.log(n + 2) / Math.log(2) - 0.3277);
            }
        }
        out.printf("Rotações ............. %d (à esquerda: %d, à direita: %d)%n",
                est.rotacoes(), est.rotacoesEsquerda(), est.rotacoesDireita());
        out.println("Recolorações ......... " + est.recoloracoes()
                + (arvore instanceof ArvoreAVL ? " (a AVL não usa cores)" : ""));
        out.println("Comparações em buscas  " + est.comparacoes());
        List<String> violacoes = arvore.verificarPropriedades();
        if (violacoes.isEmpty()) {
            out.println("Propriedades ......... OK: todas as propriedades da estrutura estão válidas.");
        } else {
            out.println("Propriedades ......... VIOLADAS:");
            violacoes.forEach(v -> out.println("  - " + v));
        }
    }

    private void alternarModoDetalhado() {
        arvore.setRastreamento(!arvore.isRastreamento());
        out.println("Modo detalhado " + (arvore.isRastreamento() ? "LIGADO: cada rotação/ajuste será descrito"
                + " e a árvore redesenhada (até " + LIMITE_DESENHO_AUTOMATICO + " nós)." : "desligado."));
    }

    private void carregarSequencia() {
        out.println("Tipo de sequência: 1) crescente  2) decrescente  3) aleatória");
        String tipo = lerLinha("Tipo: ");
        if (tipo == null || !List.of("1", "2", "3").contains(tipo.trim())) {
            out.println("Tipo inválido.");
            return;
        }
        Integer quantidade = lerInteiro("Quantidade de IDs: ");
        if (quantidade == null || quantidade <= 0 || quantidade > 2_000_000) {
            out.println("Quantidade inválida (use de 1 a 2.000.000).");
            return;
        }
        Integer inicio = lerInteiro("Primeiro ID (ex.: 1): ");
        if (inicio == null) {
            return;
        }
        int[] ids = gerarSequencia(tipo.trim(), quantidade, inicio);

        boolean detalhado = arvore.isRastreamento();
        if (detalhado && quantidade > LIMITE_DESENHO_AUTOMATICO) {
            arvore.setRastreamento(false); // evita despejar milhares de linhas
        }
        long rotacoes = arvore.estatisticas().rotacoes();
        long cores = arvore.estatisticas().recoloracoes();
        int inseridos = 0;
        for (int id : ids) {
            if (arvore.inserir(id)) {
                inseridos++;
            }
            if (arvore.isRastreamento()) {
                out.println("Inserindo " + id + "...");
                mostrarPassoAPasso();
            }
        }
        arvore.setRastreamento(detalhado);
        out.printf("%d ID(s) inserido(s), %d repetido(s) ignorado(s).%n", inseridos, ids.length - inseridos);
        out.printf("Rotações nesta carga: %d | recolorações: %d | altura atual: %d%n",
                arvore.estatisticas().rotacoes() - rotacoes, arvore.estatisticas().recoloracoes() - cores,
                arvore.altura());
    }

    private void limpar() {
        arvore.limpar();
        arvore.estatisticas().zerar();
        out.println("Árvore esvaziada e contadores zerados.");
    }

    private void trocarEstrutura() {
        List<Integer> ids = arvore.emOrdem();
        boolean detalhado = arvore.isRastreamento();
        arvore = criar(arvore instanceof ArvoreAVL ? 2 : 1);
        if (!ids.isEmpty()) {
            String resposta = lerLinha("Transferir os " + ids.size() + " IDs atuais para a nova estrutura? (s/n): ");
            if (resposta != null && resposta.trim().equalsIgnoreCase("s")) {
                ids.forEach(arvore::inserir);
                out.println(ids.size() + " IDs transferidos (inseridos em ordem crescente).");
            }
        }
        arvore.setRastreamento(detalhado);
    }

    /** Executa a mesma sequência nas duas árvores e mostra os resultados lado a lado. */
    private void comparar() {
        out.println("Sequência: 1) crescente  2) decrescente  3) aleatória  4) digitar os IDs");
        String tipo = lerLinha("Tipo: ");
        if (tipo == null) {
            return;
        }
        int[] ids;
        switch (tipo.trim()) {
            case "1", "2", "3" -> {
                Integer quantidade = lerInteiro("Quantidade de IDs: ");
                if (quantidade == null || quantidade <= 0 || quantidade > 2_000_000) {
                    out.println("Quantidade inválida (use de 1 a 2.000.000).");
                    return;
                }
                ids = gerarSequencia(tipo.trim(), quantidade, 1);
            }
            case "4" -> ids = lerIds("IDs (na ordem de chegada): ").stream().mapToInt(Integer::intValue).toArray();
            default -> {
                out.println("Tipo inválido.");
                return;
            }
        }
        if (ids.length == 0) {
            return;
        }

        ArvoreBalanceada avl = new ArvoreAVL();
        ArvoreRubroNegra rn = new ArvoreRubroNegra();
        rn.setCoresAnsi(coresAnsi);
        for (ArvoreBalanceada a : List.of(avl, rn)) {
            for (int id : ids) {
                a.inserir(id);
            }
        }
        out.printf("%-26s %12s %20s%n", "Mesma sequência (" + ids.length + " IDs)", "AVL", "Rubro-Negra");
        out.printf("%-26s %12d %20d%n", "Altura", avl.altura(), rn.altura());
        out.printf("%-26s %12d %20d%n", "Rotações", avl.estatisticas().rotacoes(), rn.estatisticas().rotacoes());
        out.printf("%-26s %12d %20d%n", "Recolorações", 0, rn.estatisticas().recoloracoes());
        out.printf("%-26s %12s %20s%n", "Propriedades válidas",
                avl.verificarPropriedades().isEmpty() ? "sim" : "NÃO",
                rn.verificarPropriedades().isEmpty() ? "sim" : "NÃO");
        if (avl.tamanho() <= LIMITE_DESENHO_AUTOMATICO) {
            out.println("\nÁrvore AVL:");
            out.print(avl.visualizar());
            out.println("\nÁrvore Rubro-Negra:");
            out.print(rn.visualizar());
        }
    }

    // ------------------------------------------------------------------ auxiliares

    /** No modo detalhado, mostra os passos de balanceamento e a árvore resultante. */
    private void mostrarPassoAPasso() {
        if (!arvore.isRastreamento()) {
            return;
        }
        List<String> passos = arvore.consumirRastro();
        if (passos.isEmpty()) {
            out.println("  (nenhum ajuste de balanceamento foi necessário)");
        } else {
            passos.forEach(p -> out.println("  > " + p));
        }
        if (arvore.tamanho() <= LIMITE_DESENHO_AUTOMATICO) {
            out.print(indentar(arvore.visualizar()));
        }
    }

    private static String indentar(String texto) {
        return "    " + texto.replace("\n", "\n    ").stripTrailing() + "\n";
    }

    private static int[] gerarSequencia(String tipo, int quantidade, int inicio) {
        int[] ids = new int[quantidade];
        for (int i = 0; i < quantidade; i++) {
            ids[i] = tipo.equals("2") ? inicio + quantidade - 1 - i : inicio + i;
        }
        if (tipo.equals("3")) {
            Random aleatorio = new Random(42); // semente fixa: resultados reproduzíveis
            for (int i = quantidade - 1; i > 0; i--) {
                int j = aleatorio.nextInt(i + 1);
                int tmp = ids[i];
                ids[i] = ids[j];
                ids[j] = tmp;
            }
        }
        return ids;
    }

    private List<Integer> lerIds(String mensagem) {
        List<Integer> ids = new ArrayList<>();
        String linha = lerLinha(mensagem);
        if (linha == null) {
            return ids;
        }
        for (String token : linha.split("[\\s,;]+")) {
            if (token.isEmpty()) {
                continue;
            }
            try {
                ids.add(Integer.parseInt(token));
            } catch (NumberFormatException e) {
                out.println("Valor ignorado (não é um ID inteiro válido): \"" + token + "\"");
            }
        }
        if (ids.isEmpty()) {
            out.println("Nenhum ID válido informado.");
        }
        return ids;
    }

    private Integer lerInteiro(String mensagem) {
        String linha = lerLinha(mensagem);
        if (linha == null) {
            return null;
        }
        try {
            return Integer.parseInt(linha.trim());
        } catch (NumberFormatException e) {
            out.println("Número inválido: \"" + linha.trim() + "\"");
            return null;
        }
    }

    /** Lê uma linha; devolve {@code null} no fim da entrada (ex.: entrada redirecionada de arquivo). */
    private String lerLinha(String mensagem) {
        out.print(mensagem);
        out.flush();
        if (!entrada.hasNextLine()) {
            encerrar = true;
            out.println();
            return null;
        }
        String linha = entrada.nextLine();
        if (ecoarEntrada) {
            out.println(linha); // entrada vinda de arquivo/pipe: repete para o registro ficar legível
        }
        return linha;
    }
}
