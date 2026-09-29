# Responsabilidades e handoff da integração

Esta página define os limites entre o frontend (Renato), o backend (Walisson)
e o agente desktop. Ela descreve o contrato de integração esperado; os
endereços concretos dos serviços web, a autenticação da aplicação e a
infraestrutura do backend pertencem aos repositórios correspondentes.

## Regra de fronteira

O browser fala somente com o agente pelo WebSocket local e com o backend pela
API web da aplicação. O backend nunca abre conexão direta com a impressora do
usuário, e o frontend nunca recebe nem armazena a chave privada de assinatura.

```text
frontend no browser ── WebSocket local ──> agente desktop ──> impressora
        │
        └────────────── HTTPS ───────────> backend de assinatura
```

O agente é a autoridade para acesso ao computador: valida certificado e
assinatura, aplica origem e preferências, apresenta autorizações e despacha a
impressão. Uma conexão WebSocket aberta não concede, por si só, permissão
permanente para uma operação.

## Matriz de responsabilidades

| Tema | Frontend — Renato | Backend — Walisson | Agente desktop |
| --- | --- | --- | --- |
| Biblioteca cliente | Publicar e carregar a cópia compatível de `qz-tray.js`; configurar os callbacks de segurança e a conexão. | Não aplicável. | Mantém o protocolo consumido pela biblioteca e publica a cópia de desenvolvimento em `js/`. |
| Certificado público | Buscar o PEM do endpoint autenticado e entregá-lo sem transformação a `setCertificatePromise`. | Expor o certificado público e manter a chave privada fora da resposta, do frontend, de logs e do repositório. | Recebe o certificado no handshake e avalia confiança/estado para a sessão. |
| Assinatura | Enviar o `toSign` recebido de `setSignaturePromise` ao backend e retornar a resposta ao `resolve`. Não serializar, normalizar ou reutilizar a assinatura. | Autorizar a chamada, assinar o conteúdo recebido com a chave privada e devolver a assinatura no formato aceito pela versão distribuída de `qz-tray.js`. Controlar rotação e auditoria sem registrar a chave ou o conteúdo sensível em claro. | Valida assinatura, algoritmo e validade temporal antes de permitir chamadas que exibem autorização. |
| Conexão e UX | Conectar/desconectar, tratar indisponibilidade do agente, autorização recusada e perda de conexão; mostrar instrução de instalação/execução quando necessário. | Disponibilizar os endpoints de certificado e assinatura com autenticação da aplicação. | Inicia WS/WSS, expõe `/json`, aplica origem configurada e mostra autorizações locais. |
| Impressoras e trabalhos | Listar impressoras, permitir selecionar a fila do SO e montar o payload compatível para `qz.print`. Não conectar o browser diretamente à impressora. | Armazenar apenas preferências funcionais que façam parte do domínio do produto; não presumir que a fila exista no computador atual. | Descobre filas/dispositivos, aplica permissões e encaminha o trabalho para a fila, rede ou dispositivo configurado. |
| Distribuição e suporte | Comunicar requisito de instalar/atualizar o agente e registrar erros de integração úteis ao usuário. | Operar os serviços de assinatura e observar falhas do endpoint. | Gerar JAR/instaladores, manter logs locais, certificados, preferências e compatibilidade por SO. |

## Contrato entre frontend e backend

O frontend precisa de dois recursos web autenticados, cujos caminhos devem ser
definidos e documentados no repositório da aplicação:

| Recurso | Entrada | Saída | Responsável |
| --- | --- | --- | --- |
| Certificado | Contexto da sessão autenticada | Certificado público PEM, como texto | Backend |
| Assinatura | Exatamente o valor `toSign` entregue pela biblioteca e o contexto autenticado | Assinatura correspondente, como texto no formato da biblioteca | Backend |

No frontend, a integração deve preservar o padrão abaixo. Os caminhos são
apenas exemplos, não contrato deste repositório:

```js
qz.security.setCertificatePromise(function(resolve, reject) {
    fetch("/api/agente/certificado")
        .then(function(response) {
            if (!response.ok) throw new Error("Não foi possível obter o certificado");
            return response.text();
        })
        .then(resolve, reject);
});

qz.security.setSignatureAlgorithm("SHA512");
qz.security.setSignaturePromise(function(toSign) {
    return function(resolve, reject) {
        fetch("/api/agente/assinar", {
            method: "POST",
            headers: { "Content-Type": "text/plain" },
            body: toSign
        })
        .then(function(response) {
            if (!response.ok) throw new Error("Não foi possível assinar a solicitação");
            return response.text();
        })
        .then(resolve, reject);
    };
});
```

O algoritmo, a chave/certificado e a versão de `qz-tray.js` devem ser
compatíveis entre as três partes. Alterar qualquer um deles é uma mudança de
contrato: não deve ser publicado isoladamente.

## Fluxo de entrega e validação compartilhada

1. **Backend:** disponibiliza primeiro certificado e assinatura em ambiente de
   teste, com autenticação e uma chave descartável ou aprovada para esse
   ambiente. A chave privada nunca sai do backend.
2. **Frontend:** integra os callbacks, conecta ao agente, trata erros de rede e
   autorização e envia uma impressão de teste para uma fila conhecida.
3. **Agente:** gera o JAR/instalador e preserva a validação de certificado,
   assinatura, origem e permissões; não é aceitável contornar esses controles
   para concluir o teste.
4. **Validação conjunta:** confirme versão do agente em
   `http://127.0.0.1:8182/json` (se a configuração padrão estiver ativa),
   conexão no browser, listagem da fila, assinatura aceita e impressão. O
   emulador ESC/POS e a fila CUPS descritos em
   [Teste com impressora emulada](teste-impressora-emulada.md) permitem esse
   teste sem hardware físico.

Ao investigar uma falha, registre a versão do agente, sistema operacional,
nome da fila (sem dados do pedido), transporte tentado (`ws` ou `wss`), etapa
que falhou e mensagem exibida. Não compartilhe chave privada, token de sessão,
certificado de produção ou conteúdo real de pedidos.

## Mudanças que exigem alinhamento prévio

As três partes devem aprovar e testar juntas mudanças em:

- versão ou comportamento de `qz-tray.js`, mensagens WebSocket e payload de
  impressão;
- algoritmo de assinatura, certificado, formato da assinatura, validade ou
  política de autorização;
- origens permitidas, portas, transporte WS/WSS, bind de rede ou TLS;
- identificação/persistência da impressora selecionada e comportamento de
  impressão automática;
- versão mínima do agente ou formato dos instaladores.

Mudanças só de interface web, sem alterar essas fronteiras, permanecem no
frontend. Mudanças na autenticação, nos endpoints ou na guarda da chave
permanecem no backend. Mudanças em impressão local, WebSocket, certificados
locais, permissões, instaladores e bandeja permanecem neste repositório.

Consulte também [Impressão e integração](impressao-e-integracao.md) para a API
do cliente e [Configuração e segurança](configuracao-e-seguranca.md) para os
controles aplicados pelo agente.
