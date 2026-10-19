package app;

import analise.Benchmark;
import testes.TestesArvores;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Ponto de entrada do protótipo.
 *
 * <pre>
 *   java -jar arvores-balanceadas.jar                 menu interativo
 *   java -jar arvores-balanceadas.jar --avl           menu já com a Árvore AVL
 *   java -jar arvores-balanceadas.jar --rn            menu já com a Árvore Rubro-Negra
 *   java -jar arvores-balanceadas.jar --testes        bateria de testes automatizados
 *   java -jar arvores-balanceadas.jar --benchmark     análise comparativa completa
 *   java -jar arvores-balanceadas.jar --benchmark --rapido
 *   opções extras: --cores / --sem-cores (cores ANSI na Rubro-Negra)
 * </pre>
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        List<String> opcoes = Arrays.asList(args);

        if (opcoes.contains("--ajuda") || opcoes.contains("-h") || opcoes.contains("--help")) {
            imprimirAjuda();
            return;
        }
        if (opcoes.contains("--testes")) {
            System.exit(TestesArvores.executar() ? 0 : 1);
        }
        if (opcoes.contains("--benchmark")) {
            new Benchmark(opcoes.contains("--rapido"), System.out, Path.of("resultados")).executar();
            return;
        }

        boolean cores = opcoes.contains("--cores")
                || (!opcoes.contains("--sem-cores") && terminalSuportaAnsi());
        String estrutura = opcoes.contains("--avl") ? "avl" : opcoes.contains("--rn") ? "rn" : null;
        // Sem console (entrada redirecionada de arquivo), o menu repete o que leu para o registro ficar legível.
        new MenuInterativo(System.in, System.out, cores, System.console() == null).iniciar(estrutura);
    }

    /** Windows Terminal, terminais Unix e o terminal do VS Code interpretam cores ANSI. */
    private static boolean terminalSuportaAnsi() {
        return System.console() != null
                && (System.getenv("WT_SESSION") != null
                || System.getenv("TERM_PROGRAM") != null
                || (System.getenv("TERM") != null && !System.getProperty("os.name").startsWith("Windows")));
    }

    private static void imprimirAjuda() {
        System.out.println("""
                Uso: java -jar arvores-balanceadas.jar [opções]

                  (sem opções)        abre o menu interativo e pergunta qual árvore usar
                  --avl               abre o menu já usando a Árvore AVL
                  --rn                abre o menu já usando a Árvore Rubro-Negra
                  --testes            executa a bateria de testes automatizados
                  --benchmark         executa a análise comparativa (gera a pasta resultados/)
                  --rapido            com --benchmark: versão reduzida, termina em segundos
                  --cores             força cores ANSI (nós vermelhos em vermelho)
                  --sem-cores         desativa cores ANSI
                  --ajuda             mostra esta mensagem
                """);
    }
}
