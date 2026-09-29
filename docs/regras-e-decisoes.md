# Regras, decisões e glossário

## Princípios do projeto

- **Compatibilidade primeiro:** o protocolo consumido pelo cliente JavaScript e
  as ações de impressão existentes são contratos. Mudanças precisam ser
  compatíveis ou ter migração explícita.
- **Segurança não é cosmética:** aparência, conveniência e testes locais não
  justificam relaxar certificados, assinaturas, permissões ou origens.
- **Marca local:** logos, fontes e ícones são recursos do aplicativo; não
  introduza carregamento remoto de assets em runtime.
- **UI sem perda funcional:** modernizações preservam ações, estado, atalhos,
  feedback e acessibilidade. O menu da bandeja mantém Avançado inline dentro do
  painel principal.
- **Documentação verificável:** documente comportamento comprovado no código e
  mantenha comandos e valores sincronizados com a implementação.
- **Compatibilidade multiplataforma:** bandeja, certificados, dispositivos,
  instaladores e impressão variam por plataforma; teste ou indique claramente
  o limite de validação.

## Decisões de interface atuais

- O menu principal da bandeja inclui logo e número de versão, separados por 8px
  de respiro, com o botão de minimizar usando um ícone da marca.
- Opções de diagnóstico são linhas do painel avançado inline, não uma janela de
  submenu flutuante.
- O estado hover/foco usa o vermelho primário; os ícones de ação usam o marrom
  cocoa para permanecer visíveis.
- O texto do hover nunca clareia: cada componente que preserva a cor da marca
  fixa a própria cor por `FlatLaf.style` e por uma guarda de `MouseListener`,
  porque o FlatLaf pinta o hover a partir do estilo dele, não do
  `setForeground`.
- **A bandeja é uma superfície de marca e não segue `prefer-dark`.** O menu é
  creme (`#FFF3E7`) com linhas brancas e texto cocoa, independentemente do tema
  do desktop, porque carrega o logotipo. As demais janelas continuam seguindo o
  tema claro/escuro. Implementado em `Constants`, `TrayManager.styleTrayPopup`,
  `TrayManager.styleTrayRow` e `TaskbarTrayIcon`; a tabela de cores está em
  [`interface-e-identidade.md`](interface-e-identidade.md).
- **A posição do menu é derivada da âncora do ícone, nunca do ponteiro.** Como o
  `pack()` de "Avançado" muda a altura, a posição é recalculada a cada mudança,
  senão a janela fica deslocada. A aritmética vive em `TrayMenuPlacement` e é
  limitada pelos limites da tela, não pela área de trabalho, porque o ícone
  fica no painel da borda.
- **Hover não pode apagar o texto.** Links das janelas, o minimizar da bandeja e as
  linhas do menu precisam de `ThemeUtilities.applyBrandHover`, senão o Look & Feel
  troca o texto para a cor clara dele e ele desaparece sobre o creme. A função
  grava `FlatClientProperties.STYLE` e instala a guarda de `MouseListener`; a
  tabela de marca define `Button.hoverForeground` e `Button.focusedForeground`
  para os demais botões. O texto do hover é sempre escuro: a cor da marca nos
  links e cocoa nas linhas, que ganham um tom quente de 12% (`#F7E7E4`) de
  fundo. Implementado em `ThemeUtilities.applyBrandHover`,
  `LinkLabel.setForeground`, `TrayManager.styleTrayRow` e no botão
  `tray-minimize`; coberto por `test/qz/ui/ThemeUtilitiesTests`, que confere os
  pixels pintados no hover nos dois Look & Feel do FlatLaf.
- **Uma regra só de cor de linha.** Linhas e popup não podem ter a mesma cor, e
  `setOpaque`/`setFocusPainted` não são desligados: o arredondado e o foco de
  teclado fazem parte do comportamento. Quem abre e fecha o menu reaplica
  `TrayManager.styleTrayRow`; não duplique a tabela de cores.
- Janelas de uso comum não bloqueiam a bandeja. Confirmação e autorização
  continuam modais.

Confirme a implementação em `TrayManager`, `TaskbarTrayIcon`,
`TrayMenuPlacement`, `BasicDialog` e `ThemeUtilities` antes de tratar estes
itens como comportamento publicado.

## Glossário

| Termo | Definição |
| --- | --- |
| Agente | Este aplicativo desktop Java, que expõe a ponte local para impressão |
| Cliente | Biblioteca/aplicação JavaScript que conversa com o agente |
| WSS / WS | WebSocket sobre TLS / WebSocket sem TLS |
| Gateway | Fluxo visual para aprovar ou bloquear uma solicitação de recurso local |
| Provisionamento | Aplicação de propriedades, certificados ou recursos definidos para uma instalação |
| Tray | Área de indicadores/bandeja do sistema operacional |
| Headless | Execução sem UI interativa ou sem ambiente gráfico compatível |
| Recurso | Arquivo estático empacotado no JAR, como ícone, fonte ou configuração |

## Quando revisar esta página

Atualize decisões e definições quando mudar a integração, segurança, experiência
da bandeja ou significado de uma preferência. Registre o motivo e os arquivos
que implementam a decisão para que a documentação não se torne uma promessa
desconectada do produto.
