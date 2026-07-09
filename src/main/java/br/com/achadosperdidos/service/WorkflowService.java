package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.ItemHistoricoResponse;
import br.com.achadosperdidos.controller.dto.ItemMovimentacaoCreateRequest;
import br.com.achadosperdidos.controller.dto.ItemMovimentacaoResponse;
import br.com.achadosperdidos.entity.ItemMovimentacao;
import br.com.achadosperdidos.entity.ItemHistorico;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.repository.ItemHistoricoRepository;
import br.com.achadosperdidos.repository.ItemMovimentacaoRepository;
import br.com.achadosperdidos.repository.ItemRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class WorkflowService {
    private final ItemMovimentacaoRepository itemMovimentacaoRepository;
    private final ItemHistoricoRepository itemHistoricoRepository;
    private final ItemRepository itemRepository;
    private final LocalizacaoService localizacaoService;
    private final SignedResourceIdCodec idCodec;

    public WorkflowService(ItemMovimentacaoRepository itemMovimentacaoRepository, ItemHistoricoRepository itemHistoricoRepository,
                           ItemRepository itemRepository, LocalizacaoService localizacaoService, SignedResourceIdCodec idCodec) {
        this.itemMovimentacaoRepository = itemMovimentacaoRepository;
        this.itemHistoricoRepository = itemHistoricoRepository;
        this.itemRepository = itemRepository;
        this.localizacaoService = localizacaoService;
        this.idCodec = idCodec;
    }

    @Transactional
    public ItemMovimentacaoResponse registrarMovimentacao(ItemMovimentacaoCreateRequest request) {
        ItemMovimentacao m = new ItemMovimentacao();
        m.setItem(itemRepository.findById(idCodec.decodeItemId(request.idItem()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item não encontrado.")));
        if (request.idLocalizacaoOrigem() != null && !request.idLocalizacaoOrigem().isBlank()) {
            m.setLocalizacaoOrigem(localizacaoService.findEntity(idCodec.decodeLocalizacaoId(request.idLocalizacaoOrigem())));
        }
        m.setLocalizacaoDestino(localizacaoService.findEntity(idCodec.decodeLocalizacaoId(request.idLocalizacaoDestino())));
        m.setTpMovimento(request.tpMovimento());
        m.setDsMotivo(request.dsMotivo());
        m.setDtMovimento(LocalDateTime.now());
        m.setDtCadastro(LocalDateTime.now());
        m.setFgAtivo(true);
        m.setFgExcluido(false);
        return toResponse(itemMovimentacaoRepository.save(m));
    }

    @Transactional(readOnly = true)
    public List<ItemMovimentacaoResponse> historicoItem(String idItem) {
        return itemMovimentacaoRepository.findByItem_IdAndFgExcluidoFalseOrderByDtMovimentoDesc(idCodec.decodeItemId(idItem))
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ItemHistoricoResponse> historicoStatusItem(String idItem) {
        return itemHistoricoRepository.findByItem_IdAndFgExcluidoFalseOrderByDtHistoricoDesc(idCodec.decodeItemId(idItem))
                .stream().map(this::toHistoricoResponse).toList();
    }

    private ItemHistoricoResponse toHistoricoResponse(ItemHistorico h) {
        return new ItemHistoricoResponse(
                idCodec.encodeItemHistoricoId(h.getId()),
                idCodec.encodeItemId(h.getItem().getId()),
                h.getStatusAnterior() != null ? idCodec.encodeStatusId(h.getStatusAnterior().getId()) : null,
                idCodec.encodeStatusId(h.getStatusNovo().getId()),
                h.getStatusAnterior() != null ? h.getStatusAnterior().getNmStatus() : null,
                h.getStatusNovo().getNmStatus(),
                h.getDsHistorico(),
                h.getDtHistorico());
    }

    private ItemMovimentacaoResponse toResponse(ItemMovimentacao m) {
        return new ItemMovimentacaoResponse(
                idCodec.encodeMovimentacaoId(m.getId()),
                idCodec.encodeItemId(m.getItem().getId()),
                m.getLocalizacaoOrigem() != null ? idCodec.encodeLocalizacaoId(m.getLocalizacaoOrigem().getId()) : null,
                idCodec.encodeLocalizacaoId(m.getLocalizacaoDestino().getId()),
                m.getTpMovimento(),
                m.getDsMotivo(),
                m.getDtMovimento());
    }
}
