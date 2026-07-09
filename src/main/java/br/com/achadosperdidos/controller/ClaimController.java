package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.ClaimCreateRequest;
import br.com.achadosperdidos.controller.dto.ClaimResponse;
import br.com.achadosperdidos.pagination.ApiPage;
import br.com.achadosperdidos.service.ClaimService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/claims")
@Tag(name = "Claims")
@SecurityRequirement(name = "bearerAuth")
public class ClaimController {
    private final ClaimService claimService;
    public ClaimController(ClaimService claimService) { this.claimService = claimService; }
    @PostMapping @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE')")
    public ResponseEntity<ClaimResponse> create(@Valid @RequestBody ClaimCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(claimService.create(request));
    }
    @GetMapping @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE','CONSULTA')")
    public ApiPage<ClaimResponse> findAll(@RequestParam(required = false) Integer page,
                                          @RequestParam(required = false) Integer limit,
                                          @RequestParam(required = false) String idEvento) {
        return claimService.findAll(page, limit, idEvento);
    }
    @GetMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE','CONSULTA')")
    public ClaimResponse findById(@PathVariable String id) { return claimService.findById(id); }
    @DeleteMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE')")
    public ResponseEntity<Void> delete(@PathVariable String id) { claimService.softDelete(id); return ResponseEntity.noContent().build(); }
}
