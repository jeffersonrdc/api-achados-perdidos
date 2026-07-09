package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.ClaimValidacaoCreateRequest;
import br.com.achadosperdidos.controller.dto.ClaimValidacaoResponse;
import br.com.achadosperdidos.entity.ClaimValidacao;
import br.com.achadosperdidos.repository.ClaimRepository;
import br.com.achadosperdidos.repository.ClaimValidacaoRepository;
import br.com.achadosperdidos.repository.ItemRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ClaimValidacaoService {
    private final ClaimValidacaoRepository claimValidacaoRepository;
    private final ClaimRepository claimRepository;
    private final ItemRepository itemRepository;
    private final SignedResourceIdCodec idCodec;

    public ClaimValidacaoService(ClaimValidacaoRepository claimValidacaoRepository, ClaimRepository claimRepository,
                                 ItemRepository itemRepository, SignedResourceIdCodec idCodec) {
        this.claimValidacaoRepository = claimValidacaoRepository;
        this.claimRepository = claimRepository;
        this.itemRepository = itemRepository;
        this.idCodec = idCodec;
    }

    @Transactional
    public ClaimValidacaoResponse create(ClaimValidacaoCreateRequest request) {
        ClaimValidacao v = new ClaimValidacao();
        v.setClaim(claimRepository.getReferenceById(idCodec.decodeClaimId(request.idClaim())));
        v.setItem(itemRepository.getReferenceById(idCodec.decodeItemId(request.idItem())));
        v.setQtSimilaridade(request.qtSimilaridade());
        v.setStResultado(request.stResultado() != null ? request.stResultado().trim().toUpperCase() : "PENDENTE");
        v.setDtValidacao("APROVADO".equals(v.getStResultado()) || "REJEITADO".equals(v.getStResultado()) ? LocalDateTime.now() : null);
        v.setDtCadastro(LocalDateTime.now());
        v.setFgExcluido(false);
        return toResponse(claimValidacaoRepository.save(v));
    }

    @Transactional(readOnly = true)
    public List<ClaimValidacaoResponse> findByClaim(String idClaim) {
        return claimValidacaoRepository.findByClaim_IdAndFgExcluidoFalseOrderByDtCadastroDesc(idCodec.decodeClaimId(idClaim))
                .stream().map(this::toResponse).toList();
    }

    private ClaimValidacaoResponse toResponse(ClaimValidacao v) {
        return new ClaimValidacaoResponse(
                idCodec.encodeClaimValidacaoId(v.getId()),
                idCodec.encodeClaimId(v.getClaim().getId()),
                idCodec.encodeItemId(v.getItem().getId()),
                v.getQtSimilaridade(),
                v.getStResultado(),
                v.getDtValidacao());
    }
}
