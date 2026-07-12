package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.EtiquetaConteudoResponse;
import br.com.achadosperdidos.controller.dto.EtiquetaImprimirRequest;
import br.com.achadosperdidos.controller.dto.EtiquetaImpressaoResponse;
import br.com.achadosperdidos.entity.EtiquetaImpressao;
import br.com.achadosperdidos.entity.Item;
import br.com.achadosperdidos.entity.Usuario;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.repository.EtiquetaImpressaoRepository;
import br.com.achadosperdidos.repository.ItemRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class EtiquetaService {
    private static final Set<String> TIPOS = Set.of("IMPRESSAO", "REIMPRESSAO");
    private static final String IMPRESSORA_PADRAO = "Zebra ZQ320";

    private final EtiquetaImpressaoRepository etiquetaRepository;
    private final ItemRepository itemRepository;
    private final UsuarioContextService usuarioContextService;
    private final SignedResourceIdCodec idCodec;

    public EtiquetaService(EtiquetaImpressaoRepository etiquetaRepository, ItemRepository itemRepository,
                           UsuarioContextService usuarioContextService, SignedResourceIdCodec idCodec) {
        this.etiquetaRepository = etiquetaRepository;
        this.itemRepository = itemRepository;
        this.usuarioContextService = usuarioContextService;
        this.idCodec = idCodec;
    }

    @Transactional(readOnly = true)
    public EtiquetaConteudoResponse conteudo(String idItem) {
        return toConteudo(findItem(idItem));
    }

    @Transactional
    public EtiquetaImpressaoResponse imprimir(String idItem, EtiquetaImprimirRequest request) {
        Item item = findItem(idItem);
        EtiquetaImpressao e = new EtiquetaImpressao();
        e.setItem(item);
        e.setOperador(usuarioLogadoOuNulo());
        e.setTpImpressao(validarTipo(request.tpImpressao()));
        e.setNmImpressora(request.nmImpressora() != null && !request.nmImpressora().isBlank()
                ? request.nmImpressora().trim() : IMPRESSORA_PADRAO);
        e.setNrIdentificador(request.nrIdentificador());
        e.setDsMotivo(request.dsMotivo());
        e.setDtImpressao(LocalDateTime.now());
        e.setDtCadastro(LocalDateTime.now());
        e.setFgAtivo(true);
        e.setFgExcluido(false);
        return toResponse(etiquetaRepository.save(e));
    }

    @Transactional(readOnly = true)
    public List<EtiquetaImpressaoResponse> historico(String idItem) {
        Long itemId = idCodec.decodeItemId(idItem);
        return etiquetaRepository.findByItem_IdAndFgExcluidoFalseOrderByDtImpressaoDesc(itemId)
                .stream().map(this::toResponse).toList();
    }

    private String validarTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) return "IMPRESSAO";
        String t = tipo.trim().toUpperCase();
        if (!TIPOS.contains(t)) {
            throw new IllegalArgumentException("Tipo de impressão inválido: " + tipo + ". Use IMPRESSAO ou REIMPRESSAO.");
        }
        return t;
    }

    private Item findItem(String idItem) {
        return itemRepository.findById(idCodec.decodeItemId(idItem))
                .filter(i -> !Boolean.TRUE.equals(i.getFgExcluido()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item não encontrado."));
    }

    private Usuario usuarioLogadoOuNulo() {
        try {
            return usuarioContextService.requireUsuarioLogado();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private EtiquetaConteudoResponse toConteudo(Item i) {
        return new EtiquetaConteudoResponse(
                idCodec.encodeItemId(i.getId()),
                i.getCdItem(),
                idCodec.encodeItemId(i.getId()),
                i.getNmTitulo(),
                i.getCategoria() != null ? i.getCategoria().getNmCategoria() : null,
                i.getTpPrioridade(),
                i.getFgSensivel(),
                i.getDtEncontrado(),
                i.getHrEncontrado(),
                i.getNmLocalEncontrado(),
                IMPRESSORA_PADRAO);
    }

    private EtiquetaImpressaoResponse toResponse(EtiquetaImpressao e) {
        return new EtiquetaImpressaoResponse(
                idCodec.encodeEtiquetaId(e.getId()),
                idCodec.encodeItemId(e.getItem().getId()),
                e.getTpImpressao(),
                e.getNmImpressora(),
                e.getNrIdentificador(),
                e.getDsMotivo(),
                e.getOperador() != null ? idCodec.encodeUsuarioId(e.getOperador().getId()) : null,
                e.getOperador() != null ? e.getOperador().getNmUsuario() : null,
                e.getDtImpressao());
    }
}
