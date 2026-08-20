package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.FluxoConfigRequest;
import br.com.achadosperdidos.controller.dto.FluxoConfigResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FluxoConfigService {

    private final SistemaParametroService parametros;

    public FluxoConfigService(SistemaParametroService parametros) {
        this.parametros = parametros;
    }

    @Transactional(readOnly = true)
    public FluxoConfigResponse obter() {
        return new FluxoConfigResponse(triagemObrigatoria());
    }

    @Transactional
    public FluxoConfigResponse salvar(FluxoConfigRequest request) {
        boolean on = Boolean.TRUE.equals(request.triagemObrigatoria());
        parametros.set(
                SistemaParametroService.FLUXO_TRIAGEM_OBRIGATORIA,
                on ? "true" : "false",
                "Se true, novos itens entram na fila de triagem. Se false, vão direto ao estoque e ao portal.");
        return obter();
    }

    @Transactional(readOnly = true)
    public boolean triagemObrigatoria() {
        return parametros.isTrue(SistemaParametroService.FLUXO_TRIAGEM_OBRIGATORIA, true);
    }
}
