package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.ItemHistoricoResponse;
import br.com.achadosperdidos.controller.dto.ItemMovimentacaoCreateRequest;
import br.com.achadosperdidos.controller.dto.ItemMovimentacaoResponse;
import br.com.achadosperdidos.controller.dto.ItemTransicaoRequest;
import br.com.achadosperdidos.controller.dto.ItemTransicaoResponse;
import br.com.achadosperdidos.entity.Item;
import br.com.achadosperdidos.entity.ItemMovimentacao;
import br.com.achadosperdidos.entity.ItemHistorico;
import br.com.achadosperdidos.entity.StatusItem;
import br.com.achadosperdidos.entity.Usuario;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.exception.TransicaoInvalidaException;
import br.com.achadosperdidos.repository.ItemHistoricoRepository;
import br.com.achadosperdidos.repository.ItemMovimentacaoRepository;
import br.com.achadosperdidos.repository.ItemRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class WorkflowService {

    /**
     * Grafo de transicoes do ciclo de vida do item, conforme a Especificacao
     * Funcional (secoes 4, 6, 8 e 11). A chave e o status atual; o valor sao os
     * destinos permitidos.
     */
    private static final Map<String, Set<String>> TRANSICOES = Map.ofEntries(
            Map.entry("Encontrado", Set.of("Coletado", "Descartado")),
            Map.entry("Coletado", Set.of("Aguardando triagem", "Descartado")),
            Map.entry("Aguardando triagem", Set.of("Em triagem", "Descartado")),
            Map.entry("Em triagem", Set.of("Em transporte para estoque", "Aguardando triagem", "Descartado")),
            Map.entry("Em transporte para estoque", Set.of("Em estoque")),
            Map.entry("Em estoque", Set.of("Com pedido de devolucao", "Aguardando retirada", "Descartado")),
            Map.entry("Com pedido de devolucao", Set.of("Aguardando retirada", "Em estoque", "Descartado")),
            Map.entry("Aguardando retirada", Set.of("Devolvido", "Em estoque")),
            Map.entry("Devolvido", Set.of("Finalizado")),
            Map.entry("Descartado", Set.of("Finalizado")),
            Map.entry("Finalizado", Set.of())
    );

    private final ItemMovimentacaoRepository itemMovimentacaoRepository;
    private final ItemHistoricoRepository itemHistoricoRepository;
    private final ItemRepository itemRepository;
    private final LocalizacaoService localizacaoService;
    private final StatusItemService statusItemService;
    private final UsuarioContextService usuarioContextService;
    private final SignedResourceIdCodec idCodec;

    public WorkflowService(ItemMovimentacaoRepository itemMovimentacaoRepository, ItemHistoricoRepository itemHistoricoRepository,
                           ItemRepository itemRepository, LocalizacaoService localizacaoService,
                           StatusItemService statusItemService, UsuarioContextService usuarioContextService,
                           SignedResourceIdCodec idCodec) {
        this.itemMovimentacaoRepository = itemMovimentacaoRepository;
        this.itemHistoricoRepository = itemHistoricoRepository;
        this.itemRepository = itemRepository;
        this.localizacaoService = localizacaoService;
        this.statusItemService = statusItemService;
        this.usuarioContextService = usuarioContextService;
        this.idCodec = idCodec;
    }

    // ---------------------------------------------------------------------
    // Transicoes de status (motor de workflow do item)
    // ---------------------------------------------------------------------

    @Transactional
    public ItemTransicaoResponse transitar(String idItem, ItemTransicaoRequest request) {
        Item item = itemRepository.findById(idCodec.decodeItemId(idItem))
                .filter(i -> !Boolean.TRUE.equals(i.getFgExcluido()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item não encontrado."));

        StatusItem statusAtual = item.getStatus();
        String origem = statusAtual.getNmStatus();
        String destino = request.nmStatusDestino().trim();

        if (origem.equalsIgnoreCase(destino)) {
            throw new TransicaoInvalidaException("O item já está no status \"" + destino + "\".");
        }
        Set<String> permitidos = TRANSICOES.getOrDefault(origem, Set.of());
        boolean ok = permitidos.stream().anyMatch(s -> s.equalsIgnoreCase(destino));
        if (!ok) {
            throw new TransicaoInvalidaException(
                    "Transição não permitida de \"" + origem + "\" para \"" + destino + "\". "
                            + "Destinos válidos: " + (permitidos.isEmpty() ? "nenhum (status final)" : String.join(", ", permitidos)) + ".");
        }

        StatusItem statusNovo = statusItemService.findByNomeOrDefault(destino, destino);
        item.setStatus(statusNovo);
        // Espelha os flags de negocio consistentes com o status final atingido.
        if ("Devolvido".equalsIgnoreCase(destino)) {
            item.setFgEntregue(true);
        } else if ("Descartado".equalsIgnoreCase(destino)) {
            item.setFgDescartado(true);
        }
        item.setDtAlteracao(LocalDateTime.now());
        itemRepository.save(item);

        ItemHistorico historico = registrarHistorico(item, statusAtual, statusNovo, request.dsObservacao());

        return new ItemTransicaoResponse(
                idCodec.encodeItemId(item.getId()),
                statusAtual.getNmStatus(),
                statusNovo.getNmStatus(),
                historico.getDsHistorico(),
                historico.getDtHistorico(),
                List.copyOf(TRANSICOES.getOrDefault(statusNovo.getNmStatus(), Set.of())));
    }

    /**
     * Executa a transicao apenas se ela for permitida a partir do status atual,
     * sem lancar excecao caso nao seja. Usado por fluxos que apenas "sugerem" a
     * mudanca de status (ex.: conclusao de devolucao -> Devolvido). Retorna true
     * se a transicao foi aplicada.
     */
    @Transactional
    public boolean transitarSePermitido(String idItem, String destino, String observacao) {
        Item item = itemRepository.findById(idCodec.decodeItemId(idItem))
                .filter(i -> !Boolean.TRUE.equals(i.getFgExcluido()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item não encontrado."));
        Set<String> permitidos = TRANSICOES.getOrDefault(item.getStatus().getNmStatus(), Set.of());
        if (permitidos.stream().noneMatch(s -> s.equalsIgnoreCase(destino))) {
            return false;
        }
        transitar(idItem, new ItemTransicaoRequest(destino, observacao));
        return true;
    }

    @Transactional(readOnly = true)
    public List<String> transicoesPermitidas(String idItem) {
        Item item = itemRepository.findById(idCodec.decodeItemId(idItem))
                .filter(i -> !Boolean.TRUE.equals(i.getFgExcluido()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item não encontrado."));
        return List.copyOf(TRANSICOES.getOrDefault(item.getStatus().getNmStatus(), Set.of()));
    }

    /**
     * Grava um registro na linha do tempo de status do item. Reutilizado pelo
     * cadastro inicial (statusAnterior == null) e pelas transicoes.
     */
    @Transactional
    public ItemHistorico registrarHistorico(Item item, StatusItem statusAnterior, StatusItem statusNovo, String observacao) {
        ItemHistorico h = new ItemHistorico();
        h.setItem(item);
        h.setStatusAnterior(statusAnterior);
        h.setStatusNovo(statusNovo);
        h.setUsuario(usuarioLogadoOuNulo());
        h.setDsHistorico(descricaoHistorico(statusAnterior, statusNovo, observacao));
        h.setDtHistorico(LocalDateTime.now());
        h.setDtCadastro(LocalDateTime.now());
        h.setFgExcluido(false);
        return itemHistoricoRepository.save(h);
    }

    private String descricaoHistorico(StatusItem anterior, StatusItem novo, String observacao) {
        String base = anterior == null
                ? "Item cadastrado com status \"" + novo.getNmStatus() + "\"."
                : "Status alterado de \"" + anterior.getNmStatus() + "\" para \"" + novo.getNmStatus() + "\".";
        return (observacao != null && !observacao.isBlank()) ? base + " " + observacao.trim() : base;
    }

    private Usuario usuarioLogadoOuNulo() {
        try {
            return usuarioContextService.requireUsuarioLogado();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    // ---------------------------------------------------------------------
    // Movimentacoes fisicas / consultas de historico
    // ---------------------------------------------------------------------

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
