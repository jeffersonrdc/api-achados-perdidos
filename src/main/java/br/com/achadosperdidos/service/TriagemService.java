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
    private static final List<String> STATUS_FILA = List.of("Aguardando triagem", "Em Análise", "Em triagem");

    private final TriagemRepository triagemRepository;
    private final ItemRepository itemRepository;
    private final ItemService itemService;
    private final WorkflowService workflowService;
    private final CategoriaService categoriaService;
    private final LocalizacaoService localizacaoService;
    private final UsuarioContextService usuarioContextService;
    private final SignedResourceIdCodec idCodec;

    public TriagemService(TriagemRepository triagemRepository, ItemRepository itemRepository,
                          ItemService itemService, WorkflowService workflowService, CategoriaService categoriaService,
                          LocalizacaoService localizacaoService, UsuarioContextService usuarioContextService,
                          SignedResourceIdCodec idCodec) {
        this.triagemRepository = triagemRepository;
        this.itemRepository = itemRepository;
        this.itemService = itemService;
        this.workflowService = workflowService;
        this.categoriaService = categoriaService;
        this.localizacaoService = localizacaoService;
        this.usuarioContextService = usuarioContextService;
        this.idCodec = idCodec;
    }

    /** Inicia a triagem: transiciona o item para "Em triagem" e abre o registro. */
    @Transactional
    public TriagemResponse iniciar(String idItem) {
        Item item = findItem(idItem);
        workflowService.transitar(idItem, new ItemTransicaoRequest("Em triagem", "Triagem iniciada"));
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

    /** Marca o item como "Em Análise" (botão Analisar Item) e abre/atualiza o registro de triagem. */
    @Transactional
    public TriagemResponse analisar(String idItem) {
        Item item = findItem(idItem);
        // Só transiciona se ainda não estiver em análise (evita erro de transição para o mesmo status).
        if (!"Em Análise".equalsIgnoreCase(item.getStatus() != null ? item.getStatus().getNmStatus() : "")) {
            workflowService.transitar(idItem, new ItemTransicaoRequest("Em Análise", "Item em análise na triagem"));
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
        Item item = findItem(idItem);
        Triagem triagem = getOrCreate(item);
        aplicar(item, triagem, request);
        return toResponse(triagemRepository.save(triagem));
    }

    /** Conclui a triagem: aplica a classificacao, define a localizacao e encaminha ao estoque. */
    @Transactional
    public TriagemResponse concluir(String idItem, TriagemSalvarRequest request) {
        Item item = findItem(idItem);
        Triagem triagem = getOrCreate(item);
        aplicar(item, triagem, request);
        if (triagem.getLocalizacaoInicial() != null) {
            item.setLocalizacao(triagem.getLocalizacaoInicial());
        }
        triagem.setTpStatus(STATUS_CONCLUIDA);
        triagem.setDtConclusao(LocalDateTime.now());
        if (triagem.getOperador() == null) triagem.setOperador(usuarioLogadoOuNulo());
        triagemRepository.save(triagem);
        // Encaminha para o estoque: Em triagem -> Em transporte para estoque -> Em estoque.
        // Ao chegar em "Em estoque" o item fica disponível na consulta pública (portal).
        workflowService.transitar(idItem, new ItemTransicaoRequest("Em transporte para estoque", "Triagem concluída"));
        workflowService.transitar(idItem, new ItemTransicaoRequest("Em estoque", "Item disponibilizado no estoque após triagem"));
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
            // Restringe à fila de triagem (ou ao status específico, se filtrado e válido).
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
    public TriagemResumoResponse resumo(String idEvento) {
        Long ev = idCodec.decodeEventoId(idEvento);
        long aguardando = itemRepository.countByEvento_IdAndFgExcluidoFalseAndStatus_NmStatus(ev, "Aguardando triagem");
        long emAnalise = itemRepository.countByEvento_IdAndFgExcluidoFalseAndStatus_NmStatus(ev, "Em Análise");
        long emTriagem = itemRepository.countByEvento_IdAndFgExcluidoFalseAndStatus_NmStatus(ev, "Em triagem");
        long total = itemRepository.countByEvento_IdAndFgExcluidoFalseAndStatus_NmStatusIn(ev, STATUS_FILA);
        long sensiveis = itemRepository.countByEvento_IdAndFgExcluidoFalseAndFgSensivelTrueAndStatus_NmStatusIn(ev, STATUS_FILA);
        long categorias = itemRepository.countCategoriasDistintas(ev, STATUS_FILA);
        List<TriagemResumoResponse.CategoriaQt> porCategoria = itemRepository.contagemPorCategoria(ev, STATUS_FILA)
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
        List<ColetaFiltrosResponse.Opcao> status = STATUS_FILA.stream()
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
        if (r.nmEstado() != null) triagem.setNmEstado(r.nmEstado());
        if (r.dsTags() != null) triagem.setDsTags(r.dsTags());
        if (r.dsObservacao() != null) triagem.setDsObservacao(r.dsObservacao());
        if (r.dsSugestaoIa() != null) triagem.setDsSugestaoIa(r.dsSugestaoIa());
        if (r.vlConfiancaIa() != null) triagem.setVlConfiancaIa(r.vlConfiancaIa());
        triagem.setDtAlteracao(LocalDateTime.now());
    }

    private Triagem getOrCreate(Item item) {
        return triagemRepository.findByItem_IdAndFgExcluidoFalse(item.getId())
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
        return new TriagemResponse(
                idCodec.encodeTriagemId(t.getId()),
                idCodec.encodeItemId(i.getId()),
                i.getCdItem(),
                i.getNmTitulo(),
                i.getStatus() != null ? i.getStatus().getNmStatus() : null,
                t.getTpStatus(),
                t.getOperador() != null ? idCodec.encodeUsuarioId(t.getOperador().getId()) : null,
                t.getOperador() != null ? t.getOperador().getNmUsuario() : null,
                t.getNmEstado(),
                t.getDsTags(),
                t.getDsObservacao(),
                t.getDsSugestaoIa(),
                t.getVlConfiancaIa(),
                t.getLocalizacaoInicial() != null ? idCodec.encodeLocalizacaoId(t.getLocalizacaoInicial().getId()) : null,
                t.getDtInicio(),
                t.getDtConclusao());
    }

    /** Resposta baseada apenas no item, quando ainda não há registro de triagem. */
    private TriagemResponse toResponseFromItem(Item i) {
        return new TriagemResponse(
                null,
                idCodec.encodeItemId(i.getId()),
                i.getCdItem(),
                i.getNmTitulo(),
                i.getStatus() != null ? i.getStatus().getNmStatus() : null,
                null,
                null,
                i.getUsuarioCadastro() != null ? i.getUsuarioCadastro().getNmUsuario() : i.getNmEncontradoPor(),
                i.getNmEstado(),
                null,
                i.getDsObservacoes(),
                null,
                null,
                null,
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
                recebido);
    }
}
