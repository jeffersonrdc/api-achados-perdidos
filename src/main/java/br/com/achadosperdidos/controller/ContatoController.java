package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.ContatoCreateRequest;
import br.com.achadosperdidos.controller.dto.ContatoResponse;
import br.com.achadosperdidos.service.ContatoService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/contatos")
@Tag(name = "Contatos")
@SecurityRequirement(name = "bearerAuth")
public class ContatoController {
    private final ContatoService contatoService;

    public ContatoController(ContatoService contatoService) {
        this.contatoService = contatoService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE')")
    public ResponseEntity<ContatoResponse> create(@Valid @RequestBody ContatoCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contatoService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE','CONSULTA')")
    public List<ContatoResponse> findAll() {
        return contatoService.findAll();
    }
}
