package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.ArquivoCreateRequest;
import br.com.achadosperdidos.controller.dto.ArquivoResponse;
import br.com.achadosperdidos.service.ArquivoService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/arquivos")
@Tag(name = "Arquivos")
@SecurityRequirement(name = "bearerAuth")
public class ArquivoController {
    private final ArquivoService arquivoService;

    public ArquivoController(ArquivoService arquivoService) {
        this.arquivoService = arquivoService;
    }

    @PostMapping
    @PreAuthorize("@authz.pode('arquivo.gerenciar')")
    public ResponseEntity<ArquivoResponse> create(@Valid @RequestBody ArquivoCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(arquivoService.create(request));
    }

    @GetMapping
    @PreAuthorize("@authz.pode('arquivo.listar')")
    public List<ArquivoResponse> findByEntidade(
            @RequestParam String tpEntidade,
            @RequestParam String idEntidade) {
        return arquivoService.findByEntidade(tpEntidade, idEntidade);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@authz.pode('arquivo.gerenciar')")
    public ResponseEntity<ArquivoResponse> upload(
            @RequestParam String tpEntidade,
            @RequestParam String idEntidade,
            @RequestParam(required = false, defaultValue = "FOTO") String tpArquivo,
            @RequestParam(required = false) Boolean fgPrincipal,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(arquivoService.upload(tpEntidade, idEntidade, tpArquivo, file, fgPrincipal));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("@authz.pode('arquivo.listar')")
    public ResponseEntity<Resource> download(@PathVariable String id) {
        var conteudo = arquivoService.carregarConteudo(id);
        MediaType mime = conteudo.tpMime() != null
                ? MediaType.parseMediaType(conteudo.tpMime())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(mime)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + conteudo.nmArquivo() + "\"")
                .body(new FileSystemResource(conteudo.caminho()));
    }
}
