package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.Lacre;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LacreRepository extends JpaRepository<Lacre, Long> {
    Optional<Lacre> findByNrLacreAndFgExcluidoFalse(String nrLacre);
}
