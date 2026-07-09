package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.ItemCreateRequest;
import br.com.achadosperdidos.controller.dto.ItemResponse;
import br.com.achadosperdidos.entity.Evento;
import br.com.achadosperdidos.entity.Item;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.pagination.ApiPage;
import br.com.achadosperdidos.pagination.PaginationMeta;
import br.com.achadosperdidos.pagination.PaginationParams;
import br.com.achadosperdidos.repository.EventoRepository;
import br.com.achadosperdidos.repository.ItemRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ItemService {
    private final ItemRepository itemRepository;
    private final EventoRepository eventoRepository;
    private final CategoriaService categoriaService;
    private final StatusItemService statusItemService;
    private final SignedResourceIdCodec idCodec;
    private final UsuarioContextService usuarioContextService;

    public ItemService(ItemRepository itemRepository, EventoRepository eventoRepository,
                       CategoriaService categoriaService, StatusItemService statusItemService,
                       SignedResourceIdCodec idCodec, UsuarioContextService usuarioContextService) {
        this.itemRepository = itemRepository; this.eventoRepository = eventoRepository;
        this.categoriaService = categoriaService; this.statusItemService = statusItemService;
        this.idCodec = idCodec; this.usuarioContextService = usuarioContextService;
    }

    @Transactional
    public ItemResponse create(ItemCreateRequest request) {
        Long eventoId = idCodec.decodeEventoId(request.idEvento());
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento não encontrado."));
        Item item = new Item();
        item.setEvento(evento);
        item.setCategoria(categoriaService.findEntity(idCodec.decodeCategoriaId(request.idCategoria())));
        item.setStatus(request.idStatus() != null && !request.idStatus().isBlank()
                ? statusItemService.findEntity(idCodec.decodeStatusId(request.idStatus()))
                : statusItemService.findByNomeOrDefault(null, "Recebido"));
        item.setCdItem(gerarCodigoItem());
        item.setNmTitulo(request.nmTitulo().trim());
        item.setDsItem(request.dsItem());
        item.setNmMarca(request.nmMarca());
        item.setNmModelo(request.nmModelo());
        item.setNmCor(request.nmCor());
        item.setDtEncontrado(request.dtEncontrado());
        item.setHrEncontrado(request.hrEncontrado());
        item.setNmLocalEncontrado(request.nmLocalEncontrado());
        item.setVlEstimado(request.vlEstimado());
        item.setDtCadastro(LocalDateTime.now());
        item.setFgAtivo(true);
        item.setFgExcluido(false);
        return toResponse(itemRepository.save(item));
    }

    @Transactional(readOnly = true)
    public ApiPage<ItemResponse> findAll(Integer page, Integer limit, String idEvento) {
        int p = PaginationParams.resolvePage(page);
        int l = PaginationParams.resolveLimit(limit);
        Page<Item> result = (idEvento != null && !idEvento.isBlank())
                ? itemRepository.findByEvento_IdAndFgExcluidoFalse(idCodec.decodeEventoId(idEvento), PageRequest.of(p - 1, l))
                : itemRepository.findByFgExcluidoFalse(PageRequest.of(p - 1, l));
        var content = result.getContent().stream().map(this::toResponse).toList();
        var meta = new PaginationMeta(p, l, result.getTotalElements(), result.getTotalPages());
        return ApiPage.paged(content, meta);
    }

    @Transactional(readOnly = true)
    public ItemResponse findById(String idToken) {
        return toResponse(findEntity(idCodec.decodeItemId(idToken)));
    }

    @Transactional
    public void softDelete(String idToken) {
        Item item = findEntity(idCodec.decodeItemId(idToken));
        item.setFgExcluido(true);
        item.setFgAtivo(false);
        item.setDtAlteracao(LocalDateTime.now());
        itemRepository.save(item);
    }

    private Item findEntity(Long id) {
        return itemRepository.findById(id)
                .filter(i -> !Boolean.TRUE.equals(i.getFgExcluido()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item não encontrado."));
    }

    private String gerarCodigoItem() {
        return "ITM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private ItemResponse toResponse(Item i) {
        return new ItemResponse(
                idCodec.encodeItemId(i.getId()), i.getCdItem(), i.getNmTitulo(), i.getDsItem(),
                i.getNmMarca(), i.getNmModelo(), i.getNmCor(), i.getDtEncontrado(), i.getVlEstimado(),
                i.getStatus().getNmStatus(), i.getCategoria().getNmCategoria(), i.getEvento().getNmEvento(),
                i.getFgEntregue(), i.getFgDescartado(), i.getDtCadastro());
    }
}
