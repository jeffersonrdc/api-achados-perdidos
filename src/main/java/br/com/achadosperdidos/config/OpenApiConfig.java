package br.com.achadosperdidos.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.method.HandlerMethod;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_SCHEME = "bearerAuth";

    private static final Pattern AUTHZ_PODE = Pattern.compile("@authz\\.pode\\('([^']+)'\\)");
    private static final Pattern HAS_ROLE = Pattern.compile("hasRole\\('([^']+)'\\)");

    private static final String API_DESCRIPTION = """
            API REST para gestão operacional de **Achados e Perdidos** em eventos \
            (coleta, triagem, estoque, claims, devoluções, portal do participante e analytics).

            ## Autenticação
            1. `POST /api/v1/auth/login` — retorna `accessToken` + `refreshToken` (`tipoToken: Bearer`).
            2. Envie o access token no header: `Authorization: Bearer <accessToken>`.
            3. `POST /api/v1/auth/refresh` — rotaciona o refresh token (o anterior é revogado).
            4. `POST /api/v1/auth/logout` — revoga o refresh token informado.

            Access tokens carregam claim `typ=access`; refresh tokens usam `typ=refresh` e `jti` persistido.

            ## IDs assinados (anti-enumeração)
            Quase todos os identificadores de recurso na API são tokens opacos no formato \
            `s2.<payloadBase64Url>.<hmacBase64Url>` (HMAC-SHA256), **não** IDs numéricos sequenciais.

            - Gerados pela API nas respostas (`encode`).
            - Validados na entrada (`decode`) — assinatura inválida → `400` *"Token de ID adulterado."*
            - Trocar `RESOURCE_ID_SECRET` invalida todos os IDs já emitidos (refaça login / recarregue listagens).

            ## Autorização (RBAC)
            Além de autenticação JWT, endpoints administrativos exigem permissão no formato \
            `modulo.acao` (ex.: `item.criar`, `claim.validar`), avaliada por `@PreAuthorize("@authz.pode(...)")`. \
            Usuários com `ROLE_ADMIN` têm bypass. No portal, rotas autenticadas exigem `ROLE_PARTICIPANTE`.

            ## Erros (RFC 7807 — Problem Details)
            Respostas de erro usam `application/problem+json` (`ProblemDetail`): `type`, `title`, `status`, `detail` \
            e propriedades extras quando aplicável (ex.: `retryAfterSeconds` no HTTP 429).

            | Status | Situação típica |
            |--------|-----------------|
            | 400 | Validação, JSON inválido, ID assinado inválido/adulterado |
            | 401 | Token ausente/inválido ou credenciais incorretas |
            | 403 | Sem permissão ou portal indisponível |
            | 404 | Recurso não encontrado |
            | 409 | Conflito de dados / transição de status inválida / e-mail em uso |
            | 429 | Rate limit de login (IP ou conta) |

            ## Segurança — OWASP Top 10 (implementações nesta API)

            | OWASP | Controles implementados |
            |-------|-------------------------|
            | **A01 Broken Access Control** | Spring Security + `@PreAuthorize` por permissão `modulo.acao`; regras específicas do portal (`PARTICIPANTE`); 401/403 JSON padronizados |
            | **A02 Cryptographic Failures** | JWT HS256 com segredo obrigatório (≥32 chars, fail-fast); senhas BCrypt; IDs HMAC-SHA256; comparação timing-safe |
            | **A03 Injection** | JPA/JPQL e SQL nativo apenas com parâmetros nomeados; Bean Validation; rejeição de propriedades JSON desconhecidas |
            | **A04 Insecure Design** | Rate limit de login (Bucket4j); workflow de claims/itens com validação de transição; refresh com rotação/revogação; IDs opacos |
            | **A05 Security Misconfiguration** | CORS configurável; CSRF desabilitado (API stateless JWT); Swagger desligável (`SPRINGDOC_ENABLED=false`); multipart limitado; `open-in-view=false` |
            | **A06 Vulnerable Components** | Dependências pinadas; scan Trivy (HIGH/CRITICAL) no CI |
            | **A07 Auth Failures** | Mensagem genérica de credenciais; rate limit por IP e conta antes da autenticação; access/refresh separados; logout revoga refresh; usuário inativo rejeitado |
            | **A08 Software/Data Integrity** | IDs assinados anti-tampering; refresh rastreável por `jti`; upload com UUID e path normalizado |
            | **A09 Logging & Monitoring** | Auditoria via contexto de sessão MySQL + consulta `/api/v1/auditoria`; log de login (IP/dispositivo); histórico de claims |
            | **A10 SSRF** | Sem fetch de URLs arbitrárias do cliente nesta API |

            ## Ambientes
            Em produção, desabilite a documentação com `SPRINGDOC_ENABLED=false`.
            """;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Achados e Perdidos")
                        .description(API_DESCRIPTION)
                        .version("v1")
                        .contact(new Contact()
                                .name("Equipe Achados e Perdidos")
                                .email("achadosperdidos@mastersdevs.com.br"))
                        .license(new License()
                                .name("Uso interno / proprietário")
                                .url("https://mastersdevs.com.br")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local"),
                        new Server().url("/").description("Servidor atual (relativo)")))
                .tags(apiTags())
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, bearerScheme())
                        .addSchemas("ProblemDetail", problemDetailSchema())
                        .addResponses("BadRequest", problemResponse(400, "Requisição inválida",
                                "Token de ID adulterado.", Map.of(
                                        "validacao", "Campos obrigatórios ausentes",
                                        "idAdulterado", "Token de ID adulterado.")))
                        .addResponses("Unauthorized", problemResponse(401, "Não autorizado",
                                "Token JWT ausente ou inválido.", Map.of(
                                        "token", "Token JWT ausente ou inválido.",
                                        "credenciais", "Credenciais inválidas")))
                        .addResponses("Forbidden", problemResponse(403, "Proibido",
                                "Você não tem permissão para acessar este recurso.", Map.of(
                                        "permissao", "Você não tem permissão para acessar este recurso.",
                                        "portal", "Portal indisponível para este evento.")))
                        .addResponses("NotFound", problemResponse(404, "Recurso não encontrado",
                                "Recurso não encontrado.", Map.of()))
                        .addResponses("Conflict", problemResponse(409, "Conflito de dados",
                                "Operação conflita com dados existentes.", Map.of(
                                        "integridade", "Operação conflita com dados existentes.",
                                        "transicao", "Transição de status inválida.",
                                        "email", "E-mail já está em uso.")))
                        .addResponses("TooManyRequests", tooManyRequestsResponse()));
    }

    @Bean
    public OperationCustomizer openApiOperationCustomizer() {
        return (Operation operation, HandlerMethod handlerMethod) -> {
            ensureSummary(operation, handlerMethod);
            enrichWithPermission(operation, handlerMethod);
            enrichSignedIdParameters(operation);
            ensureCommonResponses(operation, handlerMethod);
            clearSecurityForPublicAuth(operation, handlerMethod);
            return operation;
        };
    }

    private static void ensureSummary(Operation operation, HandlerMethod handlerMethod) {
        if (operation.getSummary() != null && !operation.getSummary().isBlank()) {
            return;
        }
        String name = handlerMethod.getMethod().getName();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (i > 0 && Character.isUpperCase(c)) {
                sb.append(' ');
            }
            sb.append(i == 0 ? Character.toUpperCase(c) : c);
        }
        operation.setSummary(sb.toString());
    }

    private static void enrichWithPermission(Operation operation, HandlerMethod handlerMethod) {
        PreAuthorize pre = handlerMethod.getMethodAnnotation(PreAuthorize.class);
        if (pre == null) {
            pre = handlerMethod.getBeanType().getAnnotation(PreAuthorize.class);
        }
        if (pre == null) {
            return;
        }
        String expr = pre.value();
        StringBuilder extra = new StringBuilder();
        Matcher pode = AUTHZ_PODE.matcher(expr);
        if (pode.find()) {
            extra.append("**Permissão exigida:** `").append(pode.group(1)).append("`");
        }
        Matcher role = HAS_ROLE.matcher(expr);
        if (role.find()) {
            if (!extra.isEmpty()) extra.append("  \n");
            extra.append("**Role exigida:** `ROLE_").append(role.group(1)).append("`");
        }
        if (extra.isEmpty()) {
            extra.append("**Autorização:** `").append(expr).append("`");
        }
        String current = operation.getDescription();
        operation.setDescription(current == null || current.isBlank()
                ? extra.toString()
                : current + "\n\n" + extra);
    }

    private static void enrichSignedIdParameters(Operation operation) {
        if (operation.getParameters() == null) {
            return;
        }
        for (Parameter parameter : operation.getParameters()) {
            String name = parameter.getName();
            if (name == null) continue;
            boolean looksLikeSignedId = "id".equals(name)
                    || name.startsWith("id")
                    || name.endsWith("Id")
                    || "idEvento".equals(name)
                    || "idItem".equals(name)
                    || "idDeposito".equals(name);
            if (!looksLikeSignedId || "page".equals(name) || "limit".equals(name)) {
                continue;
            }
            if (parameter.getDescription() == null || parameter.getDescription().isBlank()) {
                parameter.setDescription(
                        "ID assinado do recurso (formato `s2.<payload>.<hmac>`). "
                                + "Não use ID numérico sequencial — obtenha o valor nas listagens da API.");
            }
            parameter.setExample("s2.MXxFVlR8OA.xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
            if (parameter.getSchema() == null) {
                parameter.setSchema(new StringSchema());
            }
        }
    }

    private static void ensureCommonResponses(Operation operation, HandlerMethod handlerMethod) {
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            responses = new ApiResponses();
            operation.setResponses(responses);
        }
        responses.addApiResponse("400", new ApiResponse().$ref("#/components/responses/BadRequest"));
        responses.addApiResponse("401", new ApiResponse().$ref("#/components/responses/Unauthorized"));
        responses.addApiResponse("403", new ApiResponse().$ref("#/components/responses/Forbidden"));
        responses.addApiResponse("404", new ApiResponse().$ref("#/components/responses/NotFound"));
        responses.addApiResponse("409", new ApiResponse().$ref("#/components/responses/Conflict"));

        String className = handlerMethod.getBeanType().getSimpleName();
        if ("AuthController".equals(className)
                && handlerMethod.getMethod().getName().equals("login")) {
            responses.addApiResponse("429", new ApiResponse().$ref("#/components/responses/TooManyRequests"));
        }
    }

    private static void clearSecurityForPublicAuth(Operation operation, HandlerMethod handlerMethod) {
        String className = handlerMethod.getBeanType().getSimpleName();
        if ("AuthController".equals(className)) {
            operation.setSecurity(List.of());
            return;
        }
        if ("PortalController".equals(className)) {
            PreAuthorize pre = handlerMethod.getMethodAnnotation(PreAuthorize.class);
            if (pre == null) {
                operation.setSecurity(List.of());
            }
        }
    }

    private static SecurityScheme bearerScheme() {
        return new SecurityScheme()
                .name(BEARER_SCHEME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("""
                        JWT de acesso (claim `typ=access`).
                        Obtenha via `POST /api/v1/auth/login` ou `POST /api/v1/auth/refresh`.
                        Header: `Authorization: Bearer <accessToken>`.
                        """);
    }

    private static Schema<?> problemDetailSchema() {
        return new ObjectSchema()
                .description("Erro no formato RFC 7807 (Problem Details)")
                .addProperty("type", new StringSchema().example("https://api.achadosperdidos.com/errors/validation"))
                .addProperty("title", new StringSchema().example("Dados inválidos"))
                .addProperty("status", new IntegerSchema().example(400))
                .addProperty("detail", new StringSchema().example("identificador: não deve estar em branco"))
                .addProperty("instance", new StringSchema().example("/api/v1/auth/login"))
                .addProperty("retryAfterSeconds", new IntegerSchema()
                        .description("Presente apenas em HTTP 429")
                        .example(60));
    }

    private static ApiResponse problemResponse(int status, String title, String defaultDetail,
                                               Map<String, String> examples) {
        MediaType mediaType = new MediaType()
                .schema(new Schema<>().$ref("#/components/schemas/ProblemDetail"))
                .example(problemExample(status, title, defaultDetail));
        if (examples != null && !examples.isEmpty()) {
            Map<String, Example> exampleMap = new LinkedHashMap<>();
            examples.forEach((name, detail) ->
                    exampleMap.put(name, new Example().value(problemExample(status, title, detail))));
            mediaType.setExamples(exampleMap);
        }
        return new ApiResponse()
                .description(title + " (HTTP " + status + ")")
                .content(new Content().addMediaType("application/problem+json", mediaType));
    }

    private static ApiResponse tooManyRequestsResponse() {
        Map<String, Object> body = problemExample(429, "Muitas requisições",
                "Muitas tentativas de login. Tente novamente em breve.");
        body.put("retryAfterSeconds", 60);
        return new ApiResponse()
                .description("Rate limit de login (OWASP A07) — IP ou conta bloqueados temporariamente")
                .content(new Content().addMediaType("application/problem+json",
                        new MediaType()
                                .schema(new Schema<>().$ref("#/components/schemas/ProblemDetail"))
                                .example(body)));
    }

    private static Map<String, Object> problemExample(int status, String title, String detail) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", "about:blank");
        map.put("title", title);
        map.put("status", status);
        map.put("detail", detail);
        return map;
    }

    private static List<Tag> apiTags() {
        return List.of(
                tag("Autenticação", "Login, refresh e logout. Endpoints públicos com rate limit no login (A07)."),
                tag("Portal do Participante", "Área pública do evento: catálogo, claims, crianças e registro. "
                        + "`meus-claims` exige ROLE_PARTICIPANTE."),
                tag("Analytics", "Indicadores e evolução por evento. Permissão: `analytics.visualizar`."),
                tag("Arquivos", "Upload/download de anexos (UUID no disco, path normalizado — A08)."),
                tag("Auditoria", "Trilha de alterações (A09). Permissão: `auditoria.consultar`."),
                tag("Campos de Categoria", "Definição de campos dinâmicos por categoria."),
                tag("Campos de Item", "Valores dinâmicos de campos por item."),
                tag("Catálogo", "Cores, marcas e modelos auxiliares."),
                tag("Categorias", "Árvore de categorias de itens."),
                tag("Claims", "Pedidos de devolução e workflow (análise, aprovar, reprovar, solicitar info)."),
                tag("Configuração de E-mail", "SMTP e parâmetros de templates de e-mail do workflow de claims."),
                tag("Contatos", "Contatos vinculados a claims/itens."),
                tag("Crianças", "Cadastro operacional de crianças e responsáveis (backoffice)."),
                tag("Dashboard", "Painéis operacionais e SLA pendente."),
                tag("Depósitos", "Depósitos físicos de armazenamento."),
                tag("Devoluções", "Registro e status de devoluções."),
                tag("Empresas", "Empresas organizadoras vinculadas aos eventos."),
                tag("Equipes", "Equipes e membros por evento/local."),
                tag("Etiqueta", "Geração/consulta de etiqueta do item."),
                tag("Eventos", "CRUD de eventos e configuração operacional."),
                tag("Itens", "Coleta, estoque, movimentação e exclusão lógica de achados."),
                tag("Lacres", "Controle de lacres físicos."),
                tag("Locais", "Locais do evento (pontos de coleta/atendimento)."),
                tag("Localizações", "Endereçamento interno em depósitos."),
                tag("Logs de Acesso", "Eventos de autenticação (login, bloqueio, refresh, logout). Aba Acessos. "
                        + "Permissão: `logs.consultar`."),
                tag("Perfis", "Perfis de acesso e vínculo de permissões."),
                tag("Permissões", "Catálogo de permissões `modulo.acao`."),
                tag("Relatórios", "Relatórios gerenciais. Permissão: `relatorio.visualizar`."),
                tag("SLA", "Regras e pendências de SLA."),
                tag("Status", "Status possíveis de item."),
                tag("Transferências", "Transferência de itens entre depósitos/locais."),
                tag("Triagem", "Fila e conclusão de triagem de itens."),
                tag("Usuários", "Usuários internos, permissões efetivas e CRUD."),
                tag("Validação de Claims", "Registros de validação associados a claims."),
                tag("Workflow", "Movimentações e transições de status de itens.")
        );
    }

    private static Tag tag(String name, String description) {
        return new Tag().name(name).description(description);
    }
}
