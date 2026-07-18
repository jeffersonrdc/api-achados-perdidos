package br.com.achadosperdidos.service;

import br.com.achadosperdidos.config.TimeConfig;
import br.com.achadosperdidos.controller.dto.AuthEventResponse;
import br.com.achadosperdidos.entity.AuthEvent;
import br.com.achadosperdidos.entity.Usuario;
import br.com.achadosperdidos.pagination.ApiPage;
import br.com.achadosperdidos.pagination.PaginationMeta;
import br.com.achadosperdidos.pagination.PaginationParams;
import br.com.achadosperdidos.repository.AuthEventRepository;
import br.com.achadosperdidos.repository.UsuarioRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Locale;

/**
 * Trilha append-only de eventos de autenticação (A09).
 * Falhas usam {@link Propagation#REQUIRES_NEW} para não serem revertidas com a autenticação.
 */
@Service
public class AuthEventService {

    public static final String LOGIN_SUCESSO = "LOGIN_SUCESSO";
    public static final String LOGIN_CREDENCIAL_INVALIDA = "LOGIN_CREDENCIAL_INVALIDA";
    public static final String LOGIN_RATE_LIMIT_IP = "LOGIN_RATE_LIMIT_IP";
    public static final String LOGIN_RATE_LIMIT_CONTA = "LOGIN_RATE_LIMIT_CONTA";
    public static final String REFRESH_SUCESSO = "REFRESH_SUCESSO";
    public static final String REFRESH_INVALIDO = "REFRESH_INVALIDO";
    public static final String LOGOUT = "LOGOUT";

    public static final String RESULTADO_SUCESSO = "SUCESSO";
    public static final String RESULTADO_FALHA = "FALHA";
    public static final String RESULTADO_BLOQUEIO = "BLOQUEIO";

    private static final Logger log = LoggerFactory.getLogger(AuthEventService.class);

    private final AuthEventRepository authEventRepository;
    private final UsuarioRepository usuarioRepository;
    private final SignedResourceIdCodec idCodec;

    public AuthEventService(AuthEventRepository authEventRepository,
                            UsuarioRepository usuarioRepository,
                            SignedResourceIdCodec idCodec) {
        this.authEventRepository = authEventRepository;
        this.usuarioRepository = usuarioRepository;
        this.idCodec = idCodec;
    }

    /** Grava em transação própria — seguro para falhas de login / rate limit. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarIndependente(String tpEvento, String tpResultado, String cdMotivo,
                                      Long usuarioId, String identificador,
                                      String ip, String dispositivo, String navegador) {
        try {
            salvar(tpEvento, tpResultado, cdMotivo, usuarioId, identificador, ip, dispositivo, navegador);
        } catch (RuntimeException ex) {
            log.warn("Falha ao registrar auth_event {}: {}", tpEvento, ex.getMessage());
        }
    }

    /** Grava na mesma transação do chamador (login sucesso / logout / refresh). */
    @Transactional
    public void registrar(String tpEvento, String tpResultado, String cdMotivo,
                          Long usuarioId, String identificador,
                          String ip, String dispositivo, String navegador) {
        try {
            salvar(tpEvento, tpResultado, cdMotivo, usuarioId, identificador, ip, dispositivo, navegador);
        } catch (RuntimeException ex) {
            log.warn("Falha ao registrar auth_event {}: {}", tpEvento, ex.getMessage());
        }
    }

    private void salvar(String tpEvento, String tpResultado, String cdMotivo,
                        Long usuarioId, String identificador,
                        String ip, String dispositivo, String navegador) {
        AuthEvent e = new AuthEvent();
        if (usuarioId != null) {
            e.setUsuario(usuarioRepository.getReferenceById(usuarioId));
        }
        e.setTpEvento(tpEvento);
        e.setTpResultado(tpResultado);
        e.setCdMotivo(cdMotivo);
        e.setDsIdentificadorMascarado(mascararIdentificador(identificador));
        e.setNrIp(ip);
        e.setNmDispositivo(truncar(dispositivo, 150));
        e.setNmNavegador(truncar(navegador, 150));
        e.setDtEvento(TimeConfig.now());
        e.setFgExcluido(false);
        authEventRepository.save(e);
    }

    @Transactional(readOnly = true)
    public ApiPage<AuthEventResponse> listar(Integer page, Integer limit,
                                             String tpEvento, String tpResultado,
                                             String idUsuario, String nrIp,
                                             LocalDate dataInicio, LocalDate dataFim) {
        int p = PaginationParams.resolvePage(page);
        int l = PaginationParams.resolveLimit(limit);
        Long usuarioId = StringUtils.hasText(idUsuario) ? idCodec.decodeUsuarioId(idUsuario.trim()) : null;
        LocalDateTime de = dataInicio != null ? dataInicio.atStartOfDay() : null;
        LocalDateTime ate = dataFim != null ? dataFim.atTime(LocalTime.MAX) : null;
        Page<AuthEvent> result = authEventRepository.buscarFiltrado(
                blankToNull(tpEvento), blankToNull(tpResultado), usuarioId, blankToNull(nrIp),
                de, ate, PageRequest.of(p - 1, l));
        var content = result.getContent().stream().map(this::toResponse).toList();
        return ApiPage.paged(content, new PaginationMeta(p, l, result.getTotalElements(), result.getTotalPages()));
    }

    private AuthEventResponse toResponse(AuthEvent e) {
        Usuario u = e.getUsuario();
        return new AuthEventResponse(
                idCodec.encodeAuthEventId(e.getId()),
                e.getTpEvento(),
                e.getTpResultado(),
                e.getCdMotivo(),
                u != null ? idCodec.encodeUsuarioId(u.getId()) : null,
                u != null ? u.getNmUsuario() : null,
                e.getDsIdentificadorMascarado(),
                e.getNrIp(),
                e.getNmDispositivo(),
                e.getNmNavegador(),
                e.getDtEvento());
    }

    static String mascararIdentificador(String identificador) {
        if (identificador == null || identificador.isBlank()) return null;
        String v = identificador.trim();
        int at = v.indexOf('@');
        if (at > 0) {
            String local = v.substring(0, at);
            String dominio = v.substring(at);
            String prefixo = local.length() <= 2 ? local.charAt(0) + "*" : local.substring(0, 2) + "***";
            return prefixo + dominio;
        }
        if (v.length() <= 2) return v.charAt(0) + "*";
        return v.substring(0, 2) + "***";
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String truncar(String v, int max) {
        if (v == null) return null;
        String t = v.trim();
        return t.length() > max ? t.substring(0, max) : t;
    }

    public static String normalizarIdentificador(String identificador) {
        return identificador == null ? null : identificador.trim().toLowerCase(Locale.ROOT);
    }
}
