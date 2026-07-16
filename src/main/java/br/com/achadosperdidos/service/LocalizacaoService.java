package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.LocalizacaoCreateRequest;
import br.com.achadosperdidos.controller.dto.LocalizacaoResponse;
import br.com.achadosperdidos.entity.Localizacao;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.repository.LocalizacaoRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LocalizacaoService {
    private final LocalizacaoRepository localizacaoRepository;
    private final DepositoService depositoService;
    private final SignedResourceIdCodec idCodec;

    public LocalizacaoService(LocalizacaoRepository localizacaoRepository, DepositoService depositoService, SignedResourceIdCodec idCodec) {
        this.localizacaoRepository = localizacaoRepository;
        this.depositoService = depositoService;
        this.idCodec = idCodec;
    }

    @Transactional
    public LocalizacaoResponse create(LocalizacaoCreateRequest request) {
        Localizacao l = new Localizacao();
        l.setDeposito(depositoService.findEntity(idCodec.decode(SignedResourceIdCodec.Kind.DEP, request.idDeposito())));
        l.setNmSetor(request.nmSetor());
        l.setNmCorredor(request.nmCorredor());
        l.setNmEstante(request.nmEstante());
        l.setNmPrateleira(request.nmPrateleira());
        l.setNmCaixa(request.nmCaixa());
        l.setNmPosicao(request.nmPosicao());
        l.setDtCadastro(LocalDateTime.now());
        l.setFgAtivo(true);
        l.setFgExcluido(false);
        return toResponse(localizacaoRepository.save(l));
    }

    @Transactional(readOnly = true)
    public List<LocalizacaoResponse> findByDeposito(String idDeposito) {
        return localizacaoRepository.findByDeposito_IdAndFgExcluidoFalseOrderByIdAsc(idCodec.decode(SignedResourceIdCodec.Kind.DEP, idDeposito))
                .stream().map(this::toResponse).toList();
    }

    Localizacao findEntity(Long id) {
        return localizacaoRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Localização não encontrada."));
    }

    /** Reaproveita uma localização idêntica no depósito ou cria uma nova. */
    @Transactional
    public Localizacao findOrCreateEntity(Long depositoId, String setor, String corredor, String estante,
                                          String prateleira, String caixa, String posicao) {
        var deposito = depositoService.findEntity(depositoId);
        return localizacaoRepository.findByDeposito_IdAndFgExcluidoFalseOrderByIdAsc(depositoId).stream()
                .filter(l -> eq(l.getNmSetor(), setor) && eq(l.getNmCorredor(), corredor)
                        && eq(l.getNmEstante(), estante) && eq(l.getNmPrateleira(), prateleira)
                        && eq(l.getNmCaixa(), caixa) && eq(l.getNmPosicao(), posicao))
                .findFirst()
                .orElseGet(() -> {
                    Localizacao l = new Localizacao();
                    l.setDeposito(deposito);
                    l.setNmSetor(setor);
                    l.setNmCorredor(corredor);
                    l.setNmEstante(estante);
                    l.setNmPrateleira(prateleira);
                    l.setNmCaixa(caixa);
                    l.setNmPosicao(posicao);
                    l.setDtCadastro(LocalDateTime.now());
                    l.setFgAtivo(true);
                    l.setFgExcluido(false);
                    return localizacaoRepository.save(l);
                });
    }

    private static boolean eq(String a, String b) {
        return (a == null ? "" : a.trim()).equalsIgnoreCase(b == null ? "" : b.trim());
    }

    private LocalizacaoResponse toResponse(Localizacao l) {
        return new LocalizacaoResponse(
                idCodec.encode(SignedResourceIdCodec.Kind.LOC, l.getId()),
                idCodec.encode(SignedResourceIdCodec.Kind.DEP, l.getDeposito().getId()),
                l.getNmSetor(), l.getNmCorredor(), l.getNmEstante(), l.getNmPrateleira(), l.getNmCaixa(), l.getNmPosicao());
    }
}
