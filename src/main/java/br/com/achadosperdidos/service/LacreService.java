package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.LacreCreateRequest;
import br.com.achadosperdidos.controller.dto.LacreResponse;
import br.com.achadosperdidos.entity.Evento;
import br.com.achadosperdidos.entity.Lacre;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.repository.EventoRepository;
import br.com.achadosperdidos.repository.LacreRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class LacreService {
    private final LacreRepository lacreRepository;
    private final EventoRepository eventoRepository;
    private final SignedResourceIdCodec idCodec;
    private final AuditoriaContextService auditoriaContext;

    public LacreService(LacreRepository lacreRepository, EventoRepository eventoRepository,
                        SignedResourceIdCodec idCodec, AuditoriaContextService auditoriaContext) {
        this.lacreRepository = lacreRepository;
        this.eventoRepository = eventoRepository;
        this.idCodec = idCodec;
        this.auditoriaContext = auditoriaContext;
    }

    @Transactional
    public LacreResponse create(LacreCreateRequest request) {
        auditoriaContext.marcarContexto();
        Evento evento = eventoRepository.findById(idCodec.decodeEventoId(request.idEvento()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento não encontrado."));
        Lacre l = new Lacre();
        l.setEvento(evento);
        l.setNrLacre(request.nrLacre().trim());
        l.setNrCodigoBarra(request.nrCodigoBarra());
        l.setNrQrCode(request.nrQrCode());
        l.setFgViolado(Boolean.TRUE.equals(request.fgViolado()));
        l.setDsObservacao(request.dsObservacao());
        l.setDtLacre(LocalDateTime.now());
        l.setDtCadastro(LocalDateTime.now());
        l.setFgAtivo(true);
        l.setFgExcluido(false);
        return toResponse(lacreRepository.save(l));
    }

    @Transactional(readOnly = true)
    public LacreResponse findByNumero(String nrLacre) {
        return lacreRepository.findByNrLacreAndFgExcluidoFalse(nrLacre.trim())
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Lacre não encontrado."));
    }

    private LacreResponse toResponse(Lacre l) {
        return new LacreResponse(
                idCodec.encodeLacreId(l.getId()),
                idCodec.encodeEventoId(l.getEvento().getId()),
                l.getNrLacre(), l.getNrCodigoBarra(), l.getNrQrCode(),
                l.getFgViolado(), l.getDsObservacao(), l.getDtLacre());
    }
}
