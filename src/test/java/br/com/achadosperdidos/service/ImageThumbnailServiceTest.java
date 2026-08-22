package br.com.achadosperdidos.service;

import br.com.achadosperdidos.service.ImageThumbnailService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ImageThumbnailServiceTest {

    @Test
    void nuloViraPadrao() {
        assertEquals(400, ImageThumbnailService.normalizarMaxEdge(null));
    }

    @Test
    void sobeParaODegrauSeguinte() {
        assertEquals(160, ImageThumbnailService.normalizarMaxEdge(100));
        assertEquals(320, ImageThumbnailService.normalizarMaxEdge(320));
        assertEquals(400, ImageThumbnailService.normalizarMaxEdge(390));
        assertEquals(480, ImageThumbnailService.normalizarMaxEdge(401));
        assertEquals(800, ImageThumbnailService.normalizarMaxEdge(641));
    }

    @Test
    void limitaAFaixaDocumentadaSemErro() {
        assertEquals(64, ImageThumbnailService.normalizarMaxEdge(1));
        assertEquals(64, ImageThumbnailService.normalizarMaxEdge(-10));
        assertEquals(800, ImageThumbnailService.normalizarMaxEdge(9999));
        assertEquals(800, ImageThumbnailService.normalizarMaxEdge(1400));
    }

    @Test
    void chaveDeMiniaturaPorTamanho() {
        // tamanho padrão mantém a chave legada (aproveita o acervo já gerado)
        assertEquals("ITEM/1/abc.thumb.jpg", ArquivoService.thumbKey("ITEM/1/abc.jpg", 400));
        assertEquals("ITEM/1/abc.thumb.jpg", ArquivoService.thumbKey("ITEM/1/abc.jpg"));
        assertEquals("ITEM/1/abc.thumb-800.jpg", ArquivoService.thumbKey("ITEM/1/abc.jpg", 800));
        assertEquals("ITEM/1/abc.thumb-320.jpg", ArquivoService.thumbKey("ITEM/1/abc.jpg", 320));
        // valor fora da escada é normalizado antes de virar chave
        assertEquals("ITEM/1/abc.thumb-800.jpg", ArquivoService.thumbKey("ITEM/1/abc.jpg", 9999));
    }
}
