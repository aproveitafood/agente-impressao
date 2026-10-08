# Configuração e segurança

## Portas e interfaces

As portas padrão estão em `qz.common.Constants`:

| Transporte | Portas padrão |
| --- | --- |
| WebSocket seguro (`wss`) | `8181`, `8282`, `8383`, `8484` |
| WebSocket não seguro (`ws`) | `8182`, `8283`, `8384`, `8485` |

`qz.ws.WebsocketPorts` lê `websocket.secure.ports` e
`websocket.insecure.ports`; listas inválidas ou incompatíveis podem levar ao
fallback definido no código. O caminho HTTP `/json` compartilha o contexto do
servidor.

`security.wss.host` influencia o endereço/interface de bind; o valor padrão no
código é `0.0.0.0`. Isso pode disponibilizar o serviço em interfaces além do
loopback. Para uso estritamente local, configure e verifique a interface
apropriada no `qz-tray.properties` e no próprio processo. A origem permitida
também é configurável por `security.wss.alloworigin`; CORS/origem não substitui
firewall nem autenticação. Revise `security.wss.httpsonly` e
`security.wss.snistrict` antes de alterar a política de transporte.

## Certificados, assinatura e autorização

- `qz.App` inicializa o gerenciador de certificados antes de subir o servidor.
- `qz.installer.certificate` contém geração, armazenamento e integração de
  certificados com os navegadores/sistemas suportados.
- Solicitações e certificados são avaliados por `qz.auth`; solicitações sem
  confiança podem exigir aprovação explícita.
- Preferências como `authcert.override`, `tray.strictmode` e as opções de
  arquivos/substituições impactam a confiança e devem ser tratadas como
  controles de segurança, não como ajustes visuais.
- Nunca inclua chaves privadas, certificados de produção, senhas ou tokens neste
  repositório ou em documentação de exemplo.

### Certificado Aproveita embutido nos instaladores

As versões publicadas embutem o certificado **público** de assinatura do
Aproveita (o mesmo servido pelo backend em `GET /printers/qz/cert`) como
`override.crt`, ao lado do JAR. O agente o trata como CA confiável
(`authcert.override` tem o mesmo efeito por preferência) e, por ser igual a uma CA
confiável, o grava como permitido na primeira avaliação: o diálogo **Autorizar**
não aparece para as solicitações assinadas pelo backend.

A chave privada correspondente existe só no backend (`QZ_PRIVATE_KEY`). Quem a
possuir consegue imprimir em qualquer agente instalado: trate-a como segredo e
troque o par se houver suspeita de vazamento (publicar nova versão do agente com o
certificado novo). `tray.strictmode` e a validação de assinatura não mudam.

Como conferir uma instalação: o arquivo `override.crt` existe na pasta do agente
(no Linux, `/opt/agente-impressao/override.crt`) e o log registra
`Adding CA certificate: CN=Aproveita Food QZ Signing, O=Aproveita Food`.
Detalhes do build em [Desenvolvimento](desenvolvimento.md).

## Arquivos de usuário e preferências

O nome do diretório de dados é `agente-impressao`, definido em `Constants.DATA_DIR`.

| Sistema | Diretório de usuário esperado |
| --- | --- |
| Linux/Unix | `~/.agente-impressao/` |
| macOS | `~/Library/Application Support/agente-impressao/` |
| Windows | Diretório Roaming do usuário, subdiretório `agente-impressao` |

O diretório compartilhado usa `/srv/agente-impressao/` no Linux/Unix,
`/Library/Application Support/agente-impressao/` no macOS e
`ProgramData/agente-impressao/` no Windows.
`FileUtilities` é a fonte de verdade para caminhos e fallbacks específicos.

Arquivos de preferências e operação incluem `prefs.properties`,
`qz-tray.properties`, decisões de certificados, logs e recursos provisionados.
O conjunto e a precedência dependem da opção; verifique `ArgValue`,
`PrefsSearch`, `FileUtilities` e `CertificateManager` antes de editar arquivos
manualmente.

## Regras de alteração

1. Não abra o bind para interfaces externas como atalho para um problema de
   conexão.
2. Não relaxe assinatura, confiança, origem permitida, TLS ou autorizações para
   fazer testes passarem.
3. Faça testes de rede em loopback e use certificados/dados descartáveis.
4. Ao adicionar uma preferência, documente chave, valor padrão, precedência,
   impacto e forma de validação no código e nesta página.
