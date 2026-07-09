package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.ItemCampoResponse;
import br.com.achadosperdidos.controller.dto.ItemCampoUpsertRequest;
import br.com.achadosperdidos.entity.ItemCampo;
import br.com.achadosperdidos.repository.ItemCampoRepository;
import br.com.achadosperdidos.repository.ItemRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ItemCampoService {
    private final ItemCampoRepository itemCampoRepository;
    private final ItemRepository itemRepository;
    private final CategoriaCampoService categoriaCampoService;
    private final SignedResourceIdCodec idCodec;

    public ItemCampoService(ItemCampoRepository itemCampoRepository, ItemRepository itemRepository,
                            CategoriaCampoService categoriaCampoService, SignedResourceIdCodec idCodec) {
        this.itemCampoRepository = itemCampoRepository;
        this.itemRepository = itemRepository;
        this.categoriaCampoService = categoriaCampoService;
        this.idCodec = idCodec;
    }

    @Transactional
    public ItemCampoResponse upsert(ItemCampoUpsertRequest request) {
        Long itemId = idCodec.decodeItemId(request.idItem());
        Long campoId = idCodec.decodeCategoriaCampoId(request.idCategoriaCampo());
        ItemCampo ic = itemCampoRepository.findByItem_IdAndCategoriaCampo_IdAndFgExcluidoFalse(itemId, campoId)
                .orElseGet(ItemCampo::new);
        if (ic.getId() == null) {
            ic.setItem(itemRepository.getReferenceById(itemId));
            ic.setCategoriaCampo(categoriaCampoService.findEntity(campoId));
            ic.setDtCadastro(LocalDateTime.now());
            ic.setFgAtivo(true);
            ic.setFgExcluido(false);
        }
        ic.setVlTexto(request.vlTexto());
        ic.setVlNumero(request.vlNumero());
        ic.setVlData(request.vlData());
        ic.setVlBoolean(request.vlBoolean());
        return toResponse(itemCampoRepository.save(ic));
    }

    @Transactional(readOnly = true)
    public List<ItemCampoResponse> findByItem(String idItem) {
        return itemCampoRepository.findByItem_IdAndFgExcluidoFalseOrderByIdAsc(idCodec.decodeItemId(idItem))
                .stream().map(this::toResponse).toList();
    }

    private ItemCampoResponse toResponse(ItemCampo ic) {
        return new ItemCampoResponse(
                idCodec.encodeItemCampoId(ic.getId()),
                idCodec.encodeItemId(ic.getItem().getId()),
                idCodec.encodeCategoriaCampoId(ic.getCategoriaCampo().getId()),
                ic.getCategoriaCampo().getNmCampo(),
                ic.getVlTexto(), ic.getVlNumero(), ic.getVlData(), ic.getVlBoolean());
    }
}
