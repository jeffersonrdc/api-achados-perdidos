package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.LacreCreateRequest;
import br.com.achadosperdidos.controller.dto.LacreResponse;
import br.com.achadosperdidos.service.LacreService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/lacres")
@Tag(name = "Lacres")
@SecurityRequirement(name = "bearerAuth")
public class LacreController {
    private final LacreService lacreService;

    public LacreController(LacreService lacreService) {
        this.lacreService = lacreService;
    }

    @PostMapping
    @PreAuthorize("@authz.pode('lacre.gerenciar')")
    public ResponseEntity<LacreResponse> create(@Valid @RequestBody LacreCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lacreService.create(request));
    }

    @GetMapping("/{nrLacre}")
    @PreAuthorize("@authz.pode('lacre.listar')")
    public LacreResponse findByNumero(@PathVariable String nrLacre) {
        return lacreService.findByNumero(nrLacre);
    }
}
