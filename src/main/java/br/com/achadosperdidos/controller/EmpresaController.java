package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.EmpresaResponse;
import br.com.achadosperdidos.repository.EmpresaRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/empresas")
@Tag(name = "Empresas", description = "Empresas organizadoras disponíveis para associação a eventos.")
@SecurityRequirement(name = "bearerAuth")
public class EmpresaController {
    private final EmpresaRepository empresaRepository;
    private final SignedResourceIdCodec idCodec;

    public EmpresaController(EmpresaRepository empresaRepository, SignedResourceIdCodec idCodec) {
        this.empresaRepository = empresaRepository;
        this.idCodec = idCodec;
    }

    @GetMapping
    @PreAuthorize("@authz.pode('evento.listar')")
    @Transactional(readOnly = true)
    @Operation(summary = "Listar empresas organizadoras",
            description = "Retorna empresas não excluídas, com IDs assinados.")
    public List<EmpresaResponse> listar() {
        return empresaRepository.findAll().stream()
                .filter(e -> !Boolean.TRUE.equals(e.getFgExcluido()))
                .map(e -> new EmpresaResponse(idCodec.encodeEmpresaId(e.getId()), e.getNmRazaoSocial(), e.getNmFantasia()))
                .toList();
    }
}
