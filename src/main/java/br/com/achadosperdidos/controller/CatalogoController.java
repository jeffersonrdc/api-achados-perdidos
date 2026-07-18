package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.*;
import br.com.achadosperdidos.service.CatalogoService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Catálogos (cor, marca, modelo) — selects da coleta + CRUD da tela /caracteristicas. */
@RestController
@RequestMapping("/api/v1/catalogo")
@Tag(name = "Catálogo")
@SecurityRequirement(name = "bearerAuth")
public class CatalogoController {
    private final CatalogoService catalogoService;
    public CatalogoController(CatalogoService catalogoService) { this.catalogoService = catalogoService; }

    // ---- Selects (somente nomes) ----

    @GetMapping("/cores") @PreAuthorize("@authz.pode('item.listar')")
    public List<String> cores() { return catalogoService.listarCores(); }

    @GetMapping("/marcas") @PreAuthorize("@authz.pode('item.listar')")
    public List<String> marcas() { return catalogoService.listarMarcas(); }

    @GetMapping("/modelos") @PreAuthorize("@authz.pode('item.listar')")
    public List<String> modelos(@RequestParam(required = false) String marca) {
        return catalogoService.listarModelos(marca);
    }

    // ---- CRUD admin: marcas ----

    @GetMapping("/marcas/itens") @PreAuthorize("@authz.pode('categoria.listar')")
    public List<MarcaResponse> marcasAdmin(@RequestParam(required = false, defaultValue = "false") boolean incluirInativos) {
        return catalogoService.listarMarcasAdmin(incluirInativos);
    }

    @PostMapping("/marcas") @PreAuthorize("@authz.pode('categoria.gerenciar')")
    public ResponseEntity<MarcaResponse> criarMarca(@Valid @RequestBody MarcaCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogoService.criarMarca(request));
    }

    @PutMapping("/marcas/{id}") @PreAuthorize("@authz.pode('categoria.gerenciar')")
    public MarcaResponse atualizarMarca(@PathVariable String id, @Valid @RequestBody MarcaUpdateRequest request) {
        return catalogoService.atualizarMarca(id, request);
    }

    @DeleteMapping("/marcas/{id}") @PreAuthorize("@authz.pode('categoria.gerenciar')")
    public ResponseEntity<Void> excluirMarca(@PathVariable String id) {
        catalogoService.excluirMarca(id);
        return ResponseEntity.noContent().build();
    }

    // ---- CRUD admin: modelos ----

    @GetMapping("/modelos/itens") @PreAuthorize("@authz.pode('categoria.listar')")
    public List<ModeloResponse> modelosAdmin(
            @RequestParam(required = false, defaultValue = "false") boolean incluirInativos,
            @RequestParam(required = false) String idMarca) {
        return catalogoService.listarModelosAdmin(incluirInativos, idMarca);
    }

    @PostMapping("/modelos") @PreAuthorize("@authz.pode('categoria.gerenciar')")
    public ResponseEntity<ModeloResponse> criarModelo(@Valid @RequestBody ModeloCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogoService.criarModelo(request));
    }

    @PutMapping("/modelos/{id}") @PreAuthorize("@authz.pode('categoria.gerenciar')")
    public ModeloResponse atualizarModelo(@PathVariable String id, @Valid @RequestBody ModeloUpdateRequest request) {
        return catalogoService.atualizarModelo(id, request);
    }

    @DeleteMapping("/modelos/{id}") @PreAuthorize("@authz.pode('categoria.gerenciar')")
    public ResponseEntity<Void> excluirModelo(@PathVariable String id) {
        catalogoService.excluirModelo(id);
        return ResponseEntity.noContent().build();
    }

    // ---- CRUD admin: cores ----

    @GetMapping("/cores/itens") @PreAuthorize("@authz.pode('categoria.listar')")
    public List<CorResponse> coresAdmin(@RequestParam(required = false, defaultValue = "false") boolean incluirInativos) {
        return catalogoService.listarCoresAdmin(incluirInativos);
    }

    @PostMapping("/cores") @PreAuthorize("@authz.pode('categoria.gerenciar')")
    public ResponseEntity<CorResponse> criarCor(@Valid @RequestBody CorCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogoService.criarCor(request));
    }

    @PutMapping("/cores/{id}") @PreAuthorize("@authz.pode('categoria.gerenciar')")
    public CorResponse atualizarCor(@PathVariable String id, @Valid @RequestBody CorUpdateRequest request) {
        return catalogoService.atualizarCor(id, request);
    }

    @DeleteMapping("/cores/{id}") @PreAuthorize("@authz.pode('categoria.gerenciar')")
    public ResponseEntity<Void> excluirCor(@PathVariable String id) {
        catalogoService.excluirCor(id);
        return ResponseEntity.noContent().build();
    }
}
