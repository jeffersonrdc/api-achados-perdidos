package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.EventoCreateRequest;
import br.com.achadosperdidos.controller.dto.EventoConfiguracaoResponse;
import br.com.achadosperdidos.controller.dto.EventoConfiguracaoUpdateRequest;
import br.com.achadosperdidos.controller.dto.EventoResponse;
import br.com.achadosperdidos.controller.dto.EventoUpdateRequest;
import br.com.achadosperdidos.service.EventoConfiguracaoService;
import br.com.achadosperdidos.service.EventoService;
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
@RequestMapping("/api/v1/eventos")
@Tag(name = "Eventos", description = "Cadastro, consulta e configuração de eventos.")
@SecurityRequirement(name = "bearerAuth")
public class EventoController {
    private final EventoService eventoService;
    private final EventoConfiguracaoService eventoConfiguracaoService;
    public EventoController(EventoService eventoService, EventoConfiguracaoService eventoConfiguracaoService) {
        this.eventoService = eventoService;
        this.eventoConfiguracaoService = eventoConfiguracaoService;
    }

    @PostMapping @PreAuthorize("@authz.pode('evento.criar')")
    @Operation(summary = "Criar evento")
    public ResponseEntity<EventoResponse> create(@Valid @RequestBody EventoCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventoService.create(request));
    }
    @GetMapping @PreAuthorize("@authz.pode('evento.listar')")
    @Operation(summary = "Listar eventos", description = "Por padrão retorna apenas eventos ativos.")
    public List<EventoResponse> findAll(@RequestParam(defaultValue = "false") boolean incluirInativos) {
        return eventoService.findAll(incluirInativos);
    }
    @GetMapping("/{id}") @PreAuthorize("@authz.pode('evento.listar')")
    @Operation(summary = "Buscar evento por ID assinado")
    public EventoResponse findById(@Parameter(description = "ID assinado do evento") @PathVariable String id) { return eventoService.findById(id); }
    @PutMapping("/{id}") @PreAuthorize("@authz.pode('evento.editar')")
    @Operation(summary = "Atualizar evento")
    public EventoResponse update(@Parameter(description = "ID assinado do evento") @PathVariable String id, @RequestBody EventoUpdateRequest request) {
        return eventoService.update(id, request);
    }
    @DeleteMapping("/{id}") @PreAuthorize("@authz.pode('evento.excluir')")
    @Operation(summary = "Excluir evento (soft delete)")
    public ResponseEntity<Void> delete(@Parameter(description = "ID assinado do evento") @PathVariable String id) { eventoService.softDelete(id); return ResponseEntity.noContent().build(); }

    @GetMapping("/{id}/configuracao") @PreAuthorize("@authz.pode('evento.listar')")
    @Operation(summary = "Consultar configuração do evento")
    public EventoConfiguracaoResponse getConfiguracao(@Parameter(description = "ID assinado do evento") @PathVariable String id) {
        return eventoConfiguracaoService.findByEvento(id);
    }

    @PutMapping("/{id}/configuracao") @PreAuthorize("@authz.pode('configuracao.gerenciar')")
    @Operation(summary = "Atualizar configuração do evento")
    public EventoConfiguracaoResponse updateConfiguracao(@Parameter(description = "ID assinado do evento") @PathVariable String id, @RequestBody EventoConfiguracaoUpdateRequest request) {
        return eventoConfiguracaoService.upsert(id, request);
    }
}
