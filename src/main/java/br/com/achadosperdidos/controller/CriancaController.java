package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.CriancaCreateRequest;
import br.com.achadosperdidos.controller.dto.CriancaResponsavelCreateRequest;
import br.com.achadosperdidos.controller.dto.CriancaResponsavelResponse;
import br.com.achadosperdidos.controller.dto.CriancaResponse;
import br.com.achadosperdidos.service.CriancaService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/criancas")
@Tag(name = "Crianças")
@SecurityRequirement(name = "bearerAuth")
public class CriancaController {
    private final CriancaService criancaService;

    public CriancaController(CriancaService criancaService) {
        this.criancaService = criancaService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR','ATENDENTE')")
    public ResponseEntity<CriancaResponse> create(@Valid @RequestBody CriancaCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(criancaService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR','ATENDENTE','CONSULTA')")
    public List<CriancaResponse> findByEvento(@RequestParam String idEvento) {
        return criancaService.findByEvento(idEvento);
    }

    @PostMapping("/{id}/responsaveis")
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR','ATENDENTE')")
    public ResponseEntity<CriancaResponsavelResponse> addResponsavel(
            @PathVariable String id,
            @Valid @RequestBody CriancaResponsavelCreateRequest request) {
        CriancaResponsavelCreateRequest body = new CriancaResponsavelCreateRequest(
                id,
                request.nmResponsavel(),
                request.nrCpf(),
                request.nrRg(),
                request.nmEmail(),
                request.nrTelefone(),
                request.dsParentesco(),
                request.fgPrincipal());
        return ResponseEntity.status(HttpStatus.CREATED).body(criancaService.addResponsavel(body));
    }

    @GetMapping("/{id}/responsaveis")
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR','ATENDENTE','CONSULTA')")
    public List<CriancaResponsavelResponse> findResponsaveis(@PathVariable String id) {
        return criancaService.findResponsaveis(id);
    }
}
