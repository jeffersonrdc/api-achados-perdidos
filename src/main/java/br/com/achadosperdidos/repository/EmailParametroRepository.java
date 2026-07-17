package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.EmailParametro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailParametroRepository extends JpaRepository<EmailParametro, Long> {
    Optional<EmailParametro> findByTpEvento(String tpEvento);
    List<EmailParametro> findAllByOrderByTpEventoAsc();
}
