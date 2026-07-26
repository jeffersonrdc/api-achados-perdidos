package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.PermissaoResponse;
import br.com.achadosperdidos.entity.Permissao;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.pagination.ApiPage;
import br.com.achadosperdidos.pagination.PaginationMeta;
import br.com.achadosperdidos.pagination.PaginationParams;
import br.com.achadosperdidos.repository.PermissaoRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class PermissaoService {
    private final PermissaoRepository permissaoRepository;

    public PermissaoService(PermissaoRepository permissaoRepository) {
        this.permissaoRepository = permissaoRepository;
    }

    @Transactional(readOnly = true)
    public ApiPage<PermissaoResponse> listarCatalogo(Integer page, Integer limit, String q) {
        int p = PaginationParams.resolvePage(page);
        int l = PaginationParams.resolveLimit(limit);
        Specification<Permissao> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.isFalse(root.get("fgExcluido")));
            ps.add(cb.isTrue(root.get("fgAtivo")));
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("nmPermissao")), like),
                        cb.like(cb.lower(root.get("nmModulo")), like),
                        cb.like(cb.lower(root.get("nmAcao")), like),
                        cb.like(cb.lower(root.get("dsPermissao")), like)));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<Permissao> result = permissaoRepository.findAll(spec,
                PageRequest.of(p - 1, l, Sort.by(Sort.Direction.ASC, "nmModulo").and(Sort.by(Sort.Direction.ASC, "nmAcao"))));
        var content = result.getContent().stream().map(this::toResponse).toList();
        return ApiPage.paged(content, new PaginationMeta(p, l, result.getTotalElements(), result.getTotalPages()));
    }

    Permissao findByNome(String nmPermissao) {
        return permissaoRepository.findByNmPermissao(nmPermissao.trim())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Permissão não encontrada: " + nmPermissao));
    }

    PermissaoResponse toResponse(Permissao p) {
        return new PermissaoResponse(p.getNmPermissao(), p.getNmModulo(), p.getNmAcao(), p.getDsPermissao());
    }
}
