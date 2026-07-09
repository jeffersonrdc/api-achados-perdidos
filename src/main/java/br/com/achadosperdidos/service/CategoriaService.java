package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.CategoriaResponse;
import br.com.achadosperdidos.entity.Categoria;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.repository.CategoriaRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class CategoriaService {
    private final CategoriaRepository categoriaRepository;
    private final SignedResourceIdCodec idCodec;
    public CategoriaService(CategoriaRepository categoriaRepository, SignedResourceIdCodec idCodec) {
        this.categoriaRepository = categoriaRepository; this.idCodec = idCodec;
    }
    @Transactional(readOnly = true)
    public List<CategoriaResponse> findAll() {
        return categoriaRepository.findByFgExcluidoFalseAndFgAtivoTrueOrderByOrOrdemAsc().stream().map(this::toResponse).toList();
    }
    @Transactional(readOnly = true)
    public CategoriaResponse findById(String idToken) {
        return toResponse(findEntity(idCodec.decodeCategoriaId(idToken)));
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
