package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.AuditoriaFiltrosResponse;
import br.com.achadosperdidos.controller.dto.AuditoriaResponse;
import br.com.achadosperdidos.entity.Auditoria;
import br.com.achadosperdidos.entity.Usuario;
import br.com.achadosperdidos.pagination.ApiPage;
import br.com.achadosperdidos.pagination.PaginationMeta;
import br.com.achadosperdidos.pagination.PaginationParams;
import br.com.achadosperdidos.repository.AuditoriaRepository;
import br.com.achadosperdidos.repository.UsuarioRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuditoriaService {
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final Set<String> CAMPOS_SISTEMA = Set.of(
            "DT_Cadastro", "DT_Alteracao", "IDR_UsuarioCadastro", "IDR_UsuarioAlteracao");

    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final SignedResourceIdCodec idCodec;
    /** Jackson 2 local — Spring Boot 4 não auto-configura ObjectMapper legado. */
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public AuditoriaService(AuditoriaRepository auditoriaRepository, UsuarioRepository usuarioRepository,
                            SignedResourceIdCodec idCodec) {
        this.auditoriaRepository = auditoriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.idCodec = idCodec;
    }

    @Transactional(readOnly = true)
    public ApiPage<AuditoriaResponse> findAll(Integer page, Integer limit,
                                              String nmTabela, String tpAcao,
                                              String idUsuario, String nrIp,
                                              LocalDate dataInicio, LocalDate dataFim) {
        int p = PaginationParams.resolvePage(page);
        int l = PaginationParams.resolveLimit(limit);
        Long usuarioId = StringUtils.hasText(idUsuario) ? idCodec.decodeUsuarioId(idUsuario.trim()) : null;
        LocalDateTime de = dataInicio != null ? dataInicio.atStartOfDay() : null;
        LocalDateTime ate = dataFim != null ? dataFim.atTime(LocalTime.MAX) : null;
        Page<Auditoria> result = auditoriaRepository.buscarFiltrado(
                blankToNull(nmTabela), blankToNull(tpAcao), usuarioId, blankToNull(nrIp),
                de, ate, PageRequest.of(p - 1, l));
        return mapPage(result, p, l);
    }

    @Transactional(readOnly = true)
    public ApiPage<AuditoriaResponse> findByRegistro(String nmTabela, Long idRegistro, Integer page, Integer limit) {
        int p = PaginationParams.resolvePage(page);
        int l = PaginationParams.resolveLimit(limit);
        Page<Auditoria> result = auditoriaRepository.findByNmTabelaAndIdRegistroAndFgExcluidoFalseOrderByDtAuditoriaDesc(
                nmTabela, idRegistro, PageRequest.of(p - 1, l));
        return mapPage(result, p, l);
    }

    @Transactional(readOnly = true)
    public AuditoriaFiltrosResponse filtros(String nmTabela, String idUsuario,
                                            LocalDate dataInicio, LocalDate dataFim) {
        Long usuarioId = StringUtils.hasText(idUsuario) ? idCodec.decodeUsuarioId(idUsuario.trim()) : null;
        LocalDateTime de = dataInicio != null ? dataInicio.atStartOfDay() : null;
        LocalDateTime ate = dataFim != null ? dataFim.atTime(LocalTime.MAX) : null;
        String tabela = blankToNull(nmTabela);

        List<AuditoriaFiltrosResponse.Opcao> modulos = auditoriaRepository.findDistinctTabelas().stream()
                .map(t -> new AuditoriaFiltrosResponse.Opcao(t, labelModulo(t)))
                .sorted(Comparator.comparing(AuditoriaFiltrosResponse.Opcao::label, String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<Long> usuarioIds = auditoriaRepository.findDistinctUsuarioIds();
        Map<Long, Usuario> usuariosMap = usuarioIds.isEmpty()
                ? Map.of()
                : usuarioRepository.findAllById(usuarioIds).stream()
                .collect(Collectors.toMap(Usuario::getId, u -> u, (a, b) -> a));
        List<AuditoriaFiltrosResponse.UsuarioOpcao> usuarios = usuarioIds.stream()
                .map(id -> {
                    Usuario u = usuariosMap.get(id);
                    if (u == null) {
                        return new AuditoriaFiltrosResponse.UsuarioOpcao(
                                idCodec.encodeUsuarioId(id), "Usuário #" + id, null);
                    }
                    return new AuditoriaFiltrosResponse.UsuarioOpcao(
                            idCodec.encodeUsuarioId(id),
                            u.getNmUsuario(),
                            u.getNmLogin());
                })
                .sorted(Comparator.comparing(AuditoriaFiltrosResponse.UsuarioOpcao::nome, String.CASE_INSENSITIVE_ORDER))
                .toList();

        long criacoes = auditoriaRepository.countFiltrado(tabela, "INSERT", usuarioId, de, ate);
        long alteracoes = auditoriaRepository.countFiltrado(tabela, "UPDATE", usuarioId, de, ate);
        long exclusoes = auditoriaRepository.countFiltrado(tabela, "DELETE", usuarioId, de, ate);
        long total = auditoriaRepository.countFiltrado(tabela, null, usuarioId, de, ate);

        return new AuditoriaFiltrosResponse(
                modulos,
                usuarios,
                new AuditoriaFiltrosResponse.Totais(total, criacoes, alteracoes, exclusoes));
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private ApiPage<AuditoriaResponse> mapPage(Page<Auditoria> result, int p, int l) {
        Map<Long, Usuario> usuarios = carregarUsuarios(result.getContent());
        var content = result.getContent().stream().map(a -> toResponse(a, usuarios)).toList();
        return ApiPage.paged(content, new PaginationMeta(p, l, result.getTotalElements(), result.getTotalPages()));
    }

    private Map<Long, Usuario> carregarUsuarios(List<Auditoria> registros) {
        List<Long> ids = registros.stream()
                .map(Auditoria::getIdUsuario)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) return Map.of();
        return usuarioRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Usuario::getId, u -> u, (a, b) -> a));
    }

    private AuditoriaResponse toResponse(Auditoria a, Map<Long, Usuario> usuarios) {
        Long uid = a.getIdUsuario();
        Usuario u = uid != null ? usuarios.get(uid) : null;
        Map<String, Object> antes = parseJson(a.getDsAntes());
        Map<String, Object> depois = parseJson(a.getDsDepois());
        int qtCampos = contarCamposAlterados(antes, depois, a.getTpAcao());
        LocalDateTime dtCriado = extrairData(depois, antes, "DT_Cadastro");
        LocalDateTime dtAtualizado = extrairData(depois, antes, "DT_Alteracao");
        // Fallback: no INSERT a própria auditoria é o momento da criação.
        if (dtCriado == null && "INSERT".equalsIgnoreCase(a.getTpAcao())) {
            dtCriado = a.getDtAuditoria();
        }
        if (dtAtualizado == null && "UPDATE".equalsIgnoreCase(a.getTpAcao())) {
            dtAtualizado = a.getDtAuditoria();
        }
        return new AuditoriaResponse(
                idCodec.encodeAuditoriaId(a.getId()),
                a.getNmTabela(),
                labelModulo(a.getNmTabela()),
                String.valueOf(a.getIdRegistro()),
                a.getTpAcao(),
                a.getDsAntes(),
                a.getDsDepois(),
                uid != null ? idCodec.encodeUsuarioId(uid) : null,
                u != null ? u.getNmUsuario() : null,
                u != null ? u.getNmLogin() : null,
                a.getDtAuditoria(),
                dtCriado,
                dtAtualizado,
                qtCampos,
                montarResumo(a, qtCampos),
                a.getNrIp());
    }

    private String montarResumo(Auditoria a, int qtCampos) {
        String modulo = labelModulo(a.getNmTabela());
        String acao = switch ((a.getTpAcao() == null ? "" : a.getTpAcao()).toUpperCase(Locale.ROOT)) {
            case "INSERT" -> "Criou";
            case "UPDATE" -> "Alterou";
            case "DELETE" -> "Excluiu";
            default -> "Registrou";
        };
        if ("UPDATE".equalsIgnoreCase(a.getTpAcao()) && qtCampos > 0) {
            return acao + " " + qtCampos + " campo(s) em " + modulo + " #" + a.getIdRegistro();
        }
        return acao + " registro #" + a.getIdRegistro() + " em " + modulo;
    }

    private int contarCamposAlterados(Map<String, Object> antes, Map<String, Object> depois, String tpAcao) {
        if ("INSERT".equalsIgnoreCase(tpAcao)) {
            return (int) depois.keySet().stream().filter(k -> !CAMPOS_SISTEMA.contains(k)).count();
        }
        if ("DELETE".equalsIgnoreCase(tpAcao)) {
            return (int) antes.keySet().stream().filter(k -> !CAMPOS_SISTEMA.contains(k)).count();
        }
        Set<String> chaves = new java.util.HashSet<>();
        chaves.addAll(antes.keySet());
        chaves.addAll(depois.keySet());
        int n = 0;
        for (String k : chaves) {
            if (CAMPOS_SISTEMA.contains(k)) continue;
            Object a = antes.get(k);
            Object d = depois.get(k);
            if (!Objects.equals(asTexto(a), asTexto(d))) n++;
        }
        return n;
    }

    private LocalDateTime extrairData(Map<String, Object> primario, Map<String, Object> secundario, String chave) {
        Object v = primario.get(chave);
        if (v == null) v = secundario.get(chave);
        return parseDateTime(v);
    }

    private LocalDateTime parseDateTime(Object v) {
        if (v == null) return null;
        if (v instanceof LocalDateTime ldt) return ldt;
        String s = String.valueOf(v).trim();
        if (s.isEmpty() || "null".equalsIgnoreCase(s)) return null;
        try {
            if (s.contains("T")) return LocalDateTime.parse(s.length() > 19 ? s.substring(0, 19) : s);
            if (s.length() >= 19) return LocalDateTime.parse(s.substring(0, 19), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            if (s.length() == 10) return LocalDate.parse(s).atStartOfDay();
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(s, DT_FMT);
            } catch (DateTimeParseException ignored2) {
                return null;
            }
        }
        return null;
    }

    private Map<String, Object> parseJson(String json) {
        if (!StringUtils.hasText(json)) return Map.of();
        try {
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {});
            return map != null ? map : Map.of();
        } catch (Exception e) {
            return Map.of();
        }
    }

    private static String asTexto(Object v) {
        if (v == null) return "";
        return String.valueOf(v);
    }

    static String labelModulo(String tabela) {
        if (tabela == null || tabela.isBlank()) return "—";
        return switch (tabela.toLowerCase(Locale.ROOT)) {
            case "item" -> "Itens (Coleta)";
            case "item_movimentacao" -> "Movimentações";
            case "claim" -> "Pedidos de devolução";
            case "devolucao" -> "Devoluções";
            case "transferencia" -> "Transferências";
            case "triagem" -> "Triagem";
            case "localizacao", "deposito", "estoque_endereco" -> "Estoque / Localização";
            case "usuario" -> "Usuários";
            case "usuario_permissao" -> "Permissões de usuário";
            case "equipe", "equipe_usuario" -> "Equipes";
            case "local" -> "Locais";
            case "categoria" -> "Categorias";
            case "evento" -> "Eventos";
            case "arquivo" -> "Arquivos";
            case "etiqueta_impressao" -> "Etiquetas";
            case "perfil" -> "Perfis";
            case "permissao" -> "Permissões";
            case "crianca" -> "Crianças (Achados)";
            case "lacre" -> "Lacres";
            case "marca" -> "Marcas";
            case "modelo" -> "Modelos";
            case "cor" -> "Cores";
            case "tag" -> "Tags";
            case "estado" -> "Estados";
            default -> Character.toUpperCase(tabela.charAt(0)) + tabela.substring(1).replace('_', ' ');
        };
    }
}
