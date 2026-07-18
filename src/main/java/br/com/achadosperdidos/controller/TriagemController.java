package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.ColetaFiltrosResponse;
import br.com.achadosperdidos.controller.dto.TriagemFilaResponse;
import br.com.achadosperdidos.controller.dto.TriagemIaResponse;
import br.com.achadosperdidos.controller.dto.TriagemResponse;
import br.com.achadosperdidos.controller.dto.TriagemResumoResponse;
import br.com.achadosperdidos.controller.dto.TriagemSalvarRequest;
import br.com.achadosperdidos.pagination.ApiPage;
import br.com.achadosperdidos.service.TriagemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/triagem")
@Tag(name = "Triagem", description = "Fila, análise e conclusão da triagem de itens.")
@SecurityRequirement(name = "bearerAuth")
public class TriagemController {
    private final TriagemService triagemService;

    public TriagemController(TriagemService triagemService) {
        this.triagemService = triagemService;
    }

    @GetMapping
    @PreAuthorize("@authz.pode('triagem.listar')")
    @Operation(summary = "Listar fila de triagem (paginado)")
    public ApiPage<TriagemFilaResponse> fila(@Parameter(description = "ID assinado do evento", required = true) @RequestParam String idEvento,
                                             @RequestParam(required = false) Integer page,
                                             @RequestParam(required = false) Integer limit,
                                             @RequestParam(required = false) String q,
                                             @RequestParam(required = false) String idCategoria,
                                             @RequestParam(required = false) String local,
                                             @RequestParam(required = false) String tpPrioridade,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(required = false) String data) {
        return triagemService.fila(idEvento, page, limit, q, idCategoria, local, tpPrioridade, status, data);
    }

    @GetMapping("/resumo")
    @PreAuthorize("@authz.pode('triagem.listar')")
    @Operation(summary = "Resumo da triagem do evento")
    public TriagemResumoResponse resumo(@Parameter(description = "ID assinado do evento", required = true) @RequestParam String idEvento) {
        return triagemService.resumo(idEvento);
    }

    @GetMapping("/filtros")
    @PreAuthorize("@authz.pode('triagem.listar')")
    @Operation(summary = "Opções de filtro da triagem")
    public ColetaFiltrosResponse filtros(@Parameter(description = "ID assinado do evento", required = true) @RequestParam String idEvento) {
        return triagemService.filtros(idEvento);
    }

    @PostMapping("/itens/{idItem}/analisar")
    @PreAuthorize("@authz.pode('triagem.iniciar')")
    @Operation(summary = "Analisar item na triagem (abre registro; não remove do estoque)")
    public TriagemResponse analisar(@Parameter(description = "ID assinado do item") @PathVariable String idItem) {
        return triagemService.analisar(idItem);
    }

    @GetMapping("/itens/{idItem}")
    @PreAuthorize("@authz.pode('triagem.listar')")
    @Operation(summary = "Detalhar triagem do item")
    public TriagemResponse detalhe(@Parameter(description = "ID assinado do item") @PathVariable String idItem) {
        return triagemService.detalhe(idItem);
    }

    @GetMapping("/itens/{idItem}/sugestao-ia")
    @PreAuthorize("@authz.pode('triagem.listar')")
    @Operation(summary = "Obter sugestão de IA para triagem")
    public TriagemIaResponse sugestaoIa(@Parameter(description = "ID assinado do item") @PathVariable String idItem) {
        return triagemService.sugestaoIa(idItem);
    }

    @PostMapping("/itens/{idItem}/iniciar")
    @PreAuthorize("@authz.pode('triagem.iniciar')")
    @Operation(summary = "Iniciar triagem do item")
    public TriagemResponse iniciar(@Parameter(description = "ID assinado do item") @PathVariable String idItem) {
        return triagemService.iniciar(idItem);
    }

    @PutMapping("/itens/{idItem}")
    @PreAuthorize("@authz.pode('triagem.salvar')")
    @Operation(summary = "Salvar dados da triagem")
    public TriagemResponse salvar(@Parameter(description = "ID assinado do item") @PathVariable String idItem, @Valid @RequestBody TriagemSalvarRequest request) {
        return triagemService.salvar(idItem, request);
    }

    @PostMapping("/itens/{idItem}/concluir")
    @PreAuthorize("@authz.pode('triagem.concluir')")
    @Operation(summary = "Concluir triagem do item (apenas atualiza dados; não encaminha ao estoque)")
    public TriagemResponse concluir(@Parameter(description = "ID assinado do item") @PathVariable String idItem, @Valid @RequestBody TriagemSalvarRequest request) {
        return triagemService.concluir(idItem, request);
    }
}
