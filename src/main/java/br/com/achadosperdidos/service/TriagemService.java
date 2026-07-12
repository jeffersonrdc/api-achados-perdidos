package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.ItemTransicaoRequest;
import br.com.achadosperdidos.controller.dto.TriagemFilaResponse;
import br.com.achadosperdidos.controller.dto.TriagemIaResponse;
import br.com.achadosperdidos.controller.dto.TriagemResponse;
import br.com.achadosperdidos.controller.dto.TriagemSalvarRequest;
import br.com.achadosperdidos.entity.Item;
import br.com.achadosperdidos.entity.Triagem;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.repository.ItemRepository;
import br.com.achadosperdidos.repository.TriagemRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TriagemService {
    private static final String STATUS_EM_ANDAMENTO = "EM_ANDAMENTO";
    private static final String STATUS_CONCLUIDA = "CONCLUIDA";
    private static final List<String> STATUS_FILA = List.of("Aguardando triagem", "Em triagem");

    private final TriagemRepository triagemRepository;
    private final ItemRepository itemRepository;
    private final WorkflowService workflowService;
    private final CategoriaService categoriaService;
    private final LocalizacaoService localizacaoService;
    private final UsuarioContextService usuarioContextService;
    private final SignedResourceIdCodec idCodec;

    public TriagemService(TriagemRepository triagemRepository, ItemRepository itemRepository,
                          WorkflowService workflowService, CategoriaService categoriaService,
                          LocalizacaoService localizacaoService, UsuarioContextService usuarioContextService,
                          SignedResourceIdCodec idCodec) {
        this.triagemRepository = triagemRepository;
        this.itemRepository = itemRepository;
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
        // Encaminha para estoque (Em triagem -> Em transporte para estoque).
        workflowService.transitar(idItem, new ItemTransicaoRequest("Em transporte para estoque", "Triagem concluída"));
        return toResponse(triagem);
    }

    @Transactional(readOnly = true)
    public TriagemResponse detalhe(String idItem) {
        Long itemId = idCodec.decodeItemId(idItem);
        return triagemRepository.findByItem_IdAndFgExcluidoFalse(itemId)
                .map(this::toResponse)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Triagem não encontrada para o item."));
    }

    @Transactional(readOnly = true)
    public List<TriagemFilaResponse> fila(String idEvento) {
        return itemRepository.findByEvento_IdAndStatus_NmStatusInAndFgExcluidoFalseOrderByDtCadastroAsc(
                        idCodec.decodeEventoId(idEvento), STATUS_FILA)
                .stream().map(this::toFilaResponse).toList();
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

    private TriagemFilaResponse toFilaResponse(Item i) {
        return new TriagemFilaResponse(
                idCodec.encodeItemId(i.getId()),
                i.getCdItem(),
                i.getNmTitulo(),
                i.getCategoria() != null ? i.getCategoria().getNmCategoria() : null,
                i.getStatus() != null ? i.getStatus().getNmStatus() : null,
                i.getTpPrioridade(),
                i.getFgSensivel(),
                i.getDtEncontrado());
    }
}
