# Documentação do agente de impressão

Este diretório descreve o propósito, a arquitetura, o desenvolvimento e as
regras de evolução do agente desktop do Aproveita Food. Use as páginas como
mapa e valide qualquer detalhe operacional ou comportamento diretamente no
código e nos testes correspondentes.

## Páginas

| Página | Conteúdo |
| --- | --- |
| [Arquitetura](arquitetura.md) | Componentes Java e fluxo de inicialização, conexão e impressão |
| [Desenvolvimento](desenvolvimento.md) | Pré-requisitos, build Ant, execução e testes |
| [Impressão e integração](impressao-e-integracao.md) | Protocolos de impressão, cliente JS e dispositivos |
| [Configuração e segurança](configuracao-e-seguranca.md) | Preferências, portas, diretórios, certificados e limites |
| [Interface e identidade](interface-e-identidade.md) | Marca, fontes, paleta, recursos e comportamento de bandeja |
| [Operação e diagnóstico](operacao-e-diagnostico.md) | Logs, execução local e diagnóstico de problemas |
| [Regras, decisões e glossário](regras-e-decisoes.md) | Princípios do projeto e definições usadas na documentação |

As instruções para contribuições automatizadas e assistidas por agentes estão
em [`../AGENTS.md`](../AGENTS.md).

## Escopo e fonte de verdade

Este repositório é a aplicação Java desktop e seus recursos de build, teste e
empacotamento. Ele não contém o backend nem o frontend web do Meeu Menu. A
integração deve ser confirmada nos contratos do cliente que consome o serviço,
além deste código.

Quando a documentação ficar desatualizada, atualize-a junto com a mudança. Não
use uma página descritiva como autorização para alterar um contrato, controle de
segurança ou comportamento que o código ainda não implementa.
