package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.ItemDisponivelResponse;
import br.com.achadosperdidos.controller.dto.TransferenciaCreateRequest;
import br.com.achadosperdidos.controller.dto.TransferenciaResponse;
import br.com.achadosperdidos.controller.dto.TransferenciaResumoResponse;
import br.com.achadosperdidos.pagination.ApiPage;
import br.com.achadosperdidos.service.TransferenciaService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transferencias")
@Tag(name = "Transferências")
@SecurityRequirement(name = "bearerAuth")
public class TransferenciaController {
    private final TransferenciaService transferenciaService;

    public TransferenciaController(TransferenciaService transferenciaService) {
        this.transferenciaService = transferenciaService;
    }

    @PostMapping
    @PreAuthorize("@authz.pode('item.movimentar')")
    public ResponseEntity<List<TransferenciaResponse>> criar(@Valid @RequestBody TransferenciaCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transferenciaService.criar(request));
    }

    @GetMapping
    @PreAuthorize("@authz.pode('item.listar')")
    public ApiPage<TransferenciaResponse> listar(@RequestParam String idEvento,
                                                 @RequestParam(required = false) Integer page,
                                                 @RequestParam(required = false) Integer limit,
                                                 @RequestParam(required = false) String q,
                                                 @RequestParam(required = false) String idLocalDestino,
                                                 @RequestParam(required = false) String tpStatus,
                                                 @RequestParam(required = false) String data) {
        return transferenciaService.listar(idEvento, page, limit, q, idLocalDestino, tpStatus, data);
    }

    @GetMapping("/resumo")
    @PreAuthorize("@authz.pode('item.listar')")
    public TransferenciaResumoResponse resumo(@RequestParam String idEvento) {
        return transferenciaService.resumo(idEvento);
    }

    @GetMapping("/itens-disponiveis")
    @PreAuthorize("@authz.pode('item.listar')")
    public List<ItemDisponivelResponse> itensDisponiveis(@RequestParam String idEvento,
                                                         @RequestParam(required = false) String idLocalOrigem) {
        return transferenciaService.itensDisponiveis(idEvento, idLocalOrigem);
    }
}
