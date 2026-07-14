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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final SignedResourceIdCodec idCodec;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioContextService usuarioContextService;
    private final AuditoriaContextService auditoriaContext;

    public UsuarioService(UsuarioRepository usuarioRepository, PerfilRepository perfilRepository,
                          SignedResourceIdCodec idCodec, PasswordEncoder passwordEncoder,
                          UsuarioContextService usuarioContextService,
                          AuditoriaContextService auditoriaContext) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.idCodec = idCodec;
        this.passwordEncoder = passwordEncoder;
        this.usuarioContextService = usuarioContextService;
        this.auditoriaContext = auditoriaContext;
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
        Usuario u = new Usuario();
        u.setEmpresa(admin.getEmpresa());
        u.setPerfil(perfil);
        u.setNmUsuario(request.nmUsuario().trim());
        u.setNmLogin(request.nmLogin().trim());
        u.setNmEmail(request.nmEmail().trim().toLowerCase());
        u.setNmSenha(passwordEncoder.encode(request.senha()));
        u.setDtCadastro(LocalDateTime.now());
        u.setFgAtivo(true);
        u.setFgExcluido(false);
        return toResponse(usuarioRepository.save(u));
    }

    @Transactional(readOnly = true)
    public ApiPage<UsuarioResponse> findAll(Integer page, Integer limit) {
        int p = PaginationParams.resolvePage(page);
        int l = PaginationParams.resolveLimit(limit);
        Page<Usuario> result = usuarioRepository.findByFgExcluidoFalse(PageRequest.of(p - 1, l));
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

    private Usuario findEntity(Long id) {
        return usuarioRepository.findById(id)
                .filter(u -> !Boolean.TRUE.equals(u.getFgExcluido()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
    }

    private UsuarioResponse toResponse(Usuario u) {
        return new UsuarioResponse(
                idCodec.encodeUsuarioId(u.getId()),
                u.getNmUsuario(),
                u.getNmLogin(),
                u.getNmEmail(),
                u.getPerfil().getNmPerfil(),
                u.getFgAtivo());
    }
}
