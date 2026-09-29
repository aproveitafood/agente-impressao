# Teste com impressora emulada

Ambiente Docker com ESC/POS emulator para testar impressão sem hardware real.
O que foi "impresso" aparece em painel web em tempo real.

## Subir o emulador

```bash
docker compose up -d
```

- **Web UI:** http://localhost:3000 — veja o papel virtual com tudo que foi impresso
- **Socket ESC/POS:** `127.0.0.1:9100` — para onde o agente envia os dados

## Rodar o agente localmente

```bash
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
java -jar out/dist/agente-impressao.jar
```

## Configurar uma impressora de rede no agente

No cliente JavaScript (ou na demo em `out/dist/demo/`), configure uma impressora
de rede apontando para o emulador:

```js
var config = qz.configs.create("127.0.0.1", { host: "127.0.0.1", port: 9100 });
```

Ou via tipo `"socket"` na API QZ:

```js
qz.printers.find("127.0.0.1").then(function(printer) {
    return qz.print(qz.configs.create(printer), [{
        type: "raw",
        format: "command",
        data: "\x1b\x40Hello Aproveita Food\n\n\n"
    }]);
});
```

## Parar o emulador

```bash
docker compose down
```

## Portas

| Porta | Uso |
|-------|-----|
| `9100` | Raw ESC/POS socket (destino da impressão) |
| `3000` | Web UI para visualizar o que foi impresso |
