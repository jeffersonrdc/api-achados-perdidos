package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.EquipeCreateRequest;
import br.com.achadosperdidos.controller.dto.EquipeMembroRequest;
import br.com.achadosperdidos.controller.dto.EquipeMembroResponse;
import br.com.achadosperdidos.controller.dto.EquipeResponse;
import br.com.achadosperdidos.controller.dto.EquipeUpdateRequest;
import br.com.achadosperdidos.service.EquipeService;
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
@RequestMapping("/api/v1/equipes")
@Tag(name = "Equipes", description = "Equipes operacionais do evento e seus membros.")
@SecurityRequirement(name = "bearerAuth")
public class EquipeController {
    private final EquipeService equipeService;

    public EquipeController(EquipeService equipeService) {
        this.equipeService = equipeService;
    }

    @PostMapping
    @PreAuthorize("@authz.pode('equipe.criar')")
    @Operation(summary = "Criar equipe")
    public ResponseEntity<EquipeResponse> create(@Valid @RequestBody EquipeCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(equipeService.create(request));
    }

    @GetMapping
    @PreAuthorize("@authz.pode('equipe.listar')")
    @Operation(summary = "Listar equipes do evento")
    public List<EquipeResponse> findByEvento(
            @Parameter(description = "ID assinado do evento", required = true) @RequestParam String idEvento) {
        return equipeService.findByEvento(idEvento);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.pode('equipe.listar')")
    @Operation(summary = "Detalhar equipe")
    public EquipeResponse findById(
            @Parameter(description = "ID assinado da equipe") @PathVariable String id) {
        return equipeService.findById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.pode('equipe.editar')")
    @Operation(summary = "Atualizar equipe")
    public EquipeResponse update(
            @Parameter(description = "ID assinado da equipe") @PathVariable String id,
            @Valid @RequestBody EquipeUpdateRequest request) {
        return equipeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.pode('equipe.excluir')")
    @Operation(summary = "Excluir equipe", description = "Exclusão lógica.")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID assinado da equipe") @PathVariable String id) {
        equipeService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/membros")
    @PreAuthorize("@authz.pode('equipe.membros')")
    @Operation(summary = "Adicionar membro à equipe")
    public ResponseEntity<EquipeMembroResponse> adicionarMembro(
            @Parameter(description = "ID assinado da equipe") @PathVariable String id,
            @Valid @RequestBody EquipeMembroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(equipeService.adicionarMembro(id, request));
    }

    @DeleteMapping("/{id}/membros/{idUsuario}")
    @PreAuthorize("@authz.pode('equipe.membros')")
    @Operation(summary = "Remover membro da equipe")
    public ResponseEntity<Void> removerMembro(
            @Parameter(description = "ID assinado da equipe") @PathVariable String id,
            @Parameter(description = "ID assinado do usuário") @PathVariable String idUsuario) {
        equipeService.removerMembro(id, idUsuario);
        return ResponseEntity.noContent().build();
    }
}
