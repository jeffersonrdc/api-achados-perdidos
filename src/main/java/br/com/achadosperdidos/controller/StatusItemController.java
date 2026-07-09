package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.StatusItemResponse;
import br.com.achadosperdidos.service.StatusItemService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/status-itens")
@Tag(name = "Status")
@SecurityRequirement(name = "bearerAuth")
public class StatusItemController {
    private final StatusItemService statusItemService;
    public StatusItemController(StatusItemService statusItemService) { this.statusItemService = statusItemService; }
    @GetMapping
    public List<StatusItemResponse> findAll() { return statusItemService.findAll(); }
}
