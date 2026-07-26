package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.*;
import br.com.achadosperdidos.entity.Perfil;
import br.com.achadosperdidos.entity.Usuario;
import br.com.achadosperdidos.exception.EmailEmUsoException;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.pagination.ApiPage;
import br.com.achadosperdidos.pagination.PaginationMeta;
import br.com.achadosperdidos.pagination.PaginationParams;
import br.com.achadosperdidos.repository.PerfilRepository;
import br.com.achadosperdidos.repository.UsuarioRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class UsuarioService {
    private static final String TP_CADASTRO = "USUARIO_CADASTRO";
    private static final String TP_RESET = "USUARIO_RESET_SENHA";
    private static final String SENHA_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";
    private static final int SENHA_TAMANHO = 12;
    private static final SecureRandom SENHA_RANDOM = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final SignedResourceIdCodec idCodec;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioContextService usuarioContextService;
    private final AuditoriaContextService auditoriaContext;
    private final EmailService emailService;

    public UsuarioService(UsuarioRepository usuarioRepository, PerfilRepository perfilRepository,
                          SignedResourceIdCodec idCodec, PasswordEncoder passwordEncoder,
                          UsuarioContextService usuarioContextService,
                          AuditoriaContextService auditoriaContext,
                          EmailService emailService) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.idCodec = idCodec;
        this.passwordEncoder = passwordEncoder;
        this.usuarioContextService = usuarioContextService;
        this.auditoriaContext = auditoriaContext;
        this.emailService = emailService;
    }

    @Transactional
    public UsuarioResponse create(UsuarioCreateRequest request) {
        auditoriaContext.marcarContexto();
        if (usuarioRepository.findByNmEmail(request.nmEmail().trim()).isPresent()) {
            throw new EmailEmUsoException("E-mail já cadastrado.");
        }
        Usuario admin = usuarioContextService.requireUsuarioLogado();
        Perfil perfil = perfilRepository.findByNmPerfilIgnoreCaseAndFgExcluidoFalse(request.nmPerfil().trim())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Perfil não encontrado."));
        String senhaPlain = request.senha();
        Usuario u = new Usuario();
        u.setEmpresa(admin.getEmpresa());
        u.setPerfil(perfil);
        u.setNmUsuario(request.nmUsuario().trim());
        u.setNmLogin(request.nmLogin().trim());
        u.setNmEmail(request.nmEmail().trim().toLowerCase());
        u.setNmSenha(passwordEncoder.encode(senhaPlain));
        u.setDtCadastro(LocalDateTime.now());
        u.setFgAtivo(true);
        u.setFgExcluido(false);
        Usuario salvo = usuarioRepository.save(u);
        EmailService.Resultado email = tentarEnviarCredenciais(salvo, senhaPlain, false);
        String aviso = email.enviado()
                ? null
                : (email.erro() != null ? email.erro() : "E-mail de credenciais não enviado.");
        return toResponse(salvo, email.enviado(), aviso);
    }

    /**
     * Gera senha aleatória, persiste o hash e envia por e-mail.
     * Se o e-mail falhar, a operação é revertida (usuário não fica sem acesso conhecido).
     */
    @Transactional
    public void resetarSenha(String id) {
        auditoriaContext.marcarContexto();
        Usuario u = findEntity(idCodec.decodeUsuarioId(id));
        String senhaPlain = gerarSenhaAleatoria();
        u.setNmSenha(passwordEncoder.encode(senhaPlain));
        u.setDtAlteracao(LocalDateTime.now());
        usuarioRepository.save(u);
        EmailService.Resultado email = tentarEnviarCredenciais(u, senhaPlain, true);
        if (!email.enviado()) {
            String motivo = email.erro() != null ? email.erro() : "falha desconhecida no SMTP";
            throw new IllegalArgumentException(
                    "A senha não foi alterada porque o e-mail com as credenciais não pôde ser enviado: " + motivo);
        }
    }

    @Transactional(readOnly = true)
    public ApiPage<UsuarioResponse> findAll(Integer page, Integer limit, String q, String nmPerfil, String idPerfil) {
        int p = PaginationParams.resolvePage(page);
        int l = PaginationParams.resolveLimit(limit);
        Long perfilId = (idPerfil != null && !idPerfil.isBlank()) ? idCodec.decodePerfilId(idPerfil) : null;
        Specification<Usuario> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.isFalse(root.get("fgExcluido")));
            if (perfilId != null) ps.add(cb.equal(root.get("perfil").get("id"), perfilId));
            if (nmPerfil != null && !nmPerfil.isBlank()) {
                ps.add(cb.equal(cb.lower(root.get("perfil").get("nmPerfil")), nmPerfil.trim().toLowerCase()));
            }
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("nmUsuario")), like),
                        cb.like(cb.lower(root.get("nmLogin")), like),
                        cb.like(cb.lower(root.get("nmEmail")), like)));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<Usuario> result = usuarioRepository.findAll(spec, PageRequest.of(p - 1, l));
        var content = result.getContent().stream().map(this::toResponse).toList();
        return ApiPage.paged(content, new PaginationMeta(p, l, result.getTotalElements(), result.getTotalPages()));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse findById(String id) {
        return toResponse(findEntity(idCodec.decodeUsuarioId(id)));
    }

    @Transactional
    public UsuarioResponse update(String id, UsuarioUpdateRequest request) {
        auditoriaContext.marcarContexto();
        Usuario u = findEntity(idCodec.decodeUsuarioId(id));
        if (request.nmUsuario() != null) u.setNmUsuario(request.nmUsuario().trim());
        if (request.nmEmail() != null && !request.nmEmail().equalsIgnoreCase(u.getNmEmail())) {
            if (usuarioRepository.findByNmEmail(request.nmEmail().trim()).isPresent()) {
                throw new EmailEmUsoException("E-mail já cadastrado.");
            }
            u.setNmEmail(request.nmEmail().trim().toLowerCase());
        }
        if (request.nmPerfil() != null) {
            u.setPerfil(perfilRepository.findByNmPerfilIgnoreCaseAndFgExcluidoFalse(request.nmPerfil().trim())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Perfil não encontrado.")));
        }
        if (request.fgAtivo() != null) u.setFgAtivo(request.fgAtivo());
        u.setDtAlteracao(LocalDateTime.now());
        return toResponse(usuarioRepository.save(u));
    }

    @Transactional
    public void softDelete(String id) {
        auditoriaContext.marcarContexto();
        Usuario u = findEntity(idCodec.decodeUsuarioId(id));
        u.setFgExcluido(true);
        u.setFgAtivo(false);
        u.setDtAlteracao(LocalDateTime.now());
        usuarioRepository.save(u);
    }

    @Transactional(readOnly = true)
    public UsuarioResumoResponse toResumo(Usuario usuario) {
        return new UsuarioResumoResponse(
                idCodec.encodeUsuarioId(usuario.getId()),
                usuario.getNmUsuario(),
                usuario.getNmEmail(),
                usuario.getNmLogin(),
                usuario.getPerfil().getNmPerfil());
    }

    @Transactional(readOnly = true)
    public UsuarioResumoResponse findResumoByEmail(String email) {
        return usuarioRepository.findWithPerfilByNmEmail(email).map(this::toResumo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
    }

    @Transactional(readOnly = true)
    public UsuarioResumoResponse findResumoByIdentificador(String identificador) {
        return usuarioRepository.findWithPerfilByNmEmail(identificador)
                .or(() -> usuarioRepository.findWithPerfilByNmLogin(identificador))
                .map(this::toResumo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
    }

    /** Tenta enviar credenciais; nunca lança — o caller decide o que fazer com o resultado. */
    private EmailService.Resultado tentarEnviarCredenciais(Usuario u, String senhaPlain, boolean reset) {
        String tpEvento = reset ? TP_RESET : TP_CADASTRO;
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("nomeUsuario", u.getNmUsuario() != null ? u.getNmUsuario() : "");
        vars.put("login", u.getNmLogin() != null ? u.getNmLogin() : "");
        vars.put("email", u.getNmEmail() != null ? u.getNmEmail() : "");
        vars.put("senha", senhaPlain);
        vars.put("ano", String.valueOf(Year.now().getValue()));
        if (reset) {
            vars.put("mensagemCurta", "Sua senha do painel foi redefinida.");
            vars.put("mensagem",
                    "Uma nova senha foi gerada para o seu acesso ao painel administrativo de Achados e Perdidos. "
                            + "Use os dados abaixo para entrar. A senha anterior deixa de valer imediatamente.");
        } else {
            vars.put("mensagemCurta", "Seu acesso ao painel foi criado.");
            vars.put("mensagem",
                    "Seu usuário no painel administrativo de Achados e Perdidos foi criado. "
                            + "Use os dados abaixo para acessar o sistema.");
        }
        return emailService.enviar(tpEvento, u.getNmEmail(), vars);
    }

    static String gerarSenhaAleatoria() {
        StringBuilder sb = new StringBuilder(SENHA_TAMANHO);
        for (int i = 0; i < SENHA_TAMANHO; i++) {
            sb.append(SENHA_CHARS.charAt(SENHA_RANDOM.nextInt(SENHA_CHARS.length())));
        }
        return sb.toString();
    }

    private Usuario findEntity(Long id) {
        return usuarioRepository.findById(id)
                .filter(u -> !Boolean.TRUE.equals(u.getFgExcluido()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
    }

    private UsuarioResponse toResponse(Usuario u) {
        return toResponse(u, null, null);
    }

    private UsuarioResponse toResponse(Usuario u, Boolean emailEnviado, String emailAviso) {
        return new UsuarioResponse(
                idCodec.encodeUsuarioId(u.getId()),
                u.getNmUsuario(),
                u.getNmLogin(),
                u.getNmEmail(),
                u.getPerfil().getNmPerfil(),
                u.getFgAtivo(),
                emailEnviado,
                emailAviso);
    }
}
