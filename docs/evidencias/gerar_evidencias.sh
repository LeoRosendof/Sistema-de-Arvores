#!/usr/bin/env sh
# Regenera os registros de execução usados na apresentação.
# Uso (na raiz do projeto, após ./build.sh):  sh docs/evidencias/gerar_evidencias.sh
set -e
cd "$(dirname "$0")/../.."
JAR=dist/arvores-balanceadas.jar
DIR=docs/evidencias
RUN="java -Dstdout.encoding=UTF-8 -jar $JAR"

$RUN --avl < $DIR/entrada_demo.txt > $DIR/demo_avl.txt
$RUN --rn  < $DIR/entrada_demo.txt > $DIR/demo_rubro_negra.txt
$RUN --avl < $DIR/entrada_comparacao.txt > $DIR/demo_comparacao.txt
$RUN --testes > $DIR/testes.txt
$RUN --benchmark > $DIR/benchmark.txt

echo "Evidências geradas em $DIR"
