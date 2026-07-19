package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.DepositoCreateRequest;
import br.com.achadosperdidos.controller.dto.DepositoResponse;
import br.com.achadosperdidos.controller.dto.EstoqueEnderecoCreateRequest;
import br.com.achadosperdidos.controller.dto.EstoqueEnderecoResponse;
import br.com.achadosperdidos.controller.dto.EstoqueEnderecoUpdateRequest;
import br.com.achadosperdidos.service.DepositoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@Tag(name = "Depósitos",
        description = "Depósitos do evento e hierarquia de endereçamento (SETOR/ESTANTE/PRATELEIRA/CAIXA/POSICAO).")
@SecurityRequirement(name = "bearerAuth")
public class DepositoController {
    private final DepositoService depositoService;

    public DepositoController(DepositoService depositoService) {
        this.depositoService = depositoService;
    }

    @PostMapping
    @PreAuthorize("@authz.pode('deposito.gerenciar')")
    @Operation(summary = "Criar depósito do evento")
    public ResponseEntity<DepositoResponse> create(@Valid @RequestBody DepositoCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(depositoService.create(request));
    }

    @GetMapping
    @PreAuthorize("@authz.pode('deposito.listar')")
    @Operation(summary = "Listar depósitos do evento")
    public List<DepositoResponse> findByEvento(
            @Parameter(description = "ID assinado do evento", required = true) @RequestParam String idEvento) {
        return depositoService.findByEvento(idEvento);
    }

    @GetMapping("/{idDeposito}/enderecos")
    @PreAuthorize("@authz.pode('deposito.listar')")
    @Operation(summary = "Listar nomes de endereços por nível",
            description = "Usado nos selects do estoque. Ex.: `nivel=SETOR`.")
    public List<String> enderecos(
            @Parameter(description = "ID assinado do depósito") @PathVariable String idDeposito,
            @Parameter(description = "Nível: SETOR, ESTANTE, PRATELEIRA, CAIXA ou POSICAO")
            @RequestParam(required = false) String nivel) {
        return depositoService.listarEnderecos(idDeposito, nivel);
    }

    @GetMapping("/{idDeposito}/enderecos/itens")
    @PreAuthorize("@authz.pode('deposito.listar')")
    @Operation(summary = "Listar endereços (admin)",
            description = "Listagem completa para a tela /logistica-fisica.")
    public List<EstoqueEnderecoResponse> enderecosAdmin(
            @Parameter(description = "ID assinado do depósito") @PathVariable String idDeposito,
            @Parameter(description = "Nível obrigatório da hierarquia", required = true) @RequestParam String nivel,
            @Parameter(description = "Inclui endereços inativos quando `true`")
            @RequestParam(required = false, defaultValue = "false") boolean incluirInativos,
            @Parameter(description = "ID assinado do endereço pai") @RequestParam(required = false) String idPai) {
        return depositoService.listarEnderecosAdmin(idDeposito, nivel, incluirInativos, idPai);
    }

    @PostMapping("/{idDeposito}/enderecos")
    @PreAuthorize("@authz.pode('deposito.gerenciar')")
    @Operation(summary = "Criar endereço no depósito")
    public ResponseEntity<EstoqueEnderecoResponse> criarEndereco(
            @Parameter(description = "ID assinado do depósito") @PathVariable String idDeposito,
            @Valid @RequestBody EstoqueEnderecoCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(depositoService.criarEndereco(idDeposito, request));
    }

    @PutMapping("/{idDeposito}/enderecos/{idEndereco}")
    @PreAuthorize("@authz.pode('deposito.gerenciar')")
    @Operation(summary = "Atualizar endereço do depósito")
    public EstoqueEnderecoResponse atualizarEndereco(
            @Parameter(description = "ID assinado do depósito") @PathVariable String idDeposito,
            @Parameter(description = "ID assinado do endereço") @PathVariable String idEndereco,
            @Valid @RequestBody EstoqueEnderecoUpdateRequest request) {
        return depositoService.atualizarEndereco(idDeposito, idEndereco, request);
    }

    @DeleteMapping("/{idDeposito}/enderecos/{idEndereco}")
    @PreAuthorize("@authz.pode('deposito.gerenciar')")
    @Operation(summary = "Excluir endereço do depósito", description = "Exclusão lógica.")
    public ResponseEntity<Void> excluirEndereco(
            @Parameter(description = "ID assinado do depósito") @PathVariable String idDeposito,
            @Parameter(description = "ID assinado do endereço") @PathVariable String idEndereco) {
        depositoService.excluirEndereco(idDeposito, idEndereco);
        return ResponseEntity.noContent().build();
    }
}
