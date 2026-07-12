package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.LocalizacaoCreateRequest;
import br.com.achadosperdidos.controller.dto.LocalizacaoResponse;
import br.com.achadosperdidos.service.LocalizacaoService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/localizacoes")
@Tag(name = "Localizações")
@SecurityRequirement(name = "bearerAuth")
public class LocalizacaoController {
    private final LocalizacaoService localizacaoService;

    public LocalizacaoController(LocalizacaoService localizacaoService) {
        this.localizacaoService = localizacaoService;
    }

    @PostMapping
    @PreAuthorize("@authz.pode('localizacao.gerenciar')")
    public ResponseEntity<LocalizacaoResponse> create(@Valid @RequestBody LocalizacaoCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(localizacaoService.create(request));
    }

    @GetMapping
    @PreAuthorize("@authz.pode('localizacao.listar')")
    public List<LocalizacaoResponse> findByDeposito(@RequestParam String idDeposito) {
        return localizacaoService.findByDeposito(idDeposito);
    }
}
