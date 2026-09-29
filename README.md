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

---

Aplicativo desktop que conecta o Meeu Menu às impressoras e dispositivos
disponíveis na máquina. Ele mantém um serviço local para comunicação com o
cliente JavaScript, gerencia certificados e permissões e oferece uma interface
pela bandeja do sistema.

Este repositório contém um fork Java baseado no QZ Tray 2.3.0, adaptado para a
identidade e a operação do Aproveita Food. A licença e os avisos de terceiros
continuam aplicáveis; consulte [`LICENSE.txt`](LICENSE.txt).

## Download

Baixe o instalador para sua plataforma na [página de releases](https://github.com/aproveitafood/agente-impressao/releases/latest):

| Plataforma | Arquivo |
|---|---|
| Windows | `agente-impressao-x.x.x-windows-x64.exe` |
| macOS (Apple Silicon) | `agente-impressao-x.x.x-macos-aarch64.pkg` |
| macOS (Intel) | `agente-impressao-x.x.x-macos-x64.pkg` |
| Linux | `agente-impressao-x.x.x-linux-x64.run` |

> **macOS:** o instalador é ad-hoc assinado. Na primeira execução, clique com botão direito no arquivo e escolha "Abrir" para contornar o Gatekeeper.

## Executar localmente

Requisitos: JDK 25, Apache Ant e acesso à internet para a primeira resolução das
dependências Ivy.

```bash
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
ant clean build-jar
java -jar out/dist/qz-tray.jar
```

O servidor local publica o endpoint de informações em
`http://127.0.0.1:8182/json`; a porta segura padrão é `8181`. A configuração pode
alterar portas e interfaces de rede. Não exponha o serviço a outras máquinas
sem revisar as definições de segurança.

## Validar alterações

```bash
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
ant testng
```

Os testes de integração podem depender de serviços, navegadores, impressoras ou
recursos do sistema operacional. Leia [Desenvolvimento](docs/desenvolvimento.md)
antes de escolher a validação adequada.

## Documentação

- [Índice e escopo](docs/README.md)
- [Arquitetura](docs/arquitetura.md)
- [Desenvolvimento, build e testes](docs/desenvolvimento.md)
- [Impressão e integração](docs/impressao-e-integracao.md)
- [Configuração e segurança](docs/configuracao-e-seguranca.md)
- [Interface e identidade visual](docs/interface-e-identidade.md)
- [Operação e diagnóstico](docs/operacao-e-diagnostico.md)
- [Regras, decisões e glossário](docs/regras-e-decisoes.md)
- [Instruções para agentes](AGENTS.md)

## Licença e origem

O projeto mantém código e componentes derivados do QZ Tray. Preserve os avisos
de copyright, a licença LGPL 2.1 e os termos das dependências ao modificar,
empacotar ou distribuir o aplicativo.
