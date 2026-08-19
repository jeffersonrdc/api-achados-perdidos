package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.ColetaFiltrosResponse;
import br.com.achadosperdidos.controller.dto.ItemTransicaoRequest;
import br.com.achadosperdidos.controller.dto.TriagemFilaResponse;
import br.com.achadosperdidos.controller.dto.TriagemIaResponse;
import br.com.achadosperdidos.controller.dto.TriagemResponse;
import br.com.achadosperdidos.controller.dto.TriagemResumoResponse;
import br.com.achadosperdidos.controller.dto.TriagemSalvarRequest;
import br.com.achadosperdidos.entity.Item;
import br.com.achadosperdidos.entity.Triagem;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.pagination.ApiPage;
import br.com.achadosperdidos.pagination.PaginationMeta;
import br.com.achadosperdidos.pagination.PaginationParams;
import br.com.achadosperdidos.repository.ItemRepository;
import br.com.achadosperdidos.repository.TriagemRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class TriagemService {
    private static final String STATUS_EM_ANDAMENTO = "EM_ANDAMENTO";
    private static final String STATUS_CONCLUIDA = "CONCLUIDA";
    private static final String STATUS_ESTOQUE = "Em estoque";
    private static final String STATUS_AGUARDANDO = "Aguardando triagem";
    private static final String STATUS_EM_TRIAGEM = "Em triagem";
    /** Status legado: gerado por versões anteriores do botão "Analisar item". */
    private static final String STATUS_EM_ANALISE = "Em Análise";
    /** Status que mantêm o item na fila da tela /triagem. */
    private static final List<String> STATUS_FILA = List.of(
            STATUS_AGUARDANDO, STATUS_EM_ANALISE, STATUS_EM_TRIAGEM);
    /** Status exibidos no filtro da tela /triagem. */
    private static final List<String> STATUS_FILTRO = List.of(
            STATUS_AGUARDANDO, STATUS_EM_TRIAGEM);

    private final TriagemRepository triagemRepository;
    private final ItemRepository itemRepository;
    private final ItemService itemService;
    private final WorkflowService workflowService;
    private final CategoriaService categoriaService;
    private final LocalizacaoService localizacaoService;
    private final UsuarioContextService usuarioContextService;
    private final AuditoriaContextService auditoriaContext;
    private final SignedResourceIdCodec idCodec;
    private final MatchService matchService;

    public TriagemService(TriagemRepository triagemRepository, ItemRepository itemRepository,
                          ItemService itemService, WorkflowService workflowService, CategoriaService categoriaService,
                          LocalizacaoService localizacaoService, UsuarioContextService usuarioContextService,
                          AuditoriaContextService auditoriaContext, SignedResourceIdCodec idCodec,
                          MatchService matchService) {
        this.triagemRepository = triagemRepository;
        this.itemRepository = itemRepository;
        this.itemService = itemService;
        this.workflowService = workflowService;
        this.categoriaService = categoriaService;
        this.localizacaoService = localizacaoService;
        this.usuarioContextService = usuarioContextService;
        this.auditoriaContext = auditoriaContext;
        this.idCodec = idCodec;
        this.matchService = matchService;
    }

    /** Inicia a triagem: transiciona o item para "Em triagem" e abre o registro. */
    @Transactional
    public TriagemResponse iniciar(String idItem) {
        auditoriaContext.marcarContexto();
        Item item = findItem(idItem);
        workflowService.transitar(idItem, new ItemTransicaoRequest(STATUS_EM_TRIAGEM, "Triagem iniciada"));
        Triagem triagem = triagemRepository.findByItem_IdAndFgExcluidoFalse(item.getId())
                .orElseGet(Triagem::new);
        if (triagem.getId() == null) {
            triagem.setItem(item);
            triagem.setDtCadastro(LocalDateTime.now());
            triagem.setFgAtivo(true);
            triagem.setFgExcluido(false);
        }
        triagem.setOperador(usuarioLogadoOuNulo());
        triagem.setTpStatus(STATUS_EM_ANDAMENTO);
        triagem.setDtInicio(LocalDateTime.now());
        return toResponse(triagemRepository.save(triagem));
    }

    /**
     * Botão "Analisar item": abre/atualiza o registro de triagem e move o item
     * de "Aguardando triagem" para "Em triagem".
     */
    @Transactional
    public TriagemResponse analisar(String idItem) {
        auditoriaContext.marcarContexto();
        Item item = findItem(idItem);
        String statusAtual = item.getStatus() != null ? item.getStatus().getNmStatus() : "";
        if (!STATUS_EM_TRIAGEM.equalsIgnoreCase(statusAtual)) {
            workflowService.transitarSePermitido(idItem, STATUS_EM_TRIAGEM, "Item em análise na triagem");
        }
        Triagem triagem = getOrCreate(item);
        triagem.setOperador(usuarioLogadoOuNulo());
        if (triagem.getDtInicio() == null) triagem.setDtInicio(LocalDateTime.now());
        triagem.setTpStatus(STATUS_EM_ANDAMENTO);
        return toResponse(triagemRepository.save(triagem));
    }

    /** Salva a classificacao/observacoes da triagem sem encaminhar ao estoque. */
    @Transactional
    public TriagemResponse salvar(String idItem, TriagemSalvarRequest request) {
        auditoriaContext.marcarContexto();
        Item item = findItem(idItem);
        Triagem triagem = getOrCreate(item);
        aplicar(item, triagem, request);
        return toResponse(triagemRepository.save(triagem));
    }

    /**
     * Conclui a triagem: aplica a classificacao/observacoes, remove o item da fila
     * e o libera ao estoque — é aqui que o item passa a aparecer no portal público.
     */
    @Transactional
    public TriagemResponse concluir(String idItem, TriagemSalvarRequest request) {
        auditoriaContext.marcarContexto();
        Item item = findItem(idItem);
        Triagem triagem = getOrCreate(item);
        aplicar(item, triagem, request);
        if (triagem.getLocalizacaoInicial() != null) {
            item.setLocalizacao(triagem.getLocalizacaoInicial());
        }
        // Garante local no card do portal quando o posto foi informado na coleta.
        if ((item.getNmLocalEncontrado() == null || item.getNmLocalEncontrado().isBlank())
                && item.getNmPosto() != null && !item.getNmPosto().isBlank()) {
            item.setNmLocalEncontrado(item.getNmPosto().trim());
        }
        triagem.setTpStatus(STATUS_CONCLUIDA);
        triagem.setDtConclusao(LocalDateTime.now());
        if (triagem.getOperador() == null) triagem.setOperador(usuarioLogadoOuNulo());
        triagemRepository.save(triagem);
        itemRepository.save(item);
        // Libera ao estoque: a partir daqui o item é visível em /estoque e no portal.
        // O match claim↔item é disparado na transição de status (WorkflowService).
        garantirEmEstoque(idItem);
        return toResponse(triagem);
    }

    @Transactional(readOnly = true)
    public TriagemResponse detalhe(String idItem) {
        Long itemId = idCodec.decodeItemId(idItem);
        // Itens ainda na fila (ex.: "Aguardando triagem") não possuem registro de triagem;
        // nesse caso devolve os dados do próprio item (sem 404).
        return triagemRepository.findByItem_IdAndFgExcluidoFalse(itemId)
                .map(this::toResponse)
                .orElseGet(() -> toResponseFromItem(
                        itemRepository.findById(itemId)
                                .filter(i -> !Boolean.TRUE.equals(i.getFgExcluido()))
                                .orElseThrow(() -> new RecursoNaoEncontradoException("Item não encontrado."))));
    }

    /** Fila de triagem paginada e filtrada (server-side), restrita aos status da fila. */
    @Transactional(readOnly = true)
    public ApiPage<TriagemFilaResponse> fila(String idEvento, Integer page, Integer limit,
                                             String q, String idCategoria, String local,
                                             String tpPrioridade, String status, String data) {
        int p = PaginationParams.resolvePage(page);
        int l = PaginationParams.resolveLimit(limit);
        Long eventoId = idCodec.decodeEventoId(idEvento);
        Long categoriaId = (idCategoria != null && !idCategoria.isBlank()) ? idCodec.decodeCategoriaId(idCategoria) : null;
        LocalDate dataEncontrado = parseData(data);

        Specification<Item> spec = filtros(eventoId, q, categoriaId, local, tpPrioridade, status, dataEncontrado);
        Page<Item> result = itemRepository.findAll(spec,
                PageRequest.of(p - 1, l, Sort.by(Sort.Direction.ASC, "dtCadastro")));
        var content = result.getContent().stream().map(this::toFilaResponse).toList();
        var meta = new PaginationMeta(p, l, result.getTotalElements(), result.getTotalPages());
        return ApiPage.paged(content, meta);
    }

    private Specification<Item> filtros(Long eventoId, String q, Long categoriaId, String local,
                                        String tpPrioridade, String status, LocalDate data) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.isFalse(root.get("fgExcluido")));
            ps.add(cb.equal(root.get("evento").get("id"), eventoId));

            // A fila é definida pelo status do item: sai dela ao ser liberado para o estoque.
            if (status != null && !status.isBlank() && STATUS_FILA.contains(status)) {
                ps.add(cb.equal(root.get("status").get("nmStatus"), status));
            } else {
                ps.add(root.get("status").get("nmStatus").in(STATUS_FILA));
            }

            if (categoriaId != null) ps.add(cb.equal(root.get("categoria").get("id"), categoriaId));
            if (local != null && !local.isBlank()) ps.add(cb.equal(root.get("nmLocalEncontrado"), local));
            if (tpPrioridade != null && !tpPrioridade.isBlank())
                ps.add(cb.equal(root.get("tpPrioridade"), tpPrioridade.trim().toUpperCase()));
            if (data != null) ps.add(cb.equal(root.get("dtEncontrado"), data));
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("nmTitulo")), like),
                        cb.like(cb.lower(root.get("cdItem")), like),
                        cb.like(cb.lower(root.get("nmLocalEncontrado")), like)));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
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

    /** KPIs/cards da tela de triagem. */
    @Transactional(readOnly = true)
    public TriagemResumoResponse resumo(String idEvento, String data) {
        Long ev = idCodec.decodeEventoId(idEvento);
        LocalDate dia = parseData(data);
        long aguardando = itemRepository.count(filtros(ev, null, null, null, null, STATUS_AGUARDANDO, dia));
        long emAnalise = itemRepository.count(filtros(ev, null, null, null, null, STATUS_EM_ANALISE, dia));
        long emTriagem = itemRepository.count(filtros(ev, null, null, null, null, STATUS_EM_TRIAGEM, dia));
        long total = itemRepository.countNaFilaTriagem(ev, STATUS_FILA, dia);
        long sensiveis = itemRepository.countSensiveisNaFila(ev, STATUS_FILA, dia);
        long categorias = itemRepository.countCategoriasDistintas(ev, STATUS_FILA, dia);
        List<TriagemResumoResponse.CategoriaQt> porCategoria = itemRepository.contagemPorCategoria(ev, STATUS_FILA, dia)
                .stream()
                .map(r -> new TriagemResumoResponse.CategoriaQt(
                        r[0] != null ? r[0].toString() : "Outros",
                        ((Number) r[1]).longValue()))
                .toList();
        return new TriagemResumoResponse(total, aguardando, emAnalise, emTriagem, sensiveis, categorias, porCategoria);
    }

    /** Opções para os filtros/selects (reaproveita a árvore de categorias/locais da coleta). */
    @Transactional(readOnly = true)
    public ColetaFiltrosResponse filtros(String idEvento) {
        ColetaFiltrosResponse base = itemService.coletaFiltros(idEvento);
        List<ColetaFiltrosResponse.Opcao> status = STATUS_FILTRO.stream()
                .map(s -> new ColetaFiltrosResponse.Opcao(s, s))
                .toList();
        return new ColetaFiltrosResponse(base.categorias(), status, base.locais(), base.prioridades());
    }

    /** Sugestao automatica (stub) baseada na categoria/titulo do item. */
    @Transactional(readOnly = true)
    public TriagemIaResponse sugestaoIa(String idItem) {
        Item item = findItem(idItem);
        String categoria = item.getCategoria() != null ? item.getCategoria().getNmCategoria() : "Outros";
        String sugestao = "Com base nas características, este item pode ser classificado como \""
                + categoria + "\".";
        return new TriagemIaResponse(sugestao, new BigDecimal("90.00"));
    }

    // ------------------------------------------------------------------

    private void aplicar(Item item, Triagem triagem, TriagemSalvarRequest r) {
        if (r.idCategoria() != null && !r.idCategoria().isBlank()) {
            item.setCategoria(categoriaService.findEntity(idCodec.decodeCategoriaId(r.idCategoria())));
        }
        if (r.idSubcategoria() != null) {
            item.setSubcategoria(r.idSubcategoria().isBlank() ? null
                    : categoriaService.findEntity(idCodec.decodeCategoriaId(r.idSubcategoria())));
        }
        if (r.nmMarca() != null) item.setNmMarca(r.nmMarca());
        if (r.nmModelo() != null) item.setNmModelo(r.nmModelo());
        if (r.nmCor() != null) item.setNmCor(r.nmCor());
        if (r.tpPrioridade() != null && !r.tpPrioridade().isBlank()) {
            item.setTpPrioridade(normalizarPrioridade(r.tpPrioridade()));
        }
        if (r.fgSensivel() != null) item.setFgSensivel(r.fgSensivel());
        item.setDtAlteracao(LocalDateTime.now());

        if (r.idLocalizacaoInicial() != null) {
            triagem.setLocalizacaoInicial(r.idLocalizacaoInicial().isBlank() ? null
                    : localizacaoService.findEntity(idCodec.decodeLocalizacaoId(r.idLocalizacaoInicial())));
        }
        if (r.nmEstado() != null) {
            triagem.setNmEstado(r.nmEstado());
            item.setNmEstado(r.nmEstado().isBlank() ? null : r.nmEstado().trim());
        }
        if (r.dsTags() != null) {
            triagem.setDsTags(r.dsTags());
            item.setDsTags(r.dsTags().isBlank() ? null : r.dsTags().trim());
        }
        if (r.dsObservacao() != null) {
            triagem.setDsObservacao(r.dsObservacao());
            item.setDsObservacoes(r.dsObservacao().isBlank() ? null : r.dsObservacao().trim());
        }
        if (r.dsSugestaoIa() != null) triagem.setDsSugestaoIa(r.dsSugestaoIa());
        if (r.vlConfiancaIa() != null) triagem.setVlConfiancaIa(r.vlConfiancaIa());
        triagem.setDtAlteracao(LocalDateTime.now());
    }

    private Triagem getOrCreate(Item item) {
        return triagemRepository.findByItem_Id(item.getId())
                .map(t -> {
                    if (Boolean.TRUE.equals(t.getFgExcluido())) {
                        t.setFgExcluido(false);
                        t.setFgAtivo(true);
                    }
                    return t;
                })
                .orElseGet(() -> {
                    Triagem t = new Triagem();
                    t.setItem(item);
                    t.setOperador(usuarioLogadoOuNulo());
                    t.setTpStatus(STATUS_EM_ANDAMENTO);
                    t.setDtInicio(LocalDateTime.now());
                    t.setDtCadastro(LocalDateTime.now());
                    t.setFgAtivo(true);
                    t.setFgExcluido(false);
                    return t;
                });
    }

    /** Garante status "Em estoque" após concluir a triagem. */
    private void garantirEmEstoque(String idItem) {
        Item item = findItem(idItem);
        String status = item.getStatus() != null ? item.getStatus().getNmStatus() : "";
        if (STATUS_ESTOQUE.equalsIgnoreCase(status)) {
            matchService.recalcularMatchesPorItem(item);
            return;
        }
        String motivo = "Item disponível no estoque após triagem";
        if (workflowService.transitarSePermitido(idItem, STATUS_ESTOQUE, motivo)) return;
        // Conclusão sem passar pelo botão "Analisar item": entra em triagem e segue ao estoque.
        workflowService.transitarSePermitido(idItem, STATUS_EM_TRIAGEM, "Triagem concluída");
        workflowService.transitarSePermitido(idItem, STATUS_ESTOQUE, motivo);
    }

    private Item findItem(String idItem) {
        return itemRepository.findById(idCodec.decodeItemId(idItem))
                .filter(i -> !Boolean.TRUE.equals(i.getFgExcluido()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item não encontrado."));
    }

    private String normalizarPrioridade(String prioridade) {
        String p = prioridade.trim().toUpperCase();
        if (!p.equals("ALTA") && !p.equals("MEDIA") && !p.equals("BAIXA")) {
            throw new IllegalArgumentException("Prioridade inválida: " + prioridade + ". Use ALTA, MEDIA ou BAIXA.");
        }
        return p;
    }

    private br.com.achadosperdidos.entity.Usuario usuarioLogadoOuNulo() {
        try {
            return usuarioContextService.requireUsuarioLogado();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private TriagemResponse toResponse(Triagem t) {
        Item i = t.getItem();
        String operador = t.getOperador() != null
                ? t.getOperador().getNmUsuario()
                : (i.getUsuarioCadastro() != null ? i.getUsuarioCadastro().getNmUsuario() : i.getNmEncontradoPor());
        String tags = t.getDsTags() != null && !t.getDsTags().isBlank() ? t.getDsTags() : i.getDsTags();
        String estado = t.getNmEstado() != null && !t.getNmEstado().isBlank() ? t.getNmEstado() : i.getNmEstado();
        String obs = t.getDsObservacao() != null && !t.getDsObservacao().isBlank()
                ? t.getDsObservacao()
                : i.getDsObservacoes();
        return new TriagemResponse(
                idCodec.encodeTriagemId(t.getId()),
                idCodec.encodeItemId(i.getId()),
                i.getCdItem(),
                i.getNmTitulo(),
                i.getStatus() != null ? i.getStatus().getNmStatus() : null,
                t.getTpStatus(),
                t.getOperador() != null ? idCodec.encodeUsuarioId(t.getOperador().getId()) : null,
                operador,
                estado,
                i.getCategoria() != null ? i.getCategoria().getNmCategoria() : null,
                i.getSubcategoria() != null ? i.getSubcategoria().getNmCategoria() : null,
                i.getNmCor(),
                i.getNmMarca(),
                i.getNmModelo(),
                i.getDsItem(),
                i.getDsWallpaper(),
                tags,
                t.getDsObservacao(),
                obs,
                t.getDsSugestaoIa(),
                t.getVlConfiancaIa(),
                t.getLocalizacaoInicial() != null ? idCodec.encodeLocalizacaoId(t.getLocalizacaoInicial().getId()) : null,
                i.getNmPosto(),
                i.getNmLocalEncontrado(),
                i.getFgSensivel(),
                i.getTpPrioridade(),
                t.getDtInicio(),
                t.getDtConclusao());
    }

    /** Resposta baseada apenas no item, quando ainda não há registro de triagem. */
    private TriagemResponse toResponseFromItem(Item i) {
        String operador = i.getUsuarioCadastro() != null ? i.getUsuarioCadastro().getNmUsuario() : i.getNmEncontradoPor();
        return new TriagemResponse(
                null,
                idCodec.encodeItemId(i.getId()),
                i.getCdItem(),
                i.getNmTitulo(),
                i.getStatus() != null ? i.getStatus().getNmStatus() : null,
                null,
                null,
                operador,
                i.getNmEstado(),
                i.getCategoria() != null ? i.getCategoria().getNmCategoria() : null,
                i.getSubcategoria() != null ? i.getSubcategoria().getNmCategoria() : null,
                i.getNmCor(),
                i.getNmMarca(),
                i.getNmModelo(),
                i.getDsItem(),
                i.getDsWallpaper(),
                i.getDsTags(),
                null,
                i.getDsObservacoes(),
                null,
                null,
                null,
                i.getNmPosto(),
                i.getNmLocalEncontrado(),
                i.getFgSensivel(),
                i.getTpPrioridade(),
                null,
                null);
    }

    private TriagemFilaResponse toFilaResponse(Item i) {
        String recebido = i.getUsuarioCadastro() != null ? i.getUsuarioCadastro().getNmUsuario() : i.getNmEncontradoPor();
        return new TriagemFilaResponse(
                idCodec.encodeItemId(i.getId()),
                i.getCdItem(),
                i.getNmTitulo(),
                i.getCategoria() != null ? i.getCategoria().getNmCategoria() : null,
                i.getSubcategoria() != null ? i.getSubcategoria().getNmCategoria() : null,
                i.getNmCor(),
                i.getNmMarca(),
                i.getNmModelo(),
                i.getNmEstado(),
                i.getStatus() != null ? i.getStatus().getNmStatus() : null,
                i.getTpPrioridade(),
                i.getFgSensivel(),
                i.getDtEncontrado(),
                i.getHrEncontrado(),
                i.getNmLocalEncontrado(),
                i.getNmPosto(),
                recebido,
                i.getDsItem(),
                i.getDsWallpaper(),
                i.getDsObservacoes(),
                i.getDsTags());
    }
}
