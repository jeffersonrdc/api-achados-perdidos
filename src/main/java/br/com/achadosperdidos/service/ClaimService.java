package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.ClaimCreateRequest;
import br.com.achadosperdidos.controller.dto.ClaimResponse;
import br.com.achadosperdidos.entity.Claim;
import br.com.achadosperdidos.entity.Evento;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.pagination.ApiPage;
import br.com.achadosperdidos.pagination.PaginationMeta;
import br.com.achadosperdidos.pagination.PaginationParams;
import br.com.achadosperdidos.repository.ClaimRepository;
import br.com.achadosperdidos.repository.EventoRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class ClaimService {
    private final ClaimRepository claimRepository;
    private final EventoRepository eventoRepository;
    private final CategoriaService categoriaService;
    private final StatusItemService statusItemService;
    private final SignedResourceIdCodec idCodec;

    public ClaimService(ClaimRepository claimRepository, EventoRepository eventoRepository,
                        CategoriaService categoriaService, StatusItemService statusItemService,
                        SignedResourceIdCodec idCodec) {
        this.claimRepository = claimRepository; this.eventoRepository = eventoRepository;
        this.categoriaService = categoriaService; this.statusItemService = statusItemService; this.idCodec = idCodec;
    }

    @Transactional
    public ClaimResponse create(ClaimCreateRequest request) {
        Long eventoId = idCodec.decodeEventoId(request.idEvento());
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento não encontrado."));
        Claim claim = new Claim();
        claim.setEvento(evento);
        claim.setCategoria(categoriaService.findEntity(idCodec.decodeCategoriaId(request.idCategoria())));
        claim.setStatus(request.idStatus() != null && !request.idStatus().isBlank()
                ? statusItemService.findEntity(idCodec.decodeStatusId(request.idStatus()))
                : statusItemService.findByNomeOrDefault(null, "Claim Aberto"));
        claim.setNmNome(request.nmNome().trim());
        claim.setNrCpf(request.nrCpf());
        claim.setNmEmail(request.nmEmail());
        claim.setNrTelefone(request.nrTelefone());
        claim.setNmObjeto(request.nmObjeto().trim());
        claim.setDsObjeto(request.dsObjeto());
        claim.setNmMarca(request.nmMarca());
        claim.setNmModelo(request.nmModelo());
        claim.setNmCor(request.nmCor());
        claim.setDtPerdeu(request.dtPerdeu());
        claim.setHrPerdeu(request.hrPerdeu());
        claim.setNmLocal(request.nmLocal());
        claim.setDtCadastro(LocalDateTime.now());
        claim.setFgAtivo(true);
        claim.setFgExcluido(false);
        return toResponse(claimRepository.save(claim));
    }

    @Transactional(readOnly = true)
    public ApiPage<ClaimResponse> findAll(Integer page, Integer limit, String idEvento) {
        int p = PaginationParams.resolvePage(page);
        int l = PaginationParams.resolveLimit(limit);
        Page<Claim> result = (idEvento != null && !idEvento.isBlank())
                ? claimRepository.findByEvento_IdAndFgExcluidoFalse(idCodec.decodeEventoId(idEvento), PageRequest.of(p - 1, l))
                : claimRepository.findByFgExcluidoFalse(PageRequest.of(p - 1, l));
        var content = result.getContent().stream().map(this::toResponse).toList();
        return ApiPage.paged(content, new PaginationMeta(p, l, result.getTotalElements(), result.getTotalPages()));
    }

    @Transactional(readOnly = true)
    public ClaimResponse findById(String idToken) {
        return toResponse(findEntity(idCodec.decodeClaimId(idToken)));
    }

    @Transactional
    public void softDelete(String idToken) {
        Claim claim = findEntity(idCodec.decodeClaimId(idToken));
        claim.setFgExcluido(true);
        claim.setFgAtivo(false);
        claim.setDtAlteracao(LocalDateTime.now());
        claimRepository.save(claim);
    }

    private Claim findEntity(Long id) {
        return claimRepository.findById(id)
                .filter(c -> !Boolean.TRUE.equals(c.getFgExcluido()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Claim não encontrado."));
    }

    private ClaimResponse toResponse(Claim c) {
        return new ClaimResponse(
                idCodec.encodeClaimId(c.getId()), c.getNmNome(), c.getNmObjeto(), c.getNmMarca(), c.getNmModelo(), c.getNmCor(),
                c.getDtPerdeu(), c.getStatus().getNmStatus(), c.getCategoria().getNmCategoria(), c.getEvento().getNmEvento(), c.getDtCadastro(),
                c.getNrCpf(), c.getNmEmail(), c.getNrTelefone(), c.getNmLocal(), c.getDsObjeto());
    }
}
