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
import org.springdoc.core.models.GroupedOpenApi;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_SCHEME = "bearerAuth";

    private static final Pattern AUTHZ_PODE = Pattern.compile("@authz\\.pode(?:Qualquer)?\\(([^)]+)\\)");
    private static final Pattern PERMISSAO_LITERAL = Pattern.compile("'([^']+)'");
    private static final Pattern HAS_ROLE = Pattern.compile("hasRole\\('([^']+)'\\)");
    private static final java.util.Set<String> NAO_ID_ASSINADO = java.util.Set.of(
            "idRegistro", "nrLacre", "page", "limit", "dias", "nivel");
    private static final Map<String, String> METHOD_SUMMARIES = Map.ofEntries(
            Map.entry("create", "Cadastrar"),
            Map.entry("criar", "Cadastrar"),
            Map.entry("findAll", "Listar"),
            Map.entry("listar", "Listar"),
            Map.entry("findById", "Consultar por ID"),
            Map.entry("detalhar", "Detalhar"),
            Map.entry("update", "Atualizar"),
            Map.entry("atualizar", "Atualizar"),
            Map.entry("delete", "Excluir"),
            Map.entry("excluir", "Excluir"),
            Map.entry("softDelete", "Excluir logicamente"),
            Map.entry("resumo", "Consultar resumo"),
            Map.entry("filtros", "Consultar opções de filtro"),
            Map.entry("findByEvento", "Listar por evento"),
            Map.entry("findByItem", "Listar por item"),
            Map.entry("findByClaim", "Listar por claim"),
            Map.entry("findByEntidade", "Listar por entidade"),
            Map.entry("salvar", "Salvar"),
            Map.entry("testar", "Testar configuração"),
            Map.entry("aprovar", "Aprovar"),
            Map.entry("reprovar", "Reprovar"),
            Map.entry("iniciar", "Iniciar"),
            Map.entry("concluir", "Concluir"),
            Map.entry("historico", "Consultar histórico"),
            Map.entry("mensagens", "Listar mensagens"),
            Map.entry("enviarMensagem", "Enviar mensagem"));

    private static final String API_DESCRIPTION = """
            API REST para gestão operacional de **Achados e Perdidos** em eventos \
            (coleta, triagem, estoque, claims, devoluções, portal do participante e analytics).

            ## Documentações por frontend
            Use o seletor no topo do Swagger UI:

            - **01 — Painel Administrativo:** autenticação e todos os módulos internos consumidos pelo painel.
            - **02 — Portal Público:** catálogo, relatos, solicitações de retirada, anexos e respostas públicas.

            ## Exportar especificação JSON
            Use o botão **Exportar JSON** na barra superior, ou baixe diretamente:

            - [Painel Administrativo](/api-docs/01-painel-administrativo?export=1)
            - [Portal Público](/api-docs/02-portal-publico?export=1)
            - [Spec completa (default)](/api-docs?export=1)

            Cada operação informa resumo, descrição, parâmetros, schemas, segurança, permissão RBAC e respostas de erro.

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
                        .addResponses("PayloadTooLarge", problemResponse(413, "Conteúdo muito grande",
                                "Arquivo ou requisição excede o limite configurado.", Map.of()))
                        .addResponses("UnsupportedMediaType", problemResponse(415, "Tipo de mídia não suportado",
                                "Tipo de arquivo ou Content-Type não permitido.", Map.of()))
                        .addResponses("InternalServerError", problemResponse(500, "Erro interno",
                                "Ocorreu um erro interno inesperado.", Map.of()))
                        .addResponses("TooManyRequests", tooManyRequestsResponse())
                        .addResponses("PublicTooManyRequests", publicTooManyRequestsResponse()));
    }

    @Bean
    public GroupedOpenApi painelAdminOpenApi(OperationCustomizer openApiOperationCustomizer) {
        return GroupedOpenApi.builder()
                .group("01-painel-administrativo")
                .displayName("01 — Painel Administrativo")
                .pathsToMatch("/api/v1/**")
                .pathsToExclude("/api/v1/portal/**")
                .addOperationCustomizer(openApiOperationCustomizer)
                .build();
    }

    @Bean
    public GroupedOpenApi portalPublicoOpenApi(OperationCustomizer openApiOperationCustomizer) {
        return GroupedOpenApi.builder()
                .group("02-portal-publico")
                .displayName("02 — Portal Público")
                .pathsToMatch("/api/v1/portal/**")
                .addOperationCustomizer(openApiOperationCustomizer)
                .build();
    }

    @Bean
    public OperationCustomizer openApiOperationCustomizer() {
        return (Operation operation, HandlerMethod handlerMethod) -> {
            ensureOperationId(operation, handlerMethod);
            ensureSummary(operation, handlerMethod);
            ensureDescription(operation, handlerMethod);
            enrichWithPermission(operation, handlerMethod);
            enrichParameters(operation);
            ensureCommonResponses(operation, handlerMethod);
            ensureSuccessResponses(operation, handlerMethod);
            clearSecurityForPublicAuth(operation, handlerMethod);
            return operation;
        };
    }

    private static void ensureOperationId(Operation operation, HandlerMethod handlerMethod) {
        if (operation.getOperationId() == null || operation.getOperationId().isBlank()) {
            String controller = handlerMethod.getBeanType().getSimpleName().replace("Controller", "");
            operation.setOperationId(lowerFirst(controller) + "_" + handlerMethod.getMethod().getName());
        }
    }

    private static void ensureSummary(Operation operation, HandlerMethod handlerMethod) {
        if (operation.getSummary() != null && !operation.getSummary().isBlank()) {
            return;
        }
        String name = handlerMethod.getMethod().getName();
        String action = METHOD_SUMMARIES.get(name);
        if (action != null) {
            operation.setSummary(action + " — " + controllerTag(handlerMethod));
            return;
        }
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

    private static void ensureDescription(Operation operation, HandlerMethod handlerMethod) {
        if (operation.getDescription() != null && !operation.getDescription().isBlank()) {
            return;
        }
        String tag = controllerTag(handlerMethod);
        Method method = handlerMethod.getMethod();
        String action;
        if (method.isAnnotationPresent(GetMapping.class)) {
            action = "Consulta dados sem alterar o estado do sistema.";
        } else if (method.isAnnotationPresent(PostMapping.class)) {
            action = "Cria um recurso ou executa uma ação de negócio.";
        } else if (method.isAnnotationPresent(PutMapping.class)) {
            action = "Atualiza integralmente os dados informados de forma idempotente.";
        } else if (method.isAnnotationPresent(PatchMapping.class)) {
            action = "Atualiza parcialmente o recurso informado.";
        } else if (method.isAnnotationPresent(DeleteMapping.class)) {
            action = "Remove ou inativa logicamente o recurso, conforme a regra do módulo.";
        } else {
            action = "Executa uma operação de negócio.";
        }
        boolean multipart = isMultipart(method);
        String upload = multipart
                ? "\n\n**Conteúdo:** `multipart/form-data`. Respeite os limites de quantidade, tamanho e MIME descritos nos parâmetros."
                : "";
        operation.setDescription(action
                + "\n\n**Módulo:** " + tag + "."
                + "\n\nIDs de recursos são tokens assinados `s2.*`; use os valores retornados pelas consultas."
                + upload);
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
        java.util.LinkedHashSet<String> permissoes = new java.util.LinkedHashSet<>();
        Matcher pode = AUTHZ_PODE.matcher(expr);
        while (pode.find()) {
            Matcher lit = PERMISSAO_LITERAL.matcher(pode.group(1));
            while (lit.find()) {
                permissoes.add(lit.group(1));
            }
        }
        if (!permissoes.isEmpty()) {
            boolean qualquer = expr.contains("podeQualquer") || expr.contains(" or ");
            extra.append(qualquer ? "**Permissões (qualquer uma):** " : "**Permissão exigida:** ");
            extra.append(permissoes.stream().map(p -> "`" + p + "`").reduce((a, b) -> a + ", " + b).orElse(""));
        }
        Matcher role = HAS_ROLE.matcher(expr);
        java.util.LinkedHashSet<String> roles = new java.util.LinkedHashSet<>();
        while (role.find()) {
            roles.add(role.group(1));
        }
        if (!roles.isEmpty()) {
            if (!extra.isEmpty()) extra.append("  \n");
            extra.append("**Role exigida:** ");
            extra.append(roles.stream().map(r -> "`ROLE_" + r + "`").reduce((a, b) -> a + ", " + b).orElse(""));
        }
        if (extra.isEmpty()) {
            extra.append("**Autorização:** `").append(expr).append("`");
        }
        String current = operation.getDescription();
        operation.setDescription(current == null || current.isBlank()
                ? extra.toString()
                : current + "\n\n" + extra);
    }

    private static void enrichParameters(Operation operation) {
        if (operation.getParameters() == null) {
            return;
        }
        for (Parameter parameter : operation.getParameters()) {
            String name = parameter.getName();
            if (name == null) continue;
            enrichKnownParameter(parameter, name);
            if (NAO_ID_ASSINADO.contains(name)) {
                continue;
            }
            boolean looksLikeSignedId = "id".equals(name)
                    || (name.startsWith("id") && name.length() > 2 && Character.isUpperCase(name.charAt(2)))
                    || (name.endsWith("Id") && !name.equals("idRegistro"));
            if (!looksLikeSignedId) {
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

    private static void enrichKnownParameter(Parameter parameter, String name) {
        if (parameter.getDescription() != null && !parameter.getDescription().isBlank()) return;
        switch (name) {
            case "page" -> {
                parameter.setDescription("Página baseada em 1. O padrão é `1`.");
                parameter.setExample(1);
            }
            case "limit" -> {
                parameter.setDescription("Quantidade de registros por página. O padrão é `20`; máximo conforme política da API.");
                parameter.setExample(20);
            }
            case "q", "pesquisa" -> parameter.setDescription(
                    "Texto livre para busca. Espaços nas extremidades são ignorados.");
            case "status" -> parameter.setDescription("Filtra pelo nome ou código do status.");
            case "data", "inicio", "fim" -> parameter.setDescription("Data no formato ISO-8601 (`yyyy-MM-dd`).");
            case "tipo" -> parameter.setDescription("Tipo de registro aceito pelo domínio do endpoint.");
            case "fgAtivo", "incluirInativos" -> parameter.setDescription(
                    "Quando informado, inclui ou filtra recursos inativos/ativos.");
            case "idRegistro" -> parameter.setDescription(
                    "Identificador interno numérico do registro auditado (não é token `s2.*`).");
            case "nrLacre" -> parameter.setDescription("Número físico do lacre (texto livre, não é ID assinado).");
            case "nivel" -> parameter.setDescription(
                    "Nível de endereçamento: `SETOR`, `ESTANTE`, `PRATELEIRA`, `CAIXA` ou `POSICAO`.");
            case "provider" -> parameter.setDescription("Provedor de armazenamento a testar: `LOCAL` ou `S3`.");
            default -> {
                // IDs e parâmetros específicos são tratados pelas anotações do controller ou abaixo.
            }
        }
    }

    private static void ensureCommonResponses(Operation operation, HandlerMethod handlerMethod) {
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            responses = new ApiResponses();
            operation.setResponses(responses);
        }
        responses.putIfAbsent("400", new ApiResponse().$ref("#/components/responses/BadRequest"));
        boolean publicEndpoint = isPublicEndpoint(handlerMethod);
        Method method = handlerMethod.getMethod();
        if (!publicEndpoint) {
            responses.putIfAbsent("401", new ApiResponse().$ref("#/components/responses/Unauthorized"));
            responses.putIfAbsent("403", new ApiResponse().$ref("#/components/responses/Forbidden"));
        } else if (isPortalController(handlerMethod.getBeanType().getSimpleName())) {
            responses.putIfAbsent("403", new ApiResponse().$ref("#/components/responses/Forbidden"));
        }
        boolean hasPathId = java.util.Arrays.stream(method.getParameters())
                .anyMatch(p -> p.isAnnotationPresent(org.springframework.web.bind.annotation.PathVariable.class));
        if (hasPathId || method.isAnnotationPresent(PutMapping.class)
                || method.isAnnotationPresent(DeleteMapping.class)
                || method.isAnnotationPresent(PatchMapping.class)) {
            responses.putIfAbsent("404", new ApiResponse().$ref("#/components/responses/NotFound"));
        }
        if (method.isAnnotationPresent(PostMapping.class)
                || method.isAnnotationPresent(PutMapping.class)
                || method.isAnnotationPresent(PatchMapping.class)
                || method.isAnnotationPresent(DeleteMapping.class)) {
            responses.putIfAbsent("409", new ApiResponse().$ref("#/components/responses/Conflict"));
        }
        responses.putIfAbsent("500", new ApiResponse().$ref("#/components/responses/InternalServerError"));

        String className = handlerMethod.getBeanType().getSimpleName();
        if ("AuthController".equals(className)
                && handlerMethod.getMethod().getName().equals("login")) {
            responses.putIfAbsent("429", new ApiResponse().$ref("#/components/responses/TooManyRequests"));
        }
        if (isPortalController(className)) {
            responses.putIfAbsent("429", new ApiResponse().$ref("#/components/responses/PublicTooManyRequests"));
        }
        if (isMultipart(method)) {
            responses.putIfAbsent("413", new ApiResponse().$ref("#/components/responses/PayloadTooLarge"));
            responses.putIfAbsent("415", new ApiResponse().$ref("#/components/responses/UnsupportedMediaType"));
        }
        if ("PortalController".equals(className)
                && "enviarResposta".equals(method.getName())) {
            responses.putIfAbsent("410", new ApiResponse()
                    .description("Token de resposta expirado ou já utilizado."));
        }
    }

    private static void ensureSuccessResponses(Operation operation, HandlerMethod handlerMethod) {
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            responses = new ApiResponses();
            operation.setResponses(responses);
        }
        Method method = handlerMethod.getMethod();
        Class<?> returnType = method.getReturnType();
        boolean isVoid = returnType.equals(Void.TYPE) || returnType.equals(Void.class)
                || (ResponseEntity.class.isAssignableFrom(returnType)
                && method.getGenericReturnType().getTypeName().contains("Void"));
        if (method.isAnnotationPresent(DeleteMapping.class) || isVoid) {
            responses.putIfAbsent("204", new ApiResponse().description("Operação concluída sem corpo de resposta."));
            return;
        }
        if (method.isAnnotationPresent(PostMapping.class)) {
            responses.putIfAbsent("201", new ApiResponse().description("Recurso criado com sucesso."));
            responses.putIfAbsent("200", new ApiResponse().description("Operação concluída com sucesso."));
            return;
        }
        responses.putIfAbsent("200", new ApiResponse().description("Consulta ou atualização concluída com sucesso."));
    }

    private static void clearSecurityForPublicAuth(Operation operation, HandlerMethod handlerMethod) {
        String className = handlerMethod.getBeanType().getSimpleName();
        if ("AuthController".equals(className)) {
            operation.setSecurity(List.of());
            return;
        }
        if (isPortalController(className)) {
            PreAuthorize pre = handlerMethod.getMethodAnnotation(PreAuthorize.class);
            if (pre == null) {
                operation.setSecurity(List.of());
            }
        }
    }

    private static boolean isPublicEndpoint(HandlerMethod handlerMethod) {
        String className = handlerMethod.getBeanType().getSimpleName();
        if ("AuthController".equals(className)) return true;
        if (!isPortalController(className)) return false;
        return handlerMethod.getMethodAnnotation(PreAuthorize.class) == null;
    }

    /** Controllers públicos do portal (`PortalController`, `PortalDevolucaoController`, …). */
    private static boolean isPortalController(String className) {
        return className != null && className.startsWith("Portal");
    }

    private static String controllerTag(HandlerMethod handlerMethod) {
        io.swagger.v3.oas.annotations.tags.Tag annotation =
                handlerMethod.getBeanType().getAnnotation(io.swagger.v3.oas.annotations.tags.Tag.class);
        if (annotation != null && annotation.name() != null && !annotation.name().isBlank()) {
            return annotation.name();
        }
        return handlerMethod.getBeanType().getSimpleName().replace("Controller", "");
    }

    private static String lowerFirst(String value) {
        return value == null || value.isEmpty()
                ? value
                : Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }

    private static boolean isMultipart(Method method) {
        return java.util.Arrays.stream(method.getGenericParameterTypes())
                .anyMatch(type -> type.getTypeName().contains("MultipartFile"));
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

    private static ApiResponse publicTooManyRequestsResponse() {
        Map<String, Object> body = problemExample(429, "Muitas requisições",
                "Limite de requisições públicas excedido. Tente novamente em breve.");
        body.put("retryAfterSeconds", 60);
        return new ApiResponse()
                .description("Proteção antiabuso do portal público por IP e ação")
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
                tag("Configuração de Armazenamento", "Seleção e teste do provedor Local/AWS S3 para novos uploads."),
                tag("Configuração de E-mail", "SMTP e parâmetros de templates de e-mail do workflow de claims."),
                tag("Contatos", "Contatos vinculados a claims/itens."),
                tag("Crianças", "Cadastro operacional de crianças e responsáveis (backoffice)."),
                tag("Dashboard", "Painéis operacionais e SLA pendente."),
                tag("Depósitos", "Depósitos físicos de armazenamento."),
                tag("Devoluções", "Registro e status de devoluções."),
                tag("Empresas", "Empresas organizadoras vinculadas aos eventos."),
                tag("Equipes", "Equipes e membros por evento/local."),
                tag("Estados", "Estados de conservação e apresentação dos itens."),
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
                tag("Tags", "Características pesquisáveis vinculadas às subcategorias."),
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
