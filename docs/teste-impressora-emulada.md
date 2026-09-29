# Teste com impressora emulada

Ambiente Docker com ESC/POS emulator para testar impressão sem hardware real.
O que foi "impresso" aparece em painel web em tempo real.

## Pré-requisitos

- Docker e Docker Compose instalados
- JDK 25 instalado (`/usr/lib/jvm/java-25-openjdk-amd64` no Linux)
- JAR do agente compilado (`ant clean build-jar`)

## Passo a passo completo

### 1. Subir o emulador

```bash
docker compose up -d
```

Portas expostas:

| Porta | Uso |
|-------|-----|
| `9100` | Raw ESC/POS socket — para onde o agente envia os dados |
| `3000` | Web UI — veja o papel virtual com tudo que foi impresso |

Acesse http://localhost:3000 para ver a saída da impressora em tempo real.

### 2. Rodar o agente

```bash
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
java -jar out/dist/agente-impressao.jar
```

O agente inicia e fica visível na bandeja do sistema. WebSocket disponível em:
- `wss://localhost:8181` (seguro)
- `ws://localhost:8182` (não seguro — use apenas em loopback)

### 3. Preparar a página cliente

Inclua o script do agente na sua página HTML:

```html
<script src="caminho/para/qz-tray.js"></script>
```

O arquivo `qz-tray.js` está em `out/dist/` após o build, ou na pasta `js/` do
repositório (versão de desenvolvimento).

### 4. Configurar segurança (modo anônimo — somente desenvolvimento)

**Este modo não requer certificado.** Use apenas para testes locais; produção
exige certificado assinado (ver [Integração no frontend](impressao-e-integracao.md)).

```js
// Modo anônimo: sem certificado, sem assinatura
qz.security.setCertificatePromise(function(resolve, reject) {
    resolve(); // sem argumento = modo anônimo
});

qz.security.setSignatureAlgorithm("SHA512");

// setSignaturePromise deve retornar uma função — não chamar resolve diretamente
qz.security.setSignaturePromise(function(toSign) {
    return function(resolve, reject) {
        resolve(); // sem argumento = não assina (desenvolvimento)
    };
});
```

> **Atenção — dois erros comuns:**
> - `resolve(null)` em vez de `resolve()` causa `Error: Failed to sign request`
> - Chamar `resolve()` diretamente no `setSignaturePromise` (sem retornar uma
>   função interna) causa falha silenciosa na assinatura

### 5. Conectar ao agente

```js
qz.websocket.connect().then(function() {
    console.log("Conectado ao agente de impressão");
}).catch(function(e) {
    console.error("Falha na conexão:", e);
});
```

Na primeira conexão, o agente exibe um diálogo de autorização na bandeja:

- **"An anonymous request solicita autorização..."** — é o modo anônimo funcionando
- Clique **Autorizar**
- A opção **"Lembrar esta decisão"** fica desabilitada para requisições anônimas
  (comportamento intencional de segurança do QZ Tray); isso é normal
- A conexão completa após a autorização e vale para a sessão atual

### 6. Enviar impressão para o emulador

Configure uma impressora de rede apontando para o socket do emulador:

```js
// Impressora de rede: host + porta (ESC/POS raw TCP)
var config = qz.configs.create({ host: "127.0.0.1", port: 9100 });

var ESC = "\x1b", GS = "\x1d";
var dados =
    ESC + "@" +           // inicializar impressora
    ESC + "a\x01" +       // centralizar
    "APROVEITA FOOD\n" +
    ESC + "a\x00" +       // alinhar à esquerda
    "Pedido #001\n" +
    "Item A ......... R$ 10,00\n" +
    "\n\n\n" +
    GS + "V\x41\x00";     // cortar papel

qz.print(config, [{
    type: "raw",
    format: "command",
    data: dados
}]).then(function() {
    console.log("Impresso! Veja em http://localhost:3000");
}).catch(function(e) {
    console.error("Erro ao imprimir:", e);
});
```

Após `qz.print()` resolver, acesse http://localhost:3000 para ver o cupom
renderizado.

### 7. Parar o emulador

```bash
docker compose down
```

## Fluxo completo resumido

```
browser (JS)
  → qz.websocket.connect()  →  agente (WebSocket wss/ws :8181/:8182)
  → qz.print(config, dados) →  agente roteia para impressora de rede
                             →  emulador ESC/POS (:9100)
                             →  web UI (:3000) exibe o cupom
```

## Solução de problemas

| Sintoma | Causa provável | Solução |
|---------|---------------|---------|
| `Error: Failed to sign request` | `resolve(null)` no promise de assinatura | Use `resolve()` sem argumento |
| Diálogo de autorização não aparece | Agente não está rodando | Inicie o JAR |
| Conexão recusada | Porta errada ou firewall | Verifique `ws://localhost:8182` |
| Impressão não aparece em :3000 | Emulador parado | `docker compose up -d` |
| "Autorizar" desabilitado com "Lembrar" | Comportamento normal para modo anônimo | Desmarque "Lembrar" e clique Autorizar |
