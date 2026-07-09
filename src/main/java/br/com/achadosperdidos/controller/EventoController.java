package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.EventoCreateRequest;
import br.com.achadosperdidos.controller.dto.EventoResponse;
import br.com.achadosperdidos.service.EventoService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/eventos")
@Tag(name = "Eventos")
@SecurityRequirement(name = "bearerAuth")
public class EventoController {
    private final EventoService eventoService;
    public EventoController(EventoService eventoService) { this.eventoService = eventoService; }

    @PostMapping @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventoResponse> create(@Valid @RequestBody EventoCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventoService.create(request));
    }
    @GetMapping @PreAuthorize("hasAnyRole('ADMIN','OPERADOR','ATENDENTE','CONSULTA')")
    public List<EventoResponse> findAll(@RequestParam(defaultValue = "false") boolean incluirInativos) {
        return eventoService.findAll(incluirInativos);
    }
    @GetMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','OPERADOR','ATENDENTE','CONSULTA')")
    public EventoResponse findById(@PathVariable String id) { return eventoService.findById(id); }
    @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String id) { eventoService.softDelete(id); return ResponseEntity.noContent().build(); }
}
