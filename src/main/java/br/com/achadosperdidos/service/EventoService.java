package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.EventoCreateRequest;
import br.com.achadosperdidos.controller.dto.EventoResponse;
import br.com.achadosperdidos.controller.dto.EventoUpdateRequest;
import br.com.achadosperdidos.entity.Empresa;
import br.com.achadosperdidos.entity.Evento;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.repository.EmpresaRepository;
import br.com.achadosperdidos.repository.EventoRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventoService {
    private final EventoRepository eventoRepository;
    private final EmpresaRepository empresaRepository;
    private final SignedResourceIdCodec idCodec;
    private final UsuarioContextService usuarioContextService;

    public EventoService(EventoRepository eventoRepository, EmpresaRepository empresaRepository,
                         SignedResourceIdCodec idCodec, UsuarioContextService usuarioContextService) {
        this.eventoRepository = eventoRepository;
        this.empresaRepository = empresaRepository;
        this.idCodec = idCodec;
        this.usuarioContextService = usuarioContextService;
    }

    @Transactional
    public EventoResponse create(EventoCreateRequest request) {
        Long empresaId = idCodec.decodeEmpresaId(request.idEmpresa());
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Empresa não encontrada."));
        Evento evento = new Evento();
        evento.setEmpresa(empresa);
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
        return toResponse(eventoRepository.save(evento));
    }

    @Transactional(readOnly = true)
    public List<EventoResponse> findAll(boolean incluirInativos) {
        List<Evento> list = incluirInativos
                ? eventoRepository.findByFgExcluidoFalseOrderByDtInicioDesc()
                : eventoRepository.findByFgExcluidoFalseAndFgAtivoTrueOrderByDtInicioDesc();
        return list.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public EventoResponse findById(String idToken) {
        return toResponse(findEntity(idCodec.decodeEventoId(idToken)));
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
        return toResponse(eventoRepository.save(evento));
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

    private EventoResponse toResponse(Evento e) {
        return new EventoResponse(
                idCodec.encodeEventoId(e.getId()), e.getNmEvento(), e.getDsEvento(), e.getDtInicio(), e.getDtFim(),
                e.getNmLocal(), e.getNmCidade(), e.getSgUf(), e.getQtDiasRetencao(), e.getFgAtivo());
    }
}
