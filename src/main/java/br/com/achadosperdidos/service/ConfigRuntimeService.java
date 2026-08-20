package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.ConfigRuntimeResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ConfigRuntimeService {

    private final ModulosConfigService modulos;
    private final FluxoConfigService fluxo;

    public ConfigRuntimeService(ModulosConfigService modulos, FluxoConfigService fluxo) {
        this.modulos = modulos;
        this.fluxo = fluxo;
    }

    @Transactional(readOnly = true)
    public ConfigRuntimeResponse obter() {
        Map<String, Boolean> mapa = modulos.mapaHabilitados();
        List<String> paths = new ArrayList<>();
        for (Map.Entry<String, Boolean> e : mapa.entrySet()) {
            if (Boolean.TRUE.equals(e.getValue())) paths.add(e.getKey());
        }
        return new ConfigRuntimeResponse(mapa, fluxo.triagemObrigatoria(), paths);
    }
}
