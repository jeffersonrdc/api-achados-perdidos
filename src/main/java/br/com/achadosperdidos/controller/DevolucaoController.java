package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.DevolucaoCreateRequest;
import br.com.achadosperdidos.controller.dto.DevolucaoResponse;
import br.com.achadosperdidos.service.DevolucaoService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/devolucoes")
@Tag(name = "Devoluções")
@SecurityRequirement(name = "bearerAuth")
public class DevolucaoController {
    private final DevolucaoService devolucaoService;

    public DevolucaoController(DevolucaoService devolucaoService) {
        this.devolucaoService = devolucaoService;
    }

    @PostMapping
    @PreAuthorize("@authz.pode('devolucao.realizar')")
    public ResponseEntity<DevolucaoResponse> create(@Valid @RequestBody DevolucaoCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(devolucaoService.create(request));
    }

    @GetMapping
    @PreAuthorize("@authz.pode('devolucao.listar')")
    public List<DevolucaoResponse> findAll() {
        return devolucaoService.findAll();
    }
}
