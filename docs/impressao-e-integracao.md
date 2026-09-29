# Impressão e integração

## Papel do agente

O agente disponibiliza uma ponte local para que um cliente autorizado solicite
operações com impressoras e dispositivos acessíveis ao computador. A API é
consumida por cliente JavaScript compatível com o protocolo WebSocket do agente.

O frontend/backend de produção não estão neste repositório. Antes de mudar o
formato de mensagens, autenticação, descoberta ou resposta, verifique também o
cliente e o backend consumidores.

## Como o frontend se comunica com o agente

### Visão geral do fluxo

```
frontend (browser)
  ──WebSocket──▶  agente (localhost :8181 wss / :8182 ws)
                    │
                    └──TCP raw──▶  impressora (USB, rede ou emulador)
```

O agente **não é um servidor web** — é um processo desktop que expõe um
WebSocket local. O frontend nunca se comunica diretamente com a impressora;
o agente faz esse roteamento.

### 1. Incluir a biblioteca cliente

```html
<script src="/js/qz-tray.js"></script>
```

O arquivo `qz-tray.js` está em `js/` neste repositório. Distribua junto com
o frontend ou sirva como asset estático. Não altere a versão sem testar
conectividade e impressão.

### 2. Configurar segurança

O agente exige que toda conexão apresente um certificado e assine as
requisições. Há dois modos:

#### Modo anônimo (somente desenvolvimento local)

Não requer nenhum certificado ou chave. O agente exibe um diálogo de
autorização a cada sessão:

```js
qz.security.setCertificatePromise(function(resolve, reject) {
    resolve(); // sem argumento = modo anônimo
});

qz.security.setSignatureAlgorithm("SHA512");

qz.security.setSignaturePromise(function(toSign) {
    // IMPORTANTE: deve retornar uma função — não chamar resolve() aqui diretamente
    return function(resolve, reject) {
        resolve(); // sem argumento = sem assinatura
    };
});
```

> `resolve(null)` em vez de `resolve()` causa `Error: Failed to sign request`.
> `setSignaturePromise` deve retornar uma função interna `(resolve, reject)`.

#### Modo certificado (produção)

Requer um par de chaves RSA. O backend assina o `toSign` com a chave privada
e o frontend apresenta o certificado público:

```js
qz.security.setCertificatePromise(function(resolve, reject) {
    // Busque o certificado PEM do backend
    fetch("/api/agente/certificado")
        .then(r => r.text())
        .then(resolve)
        .catch(reject);
});

qz.security.setSignatureAlgorithm("SHA512");

qz.security.setSignaturePromise(function(toSign) {
    return function(resolve, reject) {
        // Backend assina com a chave privada
        fetch("/api/agente/assinar", {
            method: "POST",
            body: toSign,
            headers: { "Content-Type": "text/plain" }
        })
        .then(r => r.text())
        .then(resolve)
        .catch(reject);
    };
});
```

A geração das chaves e a configuração do certificado no agente estão
descritas em [Configuração e segurança](configuracao-e-seguranca.md).

### 3. Conectar ao agente

```js
qz.websocket.connect()
    .then(function() {
        // Conectado — pode listar impressoras e enviar trabalhos
    })
    .catch(function(e) {
        console.error("Agente não encontrado:", e);
        // Mostre ao usuário que o agente precisa estar instalado e rodando
    });
```

Na primeira conexão (modo anônimo), o agente exibe um diálogo de autorização.
O usuário deve clicar **Autorizar**. Em modo certificado com "Lembrar esta
decisão" marcado, o agente autoriza automaticamente nas sessões seguintes.

Para desconectar:

```js
qz.websocket.disconnect();
```

### 4. Descobrir impressoras disponíveis

```js
// Listar todas as impressoras locais
qz.printers.find().then(function(printers) {
    console.log(printers); // array de nomes
});

// Buscar uma impressora específica pelo nome parcial
qz.printers.find("térmica").then(function(printer) {
    console.log(printer); // nome exato encontrado
});
```

### 5. Enviar um trabalho de impressão

#### Impressora de rede / ESC/POS raw TCP (mais comum para térmica)

Use quando a impressora é acessada por IP e porta (inclusive o emulador
de testes em `127.0.0.1:9100`):

```js
var config = qz.configs.create({ host: "127.0.0.1", port: 9100 });

var ESC = "\x1b", GS = "\x1d";
var dados =
    ESC + "@" +            // inicializar impressora
    ESC + "a\x01" +        // centralizar
    ESC + "!\x30" +        // fonte dupla (altura e largura)
    "APROVEITA FOOD\n" +
    ESC + "!\x00" +        // fonte normal
    "--------------------------------\n" +
    ESC + "E\x01" +        // negrito ligado
    "PEDIDO #0042\n" +
    ESC + "E\x00" +        // negrito desligado
    "--------------------------------\n" +
    ESC + "a\x00" +        // alinhar à esquerda
    "X Burger duplo ..... R$ 38,90\n" +
    "--------------------------------\n" +
    ESC + "a\x01" +
    "Obrigado!\n\n\n" +
    GS + "V\x41\x00";      // cortar papel

qz.print(config, [{
    type: "raw",
    format: "command",
    data: dados
}]);
```

#### Impressora local (USB / nome do sistema)

```js
qz.printers.find("nome-da-impressora").then(function(printer) {
    var config = qz.configs.create(printer);
    return qz.print(config, [{
        type: "raw",
        format: "command",
        data: "\x1b\x40Pedido recebido\n\n\n"
    }]);
});
```

### 6. Tratar erros e estado da conexão

```js
// Verificar se já está conectado antes de tentar conectar
if (qz.websocket.isActive()) {
    // já conectado
} else {
    qz.websocket.connect().then(/* ... */);
}

// Ouvir desconexões inesperadas
qz.websocket.setClosedCallbacks(function(e) {
    console.warn("Agente desconectado:", e);
});
```

## Formatos e caminhos de processamento

`qz.printer.action.ProcessorFactory` cria processadores para as famílias
`HTML`, `IMAGE`, `PDF`, `DIRECT` e `COMMAND`. As implementações e conversores
estão em `src/qz/printer/action/`, incluindo conversores de linguagens e
comandos de impressão. A enumeração de formatos, validações e estruturas de
payload devem ser lidas no código antes de modificar a integração.

O projeto também inclui comunicação com recursos locais, como arquivo, USB/HID,
serial e sockets. A disponibilidade real é condicionada pelo sistema
operacional, pelos drivers, pelas permissões e pelas preferências de segurança.

## Fluxo de autorização e segurança

- Solicitações de recursos locais podem apresentar uma janela de autorização.
- Certificados salvos e decisões de bloqueio/autorização influenciam solicitações
  futuras.
- As mensagens assinadas e o estado do certificado são avaliados em `qz.auth`.
- Não trate qualquer conexão WebSocket bem-sucedida como autorização para
  imprimir ou acessar arquivos/dispositivos.
- A opção "Lembrar esta decisão" fica desabilitada para requisições anônimas;
  isso é intencional — apenas certificados confiáveis podem ser lembrados.
- Preserve os mecanismos de aprovação, assinatura e restrição de origem ao
  alterar UI ou API.

Consulte `src/qz/ui/GatewayDialog.java`, `src/qz/auth/Request.java`,
`src/qz/auth/Certificate.java` e `src/qz/ws/PrintSocketClient.java` para
confirmar o fluxo atual.

## Teste com emulador Docker

Para testes sem hardware real, veja [Teste com impressora emulada](teste-impressora-emulada.md).

## Teste manual local (sem emulador)

1. Faça build com `ant clean build-jar`.
2. Inicie o JAR e aguarde a mensagem de inicialização do servidor.
3. Verifique `http://127.0.0.1:8182/json` se a porta padrão não foi alterada.
4. Conecte o cliente JavaScript de teste usando o protocolo e as configurações
   de certificado deste ambiente.
5. Teste descoberta e impressão somente em impressora/dispositivo de teste.

Não envie trabalhos reais nem dados pessoais durante testes sem autorização.
