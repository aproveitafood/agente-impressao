# Operação e diagnóstico

## Inicialização local

Compile e execute:

```bash
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
ant clean build-jar
java -jar out/dist/qz-tray.jar
```

O processo permanece ativo em segundo plano e publica HTTP/WebSocket local.
Confirme a porta efetivamente informada no log; a porta pode mudar se a
configuração ou outro processo ocupar a padrão.

## Bandeja Linux

O suporte depende da sessão gráfica e do ambiente desktop. Uma sessão sem
extensão de bandeja/indicator pode ocultar o ícone mesmo com o serviço ativo.
Para diagnosticar:

1. confirme que o processo Java está ativo;
2. consulte `http://127.0.0.1:8182/json` (ou a porta configurada);
3. confira se a sessão oferece suporte à bandeja e se o ícone está na área de
   indicadores ocultos;
4. leia o log antes de alterar configurações do desktop;
5. teste o menu principal e a expansão de Avançado, não apenas a presença do
   processo.

Não encerre processos por nome de forma indiscriminada: identifique o PID da
instância deste checkout.

## Logs

`qz.App.setupFileLogging()` configura rotação e nível de arquivo. O diretório do
usuário contém o arquivo `debug.log` e rotações de acordo com as preferências;
`FileUtilities` e `ArgValue` definem os caminhos e limites atuais.

O menu oferece visualização de registros e criação de arquivo compactado para
diagnóstico. Logs podem conter nomes de impressoras, caminhos e dados de
solicitação: revise e remova informações sensíveis antes de compartilhá-los.

A bandeja registra o ciclo de vida do menu em Log4j2. As mensagens úteis para
diagnosticar o comportamento da janela são:

| Mensagem | Origem | Significado |
| --- | --- | --- |
| `Tray icon activated; opening/closing the menu` | `ModernTrayIcon` | Ativação aceita; o menu abriu ou fechou |
| `Ignoring tray <evento> at <ponto> (menu <estado>)` | `ModernTrayIcon` | Evento filtrado por `TrayActivationGate`; o menu não mudou |
| `Ignoring tray release from another icon` | `ModernTrayIcon` | Soltura cujo painel não é o `TrayIcon` do aplicativo |
| `Menu shown, anchored to <ponto>` | `TaskbarTrayIcon` | Janela posicionada e ancorada nesse ponto |
| `Menu minimized to the taskbar on the user's request` | `TaskbarTrayIcon` | Minimização explícita; o guard de iconify aceita |
| `Menu window was minimized unexpectedly; restoring it` | `TaskbarTrayIcon` | Alerta: o WM minimizou a janela sem pedido; ela é restaurada |

Se o menu mudar de lugar a cada clique ou ao expandir "Avançado", confirme que
`updateMenuSize()` recalcula a posição a partir da âncora e que nada a
reposiciona pelo `MouseInfo.getPointerInfo()`. A posição esperada é a retornada
por `TrayMenuPlacement.topLeft`, com 4px de folga do ícone.

## Problemas comuns

| Sintoma | Verificações |
| --- | --- |
| O processo inicia, mas o site não conecta | Porta configurada/ocupada, WSS e certificado, origem permitida, firewall local e log |
| O endpoint local não responde | Processo, porta ativa, modo headless e falha na inicialização Jetty |
| O ícone não aparece no Linux | Sessão gráfica, suporte a tray/indicator e área de ícones ocultos |
| O menu some sozinho ao passar o mouse | Build novo, filtro de ativação da bandeja e ausência de minimização inesperada no log |
| O menu muda de lugar ao clicar em uma linha | Âncora do ícone preservada durante o `pack()` e logs `Menu shown, anchored to` |
| Impressora não aparece | Driver, CUPS/serviço nativo, permissões e logs de descoberta |
| Impressão falha no Linux com `Cannot run program "/usr/bin/lpr"` | Pacote `cups-bsd` ausente. O `.deb` o declara como dependência; com o `.run`, instale com `sudo apt install cups-bsd`. O instalador avisa no log: `"/usr/bin/lpr" wasn't found` |
| O diálogo de autorização não aparece | Modo headless, endpoint configurado para diálogos e estado do certificado |
| Fontes/logos não aparecem após a alteração | Build novo, recurso presente no JAR e caminho de recurso correto |

`certutil` ausente no Linux pode impedir a instalação do certificado em alguns
armazenamentos NSS/navegadores. Isso não substitui a validação da conexão WSS:
verifique o log e a configuração do navegador antes de concluir que o serviço
está funcional.
