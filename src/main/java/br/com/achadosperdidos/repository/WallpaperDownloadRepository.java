package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.WallpaperDownload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface WallpaperDownloadRepository extends JpaRepository<WallpaperDownload, Long> {

    /** Total de downloads do evento; com {@code dia} preenchido, apenas os daquela data. */
    @Query("""
            SELECT COUNT(w) FROM WallpaperDownload w
             WHERE w.evento.id = :eventoId AND w.fgExcluido = false
               AND (:dia IS NULL OR CAST(w.dtDownload AS date) = :dia)""")
    long contarPorEvento(@Param("eventoId") Long eventoId, @Param("dia") LocalDate dia);
}
