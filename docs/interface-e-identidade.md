# Interface e identidade visual

## Referência visual

A interface desktop usa a identidade Aproveita Food adotada no frontend:

- paprika escuro / primário: `#BC361C`;
- paprika: `#D84A2B`;
- cocoa: `#4A2B1F`;
- creme: `#FFF3E7`;
- creme claro: `#FFFAF4`;
- erva: `#6E8B4A`, com variante de contraste `#5A7539`;
- fontes Inter (texto) e Sora (títulos).

Os tokens web de referência estão em `../frontend/src/styles/tokens.css` e as
fontes em `../frontend/src/styles/fonts.css`, quando o repositório irmão estiver
disponível. Os valores e arquivos usados no JAR são copiados/versionados em
`src/qz/ui/resources/`; a aplicação não deve depender de carregar a marca pela
rede.

## Recursos estáticos

- `aproveita-logo-horizontal.png`, `aproveita-logo-icon.png` e
  `aproveita-logo-light.png`: logos de interface.
- `fonts/inter.ttf` e `fonts/sora.ttf`: fontes empacotadas para Swing.
- `qz-*.png`: ícones com nomes ainda referenciados por `IconCache`.

Mantenha os caminhos do enum `IconCache.Icon` estáveis. Atualize imagens
substituindo os recursos correspondentes in place, ou altere o enum e todos os
consumidores na mesma mudança. O build copia recursos de `src/` para o JAR.

## Tema Swing

`qz.ui.ThemeUtilities.applyBrandTheme()` configura fontes, cores, estados de
seleção, controles e tabelas depois do Look & Feel do sistema/FlatLaf. A
interface adapta superfícies ao tema claro/escuro detectado, preservando as
cores de marca em destaque e foco.

Ao adicionar componentes:

- reutilize tokens existentes em vez de introduzir cores literais sem
  necessidade;
- garanta legibilidade e foco visível nos estados normal, hover, seleção e
  desabilitado;
- não dependa de ícones brancos sobre o hover vermelho; os ícones de ação usam
  cocoa para continuar distinguíveis;
- confira o resultado visual nos ambientes suportados, pois o tema Swing pode
  variar entre sistemas operacionais.

## Cores do menu da bandeja

O menu da bandeja é uma **superfície de marca**: ele não segue a preferência
`prefer-dark` do desktop, mesmo que o tema do sistema seja escuro. Essa regra
existe porque o menu carrega o logotipo e precisa continuar legível como parte
da identidade, e não como um diálogo do sistema. As demais janelas do agente
continuam seguindo o tema claro/escuro detectado.

| Papel | Token | Valor | Onde |
| --- | --- | --- | --- |
| Superfície do menu | `--brand-cream` | `#FFF3E7` | fundo do popup e do cabeçalho |
| Linha | `--card` | `#FFFFFF` | cada linha do menu, sobre o creme |
| Texto | `--brand-cocoa` | `#4A2B1F` | rótulos e ícones das linhas |
| Texto secundário | `--muted` | `#765F55` | número de versão |
| Borda | `--border` | `#E8D9CC` | contorno do popup |
| Hover / foco / seleção | `--primary` + `--primary-fg` | `#BC361C` + `#FFFFFF` | linha sob o cursor e foco de teclado |

As cores vivem em `qz.common.Constants` (`BRAND_CREAM_COLOR`,
`BRAND_CARD_COLOR`, `BRAND_COCOA_COLOR`, `BRAND_MUTED_COLOR`,
`BRAND_BORDER_COLOR`) e são aplicadas em `TrayManager.styleTrayPopup`,
`TrayManager.styleTrayRow` e `TaskbarTrayIcon`. Não reintroduza cores literais
nesses pontos.

Regras que evitam os defeitos visuais já corrigidos:

- **A linha é mais clara que a superfície.** Se as duas usarem a mesma cor, a
  linha fica indistinguível do painel e o clique parece não funcionar.
- **Não force `setOpaque(true)` nas linhas.** Isso descarta o arredondado e o
  anel de foco do Look & Feel. Deixe o componente opaco como o tema definir.
- **Não desligue `setFocusPainted`.** Toda linha precisa manter o foco visível
  por teclado.
- **Botão transparente precisa fixar a própria cor de hover.** Um `JButton` com
  `setContentAreaFilled(false)` não pinta fundo, mas o Look & Feel mesmo assim
  troca o texto para branco no hover. O resultado é texto branco sobre creme:
  some. Todo componente que precise preservar a cor da marca no hover chama
  `ThemeUtilities.applyBrandHover(componente, repousoTexto, repousoFundo,
  hoverTexto, hoverFundo)`; passe `null` em qualquer fundo para não tocá-lo.
- **O FlatLaf pinta o hover a partir do estilo dele, não do `setForeground`.**
  Por isso `applyBrandHover` faz duas coisas: grava o estilo do componente em
  `FlatClientProperties.STYLE` (`hoverForeground` e `focusedForeground`, na
  sintaxe `chave:#RRGGBB;chave:#RRGGBB`, separador `;`) e instala uma guarda de
  `MouseListener` que reaplica as cores. A tabela de marca também define
  `Button.hoverForeground` e `Button.focusedForeground`: sem esses defaults o
  FlatLaf usa o texto claro dele, que é a origem do defeito.
  Client property `JComponent.hoverForeground` não resolve, essa chave não
  existe no FlatLaf 3.7.2. `LinkLabel` chama a função sobrescrevendo
  `setForeground`, o que cobre também o `AboutDialog`, que põe o link em branco
  sobre a barra paprika. O botão de minimizar da bandeja usa a mesma guarda, e as
  linhas de `styleTrayRow` mantêm texto cocoa no hover, com tom quente de 12%
  (`#F7E7E4`) de fundo. `test/qz/ui/ThemeUtilitiesTests` fixa esse comportamento
  conferindo os pixels pintados, porque só a propriedade do cliente não
  protegeria a cor.
- **Quem abre e fecha o menu reaplica a mesma função de cor.** O
  `PopupMenuListener` não dispara nos modos MODERN e TASKBAR, então
  `TaskbarTrayIcon` chama `TrayManager.styleTrayRow` ao abrir e ao fechar.
  Não mantenha uma segunda tabela de cores em outro arquivo.

## Menu da bandeja e janelas

O menu principal apresenta logo e versão centralizadas. “Avançado” revela as
opções no mesmo painel; não use um submenu flutuante para Diagnóstico. Preserve
as ações existentes, atalhos, tooltips e estados de preferências.

No modo Linux sem bandeja nativa, o menu é uma janela do aplicativo: mover o
cursor ou clicar fora não deve ocultá-lo. Ele permanece aberto até a janela ser
minimizada pelo sistema ou o usuário acionar o botão “Minimizar menu”.

O menu fica ancorado no ícone da bandeja, e a posição é recalculada a partir
dessa âncora sempre que o conteúdo muda de tamanho, como ao expandir
“Avançado”. A aritmética fica em `TrayMenuPlacement`, sem AWT, para poder ser
testada; `TaskbarTrayIcon` aplica o resultado. Duas regras decorrem disso:

- A posição **nunca** vem de `MouseInfo.getPointerInfo()`. O ponteiro fica
  dentro do próprio menu quando o usuário clica numa linha, e ancorar nele faz
  a janela andar a cada clique.
- O menu é limitado pelos limites **da tela**, não pela área de trabalho. O
  ícone vive no painel da borda; limitar pela área de trabalho afastava o menu
  centenas de pixels do ícone sempre que o gerenciador de janelas reservava
  bordas que o menu não estava realmente atrás.

O cabeçalho traz logotipo e versão centralizados, com 8px de respiro entre os
dois, e o botão de minimizar usa um ícone da marca (`qz-minimize.png`) em vez de
glifo. Abaixo do ícone a lista cresce; se uma tela não comportar a altura, a
janela é limitada pela tela e não pela área de trabalho.

A ativação do ícone é filtrada por `TrayActivationGate`: o menu abre e fecha
apenas com um par pressionamento/soltura, ou com a soltura de fallback quando o
host da bandeja não entrega o pressionamento. Solturas avulsas, eventos de
outro ícone e eventos repetidos dentro da janela de *debounce* são ignorados,
porque alguns hosts do GNOME entregam uma soltura espúria quando o cursor sai do
ícone em direção ao menu. Ignore também um `MOUSE_RELEASED` cujo painel não seja
o `TrayIcon` do aplicativo, para que interações internas do menu não o fechem.

As janelas comuns de Sobre, Registros e Gerenciar sites são modeless para que
não bloqueiem a bandeja. Confirmações e decisões de autorização continuam
modais. Se alterar essa distinção, valide a interação com a bandeja e os fluxos
de confirmação.
