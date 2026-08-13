package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.*;
import br.com.achadosperdidos.pagination.ApiPage;
import br.com.achadosperdidos.service.CatalogoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@Tag(name = "Catálogo", description = "Cores, marcas e modelos usados na coleta e na tela de características.")
@SecurityRequirement(name = "bearerAuth")
public class CatalogoController {
    private final CatalogoService catalogoService;
    public CatalogoController(CatalogoService catalogoService) { this.catalogoService = catalogoService; }

    // ---- Selects (somente nomes) ----

    @GetMapping("/cores")
    @PreAuthorize("@authz.pode('item.listar')")
    @Operation(summary = "Listar nomes de cores",
            description = "Retorna apenas nomes ativos para selects da coleta.")
    public List<String> cores() { return catalogoService.listarCores(); }

    @GetMapping("/marcas")
    @PreAuthorize("@authz.pode('item.listar')")
    @Operation(summary = "Listar nomes de marcas",
            description = "Retorna nomes ativos. Com `subcategoria`, filtra via marca_subcategoria (como tags por sub).")
    public List<String> marcas(
            @Parameter(description = "Nome da subcategoria para restringir o catálogo")
            @RequestParam(required = false) String subcategoria) {
        return catalogoService.listarMarcas(subcategoria);
    }

    @GetMapping("/modelos")
    @PreAuthorize("@authz.pode('item.listar')")
    @Operation(summary = "Listar nomes de modelos",
            description = "Filtra por marca; opcionalmente por subcategoria (genéricos + vinculados).")
    public List<String> modelos(
            @Parameter(description = "Nome da marca para filtrar modelos") @RequestParam(required = false) String marca,
            @Parameter(description = "Nome da subcategoria para restringir o catálogo") @RequestParam(required = false) String subcategoria) {
        return catalogoService.listarModelos(marca, subcategoria);
    }

    // ---- CRUD admin: marcas ----

    @GetMapping("/marcas/itens")
    @PreAuthorize("@authz.pode('categoria.listar')")
    @Operation(summary = "Listar marcas (admin, paginado)",
            description = "Listagem completa para a tela /caracteristicas.")
    public ApiPage<MarcaResponse> marcasAdmin(
            @Parameter(description = "Inclui marcas inativas quando `true`")
            @RequestParam(required = false, defaultValue = "false") boolean incluirInativos,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String q) {
        return catalogoService.listarMarcasAdmin(incluirInativos, page, limit, q);
    }

    @PostMapping("/marcas")
    @PreAuthorize("@authz.pode('categoria.gerenciar')")
    @Operation(summary = "Criar marca")
    public ResponseEntity<MarcaResponse> criarMarca(@Valid @RequestBody MarcaCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogoService.criarMarca(request));
    }

    @PutMapping("/marcas/{id}")
    @PreAuthorize("@authz.pode('categoria.gerenciar')")
    @Operation(summary = "Atualizar marca")
    public MarcaResponse atualizarMarca(
            @Parameter(description = "ID assinado da marca") @PathVariable String id,
            @Valid @RequestBody MarcaUpdateRequest request) {
        return catalogoService.atualizarMarca(id, request);
    }

    @DeleteMapping("/marcas/{id}")
    @PreAuthorize("@authz.pode('categoria.gerenciar')")
    @Operation(summary = "Excluir marca", description = "Exclusão lógica.")
    public ResponseEntity<Void> excluirMarca(
            @Parameter(description = "ID assinado da marca") @PathVariable String id) {
        catalogoService.excluirMarca(id);
        return ResponseEntity.noContent().build();
    }

    // ---- CRUD admin: modelos ----

    @GetMapping("/modelos/itens")
    @PreAuthorize("@authz.pode('categoria.listar')")
    @Operation(summary = "Listar modelos (admin, paginado)",
            description = "Listagem completa; opcionalmente filtrada por marca.")
    public ApiPage<ModeloResponse> modelosAdmin(
            @Parameter(description = "Inclui modelos inativos quando `true`")
            @RequestParam(required = false, defaultValue = "false") boolean incluirInativos,
            @Parameter(description = "ID assinado da marca") @RequestParam(required = false) String idMarca,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String q) {
        return catalogoService.listarModelosAdmin(incluirInativos, idMarca, page, limit, q);
    }

    @PostMapping("/modelos")
    @PreAuthorize("@authz.pode('categoria.gerenciar')")
    @Operation(summary = "Criar modelo")
    public ResponseEntity<ModeloResponse> criarModelo(@Valid @RequestBody ModeloCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogoService.criarModelo(request));
    }

    @PutMapping("/modelos/{id}")
    @PreAuthorize("@authz.pode('categoria.gerenciar')")
    @Operation(summary = "Atualizar modelo")
    public ModeloResponse atualizarModelo(
            @Parameter(description = "ID assinado do modelo") @PathVariable String id,
            @Valid @RequestBody ModeloUpdateRequest request) {
        return catalogoService.atualizarModelo(id, request);
    }

    @DeleteMapping("/modelos/{id}")
    @PreAuthorize("@authz.pode('categoria.gerenciar')")
    @Operation(summary = "Excluir modelo", description = "Exclusão lógica.")
    public ResponseEntity<Void> excluirModelo(
            @Parameter(description = "ID assinado do modelo") @PathVariable String id) {
        catalogoService.excluirModelo(id);
        return ResponseEntity.noContent().build();
    }

    // ---- CRUD admin: cores ----

    @GetMapping("/cores/itens")
    @PreAuthorize("@authz.pode('categoria.listar')")
    @Operation(summary = "Listar cores (admin, paginado)",
            description = "Listagem completa para a tela /caracteristicas.")
    public ApiPage<CorResponse> coresAdmin(
            @Parameter(description = "Inclui cores inativas quando `true`")
            @RequestParam(required = false, defaultValue = "false") boolean incluirInativos,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String q) {
        return catalogoService.listarCoresAdmin(incluirInativos, page, limit, q);
    }

    @PostMapping("/cores")
    @PreAuthorize("@authz.pode('categoria.gerenciar')")
    @Operation(summary = "Criar cor")
    public ResponseEntity<CorResponse> criarCor(@Valid @RequestBody CorCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogoService.criarCor(request));
    }

    @PutMapping("/cores/{id}")
    @PreAuthorize("@authz.pode('categoria.gerenciar')")
    @Operation(summary = "Atualizar cor")
    public CorResponse atualizarCor(
            @Parameter(description = "ID assinado da cor") @PathVariable String id,
            @Valid @RequestBody CorUpdateRequest request) {
        return catalogoService.atualizarCor(id, request);
    }

    @DeleteMapping("/cores/{id}")
    @PreAuthorize("@authz.pode('categoria.gerenciar')")
    @Operation(summary = "Excluir cor", description = "Exclusão lógica.")
    public ResponseEntity<Void> excluirCor(
            @Parameter(description = "ID assinado da cor") @PathVariable String id) {
        catalogoService.excluirCor(id);
        return ResponseEntity.noContent().build();
    }
}
