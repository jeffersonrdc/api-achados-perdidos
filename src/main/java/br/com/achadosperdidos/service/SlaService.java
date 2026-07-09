package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.SlaRegistroResponse;
import br.com.achadosperdidos.controller.dto.SlaRegraCreateRequest;
import br.com.achadosperdidos.controller.dto.SlaRegraResponse;
import br.com.achadosperdidos.entity.SlaRegra;
import br.com.achadosperdidos.repository.EventoRepository;
import br.com.achadosperdidos.repository.SlaRegistroRepository;
import br.com.achadosperdidos.repository.SlaRegraRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SlaService {
    private final SlaRegistroRepository slaRegistroRepository;
    private final SlaRegraRepository slaRegraRepository;
    private final EventoRepository eventoRepository;
    private final SignedResourceIdCodec idCodec;

    public SlaService(SlaRegistroRepository slaRegistroRepository, SlaRegraRepository slaRegraRepository,
                      EventoRepository eventoRepository, SignedResourceIdCodec idCodec) {
        this.slaRegistroRepository = slaRegistroRepository;
        this.slaRegraRepository = slaRegraRepository;
        this.eventoRepository = eventoRepository;
        this.idCodec = idCodec;
    }

    @Transactional(readOnly = true)
    public List<SlaRegistroResponse> listarPendentes() {
        return slaRegistroRepository.findByStSlaInAndFgExcluidoFalseOrderByDtLimiteAsc(List.of("EM_ANDAMENTO", "ALERTA", "ESTOURADO"))
                .stream().map(s -> new SlaRegistroResponse(
                        idCodec.encodeSlaId(s.getId()),
                        s.getTpEntidade(),
                        idCodec.encodeEntidadeId(s.getTpEntidade(), s.getIdEntidade()),
                        s.getStSla(),
                        s.getDtInicio(),
                        s.getDtLimite(),
                        s.getDtConclusao())).toList();
    }

    @Transactional
    public SlaRegraResponse createRegra(SlaRegraCreateRequest request) {
        SlaRegra r = new SlaRegra();
        if (request.idEvento() != null && !request.idEvento().isBlank()) {
            r.setEvento(eventoRepository.getReferenceById(idCodec.decodeEventoId(request.idEvento())));
        }
        r.setTpProcesso(request.tpProcesso().trim().toUpperCase());
        r.setQtHorasLimite(request.qtHorasLimite());
        r.setQtHorasAlerta(request.qtHorasAlerta());
        r.setFgEnviarAlerta(request.fgEnviarAlerta() == null || request.fgEnviarAlerta());
        r.setDsObservacao(request.dsObservacao());
        r.setDtCadastro(LocalDateTime.now());
        r.setFgAtivo(true);
        r.setFgExcluido(false);
        return toRegraResponse(slaRegraRepository.save(r));
    }

    @Transactional(readOnly = true)
    public List<SlaRegraResponse> listarRegras(String idEvento) {
        List<SlaRegra> regras = (idEvento != null && !idEvento.isBlank())
                ? slaRegraRepository.findByEvento_IdAndFgExcluidoFalseAndFgAtivoTrueOrderByTpProcessoAsc(idCodec.decodeEventoId(idEvento))
                : slaRegraRepository.findByFgExcluidoFalseAndFgAtivoTrueOrderByTpProcessoAsc();
        return regras.stream().map(this::toRegraResponse).toList();
    }

    private SlaRegraResponse toRegraResponse(SlaRegra r) {
        return new SlaRegraResponse(
                idCodec.encodeSlaRegraId(r.getId()),
                r.getEvento() != null ? idCodec.encodeEventoId(r.getEvento().getId()) : null,
                r.getTpProcesso(), r.getQtHorasLimite(), r.getQtHorasAlerta(), r.getFgEnviarAlerta(), r.getDsObservacao());
    }
}
