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

- Um familiar pode criar uma pessoa acompanhada. Pessoa e vínculo com o familiar autenticado são gravados na mesma transação. Familiares vinculados podem consultar, atualizar e excluir a pessoa.
- Profissionais vinculados podem consultar a pessoa. Familiares e profissionais podem escrever registros das pessoas vinculadas. A regra de área habilitada continua valendo e retorna `409` sem o vínculo de área.
- O perfil `ACOMPANHADO` tem leitura da própria pessoa e dos seus registros via `TB_PESSOA_ACOMPANHADA.CD_USUARIO`.
- O registro permanece associado à pessoa original. A API determina o criador no cadastro e preserva o criador na atualização. O executor pode ser a conta autenticada ou o executor já registrado. Medicamento e consulta devem pertencer à mesma pessoa.
- As áreas formam um catálogo compartilhado, com leitura por qualquer conta autenticada. Para criar, atualizar ou excluir uma área, o ID autenticado precisa estar na lista de gestores configurada no servidor.
- Provisione vínculos de pessoas existentes e acessos de profissionais em `TB_USUARIO_PESSOA_CUIDADO` após verificar quem pode acessá-las. Para vincular a conta da própria pessoa, confirme que ela tem perfil `ACOMPANHADO` e provisione `TB_PESSOA_ACOMPANHADA.CD_USUARIO`. No cadastro pelo front, envie `usuarioId: null`. No PUT, preserve o valor de `usuarioId` retornado no GET.

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
