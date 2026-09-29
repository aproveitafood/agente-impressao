# Arquitetura

## Visão geral

O aplicativo é um processo Java desktop. Ele configura o ambiente e as
preferências, prepara certificados, inicia um servidor Jetty com HTTP(S) e
WebSocket, e cria a interface de bandeja quando o ambiente gráfico permite.
As chamadas recebidas podem solicitar impressão, acesso a dispositivos locais
ou outras operações suportadas pelo protocolo.

## Inicialização e serviço local

1. `qz.App` interpreta argumentos, inicializa logging e ambiente, carrega
   certificados e preferências e executa as etapas de provisionamento de
   inicialização.
2. `qz.ws.PrintSocketServer` configura interfaces, origens e portas, inicia os
   conectores seguros/inseguros disponíveis e cria o `TrayManager`.
3. O servlet `qz.ws.HttpAboutServlet` responde em `/` e `/json`. O mesmo contexto
   registra o upgrade WebSocket usado pelo cliente.
4. `qz.ws.PrintSocketClient` e as classes de protocolo recebem e despacham
   mensagens; `qz.auth` avalia certificados, assinaturas, validade e confiança.
5. Chamadas de impressão são encaminhadas por `qz.printer.action.ProcessorFactory`
   para o processador apropriado.

Confirme detalhes de rotas e inicialização em `src/qz/App.java`,
`src/qz/ws/PrintSocketServer.java` e `src/qz/ws/PrintSocketClient.java`.

## Módulos do código

| Pacote | Responsabilidade principal |
| --- | --- |
| `qz.auth` | Certificados, confiança, validade e estado de solicitações |
| `qz.common` | Constantes, preferências comuns e gerenciamento da bandeja |
| `qz.communication` | Acesso a arquivo, USB/HID, serial e sockets |
| `qz.installer` | Integração com o SO, certificados, políticas, atalhos e instalação |
| `qz.installer.provision` | Aplicação de recursos e configuração provisionados |
| `qz.printer` | Descoberta de impressoras, processadores de impressão e status |
| `qz.ui` | Diálogos, tabelas, componentes e tema Swing |
| `qz.ui.tray` | Implementações da bandeja por ambiente/desktop |
| `qz.utils` | Preferências, sistema operacional, arquivos e utilidades transversais |
| `qz.ws` | Servidor HTTP/WebSocket, clientes, portas e substituições JSON |
| `qz.build` | Provisionamento e criação/empacotamento do runtime |

O diretório `test/` espelha áreas de teste como utilidades, instaladores,
provisionamento, impressão e WebSocket.

## Limites da arquitetura

- O serviço é local ao processo, mas o endereço/interface de bind é configurável;
  não presuma que ele só escuta em loopback.
- A interface pode operar sem bandeja em modo headless ou quando o sistema não
  oferece uma bandeja compatível.
- Descoberta, status e acesso a dispositivos dependem do SO, drivers e permissões.
- Frontend e backend ficam fora deste repositório. Verifique seus contratos nos
  repositórios correspondentes antes de alterar mensagens ou fluxo de conexão.
