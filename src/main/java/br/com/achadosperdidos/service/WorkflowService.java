package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.ItemHistoricoResponse;
import br.com.achadosperdidos.controller.dto.ItemMovimentacaoCreateRequest;
import br.com.achadosperdidos.controller.dto.ItemMovimentacaoResponse;
import br.com.achadosperdidos.controller.dto.ItemTransicaoRequest;
import br.com.achadosperdidos.controller.dto.ItemTransicaoResponse;
import br.com.achadosperdidos.controller.dto.MovimentacaoEventoResponse;
import br.com.achadosperdidos.controller.dto.MovimentacaoResumoResponse;
import br.com.achadosperdidos.pagination.ApiPage;
import br.com.achadosperdidos.pagination.PaginationMeta;
import br.com.achadosperdidos.pagination.PaginationParams;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
            Map.entry("Coletado", Set.of("Aguardando triagem", "Em estoque", "Descartado")),
            Map.entry("Aguardando triagem", Set.of("Em Análise", "Em triagem", "Descartado")),
            Map.entry("Em Análise", Set.of("Em triagem", "Em transporte para estoque", "Em estoque", "Aguardando triagem", "Descartado")),
            // Concluir a triagem leva direto ao estoque; "Em transporte" segue disponível para a logística física.
            Map.entry("Em triagem", Set.of("Em Análise", "Em transporte para estoque", "Em estoque", "Aguardando triagem", "Descartado")),
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
    private final AuditoriaContextService auditoriaContext;
    private final SignedResourceIdCodec idCodec;

    public WorkflowService(ItemMovimentacaoRepository itemMovimentacaoRepository, ItemHistoricoRepository itemHistoricoRepository,
                           ItemRepository itemRepository, LocalizacaoService localizacaoService,
                           StatusItemService statusItemService, UsuarioContextService usuarioContextService,
                           AuditoriaContextService auditoriaContext, SignedResourceIdCodec idCodec) {
        this.itemMovimentacaoRepository = itemMovimentacaoRepository;
        this.itemHistoricoRepository = itemHistoricoRepository;
        this.itemRepository = itemRepository;
        this.localizacaoService = localizacaoService;
        this.statusItemService = statusItemService;
        this.usuarioContextService = usuarioContextService;
        this.auditoriaContext = auditoriaContext;
        this.idCodec = idCodec;
    }

    // ---------------------------------------------------------------------
    // Transicoes de status (motor de workflow do item)
    // ---------------------------------------------------------------------

    @Transactional
    public ItemTransicaoResponse transitar(String idItem, ItemTransicaoRequest request) {
        auditoriaContext.marcarContexto();
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

    /** Registra uma movimentação a partir das entidades (usada por fluxos internos, ex.: estoque). */
    @Transactional
    public void registrarMovimentacao(Item item, br.com.achadosperdidos.entity.Localizacao origem,
                                      br.com.achadosperdidos.entity.Localizacao destino, String tpMovimento, String dsMotivo) {
        ItemMovimentacao m = new ItemMovimentacao();
        m.setItem(item);
        m.setLocalizacaoOrigem(origem);
        m.setLocalizacaoDestino(destino);
        m.setTpMovimento(tpMovimento);
        m.setDsMotivo(dsMotivo);
        m.setDtMovimento(LocalDateTime.now());
        m.setDtCadastro(LocalDateTime.now());
        m.setFgAtivo(true);
        m.setFgExcluido(false);
        itemMovimentacaoRepository.save(m);
    }

    @Transactional
    public ItemMovimentacaoResponse registrarMovimentacao(ItemMovimentacaoCreateRequest request) {
        auditoriaContext.marcarContexto();
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

    /** Movimentações/transferências do evento, paginadas e filtradas (server-side). */
    @Transactional(readOnly = true)
    public ApiPage<MovimentacaoEventoResponse> listarMovimentacoesPorEvento(String idEvento, Integer page, Integer limit,
                                                                            String q, String tpMovimento, String data) {
        int p = PaginationParams.resolvePage(page);
        int l = PaginationParams.resolveLimit(limit);
        Long eventoId = idCodec.decodeEventoId(idEvento);
        LocalDate dataMov = parseData(data);

        Specification<ItemMovimentacao> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.isFalse(root.get("fgExcluido")));
            ps.add(cb.equal(root.get("item").get("evento").get("id"), eventoId));
            if (tpMovimento != null && !tpMovimento.isBlank())
                ps.add(cb.equal(root.get("tpMovimento"), tpMovimento));
            if (dataMov != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("dtMovimento"), dataMov.atStartOfDay()));
                ps.add(cb.lessThan(root.get("dtMovimento"), dataMov.plusDays(1).atStartOfDay()));
            }
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("item").get("nmTitulo")), like),
                        cb.like(cb.lower(root.get("item").get("cdItem")), like)));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<ItemMovimentacao> result = itemMovimentacaoRepository.findAll(spec,
                PageRequest.of(p - 1, l, Sort.by(Sort.Direction.DESC, "dtMovimento")));
        var content = result.getContent().stream().map(this::toEventoResponse).toList();
        var meta = new PaginationMeta(p, l, result.getTotalElements(), result.getTotalPages());
        return ApiPage.paged(content, meta);
    }

    @Transactional(readOnly = true)
    public MovimentacaoResumoResponse resumoMovimentacoes(String idEvento, String data) {
        Long ev = idCodec.decodeEventoId(idEvento);
        LocalDate dia = parseData(data);
        LocalDateTime inicio = dia != null ? dia.atStartOfDay() : null;
        LocalDateTime fim = dia != null ? dia.plusDays(1).atStartOfDay() : null;

        List<MovimentacaoResumoResponse.TipoQt> porTipo = itemMovimentacaoRepository
                .contagemPorTipo(ev, inicio, fim).stream()
                .map(r -> new MovimentacaoResumoResponse.TipoQt(
                        r[0] != null ? r[0].toString() : "OUTROS", ((Number) r[1]).longValue()))
                .toList();
        long total = porTipo.stream().mapToLong(MovimentacaoResumoResponse.TipoQt::qt).sum();

        Specification<ItemMovimentacao> base = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.isFalse(root.get("fgExcluido")));
            ps.add(cb.equal(root.get("item").get("evento").get("id"), ev));
            if (inicio != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("dtMovimento"), inicio));
                ps.add(cb.lessThan(root.get("dtMovimento"), fim));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        long transferencias = itemMovimentacaoRepository.count(base.and((root, query, cb) ->
                cb.equal(root.get("tpMovimento"), "TRANSFERENCIA")));
        long armazenamentos = itemMovimentacaoRepository.count(base.and((root, query, cb) ->
                cb.equal(root.get("tpMovimento"), "ARMAZENAMENTO")));
        long outros = total - transferencias - armazenamentos;
        return new MovimentacaoResumoResponse(total, transferencias, armazenamentos, outros, porTipo);
    }

    private MovimentacaoEventoResponse toEventoResponse(ItemMovimentacao m) {
        Item item = m.getItem();
        Usuario resp = item.getUsuarioAlteracao() != null ? item.getUsuarioAlteracao() : item.getUsuarioCadastro();
        return new MovimentacaoEventoResponse(
                idCodec.encodeMovimentacaoId(m.getId()),
                idCodec.encodeItemId(item.getId()),
                item.getCdItem(),
                item.getNmTitulo(),
                item.getCategoria() != null ? item.getCategoria().getNmCategoria() : null,
                m.getTpMovimento(),
                m.getDsMotivo(),
                localizacaoLabel(m.getLocalizacaoOrigem()),
                localizacaoLabel(m.getLocalizacaoDestino()),
                resp != null ? resp.getNmUsuario() : null,
                m.getDtMovimento());
    }

    private LocalDate parseData(String data) {
        if (data == null || data.isBlank()) return null;
        String v = data.trim();
        try {
            if (v.contains("/")) return LocalDate.parse(v, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            return LocalDate.parse(v);
        } catch (Exception e) {
            return null;
        }
    }

    private String localizacaoLabel(br.com.achadosperdidos.entity.Localizacao loc) {
        if (loc == null) return null;
        var partes = new java.util.ArrayList<String>();
        if (loc.getDeposito() != null) partes.add(loc.getDeposito().getNmDeposito());
        for (String p : new String[]{loc.getNmSetor(), loc.getNmEstante(), loc.getNmPrateleira(), loc.getNmCaixa(), loc.getNmPosicao()}) {
            if (p != null && !p.isBlank()) partes.add(p);
        }
        return String.join(" · ", partes);
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
