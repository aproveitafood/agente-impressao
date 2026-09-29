# Instruções para trabalhar neste projeto

## Antes de alterar

1. Leia [`README.md`](README.md) e o índice em [`docs/README.md`](docs/README.md).
2. Leia as páginas de documentação diretamente relacionadas à tarefa.
3. Confirme no código, nos testes e na configuração como o comportamento funciona
   hoje. A documentação explica a intenção, mas não substitui a implementação.
4. Se documentação e código divergirem, trate o código e os testes como evidência
   do comportamento atual; corrija a documentação quando a mudança for
   intencional.

## Princípios do produto

- Este é o agente desktop de impressão do Aproveita Food, baseado no QZ Tray.
- Preserve os contratos usados pelo cliente JavaScript, o protocolo WebSocket,
  os fluxos de impressão, a validação de certificados e as permissões existentes.
  Mudanças nesses contratos ou controles de segurança exigem justificativa,
  testes e atualização da documentação.
- Preserve as ações existentes ao modernizar a interface. No menu da bandeja,
  mantenha as opções em uma única caixa; “Avançado” expande conteúdo no próprio
  painel, sem criar submenu flutuante de diagnóstico.
- Use as cores, fontes e arquivos de marca já adotados pelo frontend. Os tokens
  de referência estão em `../frontend/src/styles/tokens.css` e
  `../frontend/src/styles/fonts.css` quando esse checkout estiver disponível.
  O JAR usa os recursos versionados em `src/qz/ui/resources/`.
- Não mude caminhos/names de ícones consumidos por `IconCache.Icon` sem atualizar
  todas as referências e validar o JAR resultante.
- As janelas comuns (Sobre, Registros e Gerenciar sites) não devem bloquear o
  acesso à bandeja. Confirmações de ações sensíveis e solicitações de autorização
  continuam modais, salvo requisito explícito em contrário.

## Implementação

- Linguagem principal: Java. Siga o estilo do arquivo vizinho e use UTF-8.
- O build usa Apache Ant e Ivy; propriedades de compilação ficam em
  `ant/project.properties`.
- Mantenha alterações cirúrgicas. Não remova recursos, opções, verificações ou
  fluxos para simplificar a implementação sem autorização.
- Não inclua certificados privados, chaves, senhas, tokens, dados de usuário ou
  arquivos de configuração local.
- Não edite arquivos gerados em `out/` como fonte da mudança; altere `src/`,
  `test/` ou a configuração apropriada.
- Atualize `docs/` e o README quando comandos, interfaces, decisões ou
  comportamentos documentados mudarem.

## Validação

Use JDK 25 explicitamente neste ambiente:

```bash
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
ant clean build-jar
```

Para a suíte TestNG:

```bash
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
ant testng
```

Rode a validação mais específica que cubra a alteração; amplie para a suíte
completa quando o escopo ou o resultado exigir. Para alterações do agente
desktop, quando viável, execute o JAR novo e verifique o endpoint local em
`http://127.0.0.1:8182/json`. Para interface Swing, valide também a interação
visual afetada; compilação não prova posicionamento, foco, cores ou acessibilidade.
Informe claramente testes que não puderam ser executados e a razão.

## Segurança, compatibilidade e distribuição

- Não enfraqueça validação de assinatura/certificado, limites de origem,
  permissões de arquivo ou comportamento TLS como correção visual.
- Não altere silenciosamente portas, interface de bind, origem permitida,
  preferências, nomes de recursos ou versão do produto.
- Preserve `LICENSE.txt`, avisos de copyright e as licenças das dependências.
- Não presuma que o que funciona em Linux funciona em Windows ou macOS: os
  adaptadores de bandeja, certificados, impressão e instaladores variam por SO.
