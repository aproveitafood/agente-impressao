# Impressão e integração

## Papel do agente

O agente disponibiliza uma ponte local para que um cliente autorizado solicite
operações com impressoras e dispositivos acessíveis ao computador. A API é
consumida por cliente JavaScript compatível com o protocolo WebSocket do agente; o exemplo
versionado em `js/README.md` ilustra conexão, descoberta de impressoras e envio
de um trabalho.

O frontend/backend de produção não estão neste repositório. Antes de mudar o
formato de mensagens, autenticação, descoberta ou resposta, verifique também o
cliente e o backend consumidores.

## Formatos e caminhos de processamento

`qz.printer.action.ProcessorFactory` cria processadores para as famílias
`HTML`, `IMAGE`, `PDF`, `DIRECT` e `COMMAND`. As implementações e conversores
estão em `src/qz/printer/action/`, incluindo conversores de linguagens e
comandos de impressão. A enumeração de formatos, validações e estruturas de
payload devem ser lidas no código antes de modificar a integração.

O projeto também inclui comunicação com recursos locais, como arquivo, USB/HID,
serial e sockets. A disponibilidade real é condicionada pelo sistema
operacional, pelos drivers, pelas permissões e pelas preferências de segurança.

## Fluxo seguro

- Solicitações de recursos locais podem apresentar uma janela de autorização.
- Certificados salvos e decisões de bloqueio/autorização influenciam solicitações
  futuras.
- As mensagens assinadas e o estado do certificado são avaliados em `qz.auth`.
- Não trate qualquer conexão WebSocket bem-sucedida como autorização para
  imprimir ou acessar arquivos/dispositivos.
- Preserve os mecanismos de aprovação, assinatura e restrição de origem ao
  alterar UI ou API.

Consulte `src/qz/ui/GatewayDialog.java`, `src/qz/auth/Request.java`,
`src/qz/auth/Certificate.java` e `src/qz/ws/PrintSocketClient.java` para
confirmar o fluxo atual.

## Teste manual local

1. Faça build com `ant clean build-jar`.
2. Inicie o JAR e aguarde a mensagem de inicialização do servidor.
3. Verifique `http://127.0.0.1:8182/json` se a porta padrão não foi alterada.
4. Conecte o cliente JavaScript de teste usando o protocolo e as configurações
   de certificado deste ambiente.
5. Teste descoberta e impressão somente em impressora/dispositivo de teste.

Não envie trabalhos reais nem dados pessoais durante testes sem autorização.
