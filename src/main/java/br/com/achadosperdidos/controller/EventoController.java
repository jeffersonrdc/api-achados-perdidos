package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.EventoCreateRequest;
import br.com.achadosperdidos.controller.dto.EventoConfiguracaoResponse;
import br.com.achadosperdidos.controller.dto.EventoConfiguracaoUpdateRequest;
import br.com.achadosperdidos.controller.dto.EventoResponse;
import br.com.achadosperdidos.service.EventoConfiguracaoService;
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
    private final EventoConfiguracaoService eventoConfiguracaoService;
    public EventoController(EventoService eventoService, EventoConfiguracaoService eventoConfiguracaoService) {
        this.eventoService = eventoService;
        this.eventoConfiguracaoService = eventoConfiguracaoService;
    }

    @PostMapping @PreAuthorize("@authz.pode('evento.criar')")
    public ResponseEntity<EventoResponse> create(@Valid @RequestBody EventoCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventoService.create(request));
    }
    @GetMapping @PreAuthorize("@authz.pode('evento.listar')")
    public List<EventoResponse> findAll(@RequestParam(defaultValue = "false") boolean incluirInativos) {
        return eventoService.findAll(incluirInativos);
    }
    @GetMapping("/{id}") @PreAuthorize("@authz.pode('evento.listar')")
    public EventoResponse findById(@PathVariable String id) { return eventoService.findById(id); }
    @DeleteMapping("/{id}") @PreAuthorize("@authz.pode('evento.excluir')")
    public ResponseEntity<Void> delete(@PathVariable String id) { eventoService.softDelete(id); return ResponseEntity.noContent().build(); }

    @GetMapping("/{id}/configuracao") @PreAuthorize("@authz.pode('evento.listar')")
    public EventoConfiguracaoResponse getConfiguracao(@PathVariable String id) {
        return eventoConfiguracaoService.findByEvento(id);
    }

    @PutMapping("/{id}/configuracao") @PreAuthorize("@authz.pode('configuracao.gerenciar')")
    public EventoConfiguracaoResponse updateConfiguracao(@PathVariable String id, @RequestBody EventoConfiguracaoUpdateRequest request) {
        return eventoConfiguracaoService.upsert(id, request);
    }
}
