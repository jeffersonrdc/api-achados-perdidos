package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.*;
import br.com.achadosperdidos.pagination.ApiPage;
import br.com.achadosperdidos.service.PortalService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/portal")
@Tag(name = "Portal do Participante", description = "Área pública do evento para consulta, claims e cadastro de crianças")
public class PortalController {

    private final PortalService portalService;

    public PortalController(PortalService portalService) {
        this.portalService = portalService;
    }

    @GetMapping("/eventos")
    public List<PortalEventoResumoResponse> listarEventos() {
        return portalService.listarEventosAbertos();
    }

    @GetMapping("/eventos/{idEvento}")
    public PortalEventoResumoResponse detalharEvento(@PathVariable String idEvento) {
        return portalService.detalharEvento(idEvento);
    }

    @GetMapping("/categorias")
    public List<CategoriaResponse> listarCategorias() {
        return portalService.listarCategorias();
    }

    @GetMapping("/eventos/{idEvento}/locais")
    public List<PortalLocalResponse> listarLocais(@PathVariable String idEvento) {
        return portalService.listarLocais(idEvento);
    }

    @GetMapping("/eventos/{idEvento}/itens")
    public ApiPage<PortalItemCatalogoResponse> catalogoItens(
            @PathVariable String idEvento,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String pesquisa) {
        return portalService.catalogoItens(idEvento, page, limit, pesquisa);
    }

    @PostMapping("/eventos/{idEvento}/claims")
    public ResponseEntity<ClaimResponse> registrarObjetoPerdido(
            @PathVariable String idEvento,
            @Valid @RequestBody PortalClaimCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(portalService.registrarObjetoPerdido(idEvento, request));
    }

    @PostMapping("/eventos/{idEvento}/claims/item")
    public ResponseEntity<PortalClaimResultResponse> reclamarItem(
            @PathVariable String idEvento,
            @Valid @RequestBody PortalClaimItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(portalService.reclamarItem(idEvento, request));
    }

    @GetMapping("/eventos/{idEvento}/meus-claims")
    @PreAuthorize("hasRole('PARTICIPANTE')")
    public List<ClaimResponse> meusClaims(@PathVariable String idEvento, Authentication auth) {
        return portalService.meusClaims(idEvento, auth.getName());
    }

    @PostMapping("/eventos/{idEvento}/criancas")
    public ResponseEntity<CriancaResponse> cadastrarCrianca(
            @PathVariable String idEvento,
            @Valid @RequestBody CriancaCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(portalService.cadastrarCrianca(idEvento, request));
    }

    @PostMapping("/eventos/{idEvento}/criancas/responsaveis")
    public ResponseEntity<CriancaResponsavelResponse> vincularResponsavel(
            @PathVariable String idEvento,
            @Valid @RequestBody CriancaResponsavelCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(portalService.vincularResponsavel(request));
    }

    @PostMapping("/auth/registro")
    public ResponseEntity<UsuarioResponse> registrarParticipante(@Valid @RequestBody PortalParticipanteRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(portalService.registrarParticipante(request));
    }
}
