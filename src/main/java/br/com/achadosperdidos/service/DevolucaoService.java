package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.DevolucaoCreateRequest;
import br.com.achadosperdidos.controller.dto.DevolucaoResponse;
import br.com.achadosperdidos.entity.Devolucao;
import br.com.achadosperdidos.entity.Item;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.repository.ClaimRepository;
import br.com.achadosperdidos.repository.DevolucaoRepository;
import br.com.achadosperdidos.repository.ItemRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DevolucaoService {
    private final DevolucaoRepository devolucaoRepository;
    private final ItemRepository itemRepository;
    private final ClaimRepository claimRepository;
    private final SignedResourceIdCodec idCodec;

    public DevolucaoService(DevolucaoRepository devolucaoRepository, ItemRepository itemRepository, ClaimRepository claimRepository, SignedResourceIdCodec idCodec) {
        this.devolucaoRepository = devolucaoRepository;
        this.itemRepository = itemRepository;
        this.claimRepository = claimRepository;
        this.idCodec = idCodec;
    }

    @Transactional
    public DevolucaoResponse create(DevolucaoCreateRequest request) {
        Item item = itemRepository.findById(idCodec.decodeItemId(request.idItem()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item não encontrado."));
        Devolucao d = new Devolucao();
        d.setItem(item);
        if (request.idClaim() != null && !request.idClaim().isBlank()) {
            d.setClaim(claimRepository.findById(idCodec.decodeClaimId(request.idClaim()))
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Claim não encontrado.")));
        }
        d.setTpDevolucao(request.tpDevolucao());
        d.setNmRecebedor(request.nmRecebedor());
        d.setNrCpf(request.nrCpf());
        d.setDsObservacao(request.dsObservacao());
        d.setFgAssinado(Boolean.TRUE.equals(request.fgAssinado()));
        d.setFgConcluido(Boolean.TRUE.equals(request.fgConcluido()));
        d.setDtDevolucao(LocalDateTime.now());
        d.setDtCadastro(LocalDateTime.now());
        d.setFgAtivo(true);
        d.setFgExcluido(false);
        if (Boolean.TRUE.equals(d.getFgConcluido())) {
            item.setFgEntregue(true);
            item.setDtAlteracao(LocalDateTime.now());
            itemRepository.save(item);
        }
        return toResponse(devolucaoRepository.save(d));
    }

    @Transactional(readOnly = true)
    public List<DevolucaoResponse> findAll() {
        return devolucaoRepository.findByFgExcluidoFalseOrderByDtDevolucaoDesc().stream().map(this::toResponse).toList();
    }

    private DevolucaoResponse toResponse(Devolucao d) {
        return new DevolucaoResponse(
                idCodec.encodeDevolucaoId(d.getId()),
                idCodec.encodeItemId(d.getItem().getId()),
                d.getClaim() != null ? idCodec.encodeClaimId(d.getClaim().getId()) : null,
                d.getTpDevolucao(), d.getNmRecebedor(), d.getFgAssinado(), d.getFgConcluido(), d.getDtDevolucao());
    }
}
