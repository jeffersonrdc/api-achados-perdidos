package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.PortalCategoriaCapaResponse;
import br.com.achadosperdidos.service.PortalCategoriaCapaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/config/portal-categoria-capas")
@Tag(name = "Imagens de categoria no portal",
        description = "Imagem substituta no catálogo público por categoria (ex.: Documentos), sem expor a foto original.")
@SecurityRequirement(name = "bearerAuth")
public class PortalCategoriaCapaController {

    private final PortalCategoriaCapaService service;

    public PortalCategoriaCapaController(PortalCategoriaCapaService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("@authz.pode('configuracao.gerenciar')")
    @Operation(summary = "Listar imagens vinculadas a categorias")
    public List<PortalCategoriaCapaResponse> listar() {
        return service.listar();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@authz.pode('configuracao.gerenciar')")
    @Operation(summary = "Enviar ou substituir a imagem de uma categoria")
    public ResponseEntity<PortalCategoriaCapaResponse> salvar(
            @Parameter(description = "ID assinado da categoria-pai") @RequestParam String idCategoria,
            @Parameter(description = "ID assinado do evento (storage)") @RequestParam String idEvento,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.salvar(idCategoria, idEvento, file));
    }

    @DeleteMapping("/{idCategoria}")
    @PreAuthorize("@authz.pode('configuracao.gerenciar')")
    @Operation(summary = "Remover a imagem vinculada à categoria (volta a foto original no portal)")
    public ResponseEntity<Void> remover(
            @Parameter(description = "ID assinado da categoria-pai") @PathVariable String idCategoria) {
        service.remover(idCategoria);
        return ResponseEntity.noContent().build();
    }
}
