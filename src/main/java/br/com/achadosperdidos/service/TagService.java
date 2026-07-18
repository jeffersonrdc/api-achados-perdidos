package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.TagCreateRequest;
import br.com.achadosperdidos.controller.dto.TagResponse;
import br.com.achadosperdidos.controller.dto.TagUpdateRequest;
import br.com.achadosperdidos.entity.Categoria;
import br.com.achadosperdidos.entity.Tag;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.repository.TagRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TagService {
    private final TagRepository tagRepository;
    private final CategoriaService categoriaService;
    private final SignedResourceIdCodec idCodec;
    private final AuditoriaContextService auditoriaContext;

    public TagService(TagRepository tagRepository, CategoriaService categoriaService,
                      SignedResourceIdCodec idCodec, AuditoriaContextService auditoriaContext) {
        this.tagRepository = tagRepository;
        this.categoriaService = categoriaService;
        this.idCodec = idCodec;
        this.auditoriaContext = auditoriaContext;
    }

    @Transactional(readOnly = true)
    public List<TagResponse> findAll(boolean incluirInativos, String idSubcategoria) {
        List<Tag> lista;
        if (idSubcategoria != null && !idSubcategoria.isBlank()) {
            Long idSub = idCodec.decodeCategoriaId(idSubcategoria);
            lista = incluirInativos
                    ? tagRepository.findBySubcategoria_IdAndFgExcluidoFalseOrderByOrOrdemAscNmTagAsc(idSub)
                    : tagRepository.findBySubcategoria_IdAndFgExcluidoFalseAndFgAtivoTrueOrderByOrOrdemAscNmTagAsc(idSub);
        } else {
            lista = incluirInativos
                    ? tagRepository.findByFgExcluidoFalseOrderByOrOrdemAscNmTagAsc()
                    : tagRepository.findByFgExcluidoFalseAndFgAtivoTrueOrderByOrOrdemAscNmTagAsc();
        }
        return lista.stream().map(this::toResponse).toList();
    }

    @Transactional
    public TagResponse create(TagCreateRequest request) {
        auditoriaContext.marcarContexto();
        Categoria sub = requireSubcategoria(idCodec.decodeCategoriaId(request.idSubcategoria()));
        String nome = request.nmTag().trim();
        if (tagRepository.existsByNmTagIgnoreCaseAndSubcategoria_IdAndFgExcluidoFalse(nome, sub.getId())) {
            throw new IllegalArgumentException("Já existe uma tag com este nome nesta subcategoria.");
        }
        Tag t = new Tag();
        t.setSubcategoria(sub);
        t.setNmTag(nome);
        t.setDsTag(blankToNull(request.dsTag()));
        t.setOrOrdem(request.orOrdem() != null ? request.orOrdem() : 0);
        t.setFgAtivo(request.fgAtivo() == null || request.fgAtivo());
        t.setFgExcluido(false);
        t.setDtCadastro(LocalDateTime.now());
        return toResponse(tagRepository.save(t));
    }

    @Transactional
    public TagResponse update(String idToken, TagUpdateRequest request) {
        auditoriaContext.marcarContexto();
        Tag t = findEntity(idCodec.decodeTagId(idToken));
        if (request.idSubcategoria() != null && !request.idSubcategoria().isBlank()) {
            t.setSubcategoria(requireSubcategoria(idCodec.decodeCategoriaId(request.idSubcategoria())));
        }
        if (request.nmTag() != null) {
            String nome = request.nmTag().trim();
            if (tagRepository.existsByNmTagIgnoreCaseAndSubcategoria_IdAndIdNotAndFgExcluidoFalse(
                    nome, t.getSubcategoria().getId(), t.getId())) {
                throw new IllegalArgumentException("Já existe uma tag com este nome nesta subcategoria.");
            }
            t.setNmTag(nome);
        }
        if (request.dsTag() != null) t.setDsTag(blankToNull(request.dsTag()));
        if (request.orOrdem() != null) t.setOrOrdem(request.orOrdem());
        if (request.fgAtivo() != null) t.setFgAtivo(request.fgAtivo());
        t.setDtAlteracao(LocalDateTime.now());
        return toResponse(tagRepository.save(t));
    }

    @Transactional
    public void softDelete(String idToken) {
        auditoriaContext.marcarContexto();
        Tag t = findEntity(idCodec.decodeTagId(idToken));
        t.setFgExcluido(true);
        t.setFgAtivo(false);
        t.setDtAlteracao(LocalDateTime.now());
        tagRepository.save(t);
    }

    private Categoria requireSubcategoria(Long id) {
        Categoria c = categoriaService.findEntity(id);
        if (c.getCategoriaPai() == null) {
            throw new IllegalArgumentException("A tag deve ser vinculada a uma subcategoria (não a uma categoria-pai).");
        }
        return c;
    }

    private Tag findEntity(Long id) {
        return tagRepository.findById(id)
                .filter(x -> !Boolean.TRUE.equals(x.getFgExcluido()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tag não encontrada."));
    }

    private TagResponse toResponse(Tag t) {
        Categoria sub = t.getSubcategoria();
        Categoria pai = sub.getCategoriaPai();
        return new TagResponse(
                idCodec.encodeTagId(t.getId()),
                t.getNmTag(),
                t.getDsTag(),
                t.getOrOrdem(),
                t.getFgAtivo(),
                idCodec.encodeCategoriaId(sub.getId()),
                sub.getNmCategoria(),
                pai != null ? idCodec.encodeCategoriaId(pai.getId()) : null,
                pai != null ? pai.getNmCategoria() : null);
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
