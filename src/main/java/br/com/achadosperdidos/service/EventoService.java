package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.EventoCreateRequest;
import br.com.achadosperdidos.controller.dto.EventoResponse;
import br.com.achadosperdidos.controller.dto.EventoUpdateRequest;
import br.com.achadosperdidos.entity.Arquivo;
import br.com.achadosperdidos.entity.Evento;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.pagination.ApiPage;
import br.com.achadosperdidos.pagination.PaginationMeta;
import br.com.achadosperdidos.pagination.PaginationParams;
import br.com.achadosperdidos.repository.EventoRepository;
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
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EventoService {
    private final EventoRepository eventoRepository;
    private final ArquivoService arquivoService;
    private final SignedResourceIdCodec idCodec;

    public EventoService(EventoRepository eventoRepository,
                         ArquivoService arquivoService, SignedResourceIdCodec idCodec) {
        this.eventoRepository = eventoRepository;
        this.arquivoService = arquivoService;
        this.idCodec = idCodec;
    }

    @Transactional
    public EventoResponse create(EventoCreateRequest request) {
        Evento evento = new Evento();
        evento.setNmEvento(request.nmEvento().trim());
        evento.setDsEvento(request.dsEvento());
        evento.setDtInicio(request.dtInicio());
        evento.setDtFim(request.dtFim());
        evento.setNmLocal(request.nmLocal());
        evento.setNmCidade(request.nmCidade());
        evento.setSgUf(request.sgUf());
        evento.setQtDiasRetencao(request.qtDiasRetencao() != null ? request.qtDiasRetencao() : 90);
        evento.setDtCadastro(LocalDateTime.now());
        evento.setFgAtivo(true);
        evento.setFgExcluido(false);
        return toResponse(eventoRepository.save(evento), Map.of());
    }

    @Transactional(readOnly = true)
    public ApiPage<EventoResponse> findAll(boolean incluirInativos, Integer page, Integer limit, String q) {
        int p = PaginationParams.resolvePage(page);
        int l = PaginationParams.resolveLimit(limit);
        Specification<Evento> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.isFalse(root.get("fgExcluido")));
            if (!incluirInativos) ps.add(cb.isTrue(root.get("fgAtivo")));
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("nmEvento")), like),
                        cb.like(cb.lower(root.get("nmLocal")), like),
                        cb.like(cb.lower(root.get("nmCidade")), like),
                        cb.like(cb.lower(root.get("sgUf")), like)));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<Evento> result = eventoRepository.findAll(spec,
                PageRequest.of(p - 1, l, Sort.by(Sort.Direction.DESC, "dtInicio")));
        List<Evento> list = result.getContent();
        Map<Long, Map<String, Arquivo>> imgs = arquivoService.imagensPorEventos(
                list.stream().map(Evento::getId).collect(Collectors.toList()));
        var content = list.stream().map(e -> toResponse(e, imgs.getOrDefault(e.getId(), Map.of()))).toList();
        return ApiPage.paged(content, new PaginationMeta(p, l, result.getTotalElements(), result.getTotalPages()));
    }

    @Transactional(readOnly = true)
    public EventoResponse findById(String idToken) {
        Evento e = findEntity(idCodec.decodeEventoId(idToken));
        Map<String, Arquivo> imgs = arquivoService.imagensPorEventos(List.of(e.getId()))
                .getOrDefault(e.getId(), Map.of());
        return toResponse(e, imgs);
    }

    @Transactional
    public EventoResponse update(String idToken, EventoUpdateRequest request) {
        Evento evento = findEntity(idCodec.decodeEventoId(idToken));
        if (request.nmEvento() != null && !request.nmEvento().isBlank()) evento.setNmEvento(request.nmEvento().trim());
        if (request.dsEvento() != null) evento.setDsEvento(request.dsEvento());
        if (request.dtInicio() != null) evento.setDtInicio(request.dtInicio());
        if (request.dtFim() != null) evento.setDtFim(request.dtFim());
        if (request.nmLocal() != null) evento.setNmLocal(request.nmLocal());
        if (request.nmCidade() != null) evento.setNmCidade(request.nmCidade());
        if (request.sgUf() != null) evento.setSgUf(request.sgUf());
        if (request.qtDiasRetencao() != null) evento.setQtDiasRetencao(request.qtDiasRetencao());
        if (request.fgAtivo() != null) evento.setFgAtivo(request.fgAtivo());
        evento.setDtAlteracao(LocalDateTime.now());
        Evento saved = eventoRepository.save(evento);
        Map<String, Arquivo> imgs = arquivoService.imagensPorEventos(List.of(saved.getId()))
                .getOrDefault(saved.getId(), Map.of());
        return toResponse(saved, imgs);
    }

    @Transactional
    public void softDelete(String idToken) {
        Evento evento = findEntity(idCodec.decodeEventoId(idToken));
        evento.setFgExcluido(true);
        evento.setFgAtivo(false);
        evento.setDtAlteracao(LocalDateTime.now());
        eventoRepository.save(evento);
    }

    private Evento findEntity(Long id) {
        return eventoRepository.findById(id)
                .filter(e -> !Boolean.TRUE.equals(e.getFgExcluido()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento não encontrado."));
    }

    private EventoResponse toResponse(Evento e, Map<String, Arquivo> imgs) {
        Arquivo logo = imgs != null ? imgs.get("LOGO") : null;
        Arquivo hero = imgs != null ? imgs.get("HERO") : null;
        return new EventoResponse(
                idCodec.encodeEventoId(e.getId()), e.getNmEvento(), e.getDsEvento(), e.getDtInicio(), e.getDtFim(),
                e.getNmLocal(), e.getNmCidade(), e.getSgUf(), e.getQtDiasRetencao(), e.getFgAtivo(),
                logo != null ? idCodec.encodeArquivoId(logo.getId()) : null,
                hero != null ? idCodec.encodeArquivoId(hero.getId()) : null);
    }
}
