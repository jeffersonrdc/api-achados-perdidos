package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.EquipeCreateRequest;
import br.com.achadosperdidos.controller.dto.EquipeMembroRequest;
import br.com.achadosperdidos.controller.dto.EquipeMembroResponse;
import br.com.achadosperdidos.controller.dto.EquipeResponse;
import br.com.achadosperdidos.controller.dto.EquipeUpdateRequest;
import br.com.achadosperdidos.service.EquipeService;
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
@Tag(name = "Equipes")
@SecurityRequirement(name = "bearerAuth")
public class EquipeController {
    private final EquipeService equipeService;

    public EquipeController(EquipeService equipeService) {
        this.equipeService = equipeService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR')")
    public ResponseEntity<EquipeResponse> create(@Valid @RequestBody EquipeCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(equipeService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR','ATENDENTE','CONSULTA')")
    public List<EquipeResponse> findByEvento(@RequestParam String idEvento) {
        return equipeService.findByEvento(idEvento);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR','ATENDENTE','CONSULTA')")
    public EquipeResponse findById(@PathVariable String id) {
        return equipeService.findById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR')")
    public EquipeResponse update(@PathVariable String id, @Valid @RequestBody EquipeUpdateRequest request) {
        return equipeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        equipeService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/membros")
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR')")
    public ResponseEntity<EquipeMembroResponse> adicionarMembro(@PathVariable String id,
                                                                @Valid @RequestBody EquipeMembroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(equipeService.adicionarMembro(id, request));
    }

    @DeleteMapping("/{id}/membros/{idUsuario}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR')")
    public ResponseEntity<Void> removerMembro(@PathVariable String id, @PathVariable String idUsuario) {
        equipeService.removerMembro(id, idUsuario);
        return ResponseEntity.noContent().build();
    }
}
