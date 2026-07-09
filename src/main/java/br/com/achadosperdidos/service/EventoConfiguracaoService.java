package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.EventoConfiguracaoResponse;
import br.com.achadosperdidos.controller.dto.EventoConfiguracaoUpdateRequest;
import br.com.achadosperdidos.entity.Evento;
import br.com.achadosperdidos.entity.EventoConfiguracao;
import br.com.achadosperdidos.repository.EventoConfiguracaoRepository;
import br.com.achadosperdidos.repository.EventoRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class EventoConfiguracaoService {
    private final EventoConfiguracaoRepository eventoConfiguracaoRepository;
    private final EventoRepository eventoRepository;
    private final SignedResourceIdCodec idCodec;

    public EventoConfiguracaoService(EventoConfiguracaoRepository eventoConfiguracaoRepository,
                                     EventoRepository eventoRepository, SignedResourceIdCodec idCodec) {
        this.eventoConfiguracaoRepository = eventoConfiguracaoRepository;
        this.eventoRepository = eventoRepository;
        this.idCodec = idCodec;
    }

    @Transactional(readOnly = true)
    public EventoConfiguracaoResponse findByEvento(String idEvento) {
        Long eventoId = idCodec.decodeEventoId(idEvento);
        return eventoConfiguracaoRepository.findByEvento_IdAndFgExcluidoFalse(eventoId)
                .map(this::toResponse)
                .orElseGet(() -> defaultResponse(eventoId));
    }

    @Transactional
    public EventoConfiguracaoResponse upsert(String idEvento, EventoConfiguracaoUpdateRequest request) {
        Long eventoId = idCodec.decodeEventoId(idEvento);
        Evento evento = eventoRepository.getReferenceById(eventoId);
        EventoConfiguracao cfg = eventoConfiguracaoRepository.findByEvento_IdAndFgExcluidoFalse(eventoId)
                .orElseGet(() -> {
                    EventoConfiguracao novo = new EventoConfiguracao();
                    novo.setEvento(evento);
                    novo.setDtCadastro(LocalDateTime.now());
                    novo.setFgExcluido(false);
                    return novo;
                });
        if (request.fgRecebeObjetos() != null) cfg.setFgRecebeObjetos(request.fgRecebeObjetos());
        if (request.fgAceitaClaim() != null) cfg.setFgAceitaClaim(request.fgAceitaClaim());
        if (request.fgConsultaPublica() != null) cfg.setFgConsultaPublica(request.fgConsultaPublica());
        if (request.fgFotoObrigatoria() != null) cfg.setFgFotoObrigatoria(request.fgFotoObrigatoria());
        if (request.fgValidacaoObrigatoria() != null) cfg.setFgValidacaoObrigatoria(request.fgValidacaoObrigatoria());
        if (request.qtMaxFotos() != null) cfg.setQtMaxFotos(request.qtMaxFotos());
        if (request.qtDiasDescarte() != null) cfg.setQtDiasDescarte(request.qtDiasDescarte());
        return toResponse(eventoConfiguracaoRepository.save(cfg));
    }

    private EventoConfiguracaoResponse defaultResponse(Long eventoId) {
        return new EventoConfiguracaoResponse(
                idCodec.encodeEventoId(eventoId), true, true, false, true, true, 10, 180);
    }

    private EventoConfiguracaoResponse toResponse(EventoConfiguracao cfg) {
        return new EventoConfiguracaoResponse(
                idCodec.encodeEventoId(cfg.getEvento().getId()),
                cfg.getFgRecebeObjetos(), cfg.getFgAceitaClaim(), cfg.getFgConsultaPublica(),
                cfg.getFgFotoObrigatoria(), cfg.getFgValidacaoObrigatoria(),
                cfg.getQtMaxFotos(), cfg.getQtDiasDescarte());
    }
}
