package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.PerfilCreateRequest;
import br.com.achadosperdidos.controller.dto.PerfilDetalheResponse;
import br.com.achadosperdidos.controller.dto.PerfilResponse;
import br.com.achadosperdidos.controller.dto.PerfilUpdateRequest;
import br.com.achadosperdidos.controller.dto.PermissaoResponse;
import br.com.achadosperdidos.controller.dto.PermissoesRequest;
import br.com.achadosperdidos.entity.Perfil;
import br.com.achadosperdidos.entity.PerfilPermissao;
import br.com.achadosperdidos.entity.Permissao;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.repository.PerfilPermissaoRepository;
import br.com.achadosperdidos.repository.PerfilRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PerfilService {
    private final PerfilRepository perfilRepository;
    private final PerfilPermissaoRepository perfilPermissaoRepository;
    private final PermissaoService permissaoService;
    private final SignedResourceIdCodec idCodec;

    public PerfilService(PerfilRepository perfilRepository, PerfilPermissaoRepository perfilPermissaoRepository,
                         PermissaoService permissaoService, SignedResourceIdCodec idCodec) {
        this.perfilRepository = perfilRepository;
        this.perfilPermissaoRepository = perfilPermissaoRepository;
        this.permissaoService = permissaoService;
        this.idCodec = idCodec;
    }

    @Transactional(readOnly = true)
    public List<PerfilResponse> listar() {
        return perfilRepository.findByFgExcluidoFalseOrderByNmPerfilAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PerfilDetalheResponse detalhe(String id) {
        Perfil perfil = findEntity(idCodec.decodePerfilId(id));
        return toDetalhe(perfil);
    }

    @Transactional
    public PerfilResponse criar(PerfilCreateRequest request) {
        Perfil p = new Perfil();
        p.setNmPerfil(request.nmPerfil().trim());
        p.setDsPerfil(request.dsPerfil());
        p.setDtCadastro(LocalDateTime.now());
        p.setFgAtivo(true);
        p.setFgExcluido(false);
        return toResponse(perfilRepository.save(p));
    }

    @Transactional
    public PerfilResponse atualizar(String id, PerfilUpdateRequest request) {
        Perfil p = findEntity(idCodec.decodePerfilId(id));
        if (request.nmPerfil() != null) p.setNmPerfil(request.nmPerfil().trim());
        if (request.dsPerfil() != null) p.setDsPerfil(request.dsPerfil());
        if (request.fgAtivo() != null) p.setFgAtivo(request.fgAtivo());
        p.setDtAlteracao(LocalDateTime.now());
        return toResponse(perfilRepository.save(p));
    }

    @Transactional
    public void softDelete(String id) {
        Perfil p = findEntity(idCodec.decodePerfilId(id));
        p.setFgExcluido(true);
        p.setFgAtivo(false);
        p.setDtAlteracao(LocalDateTime.now());
        perfilRepository.save(p);
    }

    @Transactional(readOnly = true)
    public List<PermissaoResponse> listarPermissoes(String id) {
        Long perfilId = idCodec.decodePerfilId(id);
        return perfilPermissaoRepository.findByPerfil_IdAndFgExcluidoFalse(perfilId)
                .stream().map(pp -> permissaoService.toResponse(pp.getPermissao())).toList();
    }

    /** Define (substitui) o conjunto de permissoes do perfil. */
    @Transactional
    public List<PermissaoResponse> definirPermissoes(String id, PermissoesRequest request) {
        Perfil perfil = findEntity(idCodec.decodePerfilId(id));
        List<String> desejadas = request.permissoes() == null ? List.of() : request.permissoes();

        // Desativa as permissoes atuais que nao estao na lista desejada.
        for (PerfilPermissao atual : perfilPermissaoRepository.findByPerfil_IdAndFgExcluidoFalse(perfil.getId())) {
            if (!desejadas.contains(atual.getPermissao().getNmPermissao())) {
                atual.setFgExcluido(true);
                atual.setFgAtivo(false);
                atual.setDtAlteracao(LocalDateTime.now());
                perfilPermissaoRepository.save(atual);
            }
        }
        // Ativa/recria as permissoes desejadas (reaproveita vinculo soft-deleted).
        for (String nome : desejadas) {
            Permissao permissao = permissaoService.findByNome(nome);
            PerfilPermissao vinculo = perfilPermissaoRepository
                    .findByPerfil_IdAndPermissao_Id(perfil.getId(), permissao.getId())
                    .orElseGet(PerfilPermissao::new);
            if (vinculo.getId() == null) {
                vinculo.setPerfil(perfil);
                vinculo.setPermissao(permissao);
                vinculo.setDtCadastro(LocalDateTime.now());
            }
            vinculo.setFgAtivo(true);
            vinculo.setFgExcluido(false);
            vinculo.setDtAlteracao(LocalDateTime.now());
            perfilPermissaoRepository.save(vinculo);
        }
        return listarPermissoes(id);
    }

    private Perfil findEntity(Long id) {
        return perfilRepository.findById(id)
                .filter(p -> !Boolean.TRUE.equals(p.getFgExcluido()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Perfil não encontrado."));
    }

    private PerfilResponse toResponse(Perfil p) {
        return new PerfilResponse(idCodec.encodePerfilId(p.getId()), p.getNmPerfil(), p.getDsPerfil(), p.getFgAtivo());
    }

    private PerfilDetalheResponse toDetalhe(Perfil p) {
        List<PermissaoResponse> permissoes = perfilPermissaoRepository.findByPerfil_IdAndFgExcluidoFalse(p.getId())
                .stream().map(pp -> permissaoService.toResponse(pp.getPermissao())).toList();
        return new PerfilDetalheResponse(idCodec.encodePerfilId(p.getId()), p.getNmPerfil(), p.getDsPerfil(), p.getFgAtivo(), permissoes);
    }
}
