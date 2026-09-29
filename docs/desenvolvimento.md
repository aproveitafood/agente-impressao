# Desenvolvimento, build e testes

## Ferramentas

- JDK 25, conforme `ant/project.properties` (`javac.source` e `javac.target`).
- Apache Ant.
- Ivy é carregado pelo build; na primeira execução, dependências podem ser
  baixadas e exigem acesso à internet.
- Recursos e bibliotecas nativas podem variar por sistema operacional e
  arquitetura.

Neste ambiente Linux, selecione o JDK explicitamente. `JAVA_HOME` pode apontar
para outra versão mesmo quando `javac` no `PATH` parece correto:

```bash
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
```

## Build local

Build incremental:

```bash
ant build-jar
```

Limpeza dos artefatos em `out/` e build limpo:

```bash
ant clean build-jar
```

O JAR executável é gerado em `out/dist/agente-impressao.jar`. Execute-o com:

```bash
java -jar out/dist/agente-impressao.jar
```

O processo deve continuar ativo para manter os serviços WebSocket disponíveis.
O comando `ant clean` remove `out/`; não coloque alterações manuais ou arquivos
persistentes nesse diretório.

## Testes

Compile e rode a suíte TestNG:

```bash
ant testng
```

O alvo agrega `unit-tests` e `integration-tests`. Para filtrar classes, o build
aceita a propriedade `testng.pattern`, por exemplo:

```bash
ant -Dtestng.pattern="**/utils/**" unit-tests
```

Testes de integração podem requerer navegadores, ambiente gráfico, serviços do
SO ou hardware. Os jobs do CI também fazem testes e empacotamento por plataforma;
consulte `.github/workflows/build.yaml` e `build.xml` para requisitos atuais.
Registre o que foi executado e não descreva uma compilação como validação de
comportamento de hardware ou de UI.

## Principais alvos Ant

| Alvo | Uso |
| --- | --- |
| `clean` | Remove `out/` |
| `build-jar` | Compila fontes, copia recursos/dependências e gera o JAR |
| `testng` | Executa testes unitários e de integração |
| `distribute` | Monta os artefatos e recursos necessários à distribuição |
| `makeself`, `nsis`, `pkgbuild`, `dmg` | Empacotadores específicos de SO |

Revise os alvos reais em `build.xml` antes de introduzir ou documentar comandos
novos.
