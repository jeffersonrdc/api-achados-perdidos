package br.com.achadosperdidos.service;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * Gera miniaturas JPEG leves a partir de imagens (portal e painel).
 * Preferência: arquivo persistido {@code *.thumb.jpg} no upload;
 * este serviço cobre geração e backfill sob demanda.
 * Usa apenas ImageIO da JDK (JPEG/PNG/GIF). Outros formatos caem no original.
 */
@Service
public class ImageThumbnailService {

    public static final int DEFAULT_MAX_EDGE = 400;
    public static final int MIN_MAX_EDGE = 64;
    public static final int MAX_MAX_EDGE = 800;

    /**
     * Tamanhos aceitos para o parâmetro {@code max}. É uma escada fechada de propósito:
     * a CDN mantém o {@code max} na chave de cache, então valores arbitrários
     * fragmentariam o cache da borda (e multiplicariam variantes no storage) sem ganho.
     */
    public static final int[] TAMANHOS = {64, 160, 320, DEFAULT_MAX_EDGE, 480, 640, MAX_MAX_EDGE};

    /**
     * Normaliza o {@code max} recebido: nulo vira o padrão, valores fora da faixa são
     * limitados a [{@value #MIN_MAX_EDGE}, {@value #MAX_MAX_EDGE}] e o resultado sobe para
     * o degrau seguinte de {@link #TAMANHOS}.
     *
     * <p>Ajustamos em vez de devolver 400 Bad Request para não quebrar cliente já
     * publicado (o portal em produção chega a pedir {@code max=1400}); o efeito visível é
     * receber o maior tamanho disponível, e não uma imagem quebrada.</p>
     */
    public static int normalizarMaxEdge(Integer maxEdge) {
        if (maxEdge == null) {
            return DEFAULT_MAX_EDGE;
        }
        int alvo = Math.clamp(maxEdge, MIN_MAX_EDGE, MAX_MAX_EDGE);
        for (int tamanho : TAMANHOS) {
            if (alvo <= tamanho) {
                return tamanho;
            }
        }
        return MAX_MAX_EDGE;
    }

    public ArquivoService.ArquivoConteudo gerar(ArquivoService.ArquivoConteudo original, int maxEdge) {
        int edge = Math.clamp(maxEdge, MIN_MAX_EDGE, MAX_MAX_EDGE);
        Resource resource = original.resource();
        if (resource == null || !resource.exists()) {
            return original;
        }
        try (InputStream in = resource.getInputStream()) {
            BufferedImage src = ImageIO.read(in);
            if (src == null) {
                return original;
            }
            int w = src.getWidth();
            int h = src.getHeight();
            if (w <= 0 || h <= 0) {
                return original;
            }
            if (w <= edge && h <= edge) {
                // Já é pequena — reencode JPEG leve para reduzir bytes (PNG grande etc.).
                byte[] bytes = encodeJpeg(toRgb(src), 0.82f);
                return toConteudo(bytes, original.nmArquivo());
            }
            double scale = Math.min((double) edge / w, (double) edge / h);
            int nw = Math.max(1, (int) Math.round(w * scale));
            int nh = Math.max(1, (int) Math.round(h * scale));
            BufferedImage dest = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = dest.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.drawImage(toRgb(src), 0, 0, nw, nh, null);
            } finally {
                g.dispose();
            }
            byte[] bytes = encodeJpeg(dest, 0.78f);
            return toConteudo(bytes, original.nmArquivo());
        } catch (IOException e) {
            return original;
        }
    }

    private static BufferedImage toRgb(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_RGB) {
            return src;
        }
        BufferedImage rgb = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        try {
            g.drawImage(src, 0, 0, java.awt.Color.WHITE, null);
        } finally {
            g.dispose();
        }
        return rgb;
    }

    private static byte[] encodeJpeg(BufferedImage image, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", out);
            return out.toByteArray();
        }
        ImageWriter writer = writers.next();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(quality);
            }
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }

    private static ArquivoService.ArquivoConteudo toConteudo(byte[] bytes, String nmArquivo) {
        String base = nmArquivo == null || nmArquivo.isBlank() ? "thumb" : nmArquivo;
        int ponto = base.lastIndexOf('.');
        String nome = (ponto > 0 ? base.substring(0, ponto) : base) + "-thumb.jpg";
        return new ArquivoService.ArquivoConteudo(
                new ByteArrayResource(bytes),
                nome,
                "image/jpeg",
                (long) bytes.length);
    }
}
