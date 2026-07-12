package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.EtiquetaConteudoResponse;
import br.com.achadosperdidos.controller.dto.EtiquetaImprimirRequest;
import br.com.achadosperdidos.controller.dto.EtiquetaImpressaoResponse;
import br.com.achadosperdidos.service.EtiquetaService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/itens/{idItem}/etiqueta")
@Tag(name = "Etiqueta")
@SecurityRequirement(name = "bearerAuth")
public class EtiquetaController {
    private final EtiquetaService etiquetaService;

    public EtiquetaController(EtiquetaService etiquetaService) {
        this.etiquetaService = etiquetaService;
    }

    @GetMapping
    @PreAuthorize("@authz.pode('etiqueta.visualizar')")
    public EtiquetaConteudoResponse conteudo(@PathVariable String idItem) {
        return etiquetaService.conteudo(idItem);
    }

    @PostMapping("/imprimir")
    @PreAuthorize("@authz.pode('etiqueta.imprimir')")
    public ResponseEntity<EtiquetaImpressaoResponse> imprimir(@PathVariable String idItem,
                                                              @Valid @RequestBody EtiquetaImprimirRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(etiquetaService.imprimir(idItem, request));
    }

    @GetMapping("/impressoes")
    @PreAuthorize("@authz.pode('etiqueta.visualizar')")
    public List<EtiquetaImpressaoResponse> historico(@PathVariable String idItem) {
        return etiquetaService.historico(idItem);
    }
}
