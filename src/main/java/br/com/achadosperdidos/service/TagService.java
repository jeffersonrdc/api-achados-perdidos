package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.TagCreateRequest;
import br.com.achadosperdidos.controller.dto.TagResponse;
import br.com.achadosperdidos.controller.dto.TagUpdateRequest;
import br.com.achadosperdidos.entity.Categoria;
import br.com.achadosperdidos.entity.Tag;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.pagination.ApiPage;
import br.com.achadosperdidos.pagination.PaginationMeta;
import br.com.achadosperdidos.pagination.PaginationParams;
import br.com.achadosperdidos.repository.TagRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

    /** Lista completa — usado pelo portal. */
    @Transactional(readOnly = true)
    public List<TagResponse> findAll(boolean incluirInativos, String idSubcategoria) {
        List<Tag> lista;
        if (idSubcategoria != null && !idSubcategoria.isBlank()) {
            Long idSub = idCodec.decodeCategoriaId(idSubcategoria);
            lista = incluirInativos
                    ? tagRepository.findBySubcategoria_IdAndFgExcluidoFalseOrderByNmTagAsc(idSub)
                    : tagRepository.findBySubcategoria_IdAndFgExcluidoFalseAndFgAtivoTrueOrderByNmTagAsc(idSub);
        } else {
            lista = incluirInativos
                    ? tagRepository.findByFgExcluidoFalseOrderByNmTagAsc()
                    : tagRepository.findByFgExcluidoFalseAndFgAtivoTrueOrderByNmTagAsc();
        }
        return lista.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ApiPage<TagResponse> findAll(boolean incluirInativos, String idSubcategoria,
                                        Integer page, Integer limit, String q) {
        int p = PaginationParams.resolvePage(page);
        int l = PaginationParams.resolveLimit(limit);
        Long subId = (idSubcategoria != null && !idSubcategoria.isBlank())
                ? idCodec.decodeCategoriaId(idSubcategoria) : null;
        Specification<Tag> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.isFalse(root.get("fgExcluido")));
            if (!incluirInativos) ps.add(cb.isTrue(root.get("fgAtivo")));
            if (subId != null) ps.add(cb.equal(root.get("subcategoria").get("id"), subId));
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("nmTag")), like),
                        cb.like(cb.lower(root.get("dsTag")), like)));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<Tag> result = tagRepository.findAll(spec,
                PageRequest.of(p - 1, l, Sort.by(Sort.Direction.ASC, "nmTag")));
        var content = result.getContent().stream().map(this::toResponse).toList();
        return ApiPage.paged(content, new PaginationMeta(p, l, result.getTotalElements(), result.getTotalPages()));
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
