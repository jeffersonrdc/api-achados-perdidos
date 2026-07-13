# Implementação — Sistema de Achados e Perdidos (Rock in Rio)

Este documento consolida tudo que foi implementado sobre a base inicial, alinhando o
backend à Especificação Funcional. Cada bloco lista a **migração SQL**, as **entidades/serviços**
e os **endpoints**. Migrações ficam em `scripts/` (aplicar em ordem no banco `achados_perdidos`).

> Stack: Spring Boot 4 · Java 25 · Spring Security + JWT · Spring Data JPA · MySQL 8.
> IDs opacos assinados (`SignedResourceIdCodec`), soft delete (`FG_Excluido`), `ProblemDetail` para erros.

---

## 1. Motor de workflow do item (migração `018`)

Ciclo de vida do item conforme a seção 11 do documento:

```
Encontrado → Coletado → Aguardando triagem → Em triagem → Em transporte para estoque
→ Em estoque → Com pedido de devolucao → Aguardando retirada → Devolvido → Finalizado
(+ Descartado a partir de vários estados)
```

- **`status_item`** remodelado para esse fluxo (mantidos os status de claim).
- Campos novos no **`item`**: `TP_Prioridade` (ALTA/MEDIA/BAIXA), `FG_Sensivel`, `IDR_Subcategoria` (FK→categoria).
- A linha do tempo de status é **controlada pela aplicação** (`WorkflowService`), gravando em `item_historico`
  com usuário e observação. Os gatilhos `TRG_item_ai_auditoria`/`TRG_item_au_status` (que duplicavam
  `item_historico`) foram removidos; os gatilhos de **auditoria** permanecem.
- Item novo nasce **"Coletado"** e já recebe o registro inicial na timeline.

| Endpoint | Descrição |
|----------|-----------|
| `POST /api/v1/workflow/itens/{id}/transicoes` | Transiciona o status (valida o grafo; **409** se inválida) |
| `GET /api/v1/workflow/itens/{id}/transicoes-permitidas` | Próximos status válidos |
| `GET /api/v1/workflow/itens/{id}/status-historico` | Linha do tempo de status |
| `POST /api/v1/workflow/movimentacoes` | Movimentação física entre localizações |

---

## 2. Triagem (migração `019`)

Etapa de triagem (seção 6): classificação, tags, observações, sugestão por IA (stub) e
localização física inicial. Tabela **`triagem`** (1:1 com item).

| Endpoint | Descrição |
|----------|-----------|
| `GET /api/v1/triagem?idEvento=` | Fila (Aguardando/Em triagem) |
| `GET /api/v1/triagem/itens/{idItem}` | Detalhe da triagem |
| `GET /api/v1/triagem/itens/{idItem}/sugestao-ia` | Sugestão automática (stub) |
| `POST /api/v1/triagem/itens/{idItem}/iniciar` | → **Em triagem** |
| `PUT /api/v1/triagem/itens/{idItem}` | Salva classificação/observações |
| `POST /api/v1/triagem/itens/{idItem}/concluir` | Aplica classificação + → **Em transporte para estoque** |

---

## 3. Cadastro de Locais (migração `019`, seção 9)

Tabela **`local`** (local de achado, posto de coleta, depósito, atendimento, operacional),
com responsável, coordenadas, horário. CRUD em `/api/v1/locais`.

- `TP_Local`: `ACHADO | COLETA | DEPOSITO | ATENDIMENTO | OPERACIONAL`.

---

## 4. Cadastro de Equipes (migração `019`, seção 10)

Tabelas **`equipe`** e **`equipe_usuario`** (membros). CRUD em `/api/v1/equipes` +
`POST/DELETE /api/v1/equipes/{id}/membros`.

- `TP_Equipe`: `COLETA | TRIAGEM | ESTOQUE | ATENDIMENTO | SUPERVISAO | ADMINISTRACAO`.

---

## 5. Impressão de etiqueta Bluetooth (migração `020`, seção 5)

Tabela **`etiqueta_impressao`** — registro de impressão/reimpressão na linha do tempo de etiquetas.

| Endpoint | Descrição |
|----------|-----------|
| `GET /api/v1/itens/{idItem}/etiqueta` | Conteúdo p/ impressora (protocolo, QR, prioridade, impressora sugerida) |
| `POST /api/v1/itens/{idItem}/etiqueta/imprimir` | Registra IMPRESSAO ou REIMPRESSAO |
| `GET /api/v1/itens/{idItem}/etiqueta/impressoes` | Histórico de impressões |

**Integração de fechamento:** ao concluir uma devolução (`POST /api/v1/devolucoes` com
`fgConcluido=true`), o item transiciona para **Devolvido** de forma *guarded* (só se o status atual permitir).

---

## 6. Banco: auditoria, FKs e auditoria de login (migração `021`)

- Gatilhos de **auditoria** (`_ai_audit`/`_au_audit`) e **bloqueio de exclusão física**
  (`_bd_softdelete`) para `local`, `equipe`, `equipe_usuario`, `triagem`, `etiqueta_impressao`.
- FKs de `IDR_UsuarioCadastro`/`IDR_UsuarioAlteracao` nessas tabelas.
- **Auditoria de login:** a tabela `login_log` (antes órfã) passa a ser gravada no login,
  com IP, dispositivo e navegador (`AuthService` + `LoginLogService`, na mesma transação da autenticação).

---

## 7. Relatórios e Analytics

Expõem as **views** já existentes no banco + contadores das telas.

| Endpoint | Fonte |
|----------|-------|
| `GET /api/v1/relatorios/itens-por-categoria?idEvento=` | `VW_Itens_Categoria` |
| `GET /api/v1/relatorios/itens-pendentes?idEvento=` | `VW_Itens_Pendentes` |
| `GET /api/v1/relatorios/itens-devolvidos?idEvento=` | `VW_Itens_Devolvidos` |
| `GET /api/v1/relatorios/itens-por-localizacao` | `VW_Itens_Localizacao` |
| `GET /api/v1/relatorios/tempo-devolucao?idEvento=` | `VW_Tempo_Devolucao` |
| `GET /api/v1/relatorios/claims-abertos?idEvento=` | `VW_Claims_Abertos` |
| `GET /api/v1/relatorios/sla-estourado?idEvento=` | `VW_Sla_Estourado` |
| `GET /api/v1/relatorios/auditoria?idEvento=` | `VW_Auditoria_Evento` |
| `GET /api/v1/analytics/eventos/{idEvento}/resumo` | Contadores por status (Telas 2/4/5) |

---

## 8. Permissionamento (migração `022`)

Modelo **permissão = `modulo.acao`** (ex.: `item.criar`). 65 permissões cobrindo todos os módulos.

- Perfil agrupa permissões (`perfil_permissao`); usuário herda do perfil **e** pode ter
  permissões **adicionais** (`usuario_permissao`). Efetivas = perfil ∪ extras.
- **Enforcement** por método: `@PreAuthorize("@authz.pode('modulo.acao')")`. **ROLE_ADMIN passa em tudo**
  (override anti-lockout). `SecurityConfig` interno exige apenas autenticação; o gate fino é por permissão.
- Authorities no login: `ROLE_<perfil>` + permissões efetivas. **Perfis podem ser criados em runtime.**
- Mudanças de permissão passam a valer no **próximo login** (JWT stateless).

| Endpoint | Descrição |
|----------|-----------|
| `GET /api/v1/permissoes` | Catálogo de permissões (por módulo/ação) |
| `GET/POST /api/v1/perfis`, `GET/PUT/DELETE /api/v1/perfis/{id}` | CRUD de perfil |
| `GET/PUT /api/v1/perfis/{id}/permissoes` | Configura permissões do perfil |
| `GET/PUT /api/v1/usuarios/{id}/permissoes` | Permissões adicionais do usuário |
| `GET /api/v1/usuarios/{id}/permissoes-efetivas` | Permissões efetivas (perfil ∪ extras) |

---

## Ordem das migrações

```
018_Fluxo_Processos_Documento.sql
019_Locais_Equipes_Triagem.sql
020_Etiqueta_Impressao.sql
021_Auditoria_FKs_Novas_Tabelas.sql
022_Permissionamento.sql
```

Todas idempotentes onde possível; `ALTER TABLE ... ADD` e `ADD CONSTRAINT` executam uma única vez.

## Testes

`mvn test` → suíte completa (contexto H2 + integração MySQL `achados_perdidos_it`): **7/7**.
