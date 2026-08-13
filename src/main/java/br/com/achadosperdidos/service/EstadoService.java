package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.EstadoCreateRequest;
import br.com.achadosperdidos.controller.dto.EstadoResponse;
import br.com.achadosperdidos.controller.dto.EstadoUpdateRequest;
import br.com.achadosperdidos.entity.Estado;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.pagination.ApiPage;
import br.com.achadosperdidos.pagination.PaginationMeta;
import br.com.achadosperdidos.pagination.PaginationParams;
import br.com.achadosperdidos.repository.EstadoRepository;
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
public class EstadoService {
    private final EstadoRepository estadoRepository;
    private final SignedResourceIdCodec idCodec;
    private final AuditoriaContextService auditoriaContext;

    public EstadoService(EstadoRepository estadoRepository, SignedResourceIdCodec idCodec,
                         AuditoriaContextService auditoriaContext) {
        this.estadoRepository = estadoRepository;
        this.idCodec = idCodec;
        this.auditoriaContext = auditoriaContext;
    }

    @Transactional(readOnly = true)
    public List<String> listarNomesAtivos() {
        return estadoRepository.findByFgExcluidoFalseAndFgAtivoTrueOrderByOrOrdemAscNmEstadoAsc()
                .stream().map(Estado::getNmEstado).toList();
    }

    @Transactional(readOnly = true)
    public ApiPage<EstadoResponse> findAll(boolean incluirInativos, Integer page, Integer limit, String q) {
        int p = PaginationParams.resolvePage(page);
        int l = PaginationParams.resolveLimit(limit);
        Specification<Estado> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.isFalse(root.get("fgExcluido")));
            if (!incluirInativos) ps.add(cb.isTrue(root.get("fgAtivo")));
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("nmEstado")), like),
                        cb.like(cb.lower(root.get("dsEstado")), like)));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<Estado> result = estadoRepository.findAll(spec,
                PageRequest.of(p - 1, l, Sort.by(Sort.Direction.ASC, "orOrdem").and(Sort.by(Sort.Direction.ASC, "nmEstado"))));
        var content = result.getContent().stream().map(this::toResponse).toList();
        return ApiPage.paged(content, new PaginationMeta(p, l, result.getTotalElements(), result.getTotalPages()));
    }

    @Transactional
    public EstadoResponse create(EstadoCreateRequest request) {
        auditoriaContext.marcarContexto();
        String nome = request.nmEstado().trim();
        if (estadoRepository.existsByNmEstadoIgnoreCaseAndFgExcluidoFalse(nome)) {
            throw new IllegalArgumentException("Já existe um estado com este nome.");
        }
        Estado e = new Estado();
        e.setNmEstado(nome);
        e.setDsEstado(blankToNull(request.dsEstado()));
        e.setOrOrdem(request.orOrdem() != null ? request.orOrdem() : 0);
        e.setFgAtivo(request.fgAtivo() == null || request.fgAtivo());
        e.setFgExcluido(false);
        e.setDtCadastro(LocalDateTime.now());
        return toResponse(estadoRepository.save(e));
    }

    @Transactional
    public EstadoResponse update(String idToken, EstadoUpdateRequest request) {
        auditoriaContext.marcarContexto();
        Estado e = findEntity(idCodec.decodeEstadoId(idToken));
        if (request.nmEstado() != null) {
            String nome = request.nmEstado().trim();
            if (estadoRepository.existsByNmEstadoIgnoreCaseAndIdNotAndFgExcluidoFalse(nome, e.getId())) {
                throw new IllegalArgumentException("Já existe um estado com este nome.");
            }
            e.setNmEstado(nome);
        }
        if (request.dsEstado() != null) e.setDsEstado(blankToNull(request.dsEstado()));
        if (request.orOrdem() != null) e.setOrOrdem(request.orOrdem());
        if (request.fgAtivo() != null) e.setFgAtivo(request.fgAtivo());
        e.setDtAlteracao(LocalDateTime.now());
        return toResponse(estadoRepository.save(e));
    }

    @Transactional
    public void softDelete(String idToken) {
        auditoriaContext.marcarContexto();
        Estado e = findEntity(idCodec.decodeEstadoId(idToken));
        e.setFgExcluido(true);
        e.setFgAtivo(false);
        e.setDtAlteracao(LocalDateTime.now());
        estadoRepository.save(e);
    }

    private Estado findEntity(Long id) {
        return estadoRepository.findById(id)
                .filter(x -> !Boolean.TRUE.equals(x.getFgExcluido()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Estado não encontrado."));
    }

    private EstadoResponse toResponse(Estado e) {
        return new EstadoResponse(idCodec.encodeEstadoId(e.getId()), e.getNmEstado(), e.getDsEstado(),
                e.getOrOrdem(), e.getFgAtivo());
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
