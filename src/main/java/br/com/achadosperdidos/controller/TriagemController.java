package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.ColetaFiltrosResponse;
import br.com.achadosperdidos.controller.dto.TriagemFilaResponse;
import br.com.achadosperdidos.controller.dto.TriagemIaResponse;
import br.com.achadosperdidos.controller.dto.TriagemResponse;
import br.com.achadosperdidos.controller.dto.TriagemResumoResponse;
import br.com.achadosperdidos.controller.dto.TriagemSalvarRequest;
import br.com.achadosperdidos.pagination.ApiPage;
import br.com.achadosperdidos.service.TriagemService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/triagem")
@Tag(name = "Triagem")
@SecurityRequirement(name = "bearerAuth")
public class TriagemController {
    private final TriagemService triagemService;

    public TriagemController(TriagemService triagemService) {
        this.triagemService = triagemService;
    }

    @GetMapping
    @PreAuthorize("@authz.pode('triagem.listar')")
    public ApiPage<TriagemFilaResponse> fila(@RequestParam String idEvento,
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
    public TriagemResumoResponse resumo(@RequestParam String idEvento) {
        return triagemService.resumo(idEvento);
    }

    @GetMapping("/filtros")
    @PreAuthorize("@authz.pode('triagem.listar')")
    public ColetaFiltrosResponse filtros(@RequestParam String idEvento) {
        return triagemService.filtros(idEvento);
    }

    @PostMapping("/itens/{idItem}/analisar")
    @PreAuthorize("@authz.pode('triagem.iniciar')")
    public TriagemResponse analisar(@PathVariable String idItem) {
        return triagemService.analisar(idItem);
    }

    @GetMapping("/itens/{idItem}")
    @PreAuthorize("@authz.pode('triagem.listar')")
    public TriagemResponse detalhe(@PathVariable String idItem) {
        return triagemService.detalhe(idItem);
    }

    @GetMapping("/itens/{idItem}/sugestao-ia")
    @PreAuthorize("@authz.pode('triagem.listar')")
    public TriagemIaResponse sugestaoIa(@PathVariable String idItem) {
        return triagemService.sugestaoIa(idItem);
    }

    @PostMapping("/itens/{idItem}/iniciar")
    @PreAuthorize("@authz.pode('triagem.iniciar')")
    public TriagemResponse iniciar(@PathVariable String idItem) {
        return triagemService.iniciar(idItem);
    }

    @PutMapping("/itens/{idItem}")
    @PreAuthorize("@authz.pode('triagem.salvar')")
    public TriagemResponse salvar(@PathVariable String idItem, @Valid @RequestBody TriagemSalvarRequest request) {
        return triagemService.salvar(idItem, request);
    }

    @PostMapping("/itens/{idItem}/concluir")
    @PreAuthorize("@authz.pode('triagem.concluir')")
    public TriagemResponse concluir(@PathVariable String idItem, @Valid @RequestBody TriagemSalvarRequest request) {
        return triagemService.concluir(idItem, request);
    }
}
