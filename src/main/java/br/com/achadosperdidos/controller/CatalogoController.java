package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.service.CatalogoService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Catálogos (cor, marca, modelo) para os selects da Coleta/Edição de itens. */
@RestController
@RequestMapping("/api/v1/catalogo")
@Tag(name = "Catálogo")
@SecurityRequirement(name = "bearerAuth")
public class CatalogoController {
    private final CatalogoService catalogoService;
    public CatalogoController(CatalogoService catalogoService) { this.catalogoService = catalogoService; }

    @GetMapping("/cores") @PreAuthorize("@authz.pode('item.listar')")
    public List<String> cores() { return catalogoService.listarCores(); }

    @GetMapping("/marcas") @PreAuthorize("@authz.pode('item.listar')")
    public List<String> marcas() { return catalogoService.listarMarcas(); }

    /** Modelos da marca selecionada (cascade). Ex.: /catalogo/modelos?marca=Apple */
    @GetMapping("/modelos") @PreAuthorize("@authz.pode('item.listar')")
    public List<String> modelos(@RequestParam(required = false) String marca) {
        return catalogoService.listarModelos(marca);
    }
}
