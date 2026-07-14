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
        return categoriaRepository.findByFgExcluidoFalseAndFgAtivoTrueOrderByOrOrdemAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> findAll(boolean incluirInativos) {
        var lista = incluirInativos
                ? categoriaRepository.findByFgExcluidoFalseOrderByOrOrdemAsc()
                : categoriaRepository.findByFgExcluidoFalseAndFgAtivoTrueOrderByOrOrdemAsc();
        return lista.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CategoriaResponse findById(String idToken) {
        return toResponse(findEntity(idCodec.decodeCategoriaId(idToken)));
    }

    @Transactional
    public CategoriaResponse create(CategoriaCreateRequest request) {
        auditoriaContext.marcarContexto();
        String nome = request.nmCategoria().trim();
        if (categoriaRepository.existsByNmCategoriaIgnoreCaseAndFgExcluidoFalse(nome)) {
            throw new IllegalArgumentException("Já existe uma categoria com este nome.");
        }
        Categoria c = new Categoria();
        c.setNmCategoria(nome);
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
        if (request.nmCategoria() != null) {
            String nome = request.nmCategoria().trim();
            if (categoriaRepository.existsByNmCategoriaIgnoreCaseAndIdNotAndFgExcluidoFalse(nome, c.getId())) {
                throw new IllegalArgumentException("Já existe uma categoria com este nome.");
            }
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

    private CategoriaResponse toResponse(Categoria c) {
        return new CategoriaResponse(idCodec.encodeCategoriaId(c.getId()), c.getNmCategoria(), c.getDsCategoria(), c.getIcIcone(), c.getOrOrdem(), c.getFgAtivo());
    }
}
