package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.CategoriaCreateRequest;
import br.com.achadosperdidos.controller.dto.CategoriaResponse;
import br.com.achadosperdidos.controller.dto.CategoriaUpdateRequest;
import br.com.achadosperdidos.entity.Categoria;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.repository.CategoriaRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CategoriaService {
    private final CategoriaRepository categoriaRepository;
    private final SignedResourceIdCodec idCodec;
    private final AuditoriaContextService auditoriaContext;

    public CategoriaService(CategoriaRepository categoriaRepository, SignedResourceIdCodec idCodec,
                            AuditoriaContextService auditoriaContext) {
        this.categoriaRepository = categoriaRepository;
        this.idCodec = idCodec;
        this.auditoriaContext = auditoriaContext;
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> findAll() {
        return categoriaRepository.findByCategoriaPaiIsNullAndFgExcluidoFalseAndFgAtivoTrueOrderByOrOrdemAsc().stream().map(this::toResponse).toList();
    }

    /** Lista apenas categorias-pai (topo). Subcategorias vêm por {@link #findSubcategorias}. */
    @Transactional(readOnly = true)
    public List<CategoriaResponse> findAll(boolean incluirInativos) {
        var lista = incluirInativos
                ? categoriaRepository.findByCategoriaPaiIsNullAndFgExcluidoFalseOrderByOrOrdemAsc()
                : categoriaRepository.findByCategoriaPaiIsNullAndFgExcluidoFalseAndFgAtivoTrueOrderByOrOrdemAsc();
        return lista.stream().map(this::toResponse).toList();
    }

    /** Subcategorias (filhos) de uma categoria-pai. */
    @Transactional(readOnly = true)
    public List<CategoriaResponse> findSubcategorias(String idPaiToken, boolean incluirInativos) {
        Long idPai = idCodec.decodeCategoriaId(idPaiToken);
        var lista = incluirInativos
                ? categoriaRepository.findByCategoriaPai_IdAndFgExcluidoFalseOrderByOrOrdemAsc(idPai)
                : categoriaRepository.findByCategoriaPai_IdAndFgExcluidoFalseAndFgAtivoTrueOrderByOrOrdemAsc(idPai);
        return lista.stream().map(this::toResponse).toList();
    }

    /** Todas as subcategorias (qualquer pai). */
    @Transactional(readOnly = true)
    public List<CategoriaResponse> findAllSubcategorias(boolean incluirInativos) {
        var lista = incluirInativos
                ? categoriaRepository.findByCategoriaPaiIsNotNullAndFgExcluidoFalseOrderByOrOrdemAsc()
                : categoriaRepository.findByCategoriaPaiIsNotNullAndFgExcluidoFalseAndFgAtivoTrueOrderByOrOrdemAsc();
        return lista.stream().map(this::toResponse).toList();
    }

    /** Categorias-pai ativas (entidades) para montagem de árvore. */
    @Transactional(readOnly = true)
    public List<br.com.achadosperdidos.entity.Categoria> findPaisAtivos() {
        return categoriaRepository.findByCategoriaPaiIsNullAndFgExcluidoFalseAndFgAtivoTrueOrderByOrOrdemAsc();
    }

    /** Subcategorias (entidades) ativas de uma categoria-pai por id numérico. */
    @Transactional(readOnly = true)
    public List<br.com.achadosperdidos.entity.Categoria> findSubcategoriasEntidades(Long idPai) {
        return categoriaRepository.findByCategoriaPai_IdAndFgExcluidoFalseAndFgAtivoTrueOrderByOrOrdemAsc(idPai);
    }

    @Transactional(readOnly = true)
    public CategoriaResponse findById(String idToken) {
        return toResponse(findEntity(idCodec.decodeCategoriaId(idToken)));
    }

    @Transactional
    public CategoriaResponse create(CategoriaCreateRequest request) {
        auditoriaContext.marcarContexto();
        String nome = request.nmCategoria().trim();
        Categoria pai = resolvePai(request.idCategoriaPai());
        assertNomeUnico(nome, pai, null);
        Categoria c = new Categoria();
        c.setNmCategoria(nome);
        c.setCategoriaPai(pai);
        c.setDsCategoria(request.dsCategoria());
        c.setIcIcone(request.icIcone());
        c.setOrOrdem(request.orOrdem() != null ? request.orOrdem() : 0);
        c.setFgAtivo(request.fgAtivo() == null || request.fgAtivo());
        c.setFgExcluido(false);
        c.setDtCadastro(LocalDateTime.now());
        return toResponse(categoriaRepository.save(c));
    }

    @Transactional
    public CategoriaResponse update(String idToken, CategoriaUpdateRequest request) {
        auditoriaContext.marcarContexto();
        Categoria c = findEntity(idCodec.decodeCategoriaId(idToken));
        if (request.idCategoriaPai() != null) {
            if (request.idCategoriaPai().isBlank()) {
                c.setCategoriaPai(null);
            } else {
                Long idPai = idCodec.decodeCategoriaId(request.idCategoriaPai());
                if (idPai.equals(c.getId())) {
                    throw new IllegalArgumentException("Uma categoria não pode ser pai de si mesma.");
                }
                Categoria pai = findEntity(idPai);
                if (pai.getCategoriaPai() != null) {
                    throw new IllegalArgumentException("O pai deve ser uma categoria de topo (sem avô).");
                }
                c.setCategoriaPai(pai);
            }
        }
        if (request.nmCategoria() != null) {
            String nome = request.nmCategoria().trim();
            assertNomeUnico(nome, c.getCategoriaPai(), c.getId());
            c.setNmCategoria(nome);
        }
        if (request.dsCategoria() != null) c.setDsCategoria(request.dsCategoria());
        if (request.icIcone() != null) c.setIcIcone(request.icIcone());
        if (request.orOrdem() != null) c.setOrOrdem(request.orOrdem());
        if (request.fgAtivo() != null) c.setFgAtivo(request.fgAtivo());
        c.setDtAlteracao(LocalDateTime.now());
        return toResponse(categoriaRepository.save(c));
    }

    @Transactional
    public void softDelete(String idToken) {
        auditoriaContext.marcarContexto();
        Categoria c = findEntity(idCodec.decodeCategoriaId(idToken));
        c.setFgExcluido(true);
        c.setFgAtivo(false);
        c.setDtAlteracao(LocalDateTime.now());
        categoriaRepository.save(c);
    }

    Categoria findEntity(Long id) {
        return categoriaRepository.findById(id)
                .filter(c -> !Boolean.TRUE.equals(c.getFgExcluido()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada."));
    }

    private Categoria resolvePai(String idPaiToken) {
        if (idPaiToken == null || idPaiToken.isBlank()) return null;
        Categoria pai = findEntity(idCodec.decodeCategoriaId(idPaiToken));
        if (pai.getCategoriaPai() != null) {
            throw new IllegalArgumentException("O pai deve ser uma categoria de topo (sem avô).");
        }
        return pai;
    }

    private void assertNomeUnico(String nome, Categoria pai, Long idAtual) {
        boolean existe;
        if (pai == null) {
            existe = idAtual == null
                    ? categoriaRepository.existsByNmCategoriaIgnoreCaseAndCategoriaPaiIsNullAndFgExcluidoFalse(nome)
                    : categoriaRepository.existsByNmCategoriaIgnoreCaseAndCategoriaPaiIsNullAndIdNotAndFgExcluidoFalse(nome, idAtual);
        } else {
            existe = idAtual == null
                    ? categoriaRepository.existsByNmCategoriaIgnoreCaseAndCategoriaPai_IdAndFgExcluidoFalse(nome, pai.getId())
                    : categoriaRepository.existsByNmCategoriaIgnoreCaseAndCategoriaPai_IdAndIdNotAndFgExcluidoFalse(nome, pai.getId(), idAtual);
        }
        if (existe) {
            throw new IllegalArgumentException(pai == null
                    ? "Já existe uma categoria com este nome."
                    : "Já existe uma subcategoria com este nome nesta categoria.");
        }
    }

    private CategoriaResponse toResponse(Categoria c) {
        String idPai = c.getCategoriaPai() != null ? idCodec.encodeCategoriaId(c.getCategoriaPai().getId()) : null;
        return new CategoriaResponse(idCodec.encodeCategoriaId(c.getId()), c.getNmCategoria(), idPai, c.getDsCategoria(), c.getIcIcone(), c.getOrOrdem(), c.getFgAtivo());
    }
}
