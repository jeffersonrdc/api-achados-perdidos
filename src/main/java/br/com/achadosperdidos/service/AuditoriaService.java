package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.AuditoriaResponse;
import br.com.achadosperdidos.entity.Auditoria;
import br.com.achadosperdidos.pagination.ApiPage;
import br.com.achadosperdidos.pagination.PaginationMeta;
import br.com.achadosperdidos.pagination.PaginationParams;
import br.com.achadosperdidos.repository.AuditoriaRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditoriaService {
    private final AuditoriaRepository auditoriaRepository;
    private final SignedResourceIdCodec idCodec;

    public AuditoriaService(AuditoriaRepository auditoriaRepository, SignedResourceIdCodec idCodec) {
        this.auditoriaRepository = auditoriaRepository;
        this.idCodec = idCodec;
    }

    @Transactional(readOnly = true)
    public ApiPage<AuditoriaResponse> findAll(Integer page, Integer limit) {
        int p = PaginationParams.resolvePage(page);
        int l = PaginationParams.resolveLimit(limit);
        Page<Auditoria> result = auditoriaRepository.findByFgExcluidoFalseOrderByDtAuditoriaDesc(PageRequest.of(p - 1, l));
        var content = result.getContent().stream().map(this::toResponse).toList();
        return ApiPage.paged(content, new PaginationMeta(p, l, result.getTotalElements(), result.getTotalPages()));
    }

    @Transactional(readOnly = true)
    public ApiPage<AuditoriaResponse> findByRegistro(String nmTabela, Long idRegistro, Integer page, Integer limit) {
        int p = PaginationParams.resolvePage(page);
        int l = PaginationParams.resolveLimit(limit);
        Page<Auditoria> result = auditoriaRepository.findByNmTabelaAndIdRegistroAndFgExcluidoFalseOrderByDtAuditoriaDesc(
                nmTabela, idRegistro, PageRequest.of(p - 1, l));
        var content = result.getContent().stream().map(this::toResponse).toList();
        return ApiPage.paged(content, new PaginationMeta(p, l, result.getTotalElements(), result.getTotalPages()));
    }

    private AuditoriaResponse toResponse(Auditoria a) {
        return new AuditoriaResponse(
                idCodec.encodeAuditoriaId(a.getId()),
                a.getNmTabela(),
                String.valueOf(a.getIdRegistro()),
                a.getTpAcao(),
                a.getDsAntes(),
                a.getDsDepois(),
                a.getIdUsuario() != null ? idCodec.encodeUsuarioId(a.getIdUsuario()) : null,
                a.getDtAuditoria(),
                a.getNrIp());
    }
}
