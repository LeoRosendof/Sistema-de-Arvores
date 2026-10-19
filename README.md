# Amazon Engineering Challenge: Árvores Balanceadas (AVL e Rubro-Negra)

Protótipo de indexação de encomendas para o sistema de logística da Black Friday.
O sistema registra, localiza e remove IDs de encomendas usando duas estruturas
implementadas do zero: **Árvore AVL** e **Árvore Rubro-Negra**. Ele também traz testes
automatizados e uma análise comparativa entre as duas.

| | |
|---|---|
| **Autor** | Leonardo Rosendo Faustino |
| **Matrícula** | 20250027331 |
| **Disciplina** | IMD0039 - Estruturas de Dados Básicas II (UFRN/IMD) |
| **Trabalho** | Unidade II: Amazon Engineering Challenge |
| **Linguagem** | Java 17+ (sem dependências externas) |
| **Repositório** | https://github.com/LeoRosendof/Sistema-de-Arvores |

---

## 1. Funcionalidades

No início da execução, o usuário escolhe qual árvore quer usar: AVL ou Rubro-Negra.
As duas oferecem as mesmas operações:

| Operação | Descrição |
|---|---|
| Inserção | Um ou vários IDs de uma vez (`10 20 30` ou `10,20,30`) |
| Remoção | Funciona para nó folha, nó com um filho e nó com dois filhos (usa o sucessor em ordem) |
| Busca | Informa se o ID existe, quantas comparações foram feitas, quantos filhos o nó tem e, na Rubro-Negra, a cor dele |
| Percurso em ordem | Lista os IDs em ordem crescente |
| Visualização | Desenha a árvore deitada (raiz à esquerda). A AVL mostra o fator de balanceamento; a Rubro-Negra mostra a cor de cada nó (`[P]` preto, `[V]` vermelho) e, em terminais compatíveis, pinta os nós vermelhos |
| Modo detalhado | Descreve passo a passo cada caso de balanceamento, rotação e recoloração, e redesenha a árvore após cada operação |
| Estatísticas | Altura, altura mínima possível, limite teórico, altura negra, rotações, recolorações e **verificação de todas as propriedades** |
| Carga de sequências | Insere IDs em ordem crescente, decrescente ou aleatória (semente fixa) |
| Comparação | Executa a mesma sequência nas duas árvores e mostra o resultado lado a lado |
| Trocar estrutura | Alterna entre AVL e Rubro-Negra, com opção de transferir os IDs |

Os balanceamentos foram todos implementados pelo autor, sem bibliotecas prontas. O
`java.util.TreeSet` aparece apenas nos testes, como referência para conferir os resultados.

## 2. Requisitos

- **Java 17 ou superior.** Para executar o `.jar` basta o JRE; para compilar é preciso o JDK.
  Verifique com `java -version`.
- Nenhuma biblioteca externa.

## 3. Como executar

A versão compilada já está no repositório, em [`dist/arvores-balanceadas.jar`](dist/arvores-balanceadas.jar):

```bash
java -jar dist/arvores-balanceadas.jar              # menu interativo (pergunta qual árvore usar)
java -jar dist/arvores-balanceadas.jar --avl        # abre direto com a Árvore AVL
java -jar dist/arvores-balanceadas.jar --rn         # abre direto com a Árvore Rubro-Negra
java -jar dist/arvores-balanceadas.jar --testes     # bateria de testes automatizados
java -jar dist/arvores-balanceadas.jar --benchmark  # análise comparativa (cerca de 30 s)
java -jar dist/arvores-balanceadas.jar --benchmark --rapido   # versão reduzida
java -jar dist/arvores-balanceadas.jar --ajuda
```

> **Acentos no Windows:** se os acentos aparecerem trocados, use o Windows Terminal ou
> execute `chcp 65001` antes. Outra opção é rodar com `java -Dstdout.encoding=UTF-8 -jar ...`.

### Compilar a partir do código-fonte

```bash
./build.sh        # Linux / macOS / Git Bash
build.bat         # Windows (cmd ou PowerShell)
```

Os scripts compilam `src/` e geram novamente `dist/arvores-balanceadas.jar`.

## 4. Exemplo de uso

Trecho real da execução com a Rubro-Negra no modo detalhado (registro completo em
[`docs/evidencias/demo_rubro_negra.txt`](docs/evidencias/demo_rubro_negra.txt)):

```text
Opção: 1
ID(s) a inserir: 10 20 30 40 50 25
...
ID 40 inserido.
  > Inserção caso 1 (tio 10 vermelho): pai 30 e tio ficam pretos, avô 20 fica vermelho
  > Raiz 20 recolorida para preto
        /-- 40 [V]
    /-- 30 [P]
    20 [P]
    \-- 10 [P]
ID 50 inserido.
  > Inserção caso 3 (tio preto, linha): pai 40 fica preto, avô 30 fica vermelho, rotação no avô
  >   rotação à esquerda em 30: 40 sobe
        /-- 50 [V]
    /-- 40 [P]
    |   \-- 30 [V]
    20 [P]
    \-- 10 [P]
```

Mesma sequência na AVL ([`docs/evidencias/demo_avl.txt`](docs/evidencias/demo_avl.txt)):

```text
ID 25 inserido.
  > Nó 20 com FB=-2 e filho direito 40 com FB=+1: caso Direita-Esquerda (rotação dupla)
  >   rotação à direita em 40: 30 sobe e vira raiz da subárvore
  >   rotação à esquerda em 20: 30 sobe e vira raiz da subárvore
        /-- 50 (fb=0)
    /-- 40 (fb=-1)
    30 (fb=0)
    |   /-- 25 (fb=0)
    \-- 20 (fb=0)
        \-- 10 (fb=0)
```

Na visualização, a raiz fica à esquerda, os filhos à direita aparecem acima do pai (`/--`)
e os filhos à esquerda aparecem abaixo (`\--`).

## 5. Como funciona

### Árvore AVL ([`src/arvores/ArvoreAVL.java`](src/arvores/ArvoreAVL.java))
- Cada nó guarda a própria altura. O fator de balanceamento é `FB = h(esq) - h(dir)` e precisa
  ficar entre -1 e +1.
- A inserção e a remoção são recursivas. Na volta da recursão, cada ancestral tem a altura
  atualizada e, se `|FB| = 2`, é corrigido por um destes quatro casos:
  - **EE:** rotação simples à direita;
  - **DD:** rotação simples à esquerda;
  - **ED:** rotação dupla, esquerda e depois direita;
  - **DE:** rotação dupla, direita e depois esquerda.
- Uma inserção precisa de no máximo uma correção (simples ou dupla). Uma remoção pode
  precisar de uma correção por nível.

### Árvore Rubro-Negra ([`src/arvores/ArvoreRubroNegra.java`](src/arvores/ArvoreRubroNegra.java))
- Segue Cormen et al. (cap. 13): usa um nó sentinela `NIL` preto e ponteiro para o pai.
- Propriedades mantidas:
  - a raiz é preta;
  - um nó vermelho não tem filho vermelho;
  - todos os caminhos até as folhas têm o mesmo número de nós pretos (altura negra).
- Inserção: o nó novo entra vermelho e a correção trata três casos:
  - **tio vermelho:** apenas recolore;
  - **triângulo:** faz uma rotação para cair no caso "linha";
  - **linha:** recolore e faz uma rotação.
- Remoção: quando sai um nó preto, a correção trata quatro casos conforme a cor do irmão e
  dos sobrinhos.
- Custo das correções: no máximo 2 rotações por inserção e 3 por remoção. O resto do ajuste
  é feito com recolorações.

### Decisões de implementação
| Situação | Decisão |
|---|---|
| Inserir ID repetido | Rejeita com aviso. A árvore fica inalterada, porque o ID de uma encomenda é único |
| Remover ID inexistente | Avisa e não altera a árvore |
| Buscar ID inexistente | Informa que não encontrou e quantas comparações fez |
| Remover nó com dois filhos | Usa o sucessor em ordem (o menor da subárvore direita) |
| Operar na árvore vazia | Nenhuma operação falha: buscar e remover devolvem "não encontrado" |
| Entrada inválida no menu | O valor é ignorado com aviso e o programa continua |
| Medida de altura | Em níveis: árvore vazia = 0, um nó = 1 |
| Contagem de rotações | Uma rotação dupla conta como 2 rotações simples, nas duas árvores |
| Contagem de recolorações | Conta apenas os nós que realmente mudaram de cor |

### Verificação das propriedades
As duas árvores implementam `verificarPropriedades()`, que percorre a estrutura inteira e
devolve a lista de violações encontradas:

- **Nas duas árvores:** ordem de busca (com limites mínimo e máximo por subárvore) e
  tamanho registrado.
- **Na AVL:** altura armazenada e `|FB| <= 1` em cada nó.
- **Na Rubro-Negra:** raiz preta, ausência de vermelho com filho vermelho, mesma altura negra
  dos dois lados de cada nó e ponteiros de pai consistentes.

Os testes chamam essa verificação **após cada inserção e remoção**.

## 6. Testes

`java -jar dist/arvores-balanceadas.jar --testes` executa 37 testes. Cada cenário roda nas
duas árvores, com os mesmos dados:

- operações em árvore vazia e em árvore com um único elemento;
- inserção em ordem crescente, decrescente e aleatória (2.000 IDs), conferindo as
  propriedades a cada inserção;
- rejeição de valores repetidos, sem nenhuma alteração na estrutura;
- busca de IDs existentes e inexistentes, incluindo valores extremos;
- remoção de folha, de nó com um filho e de nó com dois filhos (inclusive a raiz);
- remoção sucessiva de nós de cada grau (0, 1 e 2 filhos) até esvaziar a árvore;
- remoção de inexistentes e remoção do mesmo ID duas vezes;
- esvaziamento completo em três ordens diferentes;
- rotação simples (1, 2, 3) e rotação dupla (30, 10, 20), com o número exato de rotações;
- altura dentro do limite teórico com 100.000 IDs;
- 30.000 operações aleatórias comparadas com o `java.util.TreeSet`;
- cores esperadas na Rubro-Negra, incluindo o caso de recoloração sem rotação.

Resultado atual: **37 de 37 testes aprovados**
([`docs/evidencias/testes.txt`](docs/evidencias/testes.txt)).

## 7. Análise comparativa (resumo)

`--benchmark` gera [`resultados/resultados.md`](resultados/resultados.md) e os arquivos CSV.
Todos os dados usam semente fixa e são aplicados de forma idêntica às duas árvores.
Os principais números, com 100.000 IDs:

| Cenário (n = 100.000) | AVL | Rubro-Negra |
|---|---|---|
| Altura com IDs sequenciais (mínima possível = 17) | **17** | 31 |
| Altura com IDs aleatórios | 20 | 20 |
| Rotações na inserção sequencial | 99.983 | 99.969 |
| Rotações na inserção aleatória | 69.961 | **58.506** |
| Recolorações na inserção sequencial | 0 | 499.846 |
| Comparações por busca dos 10% IDs mais recentes (sequencial) | **15,3** | 19,8 |
| Comparações por busca de ID ainda não emitido (sequencial) | **17** | 31 |

**Recomendação para a Black Friday: Árvore AVL.** Os IDs de encomenda chegam em ordem
crescente, e esse é o pior caso de forma para a Rubro-Negra: ela fica 82% mais alta que a
AVL. Nesse cenário a vantagem da Rubro-Negra em rotações desaparece, porque as duas fazem
cerca de uma rotação por inserção. A AVL, por sua vez, mantém a altura mínima possível e
faz menos comparações exatamente nas encomendas mais rastreadas, as recentes.

A Rubro-Negra continua sendo a melhor escolha quando as chaves chegam em ordem aleatória e
predominam as escritas. Nesse caso ela fez 16% menos rotações, e a inserção medida foi
consistentemente mais rápida.

A discussão completa está na apresentação: [`docs/apresentacao.pdf`](docs/apresentacao.pdf) (fonte editável em [`docs/apresentacao.pptx`](docs/apresentacao.pptx)).

## 8. Estrutura do repositório

```text
src/
  arvores/   ArvoreBalanceada (interface), ArvoreAVL, ArvoreRubroNegra, Estatisticas, ArvoreBase
  app/       Main (opções de linha de comando) e MenuInterativo
  testes/    TestesArvores (bateria de testes sem dependências)
  analise/   Benchmark (análise comparativa) e ArvoreBuscaBinaria (ABB sem balanceamento, linha de base)
dist/        arvores-balanceadas.jar (versão compilada)
resultados/  relatório e CSVs gerados pelo benchmark
docs/        apresentação em PDF, gráficos e registros de execução (evidencias/)
build.sh / build.bat   scripts de compilação
```

## 9. Referências

- CORMEN, T. H.; LEISERSON, C. E.; RIVEST, R. L.; STEIN, C. *Algoritmos: Teoria e Prática*.
  3. ed. Rio de Janeiro: Elsevier, 2012. Cap. 12 e 13.
- ADELSON-VELSKY, G. M.; LANDIS, E. M. An algorithm for the organization of information.
  *Soviet Mathematics Doklady*, v. 3, p. 1259-1263, 1962.
- GUIBAS, L. J.; SEDGEWICK, R. A dichromatic framework for balanced trees. *Proc. 19th
  Annual Symposium on Foundations of Computer Science*, IEEE, 1978, p. 8-21.
- KNUTH, D. E. *The Art of Computer Programming, Vol. 3: Sorting and Searching*. 2. ed.
  Addison-Wesley, 1998. Seção 6.2.3.
- SEDGEWICK, R.; WAYNE, K. *Algorithms*. 4. ed. Addison-Wesley, 2011. Seção 3.3.
- SZWARCFITER, J. L.; MARKENZON, L. *Estruturas de Dados e seus Algoritmos*. 3. ed. LTC, 2010.
