package br.com.achadosperdidos.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Renderiza templates de e-mail (arquivos HTML em resources/templates/email)
 * substituindo placeholders {{chave}} pelos valores informados.
 */
@Service
public class EmailTemplateService {

    /** Lê o arquivo do classpath e substitui os placeholders. Lança IOException se não existir. */
    public String render(String nomeArquivo, Map<String, String> variaveis) throws IOException {
        String html = carregar(nomeArquivo);
        for (Map.Entry<String, String> e : variaveis.entrySet()) {
            String valor = e.getValue() == null ? "" : HtmlUtils.htmlEscape(e.getValue(), StandardCharsets.UTF_8.name());
            valor = valor.replace("\r\n", "<br>").replace("\n", "<br>");
            html = html.replace("{{" + e.getKey() + "}}", valor);
        }
        return html;
    }

    private String carregar(String nomeArquivo) throws IOException {
        ClassPathResource resource = new ClassPathResource("templates/email/" + nomeArquivo);
        try (InputStream in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
