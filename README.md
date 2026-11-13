<div align="center">

<p align="center">
  <img src="./docs/assets/cuidar-banner.svg" alt="Cuidar+ — Todo cuidado em uma só plataforma. API Java para a rotina de cuidados da pessoa idosa." width="100%">
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-7C3AED?style=flat-square" alt="Java 21">
  <img src="https://img.shields.io/badge/Spring_Boot-4.1.2-8B5CF6?style=flat-square" alt="Spring Boot 4.1.2">
  <img src="https://img.shields.io/badge/Persistência-JDBC-A78BFA?style=flat-square" alt="Persistência com JDBC">
  <img src="https://img.shields.io/badge/Banco-Oracle-C4B5FD?style=flat-square" alt="Banco Oracle">
</p>

<p align="center">
  <a href="#o-projeto">Projeto</a> ·
  <a href="#estado-atual">Estado atual</a> ·
  <a href="#executar">Execução</a> ·
  <a href="#contrato-da-api">API</a> ·
  <a href="#testes">Testes</a> ·
  <a href="#equipe">Equipe</a>
</p>

## O projeto

O **Cuidar+** organiza a rotina de cuidados da pessoa idosa e a colaboração entre familiares e cuidadores. Este repositório contém a API Java da solução, desenvolvida para a **Global Solution 2026/2 da FIAP**, com o tema CitizenTech.

O primeiro recurso implementado é **PessoaIdosa**: cadastro, consulta, atualização e exclusão da pessoa acompanhada, com vínculo opcional à sua própria conta de usuário e preferências de acessibilidade.

## Estado atual

| Implementado | Próximas etapas |
| :---: | :---: |
| CRUD de PessoaIdosa com cinco endpoints | Script de tabelas e sequences |
| DTOs, Bean Validation e ModelMapper | Configuração e validação no Oracle da FIAP |
| DAO com JDBC e parâmetros preparados | Demais entidades e relacionamento 1:N |
| Erros padronizados em JSON | CORS e integração com o front-end |
| Testes HTTP e de acesso a dados com mocks | Docker, Render e documentação de entrega |

> **A API ainda depende da configuração do Oracle para iniciar e atender requisições.** A compilação e os testes específicos abaixo podem ser executados sem banco. O script SQL, as credenciais e a configuração da conexão ainda não fazem parte desta etapa.

## Arquitetura

~~~text
HTTP / JSON
    │
    ▼
Controller ── Mapper / DTOs
    │
    ▼
Service
    │
    ▼
DAO ── JDBC / DataSource ── Oracle
~~~

| Pacote | Responsabilidade |
| :---: | :---: |
| `models` | Entidades do domínio |
| `dto` | Contratos de entrada, saída e erro |
| `mapper` | Conversão entre DTOs e models com ModelMapper |
| `dao` | SQL, `PreparedStatement` e fechamento dos recursos JDBC |
| `service` | Coordenação do CRUD e tratamento de registros inexistentes |
| `controller` | Rotas, validação da entrada e respostas HTTP |
| `exception` / `handler` | Exceções e erros padronizados |
| `config` | Configuração dos beans |

A organização usa o [cineFiap da professora Eliane Marion](https://github.com/ElianeMarion/fiap-ddd-2026/tree/main/cineFiap) como referência. As adaptações para a GS incluem injeção pelo construtor, `DataSource` no DAO, ModelMapper e tratamento global de exceções. O acesso ao banco usa JDBC diretamente.

## Executar

**Pré-requisitos:** JDK 21 e acesso à internet no primeiro uso do Maven Wrapper.

Na raiz do projeto, pelo PowerShell:

~~~powershell
# Compilar sem executar testes
.\mvnw.cmd -DskipTests compile

# Executar somente os testes do CRUD, sem Oracle
.\mvnw.cmd "-Dtest=PessoaIdosaCrudTests,PessoaIdosaDAOTests" test
~~~

Depois da configuração da conexão e da criação das tabelas e sequences:

~~~powershell
.\mvnw.cmd spring-boot:run
~~~

O endereço local padrão será `http://localhost:8080`. A conexão deverá ser fornecida externamente; não inclua usuário ou senha do banco no código versionado.

## Contrato da API

| Método | Rota | Sucesso |
| :---: | :---: | :---: |
| `POST` | `/idosos` | `201 Created` + `Location` + pessoa cadastrada |
| `GET` | `/idosos` | `200 OK` + lista |
| `GET` | `/idosos/{id}` | `200 OK` + pessoa |
| `PUT` | `/idosos/{id}` | `200 OK` + pessoa atualizada |
| `DELETE` | `/idosos/{id}` | `204 No Content` |

**Corpo de entrada para POST e PUT**

~~~json
{
  "usuarioId": null,
  "nome": "Maria Almeida",
  "dataNascimento": "1950-01-02",
  "observacao": null,
  "tamanhoFonte": 18,
  "altoContraste": "S"
}
~~~

O ID é gerado no banco por `SQ_TB_PESSOA_IDOSA` e aparece como `id` na resposta, junto aos campos acima. No PUT, o ID vem da rota e os campos são substituídos pelos valores enviados.

| Campo | Regra |
| :---: | :---: |
| `usuarioId` | Opcional; positivo e com até 10 dígitos. Deve referenciar um usuário existente e não vinculado a outra pessoa idosa |
| `nome` | Obrigatório, não pode conter apenas espaços; até 100 caracteres |
| `dataNascimento` | Obrigatória, no formato `AAAA-MM-DD`; não pode ser futura |
| `observacao` | Opcional; até 300 caracteres |
| `tamanhoFonte` | Obrigatório; de 12 a 32 |
| `altoContraste` | Obrigatório; `S` ou `N` |

**Erros**

`400` para entrada inválida · `404` para recurso inexistente · `405` para método não permitido · `409` para conflito de vínculo ou exclusão bloqueada por registros relacionados. Falhas inesperadas retornam `500` com mensagem genérica.

~~~json
{
  "timestamp": "2026-11-13T23:00:00Z",
  "status": 404,
  "erro": "Not Found",
  "mensagem": "Pessoa idosa não encontrada.",
  "caminho": "/idosos/99",
  "detalhes": []
}
~~~

As respostas de erro não expõem SQL, credenciais ou stack trace.

## Testes

Os **10 testes específicos do CRUD** verificam rotas, status HTTP, `Location`, validação, mapeamento, campos opcionais, tradução de conflitos Oracle e fechamento de recursos JDBC. Utilizam MockMvc e mocks do DAO ou dos objetos JDBC.

**Validação já realizada:** compilação com `release 21` e os 10 testes aprovados. Isso não substitui a integração com Oracle: execução do SQL, geração do ID pelo driver e restrições reais ainda precisam ser verificadas. O teste original `contextLoads` requer a configuração do contexto com banco e não faz parte do comando seletivo acima.

## Equipe

**FIAP · Análise e Desenvolvimento de Sistemas · 1TDSPJ**

| Integrante | RM |
| :---: | :---: |
| Guilherme Almeida | 571713 |
| Leonardo Arnaldo | 573188 |
| Thiago Santa Rosa | 572616 |
| Geovanna Secchi Egea | 573452 |
| Beatriz Urbano M. de Oliveira | 569341 |

<details>
<summary>Uso de IA no desenvolvimento</summary>

O Codex foi utilizado como apoio à configuração do ModelMapper, implementação do CRUD de PessoaIdosa, criação e execução dos testes e elaboração deste README. A compilação e os testes automatizados foram usados para verificar a implementação; a validação no Oracle e a revisão técnica pelo grupo continuam pendentes.

</details>

---

<p align="center"><sub><strong>Cuidar+</strong> · Todo cuidado em uma só plataforma.</sub></p>

</div>
