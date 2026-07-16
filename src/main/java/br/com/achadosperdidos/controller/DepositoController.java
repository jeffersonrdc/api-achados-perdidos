package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.DepositoCreateRequest;
import br.com.achadosperdidos.controller.dto.DepositoResponse;
import br.com.achadosperdidos.service.DepositoService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/depositos")
@Tag(name = "Depósitos")
@SecurityRequirement(name = "bearerAuth")
public class DepositoController {
    private final DepositoService depositoService;

    public DepositoController(DepositoService depositoService) {
        this.depositoService = depositoService;
    }

    @PostMapping
    @PreAuthorize("@authz.pode('deposito.gerenciar')")
    public ResponseEntity<DepositoResponse> create(@Valid @RequestBody DepositoCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(depositoService.create(request));
    }

    @GetMapping
    @PreAuthorize("@authz.pode('deposito.listar')")
    public List<DepositoResponse> findByEvento(@RequestParam String idEvento) {
        return depositoService.findByEvento(idEvento);
    }

    /** Endereçamento do depósito para os selects do estoque. Ex.: /depositos/{id}/enderecos?nivel=SETOR */
    @GetMapping("/{idDeposito}/enderecos")
    @PreAuthorize("@authz.pode('deposito.listar')")
    public List<String> enderecos(@PathVariable String idDeposito, @RequestParam(required = false) String nivel) {
        return depositoService.listarEnderecos(idDeposito, nivel);
    }
}
