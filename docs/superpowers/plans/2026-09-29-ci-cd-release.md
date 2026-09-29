# CI/CD Multi-Platform Release Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Commitar o fork atual, renomear o repositório para `agente-impressao`, configurar CI/CD no GitHub Actions que produz releases automáticos com instaladores para Windows (.exe), macOS (.pkg + .dmg) e Linux (.run), e subir tudo para `github.com/aproveitafood`.

**Architecture:** Três jobs paralelos no GitHub Actions — cada um em seu runner nativo (`windows-latest`, `macos-latest`, `ubuntu-latest`) — invocam os targets Ant existentes (`nsis`, `pkgbuild`+`dmg`, `makeself`). Um quarto job `release` só roda em push de tag `v*`, coleta os artefatos dos três e cria o GitHub Release. Versão é lida diretamente de `src/qz/common/Constants.java` (`VERSION = "2.3.0"`); bumps via commit que altera essa constante e cria a tag.

**Tech Stack:** Java 25 (Liberica/BellSoft via `setup-java`), Apache Ant + Ivy, GitHub Actions, NSIS (Windows), pkgbuild/hdiutil (macOS), makeself (Linux), `softprops/action-gh-release`.

**Spec:** este plano é o próprio spec; refira a `docs/desenvolvimento.md` para targets Ant e a `src/qz/common/Constants.java` para versão.

## Global Constraints

- Java 25 exato (`javac.source=25`, `javac.target=25` em `ant/project.properties`)
- JRE bundled: Liberica BellSoft `25.0.4.1+1` hotspot (via `jlink.java.vendor=BellSoft`)
- Nunca usar gradientes CSS; nunca usar emojis (regra do projeto)
- Nome do repo: `agente-impressao` na org `aproveitafood`
- Commits só com `WalysonGO` como autor; `Co-Authored-By: Claude Sonnet 4.6` na linha final
- Board: setar issue para **In Progress** ao começar, **Done** ao terminar (org `aproveitafood`, project 1)
- `ant/project.properties`: `project.name`, `project.filename`, `vendor.*` ainda precisam ser atualizados (fora do escopo deste plano — não mexa neles aqui)

## Review Focus

- **Tag sem build anterior:** push de tag v* antes do primeiro `build-jar` bem-sucedido → job `release` tenta baixar artefato inexistente. Coberto em Task 3, step de validação do `needs`.
- **Falta de segredo `GITHUB_TOKEN`:** `softprops/action-gh-release` precisa de permissão `contents: write`. Se faltar, upload silencia. Coberto em Task 3, step de permissões.
- **macOS sem identidade de assinatura:** `ant/apple/installer.xml` chama `get-identity`; sem certificado, falha. Workflow usa `build-jar` + `distribute` + `dmg` com variável de ambiente que pula assinatura (`skip.sign=true`). Coberto em Task 3.
- **Windows: NSIS não instalado no runner:** `windows-latest` não tem NSIS por padrão. Instalar via `choco install nsis`. Coberto em Task 3.
- **Linux: makeself não instalado:** `ubuntu-latest` pode não ter `makeself`. Instalar via `apt-get install makeself`. Coberto em Task 3.

---

## Task 1: Commit das alterações existentes em grupos lógicos

**Files:**
- Modify: `src/qz/ui/ThemeUtilities.java` (fix hoverForeground — já feito)
- Modify: vários arquivos de branding (Constants, ícones, TrayManager, etc — já feitos)
- Create: `AGENTS.md`, `docs/`, `src/qz/ui/resources/fonts/`, `src/qz/ui/resources/qz-minimize.png`, `src/qz/ui/tray/Tray*.java`

**Interfaces:**
- Produces: histórico git limpo com dois commits semânticos antes de criar o repo remoto

- [ ] **Step 1: Verificar o que está staged/unstaged**

```bash
cd /home/walysongo/Projetos/meeu.menu/printer_termical
git status --short
```

- [ ] **Step 2: Commit 1 — branding e identidade visual**

Agrupa todas as alterações visuais (ícones, Constants, TrayManager, BasicDialog, IconCache, LinkLabel, ModernTrayIcon, TaskbarTrayIcon, TrayType, SystemUtilities, assets/branding, SUPPORT.md removido):

```bash
git add \
  src/qz/common/Constants.java \
  src/qz/common/TrayManager.java \
  src/qz/ui/BasicDialog.java \
  src/qz/ui/component/IconCache.java \
  src/qz/ui/component/LinkLabel.java \
  src/qz/ui/resources/qz-about.png \
  src/qz/ui/resources/qz-allow.png \
  src/qz/ui/resources/qz-copy.png \
  src/qz/ui/resources/qz-desktop.png \
  src/qz/ui/resources/qz-exit.png \
  src/qz/ui/resources/qz-field.png \
  src/qz/ui/resources/qz-folder.png \
  src/qz/ui/resources/qz-log.png \
  src/qz/ui/resources/qz-logo.png \
  src/qz/ui/resources/qz-reload.png \
  src/qz/ui/resources/qz-saved.png \
  src/qz/ui/resources/qz-settings.png \
  src/qz/ui/tray/ModernTrayIcon.java \
  src/qz/ui/tray/TaskbarTrayIcon.java \
  src/qz/ui/tray/TrayType.java \
  src/qz/utils/SystemUtilities.java \
  assets/branding/create_branding.sh \
  SUPPORT.md

git commit -m "feat: rebrand QZ Tray para Aproveita Food

Substitui identidade visual, ícones, constantes e strings de marca
do QZ Industries pela identidade do Aproveita Food / Meeu Menu.

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

- [ ] **Step 3: Commit 2 — novos componentes de UI e docs**

Agrupa novos arquivos que não existiam upstream (TrayActivationGate, TrayMenuPlacement, qz-minimize.png, fonts, AGENTS.md, docs/):

```bash
git add \
  src/qz/ui/tray/TrayActivationGate.java \
  src/qz/ui/tray/TrayMenuPlacement.java \
  src/qz/ui/resources/qz-minimize.png \
  src/qz/ui/resources/fonts/ \
  AGENTS.md \
  docs/

git commit -m "feat: adiciona componentes de bandeja e documentação do projeto

Novos: TrayActivationGate, TrayMenuPlacement, ícone minimize,
fontes customizadas e documentação interna (docs/, AGENTS.md).

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

- [ ] **Step 4: Commit 3 — correção hoverForeground no FlatLaf**

```bash
git add src/qz/ui/ThemeUtilities.java

git commit -m "fix: corrige UnknownStyleException hoverForeground no FlatMenuItemUI

FlatMenuItemUI não suporta as chaves de estilo hoverForeground/
hoverBackground; usa selectionForeground/selectionBackground para
JMenuItem. Os demais componentes mantêm as chaves originais.

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

- [ ] **Step 5: Verificar log**

```bash
git log --oneline -5
```
Esperado: 3 novos commits no topo de `master`.

---

## Task 2: Atualizar README com logo centralizada e renomear pasta

**Files:**
- Modify: `README.md`
- Rename: pasta de `printer_termical` para `agente-impressao` (feito no filesystem pelo executor; git não precisa de rename de diretório-pai)

**Interfaces:**
- Produces: README com logo centralizada e seção de download (stub para quando CI/CD estiver pronto); pasta com nome correto

- [ ] **Step 1: Atualizar README.md com logo centralizada**

Substituir o cabeçalho atual:

```markdown
# Aproveita Food — agente de impressão

![Logo Aproveita Food](src/qz/ui/resources/aproveita-logo-horizontal.png)
```

Por:

```markdown
<p align="center">
  <img src="src/qz/ui/resources/aproveita-logo-horizontal.png" alt="Aproveita Food" width="320" />
</p>

<h1 align="center">Agente de Impressão</h1>

<p align="center">
  Conecta o Meeu Menu às impressoras e dispositivos da máquina via WebSocket local.
</p>

<p align="center">
  <a href="https://github.com/aproveitafood/agente-impressao/releases/latest">
    <img src="https://img.shields.io/github/v/release/aproveitafood/agente-impressao?label=download&color=1a1a1a" alt="Download" />
  </a>
  <img src="https://img.shields.io/badge/plataformas-Windows%20%7C%20macOS%20%7C%20Linux-1a1a1a" alt="Plataformas" />
  <img src="https://img.shields.io/badge/java-25-1a1a1a" alt="Java 25" />
</p>
```

- [ ] **Step 2: Adicionar seção "Download" no README antes de "Executar localmente"**

```markdown
## Download

Baixe o instalador para sua plataforma na [página de releases](https://github.com/aproveitafood/agente-impressao/releases/latest):

| Plataforma | Arquivo |
|---|---|
| Windows | `agente-impressao-x.x.x-windows-x64.exe` |
| macOS (Apple Silicon) | `agente-impressao-x.x.x-macos-aarch64.pkg` |
| macOS (Intel) | `agente-impressao-x.x.x-macos-x64.pkg` |
| Linux | `agente-impressao-x.x.x-linux-x64.run` |
```

- [ ] **Step 3: Commit do README atualizado**

```bash
git add README.md
git commit -m "docs: atualiza README com logo centralizada e seção de download

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

- [ ] **Step 4: Renomear pasta no filesystem**

```bash
cd /home/walysongo/Projetos/meeu.menu
mv printer_termical agente-impressao
```

Verificar:
```bash
ls /home/walysongo/Projetos/meeu.menu/
```
Esperado: `agente-impressao/` presente, `printer_termical/` ausente.

---

## Task 3: GitHub Actions — CI/CD com release multi-plataforma

**Files:**
- Create: `.github/workflows/build.yml`

**Interfaces:**
- Consumes: `ant nsis` (Windows), `ant makeself` (Linux), `ant pkgbuild` + `ant dmg` (macOS)
- Produces: workflow que em push de tag `v*` cria GitHub Release com todos os artefatos

- [ ] **Step 1: Criar `.github/workflows/build.yml`**

```bash
mkdir -p /home/walysongo/Projetos/meeu.menu/agente-impressao/.github/workflows
```

Criar o arquivo `.github/workflows/build.yml`:

```yaml
name: Build & Release

on:
  push:
    tags:
      - "v*"
  pull_request:
    branches:
      - master

permissions:
  contents: write

jobs:
  # ──────────────────────────────────────────────
  # Linux — makeself .run
  # ──────────────────────────────────────────────
  build-linux:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up Java 25
        uses: actions/setup-java@v4
        with:
          distribution: liberica
          java-version: "25"

      - name: Install Ant + makeself
        run: |
          sudo apt-get update -q
          sudo apt-get install -y ant makeself

      - name: Build Linux installer
        run: |
          export JAVA_HOME=$JAVA_HOME_25_X64
          ant makeself
        env:
          ANT_OPTS: "-Djlink.api.enabled=false"

      - name: Upload artifact
        uses: actions/upload-artifact@v4
        with:
          name: linux-installer
          path: out/dist/*.run
          retention-days: 1

  # ──────────────────────────────────────────────
  # Windows — NSIS .exe
  # ──────────────────────────────────────────────
  build-windows:
    runs-on: windows-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up Java 25
        uses: actions/setup-java@v4
        with:
          distribution: liberica
          java-version: "25"

      - name: Install Ant + NSIS
        run: |
          choco install ant nsis --no-progress -y

      - name: Build Windows installer
        run: |
          ant nsis
        env:
          ANT_OPTS: "-Djlink.api.enabled=false"

      - name: Upload artifact
        uses: actions/upload-artifact@v4
        with:
          name: windows-installer
          path: out/dist/*.exe
          retention-days: 1

  # ──────────────────────────────────────────────
  # macOS — .pkg + .dmg
  # ──────────────────────────────────────────────
  build-macos:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up Java 25
        uses: actions/setup-java@v4
        with:
          distribution: liberica
          java-version: "25"

      - name: Install Ant
        run: brew install ant

      - name: Build macOS pkg
        run: |
          export JAVA_HOME=$JAVA_HOME_25_ARM64
          ant pkgbuild
        env:
          ANT_OPTS: "-Djlink.api.enabled=false"
          SKIP_SIGN: "true"

      - name: Build macOS dmg
        run: |
          export JAVA_HOME=$JAVA_HOME_25_ARM64
          ant dmg
        env:
          ANT_OPTS: "-Djlink.api.enabled=false"
          SKIP_SIGN: "true"

      - name: Upload artifact
        uses: actions/upload-artifact@v4
        with:
          name: macos-installer
          path: |
            out/dist/*.pkg
            out/dist/*.dmg
          retention-days: 1

  # ──────────────────────────────────────────────
  # Release — só em push de tag v*
  # ──────────────────────────────────────────────
  release:
    needs: [build-linux, build-windows, build-macos]
    runs-on: ubuntu-latest
    if: startsWith(github.ref, 'refs/tags/v')
    steps:
      - name: Download all artifacts
        uses: actions/download-artifact@v4
        with:
          path: artifacts/

      - name: Create GitHub Release
        uses: softprops/action-gh-release@v2
        with:
          files: artifacts/**/*
          generate_release_notes: true
          draft: false
          prerelease: ${{ contains(github.ref_name, '-rc') || contains(github.ref_name, '-beta') || contains(github.ref_name, '-alpha') }}
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

- [ ] **Step 2: Verificar que o arquivo foi criado**

```bash
cat /home/walysongo/Projetos/meeu.menu/agente-impressao/.github/workflows/build.yml | head -20
```

- [ ] **Step 3: Commit do workflow**

```bash
cd /home/walysongo/Projetos/meeu.menu/agente-impressao
git add .github/workflows/build.yml
git commit -m "ci: adiciona GitHub Actions para build e release multi-plataforma

Jobs: build-linux (makeself .run), build-windows (NSIS .exe),
build-macos (pkgbuild .pkg + hdiutil .dmg). Release job agrega
artefatos e publica no GitHub Releases em push de tag v*.

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 4: Criar repositório e push para aproveitafood/agente-impressao

**Files:** nenhum arquivo novo

**Interfaces:**
- Consumes: histórico git local com todos os commits das Tasks 1–3
- Produces: repositório público em `github.com/aproveitafood/agente-impressao`

> **Nota:** o MCP do GitHub está falhando (`400: badly formatted`). As etapas abaixo usam `gh` CLI diretamente.

- [ ] **Step 1: Verificar login do gh CLI**

```bash
gh auth status
```
Esperado: conta da org `aproveitafood` ou conta com acesso a ela.

- [ ] **Step 2: Criar repositório remoto**

```bash
cd /home/walysongo/Projetos/meeu.menu/agente-impressao
gh repo create aproveitafood/agente-impressao \
  --public \
  --description "Agente de impressão desktop para o Meeu Menu — fork do QZ Tray" \
  --homepage "https://meeu.menu"
```

- [ ] **Step 3: Adicionar remote e fazer push**

```bash
git remote add origin https://github.com/aproveitafood/agente-impressao.git
git push -u origin master
```

- [ ] **Step 4: Verificar que o repositório está no ar**

```bash
gh repo view aproveitafood/agente-impressao --web
```

---

## Task 5: Criar e pushar primeira tag de release

**Files:** nenhum

**Interfaces:**
- Consumes: repositório criado em Task 4
- Produces: tag `v2.3.0` que dispara o workflow de CI/CD e cria o primeiro GitHub Release

- [ ] **Step 1: Verificar versão atual em Constants.java**

```bash
grep 'VERSION' /home/walysongo/Projetos/meeu.menu/agente-impressao/src/qz/common/Constants.java
```
Esperado: `Version.valueOf("2.3.0")`.

- [ ] **Step 2: Criar e pushar tag**

```bash
cd /home/walysongo/Projetos/meeu.menu/agente-impressao
git tag -a v2.3.0 -m "Release v2.3.0 — fork Aproveita Food do QZ Tray 2.3.0"
git push origin v2.3.0
```

- [ ] **Step 3: Acompanhar o workflow**

```bash
gh run list --repo aproveitafood/agente-impressao --limit 5
```

Abrir no browser para acompanhar:
```bash
gh run watch --repo aproveitafood/agente-impressao
```

- [ ] **Step 4: Verificar release criado**

```bash
gh release view v2.3.0 --repo aproveitafood/agente-impressao
```
Esperado: 4+ assets (`.run`, `.exe`, `.pkg`, `.dmg`).
