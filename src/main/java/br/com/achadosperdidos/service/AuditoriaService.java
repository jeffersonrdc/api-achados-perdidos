package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.AuditoriaResponse;
import br.com.achadosperdidos.entity.Auditoria;
import br.com.achadosperdidos.entity.Usuario;
import br.com.achadosperdidos.pagination.ApiPage;
import br.com.achadosperdidos.pagination.PaginationMeta;
import br.com.achadosperdidos.pagination.PaginationParams;
import br.com.achadosperdidos.repository.AuditoriaRepository;
import br.com.achadosperdidos.repository.UsuarioRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AuditoriaService {
    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final SignedResourceIdCodec idCodec;

    public AuditoriaService(AuditoriaRepository auditoriaRepository, UsuarioRepository usuarioRepository,
                            SignedResourceIdCodec idCodec) {
        this.auditoriaRepository = auditoriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.idCodec = idCodec;
    }

    @Transactional(readOnly = true)
    public ApiPage<AuditoriaResponse> findAll(Integer page, Integer limit) {
        int p = PaginationParams.resolvePage(page);
        int l = PaginationParams.resolveLimit(limit);
        Page<Auditoria> result = auditoriaRepository.findByFgExcluidoFalseOrderByDtAuditoriaDesc(PageRequest.of(p - 1, l));
        return mapPage(result, p, l);
    }

    @Transactional(readOnly = true)
    public ApiPage<AuditoriaResponse> findByRegistro(String nmTabela, Long idRegistro, Integer page, Integer limit) {
        int p = PaginationParams.resolvePage(page);
        int l = PaginationParams.resolveLimit(limit);
        Page<Auditoria> result = auditoriaRepository.findByNmTabelaAndIdRegistroAndFgExcluidoFalseOrderByDtAuditoriaDesc(
                nmTabela, idRegistro, PageRequest.of(p - 1, l));
        return mapPage(result, p, l);
    }

    private ApiPage<AuditoriaResponse> mapPage(Page<Auditoria> result, int p, int l) {
        Map<Long, String> nomes = nomesUsuarios(result.getContent());
        var content = result.getContent().stream().map(a -> toResponse(a, nomes)).toList();
        return ApiPage.paged(content, new PaginationMeta(p, l, result.getTotalElements(), result.getTotalPages()));
    }

    private Map<Long, String> nomesUsuarios(List<Auditoria> registros) {
        List<Long> ids = registros.stream()
                .map(Auditoria::getIdUsuario)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) return Map.of();
        return usuarioRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Usuario::getId, Usuario::getNmUsuario, (a, b) -> a));
    }

    private AuditoriaResponse toResponse(Auditoria a, Map<Long, String> nomes) {
        Long uid = a.getIdUsuario();
        return new AuditoriaResponse(
                idCodec.encodeAuditoriaId(a.getId()),
                a.getNmTabela(),
                String.valueOf(a.getIdRegistro()),
                a.getTpAcao(),
                a.getDsAntes(),
                a.getDsDepois(),
                uid != null ? idCodec.encodeUsuarioId(uid) : null,
                uid != null ? nomes.get(uid) : null,
                a.getDtAuditoria(),
                a.getNrIp());
    }
}
