package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.DevolucaoCreateRequest;
import br.com.achadosperdidos.controller.dto.DevolucaoResponse;
import br.com.achadosperdidos.controller.dto.DevolucaoStatusRequest;
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
    private final WorkflowService workflowService;
    private final SignedResourceIdCodec idCodec;

    public DevolucaoService(DevolucaoRepository devolucaoRepository, ItemRepository itemRepository, ClaimRepository claimRepository,
                            WorkflowService workflowService, SignedResourceIdCodec idCodec) {
        this.devolucaoRepository = devolucaoRepository;
        this.itemRepository = itemRepository;
        this.claimRepository = claimRepository;
        this.workflowService = workflowService;
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
        d.setTpStatus(Boolean.TRUE.equals(request.fgConcluido()) ? "CONCLUIDO"
                : Boolean.TRUE.equals(request.fgAssinado()) ? "ASSINADO" : "AGUARDANDO_RETIRADA");
        d.setDtDevolucao(LocalDateTime.now());
        d.setDtCadastro(LocalDateTime.now());
        d.setFgAtivo(true);
        d.setFgExcluido(false);
        if (Boolean.TRUE.equals(d.getFgConcluido())) {
            item.setFgEntregue(true);
            item.setDtAlteracao(LocalDateTime.now());
            itemRepository.save(item);
            // Fecha o ciclo: se o status atual permitir, marca o item como Devolvido.
            workflowService.transitarSePermitido(request.idItem(), "Devolvido", "Devolução concluída ao responsável.");
        }
        return toResponse(devolucaoRepository.save(d));
    }

    @Transactional(readOnly = true)
    public List<DevolucaoResponse> findAll() {
        return devolucaoRepository.findByFgExcluidoFalseOrderByDtDevolucaoDesc().stream().map(this::toResponse).toList();
    }

    private static final java.util.Set<String> STATUS = java.util.Set.of(
            "AGUARDANDO_RETIRADA", "EM_CONFERENCIA", "AGUARDANDO_ASSINATURA", "ASSINADO", "CONCLUIDO");

    /** Avança o status da devolução; ao concluir, marca o item como Devolvido. */
    @Transactional
    public DevolucaoResponse atualizarStatus(String idToken, DevolucaoStatusRequest request) {
        Devolucao d = devolucaoRepository.findById(idCodec.decodeDevolucaoId(idToken))
                .filter(x -> !Boolean.TRUE.equals(x.getFgExcluido()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Devolução não encontrada."));
        String destino = request.tpStatus().trim().toUpperCase();
        if (!STATUS.contains(destino)) {
            throw new IllegalArgumentException("Status de devolução inválido: " + request.tpStatus());
        }
        d.setTpStatus(destino);
        if (request.dsObservacao() != null && !request.dsObservacao().isBlank()) {
            d.setDsObservacao(request.dsObservacao());
        }
        if ("ASSINADO".equals(destino) || "CONCLUIDO".equals(destino)) {
            d.setFgAssinado(true);
        }
        if ("CONCLUIDO".equals(destino)) {
            d.setFgConcluido(true);
            Item item = d.getItem();
            item.setFgEntregue(true);
            item.setDtAlteracao(LocalDateTime.now());
            itemRepository.save(item);
            workflowService.transitarSePermitido(idCodec.encodeItemId(item.getId()), "Devolvido",
                    "Devolução concluída ao responsável.");
        }
        d.setDtAlteracao(LocalDateTime.now());
        return toResponse(devolucaoRepository.save(d));
    }

    private DevolucaoResponse toResponse(Devolucao d) {
        Item item = d.getItem();
        return new DevolucaoResponse(
                idCodec.encodeDevolucaoId(d.getId()),
                idCodec.encodeItemId(item.getId()),
                d.getClaim() != null ? idCodec.encodeClaimId(d.getClaim().getId()) : null,
                item.getCdItem(),
                item.getNmTitulo(),
                item.getCategoria() != null ? item.getCategoria().getNmCategoria() : null,
                item.getNmLocalEncontrado(),
                d.getTpDevolucao(), d.getNmRecebedor(), d.getTpStatus(), d.getFgAssinado(), d.getFgConcluido(), d.getDtDevolucao());
    }
}
