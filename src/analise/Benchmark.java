package analise;

import arvores.ArvoreAVL;
import arvores.ArvoreBalanceada;
import arvores.ArvoreRubroNegra;
import arvores.Estatisticas;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Análise comparativa AVL x Rubro-Negra.
 *
 * <p>Todos os conjuntos de dados e sequências de operações são gerados UMA vez,
 * com semente fixa, e aplicados de forma idêntica às duas árvores. Isso torna a
 * comparação justa e os resultados reproduzíveis.</p>
 *
 * <p>Experimentos:</p>
 * <ol>
 *   <li>Altura final x quantidade de elementos, para inserção crescente,
 *       decrescente e aleatória (com uma ABB comum como linha de base).</li>
 *   <li>Ciclo completo com n elementos: inserção, buscas (existentes e
 *       inexistentes) e remoção de metade dos elementos.</li>
 *   <li>Cargas mistas simulando a Black Friday (IDs chegando em sequência,
 *       com diferentes proporções de inserção/busca/remoção).</li>
 * </ol>
 *
 * <p>Os tempos são a mediana de várias repetições, após execuções de aquecimento
 * da JVM. Eles dependem da máquina; as contagens (altura, rotações,
 * recolorações, comparações) são determinísticas.</p>
 */
public final class Benchmark {

    private static final long SEMENTE = 20251119L;
    private static final Locale BR = Locale.forLanguageTag("pt-BR");

    private static final List<Supplier<ArvoreBalanceada>> ARVORES =
            List.of(ArvoreAVL::new, ArvoreRubroNegra::new);

    enum Ordem {
        CRESCENTE("crescente"), DECRESCENTE("decrescente"), ALEATORIA("aleatória");

        final String rotulo;

        Ordem(String rotulo) {
            this.rotulo = rotulo;
        }
    }

    private final boolean rapido;
    private final int repeticoes;
    private final int aquecimento;
    private final PrintStream out;
    private final Path diretorioSaida;
    private final StringBuilder relatorio = new StringBuilder();

    public Benchmark(boolean rapido, PrintStream out, Path diretorioSaida) {
        this.rapido = rapido;
        this.repeticoes = rapido ? 1 : 11;
        this.aquecimento = rapido ? 0 : 3;
        this.out = out;
        this.diretorioSaida = diretorioSaida;
    }

    public static void main(String[] args) throws IOException {
        boolean rapido = Arrays.asList(args).contains("--rapido");
        new Benchmark(rapido, System.out, Path.of("resultados")).executar();
    }

    public void executar() throws IOException {
        titulo("# Análise comparativa: Árvore AVL x Árvore Rubro-Negra");
        linha("Semente dos dados: " + SEMENTE + " | repetições cronometradas: " + repeticoes
                + " (+" + aquecimento + " de aquecimento) | Java " + System.getProperty("java.version"));
        linha("");
        experimentoAlturas();
        experimentoCicloCompleto();
        experimentoCargasMistas();
        if (diretorioSaida != null) {
            Files.createDirectories(diretorioSaida);
            Files.writeString(diretorioSaida.resolve("resultados.md"), relatorio.toString(), StandardCharsets.UTF_8);
            out.println("Relatório e CSVs gravados em: " + diretorioSaida.toAbsolutePath());
        }
    }

    // ================================================================ experimento 1

    private void experimentoAlturas() throws IOException {
        titulo("## 1. Altura final x número de elementos");
        linha("Altura em níveis (1 nó = altura 1). Mínima possível = ceil(log2(n+1)).");
        linha("Limite AVL = 1,44·log2(n+2) - 0,328; limite Rubro-Negra = 2·log2(n+1).");
        linha("ABB = árvore de busca binária sem balanceamento (só executada até 10.000 elementos"
                + " em ordem crescente/decrescente, pois degenera e fica O(n²)).");
        linha("");

        int[] tamanhos = rapido ? new int[] {10, 100, 1_000, 10_000}
                : new int[] {10, 100, 1_000, 10_000, 100_000, 1_000_000};
        List<String[]> linhas = new ArrayList<>();
        StringBuilder csv = new StringBuilder("ordem,n,altura_minima,altura_avl,altura_rn,limite_avl,limite_rn,altura_abb\n");

        for (Ordem ordem : Ordem.values()) {
            for (int n : tamanhos) {
                int[] dados = gerarDados(ordem, n);
                int alturaAvl = construir(ArvoreAVL::new, dados).altura();
                int alturaRn = construir(ArvoreRubroNegra::new, dados).altura();
                int alturaAbb = -1;
                if (ordem == Ordem.ALEATORIA || n <= 10_000) {
                    ArvoreBuscaBinaria abb = new ArvoreBuscaBinaria();
                    for (int chave : dados) {
                        abb.inserir(chave);
                    }
                    alturaAbb = abb.altura();
                }
                int minima = (int) Math.ceil(log2(n + 1));
                double limiteAvl = 1.4405 * log2(n + 2) - 0.3277;
                double limiteRn = 2 * log2(n + 1);
                linhas.add(new String[] {ordem.rotulo, inteiro(n), String.valueOf(minima),
                        String.valueOf(alturaAvl), String.valueOf(alturaRn),
                        decimal(limiteAvl), decimal(limiteRn), alturaAbb < 0 ? "—" : inteiro(alturaAbb)});
                csv.append(String.format(Locale.ROOT, "%s,%d,%d,%d,%d,%.2f,%.2f,%d%n",
                        ordem.name().toLowerCase(Locale.ROOT), n, minima, alturaAvl, alturaRn,
                        limiteAvl, limiteRn, alturaAbb));
            }
        }
        tabela(new String[] {"Ordem", "n", "Mínima", "AVL", "Rubro-Negra", "Limite AVL", "Limite RN", "ABB comum"}, linhas);
        gravarCsv("alturas.csv", csv);
    }

    // ================================================================ experimento 2

    private void experimentoCicloCompleto() throws IOException {
        int n = rapido ? 10_000 : 100_000;
        titulo("## 2. Ciclo completo com n = " + inteiro(n) + " IDs");
        linha("Inserção de todos os IDs; busca de todos os IDs existentes (ordem aleatória) e de n IDs");
        linha("inexistentes; busca dos 10% de IDs mais recentes (últimos a chegar); remoção de metade dos IDs");
        linha("(ordem aleatória). Mesmos dados nas duas árvores.");
        linha("");

        List<String[]> linhas = new ArrayList<>();
        StringBuilder csv = new StringBuilder("ordem,arvore,n,altura_apos_insercao,rotacoes_insercao,recoloracoes_insercao,"
                + "tempo_insercao_ms,comparacoes_medias_busca_existente,comparacoes_medias_busca_inexistente,"
                + "comparacoes_medias_busca_recentes,tempo_buscas_ms,rotacoes_remocao,recoloracoes_remocao,"
                + "altura_apos_remocao,tempo_remocao_ms,propriedades_ok\n");

        Random aleatorio = new Random(SEMENTE);
        for (Ordem ordem : Ordem.values()) {
            int[] dados = gerarDados(ordem, n);
            int[] buscas = embaralhar(dados.clone(), aleatorio);
            int[] inexistentes = new int[n];
            for (int i = 0; i < n; i++) {
                inexistentes[i] = n + 1 + i; // IDs ainda não emitidos
            }
            embaralhar(inexistentes, aleatorio);
            int[] remocoes = Arrays.copyOf(embaralhar(dados.clone(), aleatorio), n / 2);

            for (Supplier<ArvoreBalanceada> fabrica : ARVORES) {
                Ciclo c = medirCiclo(fabrica, dados, buscas, inexistentes, remocoes);
                linhas.add(new String[] {ordem.rotulo, c.nome,
                        String.valueOf(c.alturaInsercao), inteiro(c.rotacoesInsercao), inteiro(c.recoloracoesInsercao),
                        decimal(c.tempoInsercaoMs),
                        decimal(c.comparacoesExistente), decimal(c.comparacoesInexistente),
                        decimal(c.comparacoesRecentes), decimal(c.tempoBuscasMs),
                        inteiro(c.rotacoesRemocao), inteiro(c.recoloracoesRemocao), String.valueOf(c.alturaRemocao),
                        decimal(c.tempoRemocaoMs), c.propriedadesOk ? "sim" : "NÃO"});
                csv.append(String.format(Locale.ROOT, "%s,%s,%d,%d,%d,%d,%.3f,%.3f,%.3f,%.3f,%.3f,%d,%d,%d,%.3f,%b%n",
                        ordem.name().toLowerCase(Locale.ROOT), sigla(c.nome), n, c.alturaInsercao,
                        c.rotacoesInsercao, c.recoloracoesInsercao, c.tempoInsercaoMs,
                        c.comparacoesExistente, c.comparacoesInexistente, c.comparacoesRecentes, c.tempoBuscasMs,
                        c.rotacoesRemocao, c.recoloracoesRemocao, c.alturaRemocao, c.tempoRemocaoMs, c.propriedadesOk));
            }
        }
        tabela(new String[] {"Ordem", "Árvore", "Altura", "Rotações (ins.)", "Recolorações (ins.)", "Tempo ins. (ms)",
                "Comp./busca existente", "Comp./busca inexistente", "Comp./busca recentes", "Tempo buscas (ms)",
                "Rotações (rem.)", "Recolorações (rem.)", "Altura após rem.", "Tempo rem. (ms)", "Propriedades OK"}, linhas);
        gravarCsv("ciclo_completo.csv", csv);
    }

    private static final class Ciclo {
        String nome;
        int alturaInsercao;
        long rotacoesInsercao;
        long recoloracoesInsercao;
        double tempoInsercaoMs;
        double comparacoesExistente;
        double comparacoesInexistente;
        double comparacoesRecentes;
        double tempoBuscasMs;
        long rotacoesRemocao;
        long recoloracoesRemocao;
        int alturaRemocao;
        double tempoRemocaoMs;
        boolean propriedadesOk;
    }

    private Ciclo medirCiclo(Supplier<ArvoreBalanceada> fabrica, int[] dados, int[] buscas,
                             int[] inexistentes, int[] remocoes) {
        Ciclo c = new Ciclo();
        double[] tIns = new double[repeticoes];
        double[] tBus = new double[repeticoes];
        double[] tRem = new double[repeticoes];

        for (int r = -aquecimento; r < repeticoes; r++) {
            ArvoreBalanceada arvore = fabrica.get();
            Estatisticas est = arvore.estatisticas();
            c.nome = arvore.nome();

            long t0 = System.nanoTime();
            for (int chave : dados) {
                arvore.inserir(chave);
            }
            long t1 = System.nanoTime();
            c.alturaInsercao = arvore.altura();
            c.rotacoesInsercao = est.rotacoes();
            c.recoloracoesInsercao = est.recoloracoes();

            est.zerar();
            long t2 = System.nanoTime();
            for (int chave : buscas) {
                arvore.buscar(chave);
            }
            long compExistentes = est.comparacoes();
            for (int chave : inexistentes) {
                arvore.buscar(chave);
            }
            long t3 = System.nanoTime();
            c.comparacoesExistente = (double) compExistentes / buscas.length;
            c.comparacoesInexistente = (double) (est.comparacoes() - compExistentes) / inexistentes.length;

            // Encomendas mais recentes (últimos 10% a chegar): as mais rastreadas na prática
            est.zerar();
            int inicioRecentes = dados.length - dados.length / 10;
            for (int i = inicioRecentes; i < dados.length; i++) {
                arvore.buscar(dados[i]);
            }
            c.comparacoesRecentes = (double) est.comparacoes() / (dados.length - inicioRecentes);

            est.zerar();
            long t4 = System.nanoTime();
            for (int chave : remocoes) {
                arvore.remover(chave);
            }
            long t5 = System.nanoTime();
            c.rotacoesRemocao = est.rotacoes();
            c.recoloracoesRemocao = est.recoloracoes();
            c.alturaRemocao = arvore.altura();
            c.propriedadesOk = arvore.verificarPropriedades().isEmpty()
                    && arvore.tamanho() == dados.length - remocoes.length;

            if (r >= 0) {
                tIns[r] = (t1 - t0) / 1e6;
                tBus[r] = (t3 - t2) / 1e6;
                tRem[r] = (t5 - t4) / 1e6;
            }
        }
        c.tempoInsercaoMs = mediana(tIns);
        c.tempoBuscasMs = mediana(tBus);
        c.tempoRemocaoMs = mediana(tRem);
        return c;
    }

    // ================================================================ experimento 3

    private static final byte INSERIR = 0;
    private static final byte BUSCAR = 1;
    private static final byte REMOVER = 2;

    private record Carga(String nome, int pInsercao, int pBusca, int pRemocao, byte[] tipos, int[] chaves) {
    }

    private void experimentoCargasMistas() throws IOException {
        int operacoes = rapido ? 100_000 : 1_000_000;
        titulo("## 3. Cargas mistas simulando a Black Friday (" + inteiro(operacoes) + " operações)");
        linha("Novos pedidos recebem IDs sequenciais (crescentes). Buscas e remoções sorteiam um ID já emitido,");
        linha("que pode já ter sido removido (cobre busca/remoção de inexistentes). Mesma sequência nas duas árvores.");
        linha("");

        List<Carga> cargas = List.of(
                gerarCarga("Rastreamento (leitura intensiva)", 10, 85, 5, operacoes),
                gerarCarga("Operação equilibrada", 40, 40, 20, operacoes),
                gerarCarga("Pico de pedidos (escrita intensiva)", 45, 10, 45, operacoes));

        List<String[]> linhas = new ArrayList<>();
        StringBuilder csv = new StringBuilder("carga,insercao_pct,busca_pct,remocao_pct,arvore,operacoes,tamanho_final,"
                + "altura_final,rotacoes,recoloracoes,comparacoes_medias_busca,tempo_ms,propriedades_ok\n");

        for (Carga carga : cargas) {
            for (Supplier<ArvoreBalanceada> fabrica : ARVORES) {
                double[] tempos = new double[repeticoes];
                ArvoreBalanceada arvore = null;
                long buscas = 0;
                for (int r = -aquecimento; r < repeticoes; r++) {
                    arvore = fabrica.get();
                    buscas = 0;
                    long t0 = System.nanoTime();
                    for (int i = 0; i < carga.tipos.length; i++) {
                        int chave = carga.chaves[i];
                        switch (carga.tipos[i]) {
                            case INSERIR -> arvore.inserir(chave);
                            case BUSCAR -> {
                                arvore.buscar(chave);
                                buscas++;
                            }
                            default -> arvore.remover(chave);
                        }
                    }
                    long t1 = System.nanoTime();
                    if (r >= 0) {
                        tempos[r] = (t1 - t0) / 1e6;
                    }
                }
                Estatisticas est = arvore.estatisticas();
                double compMedias = buscas == 0 ? 0 : (double) est.comparacoes() / buscas;
                boolean ok = arvore.verificarPropriedades().isEmpty();
                String mix = carga.pInsercao + "/" + carga.pBusca + "/" + carga.pRemocao;
                linhas.add(new String[] {carga.nome, mix, arvore.nome(), inteiro(arvore.tamanho()),
                        String.valueOf(arvore.altura()), inteiro(est.rotacoes()), inteiro(est.recoloracoes()),
                        decimal(compMedias), decimal(mediana(tempos)), ok ? "sim" : "NÃO"});
                csv.append(String.format(Locale.ROOT, "\"%s\",%d,%d,%d,%s,%d,%d,%d,%d,%d,%.3f,%.3f,%b%n",
                        carga.nome, carga.pInsercao, carga.pBusca, carga.pRemocao, sigla(arvore.nome()),
                        carga.tipos.length, arvore.tamanho(), arvore.altura(), est.rotacoes(),
                        est.recoloracoes(), compMedias, mediana(tempos), ok));
            }
        }
        tabela(new String[] {"Carga", "% ins/busca/rem", "Árvore", "Tamanho final", "Altura final", "Rotações",
                "Recolorações", "Comp./busca", "Tempo total (ms)", "Propriedades OK"}, linhas);
        gravarCsv("cargas_mistas.csv", csv);
    }

    private static Carga gerarCarga(String nome, int pIns, int pBus, int pRem, int operacoes) {
        Random aleatorio = new Random(SEMENTE + pIns * 10_000L + pBus * 100L + pRem);
        byte[] tipos = new byte[operacoes];
        int[] chaves = new int[operacoes];
        int proximoId = 1;
        for (int i = 0; i < operacoes; i++) {
            int sorteio = aleatorio.nextInt(100);
            if (proximoId == 1 || sorteio < pIns) {
                tipos[i] = INSERIR;
                chaves[i] = proximoId++;
            } else {
                tipos[i] = sorteio < pIns + pBus ? BUSCAR : REMOVER;
                chaves[i] = 1 + aleatorio.nextInt(proximoId - 1);
            }
        }
        return new Carga(nome, pIns, pBus, pRem, tipos, chaves);
    }

    // ================================================================ utilitários

    static int[] gerarDados(Ordem ordem, int n) {
        int[] dados = new int[n];
        for (int i = 0; i < n; i++) {
            dados[i] = switch (ordem) {
                case CRESCENTE, ALEATORIA -> i + 1;
                case DECRESCENTE -> n - i;
            };
        }
        if (ordem == Ordem.ALEATORIA) {
            embaralhar(dados, new Random(SEMENTE + n));
        }
        return dados;
    }

    private static int[] embaralhar(int[] v, Random aleatorio) {
        for (int i = v.length - 1; i > 0; i--) {
            int j = aleatorio.nextInt(i + 1);
            int tmp = v[i];
            v[i] = v[j];
            v[j] = tmp;
        }
        return v;
    }

    private static ArvoreBalanceada construir(Supplier<ArvoreBalanceada> fabrica, int[] dados) {
        ArvoreBalanceada arvore = fabrica.get();
        for (int chave : dados) {
            arvore.inserir(chave);
        }
        return arvore;
    }

    private static double mediana(double[] valores) {
        double[] v = valores.clone();
        Arrays.sort(v);
        int m = v.length / 2;
        return v.length % 2 == 1 ? v[m] : (v[m - 1] + v[m]) / 2;
    }

    private static double log2(double x) {
        return Math.log(x) / Math.log(2);
    }

    private static String sigla(String nome) {
        return nome.contains("AVL") ? "AVL" : "RN";
    }

    private static String inteiro(long valor) {
        return String.format(BR, "%,d", valor);
    }

    private static String decimal(double valor) {
        return String.format(BR, "%.2f", valor);
    }

    private void titulo(String texto) {
        linha(texto);
        linha("");
    }

    private void linha(String texto) {
        out.println(texto);
        relatorio.append(texto).append('\n');
    }

    /** Imprime uma tabela em Markdown com colunas alinhadas (legível no terminal também). */
    private void tabela(String[] cabecalho, List<String[]> linhas) {
        int[] larguras = new int[cabecalho.length];
        for (int i = 0; i < cabecalho.length; i++) {
            larguras[i] = cabecalho[i].length();
            for (String[] l : linhas) {
                larguras[i] = Math.max(larguras[i], l[i].length());
            }
        }
        linha(formatarLinha(cabecalho, larguras));
        String[] separador = new String[cabecalho.length];
        for (int i = 0; i < separador.length; i++) {
            separador[i] = "-".repeat(larguras[i]);
        }
        linha(formatarLinha(separador, larguras));
        for (String[] l : linhas) {
            linha(formatarLinha(l, larguras));
        }
        linha("");
    }

    private static String formatarLinha(String[] celulas, int[] larguras) {
        StringBuilder sb = new StringBuilder("|");
        for (int i = 0; i < celulas.length; i++) {
            sb.append(' ').append(celulas[i]).append(" ".repeat(larguras[i] - celulas[i].length())).append(" |");
        }
        return sb.toString();
    }

    private void gravarCsv(String nomeArquivo, StringBuilder conteudo) throws IOException {
        if (diretorioSaida != null) {
            Files.createDirectories(diretorioSaida);
            Files.writeString(diretorioSaida.resolve(nomeArquivo), conteudo.toString(), StandardCharsets.UTF_8);
        }
    }
}
