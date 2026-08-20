package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.ModuloConfigItemResponse;
import br.com.achadosperdidos.controller.dto.ModulosConfigRequest;
import br.com.achadosperdidos.controller.dto.ModulosConfigResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ModulosConfigService {

    private final SistemaParametroService parametros;

    public ModulosConfigService(SistemaParametroService parametros) {
        this.parametros = parametros;
    }

    @Transactional(readOnly = true)
    public ModulosConfigResponse obter() {
        List<ModuloConfigItemResponse> itens = new ArrayList<>();
        for (ModuloCatalogo m : ModuloCatalogo.TODOS) {
            boolean habilitado = m.bloqueado() || parametros.isTrue(m.chaveParametro(), true);
            itens.add(new ModuloConfigItemResponse(m.path(), m.label(), habilitado, m.bloqueado()));
        }
        return new ModulosConfigResponse(itens);
    }

    @Transactional
    public ModulosConfigResponse salvar(ModulosConfigRequest request) {
        if (request.modulos() == null) {
            return obter();
        }
        for (ModulosConfigRequest.ModuloToggleRequest t : request.modulos()) {
            ModuloCatalogo cat = ModuloCatalogo.porPath(t.path());
            if (cat == null) {
                throw new IllegalArgumentException("Módulo desconhecido: " + t.path());
            }
            if (cat.bloqueado()) {
                // Sempre ligado — ignora tentativa de desabilitar.
                parametros.set(cat.chaveParametro(), "true", "Módulo " + cat.label() + " habilitado (sempre on)");
                continue;
            }
            boolean on = Boolean.TRUE.equals(t.habilitado());
            parametros.set(
                    cat.chaveParametro(),
                    on ? "true" : "false",
                    "Módulo " + cat.label() + (on ? " habilitado" : " desabilitado"));
        }
        return obter();
    }

    @Transactional(readOnly = true)
    public boolean estaHabilitado(String path) {
        ModuloCatalogo cat = ModuloCatalogo.porPath(path);
        if (cat == null) return true;
        if (cat.bloqueado()) return true;
        return parametros.isTrue(cat.chaveParametro(), true);
    }

    @Transactional(readOnly = true)
    public Map<String, Boolean> mapaHabilitados() {
        Map<String, Boolean> map = new LinkedHashMap<>();
        for (ModuloCatalogo m : ModuloCatalogo.TODOS) {
            map.put(m.path(), estaHabilitado(m.path()));
        }
        return map;
    }

    @Transactional(readOnly = true)
    public void exigirHabilitado(String path) {
        if (!estaHabilitado(path)) {
            throw new IllegalStateException(
                    "O módulo \"" + path + "\" está desabilitado nas configurações do sistema.");
        }
    }
}
