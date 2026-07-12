package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.ArquivoCreateRequest;
import br.com.achadosperdidos.controller.dto.ArquivoResponse;
import br.com.achadosperdidos.service.ArquivoService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/arquivos")
@Tag(name = "Arquivos")
@SecurityRequirement(name = "bearerAuth")
public class ArquivoController {
    private final ArquivoService arquivoService;

    public ArquivoController(ArquivoService arquivoService) {
        this.arquivoService = arquivoService;
    }

    @PostMapping
    @PreAuthorize("@authz.pode('arquivo.gerenciar')")
    public ResponseEntity<ArquivoResponse> create(@Valid @RequestBody ArquivoCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(arquivoService.create(request));
    }

    @GetMapping
    @PreAuthorize("@authz.pode('arquivo.listar')")
    public List<ArquivoResponse> findByEntidade(
            @RequestParam String tpEntidade,
            @RequestParam String idEntidade) {
        return arquivoService.findByEntidade(tpEntidade, idEntidade);
    }
}
