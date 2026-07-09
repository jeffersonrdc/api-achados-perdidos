package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.CategoriaCampo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CategoriaCampoRepository extends JpaRepository<CategoriaCampo, Long> {
    List<CategoriaCampo> findByCategoria_IdAndFgExcluidoFalseOrderByOrExibicaoAsc(Long categoriaId);
}
