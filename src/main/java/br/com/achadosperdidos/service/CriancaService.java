package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.CriancaCreateRequest;
import br.com.achadosperdidos.controller.dto.CriancaResponsavelCreateRequest;
import br.com.achadosperdidos.controller.dto.CriancaResponsavelResponse;
import br.com.achadosperdidos.controller.dto.CriancaResponse;
import br.com.achadosperdidos.entity.Crianca;
import br.com.achadosperdidos.entity.CriancaResponsavel;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.repository.CriancaRepository;
import br.com.achadosperdidos.repository.CriancaResponsavelRepository;
import br.com.achadosperdidos.repository.EventoRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CriancaService {
    private final CriancaRepository criancaRepository;
    private final CriancaResponsavelRepository responsavelRepository;
    private final EventoRepository eventoRepository;
    private final SignedResourceIdCodec idCodec;

    public CriancaService(CriancaRepository criancaRepository, CriancaResponsavelRepository responsavelRepository, EventoRepository eventoRepository, SignedResourceIdCodec idCodec) {
        this.criancaRepository = criancaRepository;
        this.responsavelRepository = responsavelRepository;
        this.eventoRepository = eventoRepository;
        this.idCodec = idCodec;
    }

    @Transactional
    public CriancaResponse create(CriancaCreateRequest request) {
        Crianca c = new Crianca();
        c.setEvento(eventoRepository.findById(idCodec.decodeEventoId(request.idEvento()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento não encontrado.")));
        c.setNmCrianca(request.nmCrianca());
        c.setDtNascimento(request.dtNascimento());
        c.setNrPulseira(request.nrPulseira());
        c.setNrQrCode(request.nrQrCode());
        c.setDsObservacao(request.dsObservacao());
        c.setDtCadastro(LocalDateTime.now());
        c.setFgAtivo(true);
        c.setFgExcluido(false);
        return toResponse(criancaRepository.save(c));
    }

    @Transactional
    public CriancaResponsavelResponse addResponsavel(CriancaResponsavelCreateRequest request) {
        Crianca crianca = criancaRepository.findById(idCodec.decodeCriancaId(request.idCrianca()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Criança não encontrada."));
        CriancaResponsavel r = new CriancaResponsavel();
        r.setCrianca(crianca);
        r.setNmResponsavel(request.nmResponsavel());
        r.setNrCpf(request.nrCpf());
        r.setNrRg(request.nrRg());
        r.setNmEmail(request.nmEmail());
        r.setNrTelefone(request.nrTelefone());
        r.setDsParentesco(request.dsParentesco());
        r.setFgPrincipal(Boolean.TRUE.equals(request.fgPrincipal()));
        r.setDtCadastro(LocalDateTime.now());
        r.setFgAtivo(true);
        r.setFgExcluido(false);
        return toResponsavelResponse(responsavelRepository.save(r));
    }

    @Transactional(readOnly = true)
    public List<CriancaResponse> findByEvento(String idEvento) {
        return criancaRepository.findByEvento_IdAndFgExcluidoFalseOrderByDtCadastroDesc(idCodec.decodeEventoId(idEvento))
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CriancaResponsavelResponse> findResponsaveis(String idCrianca) {
        return responsavelRepository.findByCrianca_IdAndFgExcluidoFalseOrderByFgPrincipalDesc(idCodec.decodeCriancaId(idCrianca))
                .stream().map(this::toResponsavelResponse).toList();
    }

    private CriancaResponse toResponse(Crianca c) {
        return new CriancaResponse(
                idCodec.encodeCriancaId(c.getId()),
                idCodec.encodeEventoId(c.getEvento().getId()),
                c.getNmCrianca(), c.getDtNascimento(), c.getNrPulseira(), c.getNrQrCode(), c.getDsObservacao());
    }

    private CriancaResponsavelResponse toResponsavelResponse(CriancaResponsavel r) {
        return new CriancaResponsavelResponse(
                idCodec.encodeCriancaResponsavelId(r.getId()),
                idCodec.encodeCriancaId(r.getCrianca().getId()),
                r.getNmResponsavel(), r.getNrCpf(), r.getNrTelefone(), r.getDsParentesco(), r.getFgPrincipal());
    }
}
