package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.CategoriaCampoCreateRequest;
import br.com.achadosperdidos.controller.dto.CategoriaCampoResponse;
import br.com.achadosperdidos.entity.CategoriaCampo;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.repository.CategoriaCampoRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CategoriaCampoService {
    private final CategoriaCampoRepository categoriaCampoRepository;
    private final CategoriaService categoriaService;
    private final SignedResourceIdCodec idCodec;

    public CategoriaCampoService(CategoriaCampoRepository categoriaCampoRepository, CategoriaService categoriaService, SignedResourceIdCodec idCodec) {
        this.categoriaCampoRepository = categoriaCampoRepository;
        this.categoriaService = categoriaService;
        this.idCodec = idCodec;
    }

    @Transactional
    public CategoriaCampoResponse create(CategoriaCampoCreateRequest request) {
        CategoriaCampo c = new CategoriaCampo();
        c.setCategoria(categoriaService.findEntity(idCodec.decodeCategoriaId(request.idCategoria())));
        c.setNmCampo(request.nmCampo().trim());
        c.setDsLabel(request.dsLabel().trim());
        c.setTpCampo(request.tpCampo().trim().toUpperCase());
        c.setQtTamanho(request.qtTamanho());
        c.setFgObrigatorio(Boolean.TRUE.equals(request.fgObrigatorio()));
        c.setFgPesquisavel(Boolean.TRUE.equals(request.fgPesquisavel()));
        c.setOrExibicao(request.orExibicao() != null ? request.orExibicao() : 0);
        c.setDtCadastro(LocalDateTime.now());
        c.setFgAtivo(true);
        c.setFgExcluido(false);
        return toResponse(categoriaCampoRepository.save(c));
    }

    @Transactional(readOnly = true)
    public List<CategoriaCampoResponse> findByCategoria(String idCategoria) {
        return categoriaCampoRepository.findByCategoria_IdAndFgExcluidoFalseOrderByOrExibicaoAsc(idCodec.decodeCategoriaId(idCategoria))
                .stream().map(this::toResponse).toList();
    }

    CategoriaCampo findEntity(Long id) {
        return categoriaCampoRepository.findById(id)
                .filter(c -> !Boolean.TRUE.equals(c.getFgExcluido()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Campo de categoria não encontrado."));
    }

    private CategoriaCampoResponse toResponse(CategoriaCampo c) {
        return new CategoriaCampoResponse(
                idCodec.encodeCategoriaCampoId(c.getId()),
                idCodec.encodeCategoriaId(c.getCategoria().getId()),
                c.getNmCampo(), c.getDsLabel(), c.getTpCampo(),
                c.getQtTamanho(), c.getFgObrigatorio(), c.getFgPesquisavel(), c.getOrExibicao());
    }
}
