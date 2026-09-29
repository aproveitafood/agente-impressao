# Agente de Impressão — build notes

Aplicativo desktop do Aproveita Food para integração com impressoras térmicas,
acionado a partir do módulo `printers` do backend (ver repo backend, issue #25).

## Building

Requires JDK 25 (installed on dev machine) + Apache Ant + Ivy (auto-resolves
deps on first `ant build-jar` run — internet required, ~110MB download).

**Gotcha:** `JAVA_HOME` on this machine defaults to JDK 17/Oracle. Ant uses
`JAVA_HOME`, not whatever `javac` resolves to on `PATH`. Set it explicitly
before building:

```bash
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
ant build-jar
```

Without this, compilation fails with `error: invalid target release: 25`.

Confirmed working 2026-09-27: `ant build-jar` → BUILD SUCCESSFUL, produces
self-signed `out/dist/qz-tray.jar` (~47MB) at `out/dist/qz-tray.jar`.
