package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.DepositoCreateRequest;
import br.com.achadosperdidos.controller.dto.DepositoResponse;
import br.com.achadosperdidos.controller.dto.EstoqueEnderecoCreateRequest;
import br.com.achadosperdidos.controller.dto.EstoqueEnderecoResponse;
import br.com.achadosperdidos.controller.dto.EstoqueEnderecoUpdateRequest;
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

    /** Listagem admin (objetos) para /logistica-fisica. */
    @GetMapping("/{idDeposito}/enderecos/itens")
    @PreAuthorize("@authz.pode('deposito.listar')")
    public List<EstoqueEnderecoResponse> enderecosAdmin(
            @PathVariable String idDeposito,
            @RequestParam String nivel,
            @RequestParam(required = false, defaultValue = "false") boolean incluirInativos,
            @RequestParam(required = false) String idPai) {
        return depositoService.listarEnderecosAdmin(idDeposito, nivel, incluirInativos, idPai);
    }

    @PostMapping("/{idDeposito}/enderecos")
    @PreAuthorize("@authz.pode('deposito.gerenciar')")
    public ResponseEntity<EstoqueEnderecoResponse> criarEndereco(
            @PathVariable String idDeposito,
            @Valid @RequestBody EstoqueEnderecoCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(depositoService.criarEndereco(idDeposito, request));
    }

    @PutMapping("/{idDeposito}/enderecos/{idEndereco}")
    @PreAuthorize("@authz.pode('deposito.gerenciar')")
    public EstoqueEnderecoResponse atualizarEndereco(
            @PathVariable String idDeposito,
            @PathVariable String idEndereco,
            @Valid @RequestBody EstoqueEnderecoUpdateRequest request) {
        return depositoService.atualizarEndereco(idDeposito, idEndereco, request);
    }

    @DeleteMapping("/{idDeposito}/enderecos/{idEndereco}")
    @PreAuthorize("@authz.pode('deposito.gerenciar')")
    public ResponseEntity<Void> excluirEndereco(
            @PathVariable String idDeposito,
            @PathVariable String idEndereco) {
        depositoService.excluirEndereco(idDeposito, idEndereco);
        return ResponseEntity.noContent().build();
    }
}
