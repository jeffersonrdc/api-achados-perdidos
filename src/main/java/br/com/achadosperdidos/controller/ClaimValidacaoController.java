package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.ClaimValidacaoCreateRequest;
import br.com.achadosperdidos.controller.dto.ClaimValidacaoResponse;
import br.com.achadosperdidos.service.ClaimValidacaoService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/claims/validacoes")
@Tag(name = "Validação de Claims")
@SecurityRequirement(name = "bearerAuth")
public class ClaimValidacaoController {
    private final ClaimValidacaoService claimValidacaoService;

    public ClaimValidacaoController(ClaimValidacaoService claimValidacaoService) {
        this.claimValidacaoService = claimValidacaoService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE')")
    public ResponseEntity<ClaimValidacaoResponse> create(@Valid @RequestBody ClaimValidacaoCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(claimValidacaoService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE','CONSULTA')")
    public List<ClaimValidacaoResponse> findByClaim(@RequestParam String idClaim) {
        return claimValidacaoService.findByClaim(idClaim);
    }
}
