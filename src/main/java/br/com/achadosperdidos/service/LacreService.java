package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.LacreCreateRequest;
import br.com.achadosperdidos.controller.dto.LacreResponse;
import br.com.achadosperdidos.entity.Lacre;
import br.com.achadosperdidos.repository.LacreRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class LacreService {
    private final LacreRepository lacreRepository;
    private final SignedResourceIdCodec idCodec;

    public LacreService(LacreRepository lacreRepository, SignedResourceIdCodec idCodec) {
        this.lacreRepository = lacreRepository;
        this.idCodec = idCodec;
    }

    @Transactional
    public LacreResponse create(LacreCreateRequest request) {
        Lacre l = new Lacre();
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
                l.getNrLacre(), l.getNrCodigoBarra(), l.getNrQrCode(),
                l.getFgViolado(), l.getDsObservacao(), l.getDtLacre());
    }
}
