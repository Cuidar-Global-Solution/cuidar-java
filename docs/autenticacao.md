# Autenticação do Cuidar+

## Contrato HTTP

Envie `Content-Type: application/json` nas requisições com corpo. Os campos e perfis abaixo usam os nomes exatos da API.

| Rota | Entrada | Sucesso |
| --- | --- | --- |
| `POST /auth/cadastro` | `{ "nome": "...", "email": "...", "senha": "...", "perfil": "FAMILIAR" }` | `201` e a conta |
| `POST /auth/login` | `{ "email": "...", "senha": "..." }` | `200` e `{ "token": "...", "expiraEmSegundos": 28800, "conta": { ... } }` |
| `GET /auth/sessao` | `Authorization: Bearer <token>` | `200` e a conta |
| `POST /auth/logout` | `Authorization: Bearer <token>`, sem corpo | `204` e revogação da sessão no Oracle |

A conta tem a forma `{ "id": 10, "nome": "Maria Exemplo", "email": "maria@example.test", "perfil": "FAMILIAR" }`. Os perfis aceitos são `FAMILIAR`, `PROFISSIONAL` e `ACOMPANHADO`. O perfil autenticado vem de `TB_USUARIO`. O cadastro cria a conta e o login abre a sessão.

O nome é obrigatório e aceita até 100 caracteres. O e-mail é obrigatório, aceita até 120 caracteres e é salvo em minúsculas após a remoção dos espaços nas pontas. A senha de cadastro deve ter ao menos 8 caracteres, com limite de 72 bytes em UTF-8. O banco recebe o hash BCrypt da senha e o hash SHA-256 do token. A API entrega o token no login.

Campos inválidos retornam `400` e e-mail duplicado retorna `409`. Credenciais incorretas, sessão ausente, expirada ou revogada retornam `401`. Uma operação que o perfil não pode realizar retorna `403`. Consultas e alterações com IDs de outra família retornam `404` quando a operação é permitida para o perfil. As listagens retornam somente as pessoas e os registros vinculados à conta. Falhas de persistência retornam `500` com mensagem genérica.

## Integração com o front

1. Configure `VITE_API_BASE_URL=https://cuidar-java.onrender.com` na Vercel.
2. Após o login, guarde o `token` em `sessionStorage` no domínio do front e escolha a navegação com base em `conta.perfil`.
3. Após F5, leia o token da mesma aba e chame `GET /auth/sessao`. Use o perfil retornado para restaurar o dashboard. Envie o mesmo cabeçalho `Authorization: Bearer <token>` em todos os CRUDs.
4. No logout, chame `POST /auth/logout`. Após `204` ou `401`, remova o token da aba. Em caso de erro de rede, ofereça nova tentativa para revogar a sessão no servidor. Ao receber `401` em uma chamada protegida, limpe a sessão local e volte ao login.

Use `credentials: 'omit'` no `fetch`. O front define `Authorization` explicitamente, e o preflight autoriza esse cabeçalho e `Content-Type` apenas nas origens configuradas. A sessão usa Bearer em HTTPS. O navegador pode bloquear cookies de terceiros sem interromper esse fluxo. A expiração é absoluta, com padrão de oito horas. O token em `sessionStorage` sobrevive a F5 na mesma aba e fica acessível ao JavaScript dessa origem. Evite registrar o token em logs e mantenha a proteção contra XSS no front.

## Permissões e provisionamento

- Um familiar pode criar uma pessoa acompanhada. Pessoa e vínculo com a conta autenticada são gravados na mesma transação, com relação `FAMILIAR`, autorização `ATIVO` e permissão `REGISTRAR`.
- Para familiares e profissionais, só vínculos `ATIVO` dão acesso. `VISUALIZAR` permite leitura e `REGISTRAR` permite também criar, atualizar e excluir registros. Apenas familiares com vínculo ativo e permissão `REGISTRAR` podem atualizar ou excluir a pessoa. A regra de área habilitada continua valendo e retorna `409` sem o vínculo de área.
- O perfil `ACOMPANHADO` tem leitura da própria pessoa e dos seus registros via `TB_PESSOA_ACOMPANHADA.CD_USUARIO`.
- O registro permanece associado à pessoa original. A API determina o criador no cadastro e preserva o criador na atualização. O executor pode ser a conta autenticada ou o executor já registrado. Medicamento e consulta devem pertencer à mesma pessoa.
- As áreas formam um catálogo compartilhado, com leitura por qualquer conta autenticada. Para criar, atualizar ou excluir uma área, o ID autenticado precisa estar na lista de gestores configurada no servidor.
- O familiar com vínculo ativo e permissão `REGISTRAR` convida uma conta profissional já cadastrada, escolhe a permissão e pode alterá-la ou revogá-la. O convite começa `PENDENTE`. Só a conta profissional destinatária pode aceitá-lo, tornando-o `ATIVO`. A revogação mantém a linha com estado `REVOGADO`. Um novo convite após revogação recebe outro ID.
- O acesso próprio de uma conta `ACOMPANHADO` depende de provisionamento autorizado de `TB_PESSOA_ACOMPANHADA.CD_USUARIO`, após verificar a identidade da pessoa e o perfil da conta. O modelo atual não registra um desafio de comprovação de identidade para associação por autoatendimento. No cadastro pelo front, omita `usuarioId` ou envie `null`. No PUT, preserve o valor retornado no GET.

## Contratos de cuidado para o front

As rotas abaixo exigem Bearer e retornam `401` sem sessão válida. Envie JSON nas rotas com corpo. IDs ou campos inválidos retornam `400`. As rotas de autenticação estão na tabela inicial deste documento.

| Método e rota | Corpo | Sucesso | Permissão e demais respostas |
| --- | --- | --- | --- |
| `GET /pessoas-acompanhadas` | — | `200` e lista de pessoas, inclusive `[]` | Todos os perfis, com filtro pelo acesso próprio ou vínculo ativo |
| `GET /pessoas-acompanhadas/{id}` | — | `200` e pessoa | Leitura da pessoa. `404` se ausente ou fora do acesso |
| `POST /pessoas-acompanhadas` | `nome`, `dataNascimento`, `tamanhoFonte`, `altoContraste`, `observacao` opcional | `201`, pessoa com `id` e `Location` | Familiar. `403` para outro perfil ou tentativa de indicar `usuarioId` |
| `PUT /pessoas-acompanhadas/{id}` | Campos do cadastro e `usuarioId` atual, quando provisionado | `200` e pessoa | Familiar com vínculo ativo `REGISTRAR`. `403` sem permissão ou por tentativa de alterar a conta. `404` fora do acesso |
| `DELETE /pessoas-acompanhadas/{id}` | — | `204` | Mesma permissão do PUT. `403/404` conforme acesso, `409` se houver dependências |
| `GET /pessoas-acompanhadas/{id}/permissoes` | — | `200` com `{"permissao":"VISUALIZAR"}` ou `REGISTRAR` | Leitura da pessoa. `404` fora do acesso. Para ações de gestão, combine `REGISTRAR` com o perfil `FAMILIAR` |
| `GET /areas-cuidado` e `GET /areas-cuidado/{id}` | — | `200` e lista ou área | Qualquer sessão. `404` para área inexistente |
| `POST /areas-cuidado`, `PUT /areas-cuidado/{id}`, `DELETE /areas-cuidado/{id}` | `nome` e `descricao` opcional no POST/PUT | `201` com área e `Location`, `200` com área ou `204`, respectivamente | Conta gestora do catálogo. `403` sem permissão, `404` se inexistente, `409` em conflito |
| `GET /pessoas-acompanhadas/{id}/areas-cuidado` | — | `200` e lista de `{id,nome,descricao}`, inclusive `[]` | Leitura da pessoa. `404` se ausente ou fora do acesso |
| `POST /pessoas-acompanhadas/{id}/areas-cuidado` | `{"areaId":7}` | `204` | Familiar com vínculo ativo `REGISTRAR`. `403` sem permissão, `404` para pessoa ou área inacessível/inexistente, `409` para duplicidade ou conflito de referência |
| `GET /pessoas-acompanhadas/{id}/registros`, `GET /registros-cuidado` e `GET /registros-cuidado/{id}` | — | `200` e lista ou registro | Leitura da pessoa. Listas filtradas pelo acesso. `404` em consulta específica fora do acesso ou inexistente |
| `POST /pessoas-acompanhadas/{id}/registros` | Dados do registro abaixo | `201`, registro e `Location` | Familiar/profissional com vínculo ativo `REGISTRAR`. `403` sem permissão ou referência vedada, `404` para pessoa fora do acesso ou área inexistente, `409` sem área habilitada |
| `POST /registros-cuidado`, `PUT /registros-cuidado/{id}` | Dados do registro e `pessoaAcompanhadaId` | `201` com registro e `Location`, ou `200` com registro | Mesmas permissões e erros do POST aninhado. PUT também retorna `404` se o registro estiver ausente ou fora do acesso |
| `DELETE /registros-cuidado/{id}` | — | `204` | Familiar/profissional com vínculo ativo `REGISTRAR`. `403` sem permissão, `404` se inacessível ou ausente |
| `GET /pessoas-acompanhadas/{id}/vinculos-profissionais` | — | `200` e lista de vínculos | Familiar com vínculo ativo `REGISTRAR`. `403/404` conforme acesso |
| `POST /pessoas-acompanhadas/{id}/vinculos-profissionais` | `{"email":"profissional@example.test","permissao":"VISUALIZAR"}` | `204`, convite pendente. Recarregue a lista para obter o ID | Mesma permissão da lista. `404` para pessoa fora do acesso ou conta profissional ausente, `409` se já houver vínculo ativo ou pendente |
| `PUT /pessoas-acompanhadas/{id}/vinculos-profissionais/{vinculoId}/permissao` | `{"permissao":"REGISTRAR"}` | `204` | Mesma permissão da lista. Aceita vínculo pendente ou ativo da pessoa. `403/404` conforme acesso |
| `DELETE /pessoas-acompanhadas/{id}/vinculos-profissionais/{vinculoId}` | — | `204`, estado revogado | Mesma permissão da lista. Aceita vínculo pendente ou ativo da pessoa. `403/404` conforme acesso |
| `GET /vinculos-profissionais/meus` | — | `200` e lista dos próprios vínculos, incluindo convites | Só profissional. `403` para outro perfil |
| `POST /vinculos-profissionais/meus/{vinculoId}/aceitar` | — | `204`, estado ativo | Só o profissional destinatário. `403` para outro perfil, `404` para convite alheio, ausente ou fora do estado pendente |

Cada vínculo tem a forma `{id,pessoaAcompanhadaId,profissionalId,nomeProfissional,permissao,autorizacao}`. `permissao` aceita `VISUALIZAR` ou `REGISTRAR` e `autorizacao` é `PENDENTE`, `ATIVO` ou `REVOGADO`. A listagem de vínculos não concede acesso aos dados da pessoa.

Para gravar um registro, envie `areaId`, `titulo`, `status` e ao menos uma data ISO local (`dataPrevista` ou `dataRealizado`). Os demais campos são `detalhe`, `usuarioRealizouId`, `medicamentoId`, `consultaId`, `medida`, `medida2` e `unidade`. Omita `usuarioCriouId` para deixar a API preenchê-lo ou preservá-lo. A área deve estar habilitada para a pessoa.

## Oracle e Render

Em um schema descartável, execute [o schema base](./sql/cuidar-schema.sql) e depois [a migração de autenticação](./sql/cuidar-autenticacao.sql), com **F5** no Oracle SQL Developer. Em um schema com dados, use somente a migração, após uma revisão dos perfis, vínculos, e-mails duplicados após normalização e contas legadas. Contas com senha legada fora do formato BCrypt precisam de redefinição antes de entrar. Planeje a limpeza periódica de sessões expiradas em `TB_SESSAO`.

| Variável no backend | Configuração |
| --- | --- |
| `SPRING_DATASOURCE_URL` | URL JDBC Oracle |
| `SPRING_DATASOURCE_USERNAME` | Usuário Oracle |
| `SPRING_DATASOURCE_PASSWORD` | Senha Oracle |
| `CUIDAR_CORS_ALLOWED_ORIGINS` | `https://cuidar-front.vercel.app` no Render. Para Vite local, inclua a origem exata, por exemplo `http://localhost:5173`. Separe múltiplas origens com vírgula |
| `CUIDAR_AUTH_SESSION_TTL` | Opcional, duração ISO 8601, padrão `PT8H` |
| `CUIDAR_AREA_MANAGER_IDS` | Opcional, IDs numéricos de contas gestoras, separados por vírgula. Vazio desabilita alterações no catálogo |
| `PORT` | Porta que o contêiner usa no Render, com padrão local `8080` |

Valide a migração e o provisionamento em um Oracle de homologação. Com dois familiares fictícios vinculados a pessoas diferentes, verifique cadastro, login, F5, logout, preflight e bloqueio de leitura e escrita cruzadas no navegador entre Vercel e Render. Revise qualquer health check que aponte para um CRUD, pois essas rotas agora exigem autenticação.
