package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.DepositoCreateRequest;
import br.com.achadosperdidos.controller.dto.DepositoResponse;
import br.com.achadosperdidos.entity.Deposito;
import br.com.achadosperdidos.entity.Evento;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.repository.DepositoRepository;
import br.com.achadosperdidos.repository.EventoRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DepositoService {
    private final DepositoRepository depositoRepository;
    private final EventoRepository eventoRepository;
    private final SignedResourceIdCodec idCodec;

    public DepositoService(DepositoRepository depositoRepository, EventoRepository eventoRepository, SignedResourceIdCodec idCodec) {
        this.depositoRepository = depositoRepository;
        this.eventoRepository = eventoRepository;
        this.idCodec = idCodec;
    }

    @Transactional
    public DepositoResponse create(DepositoCreateRequest request) {
        Evento evento = eventoRepository.findById(idCodec.decodeEventoId(request.idEvento()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento não encontrado."));
        Deposito d = new Deposito();
        d.setEvento(evento);
        d.setNmDeposito(request.nmDeposito().trim());
        d.setDsDeposito(request.dsDeposito());
        d.setFgPrincipal(Boolean.TRUE.equals(request.fgPrincipal()));
        d.setDtCadastro(LocalDateTime.now());
        d.setFgAtivo(true);
        d.setFgExcluido(false);
        return toResponse(depositoRepository.save(d));
    }

    @Transactional(readOnly = true)
    public List<DepositoResponse> findByEvento(String idEvento) {
        return depositoRepository.findByEvento_IdAndFgExcluidoFalseOrderByNmDepositoAsc(idCodec.decodeEventoId(idEvento))
                .stream().map(this::toResponse).toList();
    }

    Deposito findEntity(Long id) {
        return depositoRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Depósito não encontrado."));
    }

    private DepositoResponse toResponse(Deposito d) {
        return new DepositoResponse(
                idCodec.encode(SignedResourceIdCodec.Kind.DEP, d.getId()),
                idCodec.encodeEventoId(d.getEvento().getId()),
                d.getNmDeposito(),
                d.getDsDeposito(),
                d.getFgPrincipal());
    }
}
