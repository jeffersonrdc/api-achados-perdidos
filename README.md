# API Achados e Perdidos

API REST Spring Boot para o módulo **Achados e Perdidos**, seguindo os padrões do projeto `api-sqdg`.

## Stack

- Java 25
- Spring Boot 4.0.2
- Spring Security + JWT
- Spring Data JPA
- MySQL 8 (`achados_perdidos`)
- OpenAPI / Swagger

## Estrutura

```
br.com.achadosperdidos
├── config/          Security, CORS, OpenAPI, exceções
├── controller/      REST + DTOs (records)
├── entity/          JPA mapeando colunas ID_, NM_, FG_, DT_
├── repository/      Spring Data JPA
├── service/         Regras de negócio
├── security/        JWT + IDs assinados
├── pagination/      ApiPage
└── exception/
```

## Configuração local

1. Banco `achados_perdidos` instalado (scripts em `~/Projects/AchadosPerdidos`)
2. Copie `application-local.properties.example` → `application-local.properties`
3. Execute:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Swagger: http://localhost:8080/swagger-ui.html

## Autenticação

```http
POST /api/v1/auth/login
{ "identificador": "admin", "senha": "..." }
```

Perfis → roles Spring: `Administrador` → `ROLE_ADMIN`, `Operador` → `ROLE_OPERADOR`, etc.

> Atualize a senha do usuário `admin` com BCrypt antes do primeiro login:
> `UPDATE usuario SET NM_Senha = '$2a$10$...' WHERE NM_Login = 'admin';`

## Endpoints principais

| Módulo | Path |
|--------|------|
| Auth | `/api/v1/auth` |
| Eventos | `/api/v1/eventos` |
| Categorias | `/api/v1/categorias` |
| Status | `/api/v1/status-itens` |
| Itens | `/api/v1/itens` |
| Claims | `/api/v1/claims` |
| Depósitos | `/api/v1/depositos` |
| Localizações | `/api/v1/localizacoes` |
| Devoluções | `/api/v1/devolucoes` |
| Crianças | `/api/v1/criancas` |
| Arquivos | `/api/v1/arquivos` |
| Workflow | `/api/v1/workflow` (transições, movimentações, timeline) |
| Triagem | `/api/v1/triagem` |
| Etiqueta | `/api/v1/itens/{id}/etiqueta` (conteúdo, imprimir, impressões) |
| Locais | `/api/v1/locais` |
| Equipes | `/api/v1/equipes` (+ `/{id}/membros`) |
| SLA | `/api/v1/sla` |
| Usuários | `/api/v1/usuarios` (+ `/{id}/permissoes`, `/permissoes-efetivas`) |
| Perfis | `/api/v1/perfis` (+ `/{id}/permissoes`) |
| Permissões | `/api/v1/permissoes` |
| Relatórios | `/api/v1/relatorios/*` |
| Analytics | `/api/v1/analytics/eventos/{id}/resumo` |
| Campos dinâmicos | `/api/v1/categorias/campos`, `/api/v1/itens/campos` |
| Auditoria | `/api/v1/auditoria` |
| Validação claims | `/api/v1/claims/validacoes` |
| Contatos | `/api/v1/contatos` |
| Lacres | `/api/v1/lacres` |
| Config. evento | `/api/v1/eventos/{id}/configuracao` |
| Dashboard | `/api/v1/dashboard/eventos`, `/api/v1/dashboard/sla/*` |
| **Portal participante** | `/api/v1/portal/**` (ver `docs/ARQUITETURA_PORTAL.md`) |

> Detalhes completos da implementação (workflow, triagem, locais/equipes, etiqueta, relatórios/analytics e **permissionamento por módulo+ação**) em **[docs/IMPLEMENTACAO.md](docs/IMPLEMENTACAO.md)**.

### Permissionamento

Segurança por **permissão `modulo.acao`** (ex.: `item.criar`). Perfil agrupa permissões; usuário pode ter extras. Enforcement via `@PreAuthorize("@authz.pode('modulo.acao')")` — `ROLE_ADMIN` tem override. Ver `docs/IMPLEMENTACAO.md` §8.

## Portal do participante

Rotas públicas para quem está no evento: consulta de achados, registro de objeto perdido, claim em item da lista, cadastro de crianças/responsáveis e registro de conta (`Participante`).

Documentação completa: [docs/ARQUITETURA_PORTAL.md](docs/ARQUITETURA_PORTAL.md)

```http
GET  /api/v1/portal/eventos
GET  /api/v1/portal/eventos/{id}/itens
POST /api/v1/portal/eventos/{id}/claims
POST /api/v1/portal/eventos/{id}/claims/item
POST /api/v1/portal/auth/registro
```

Login do participante: `POST /api/v1/auth/login` (mesmo endpoint dos operadores).

## Padrões (iguais ao api-sqdg)

- Rotas versionadas `/api/v1/`
- DTOs como Java records com `@Valid`
- IDs opacos assinados na API (`SignedResourceIdCodec`)
- Soft delete (`FG_Excluido`, `FG_Ativo`)
- `ProblemDetail` para erros
- `ddl-auto=validate` em produção
- Paginação `ApiPage` (page 1-based)

## Testes

```bash
mvn test
```

- **Unitário/contexto**: `AchadosPerdidosApplicationTests` (H2 em memória).
- **Integração** (`AuthIntegrationTest`, `FluxoPrincipalIntegrationTest`): exigem MySQL em `localhost:3306` (banco `achados_perdidos_it`, schema via `ddl-auto=create`).

Variáveis opcionais:

| Variável | Padrão |
|----------|--------|
| `TEST_DB_URL` | `jdbc:mysql://localhost:3306/achados_perdidos_it?...` |
| `TEST_DB_USER` | `root` |
| `TEST_DB_PASS` | *(vazio)* |

Com Docker, também é possível usar Testcontainers (`testcontainers-mysql` já está no `pom.xml`).


```bash
cp .env.example .env
docker compose up --build
```
# api-achados-perdidos
