package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.LocalCreateRequest;
import br.com.achadosperdidos.controller.dto.LocalResponse;
import br.com.achadosperdidos.controller.dto.LocalUpdateRequest;
import br.com.achadosperdidos.service.LocalService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/locais")
@Tag(name = "Locais")
@SecurityRequirement(name = "bearerAuth")
public class LocalController {
    private final LocalService localService;

    public LocalController(LocalService localService) {
        this.localService = localService;
    }

    @PostMapping
    @PreAuthorize("@authz.pode('local.criar')")
    public ResponseEntity<LocalResponse> create(@Valid @RequestBody LocalCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(localService.create(request));
    }

    @GetMapping
    @PreAuthorize("@authz.pode('local.listar')")
    public List<LocalResponse> findByEvento(@RequestParam String idEvento) {
        return localService.findByEvento(idEvento);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.pode('local.listar')")
    public LocalResponse findById(@PathVariable String id) {
        return localService.findById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.pode('local.editar')")
    public LocalResponse update(@PathVariable String id, @Valid @RequestBody LocalUpdateRequest request) {
        return localService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.pode('local.excluir')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        localService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
