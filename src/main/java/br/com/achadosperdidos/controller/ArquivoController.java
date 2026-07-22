package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.ArquivoCreateRequest;
import br.com.achadosperdidos.controller.dto.ArquivoResponse;
import br.com.achadosperdidos.service.ArquivoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@Tag(name = "Arquivos", description = "Upload, listagem e download de anexos (Local ou S3 via API).")
@SecurityRequirement(name = "bearerAuth")
public class ArquivoController {
    private final ArquivoService arquivoService;

    public ArquivoController(ArquivoService arquivoService) {
        this.arquivoService = arquivoService;
    }

    @PostMapping
    @PreAuthorize("@authz.pode('arquivo.gerenciar')")
    @Operation(summary = "Registrar metadados de arquivo")
    public ResponseEntity<ArquivoResponse> create(@Valid @RequestBody ArquivoCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(arquivoService.create(request));
    }

    @GetMapping
    @PreAuthorize("@authz.pode('arquivo.listar')")
    @Operation(summary = "Listar arquivos da entidade")
    public List<ArquivoResponse> findByEntidade(
            @RequestParam String tpEntidade,
            @Parameter(description = "ID assinado da entidade") @RequestParam String idEntidade) {
        return arquivoService.findByEntidade(tpEntidade, idEntidade);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@authz.pode('arquivo.gerenciar')")
    @Operation(summary = "Enviar arquivo (multipart) — grava no provedor padrão (LOCAL/S3)")
    public ResponseEntity<ArquivoResponse> upload(
            @RequestParam String tpEntidade,
            @Parameter(description = "ID assinado da entidade") @RequestParam String idEntidade,
            @RequestParam(required = false, defaultValue = "FOTO") String tpArquivo,
            @RequestParam(required = false) Boolean fgPrincipal,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(arquivoService.upload(tpEntidade, idEntidade, tpArquivo, file, fgPrincipal));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("@authz.pode('arquivo.listar')")
    @Operation(summary = "Baixar arquivo por ID assinado (streaming Local/S3)")
    public ResponseEntity<Resource> download(@Parameter(description = "ID assinado do arquivo") @PathVariable String id) {
        var conteudo = arquivoService.carregarConteudo(id);
        MediaType mime = conteudo.tpMime() != null
                ? MediaType.parseMediaType(conteudo.tpMime())
                : MediaType.APPLICATION_OCTET_STREAM;
        String nomeSeguro = sanitizarNomeArquivo(conteudo.nmArquivo());
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .contentType(mime)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomeSeguro + "\"")
                .header("X-Content-Type-Options", "nosniff");
        if (conteudo.qtBytes() != null && conteudo.qtBytes() >= 0) {
            builder.contentLength(conteudo.qtBytes());
        }
        return builder.body(conteudo.resource());
    }

    @GetMapping("/{id}/thumbnail")
    @PreAuthorize("@authz.pode('arquivo.listar')")
    @Operation(summary = "Miniatura JPEG do arquivo (listagens)",
            description = "Redimensiona on-the-fly (padrão max 400px no maior lado). Query `max` opcional (64–800).")
    public ResponseEntity<Resource> thumbnail(
            @Parameter(description = "ID assinado do arquivo") @PathVariable String id,
            @Parameter(description = "Maior lado em pixels (padrão 400, máx. 800)")
            @RequestParam(required = false) Integer max) {
        var conteudo = arquivoService.carregarThumbnail(id, max);
        MediaType mime = conteudo.tpMime() != null
                ? MediaType.parseMediaType(conteudo.tpMime())
                : MediaType.IMAGE_JPEG;
        String nomeSeguro = sanitizarNomeArquivo(conteudo.nmArquivo());
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .contentType(mime)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + nomeSeguro + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=86400");
        if (conteudo.qtBytes() != null && conteudo.qtBytes() >= 0) {
            builder.contentLength(conteudo.qtBytes());
        }
        return builder.body(conteudo.resource());
    }

    private static String sanitizarNomeArquivo(String nome) {
        if (nome == null || nome.isBlank()) {
            return "arquivo";
        }
        String limpo = nome.replaceAll("[\\r\\n\"\\\\/]", "_").trim();
        return limpo.isBlank() ? "arquivo" : limpo;
    }
}
