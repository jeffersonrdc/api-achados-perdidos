package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.ContatoCreateRequest;
import br.com.achadosperdidos.controller.dto.ContatoResponse;
import br.com.achadosperdidos.entity.Contato;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.repository.ClaimRepository;
import br.com.achadosperdidos.repository.ContatoRepository;
import br.com.achadosperdidos.repository.ItemRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ContatoService {
    private final ContatoRepository contatoRepository;
    private final ItemRepository itemRepository;
    private final ClaimRepository claimRepository;
    private final SignedResourceIdCodec idCodec;

    public ContatoService(ContatoRepository contatoRepository, ItemRepository itemRepository,
                          ClaimRepository claimRepository, SignedResourceIdCodec idCodec) {
        this.contatoRepository = contatoRepository;
        this.itemRepository = itemRepository;
        this.claimRepository = claimRepository;
        this.idCodec = idCodec;
    }

    @Transactional
    public ContatoResponse create(ContatoCreateRequest request) {
        if ((request.idItem() == null || request.idItem().isBlank()) && (request.idClaim() == null || request.idClaim().isBlank())) {
            throw new IllegalArgumentException("Informe idItem ou idClaim.");
        }
        Contato c = new Contato();
        if (request.idItem() != null && !request.idItem().isBlank()) {
            c.setItem(itemRepository.findById(idCodec.decodeItemId(request.idItem()))
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Item não encontrado.")));
        }
        if (request.idClaim() != null && !request.idClaim().isBlank()) {
            c.setClaim(claimRepository.findById(idCodec.decodeClaimId(request.idClaim()))
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Claim não encontrado.")));
        }
        c.setTpContato(request.tpContato().trim().toUpperCase());
        c.setNmContato(request.nmContato().trim());
        c.setNrTelefone(request.nrTelefone());
        c.setNmEmail(request.nmEmail());
        c.setDsResumo(request.dsResumo());
        c.setDtContato(LocalDateTime.now());
        c.setDtCadastro(LocalDateTime.now());
        c.setFgExcluido(false);
        return toResponse(contatoRepository.save(c));
    }

    @Transactional(readOnly = true)
    public List<ContatoResponse> findAll() {
        return contatoRepository.findByFgExcluidoFalseOrderByDtContatoDesc().stream().map(this::toResponse).toList();
    }

    private ContatoResponse toResponse(Contato c) {
        return new ContatoResponse(
                idCodec.encodeContatoId(c.getId()),
                c.getItem() != null ? idCodec.encodeItemId(c.getItem().getId()) : null,
                c.getClaim() != null ? idCodec.encodeClaimId(c.getClaim().getId()) : null,
                c.getTpContato(), c.getNmContato(), c.getNrTelefone(), c.getNmEmail(), c.getDsResumo(), c.getDtContato());
    }
}
