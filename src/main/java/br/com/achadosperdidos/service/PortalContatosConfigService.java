package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.PortalContatosConfigRequest;
import br.com.achadosperdidos.controller.dto.PortalContatosConfigResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortalContatosConfigService {

    private final SistemaParametroService parametros;

    public PortalContatosConfigService(SistemaParametroService parametros) {
        this.parametros = parametros;
    }

    @Transactional(readOnly = true)
    public PortalContatosConfigResponse obter() {
        return new PortalContatosConfigResponse(
                parametros.get(SistemaParametroService.PORTAL_TELEFONE_CENTRAL, ""),
                parametros.get(SistemaParametroService.PORTAL_WHATSAPP, ""),
                parametros.get(SistemaParametroService.PORTAL_EMAIL_SUPORTE, ""));
    }

    @Transactional
    public PortalContatosConfigResponse salvar(PortalContatosConfigRequest request) {
        if (request.telefoneCentral() != null) {
            parametros.set(SistemaParametroService.PORTAL_TELEFONE_CENTRAL,
                    request.telefoneCentral().trim(),
                    "Telefone da central de atendimento exibido no portal /contato");
        }
        if (request.whatsapp() != null) {
            parametros.set(SistemaParametroService.PORTAL_WHATSAPP,
                    request.whatsapp().trim(),
                    "WhatsApp oficial exibido no portal /contato");
        }
        if (request.emailSuporte() != null) {
            parametros.set(SistemaParametroService.PORTAL_EMAIL_SUPORTE,
                    request.emailSuporte().trim().toLowerCase(),
                    "E-mail de suporte exibido no portal /contato");
        }
        return obter();
    }
}
