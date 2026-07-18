package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.PermissaoResponse;
import br.com.achadosperdidos.controller.dto.PermissoesRequest;
import br.com.achadosperdidos.entity.Permissao;
import br.com.achadosperdidos.entity.Usuario;
import br.com.achadosperdidos.entity.UsuarioPermissao;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.repository.PermissaoRepository;
import br.com.achadosperdidos.repository.UsuarioPermissaoRepository;
import br.com.achadosperdidos.repository.UsuarioRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** Permissoes adicionais por usuario (alem das herdadas do perfil) e permissoes efetivas. */
@Service
public class UsuarioPermissaoService {
    private final UsuarioRepository usuarioRepository;
    private final UsuarioPermissaoRepository usuarioPermissaoRepository;
    private final PermissaoRepository permissaoRepository;
    private final PermissaoService permissaoService;
    private final UsuarioContextService usuarioContextService;
    private final AuditoriaContextService auditoriaContext;
    private final SignedResourceIdCodec idCodec;

    public UsuarioPermissaoService(UsuarioRepository usuarioRepository, UsuarioPermissaoRepository usuarioPermissaoRepository,
                                   PermissaoRepository permissaoRepository, PermissaoService permissaoService,
                                   UsuarioContextService usuarioContextService, AuditoriaContextService auditoriaContext,
                                   SignedResourceIdCodec idCodec) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioPermissaoRepository = usuarioPermissaoRepository;
        this.permissaoRepository = permissaoRepository;
        this.permissaoService = permissaoService;
        this.usuarioContextService = usuarioContextService;
        this.auditoriaContext = auditoriaContext;
        this.idCodec = idCodec;
    }

    @Transactional(readOnly = true)
    public List<PermissaoResponse> listarExtras(String idUsuario) {
        Long usuarioId = idCodec.decodeUsuarioId(idUsuario);
        return usuarioPermissaoRepository.findByUsuario_IdAndFgExcluidoFalse(usuarioId)
                .stream().map(up -> permissaoService.toResponse(up.getPermissao())).toList();
    }

    /** Permissoes efetivas = permissoes do perfil UNIAO extras do usuario. */
    @Transactional(readOnly = true)
    public List<String> efetivas(String idUsuario) {
        return permissaoRepository.findPermissoesEfetivas(idCodec.decodeUsuarioId(idUsuario));
    }

    /** Permissões efetivas do próprio usuário autenticado. */
    @Transactional(readOnly = true)
    public List<String> efetivasDoUsuarioLogado() {
        return permissaoRepository.findPermissoesEfetivas(usuarioContextService.requireUsuarioLogadoId());
    }

    /** Define (substitui) o conjunto de permissoes adicionais do usuario. */
    @Transactional
    public List<PermissaoResponse> definirExtras(String idUsuario, PermissoesRequest request) {
        auditoriaContext.marcarContexto();
        Usuario usuario = usuarioRepository.findById(idCodec.decodeUsuarioId(idUsuario))
                .filter(u -> !Boolean.TRUE.equals(u.getFgExcluido()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
        List<String> desejadas = request.permissoes() == null ? List.of() : request.permissoes();

        for (UsuarioPermissao atual : usuarioPermissaoRepository.findByUsuario_IdAndFgExcluidoFalse(usuario.getId())) {
            if (!desejadas.contains(atual.getPermissao().getNmPermissao())) {
                atual.setFgExcluido(true);
                atual.setFgAtivo(false);
                atual.setDtAlteracao(LocalDateTime.now());
                usuarioPermissaoRepository.save(atual);
            }
        }
        for (String nome : desejadas) {
            Permissao permissao = permissaoService.findByNome(nome);
            UsuarioPermissao vinculo = usuarioPermissaoRepository
                    .findByUsuario_IdAndPermissao_Id(usuario.getId(), permissao.getId())
                    .orElseGet(UsuarioPermissao::new);
            if (vinculo.getId() == null) {
                vinculo.setUsuario(usuario);
                vinculo.setPermissao(permissao);
                vinculo.setDtCadastro(LocalDateTime.now());
            }
            vinculo.setFgAtivo(true);
            vinculo.setFgExcluido(false);
            vinculo.setDtAlteracao(LocalDateTime.now());
            usuarioPermissaoRepository.save(vinculo);
        }
        return listarExtras(idUsuario);
    }
}
