package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.Evento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EventoRepository extends JpaRepository<Evento, Long> {
    List<Evento> findByFgExcluidoFalseOrderByDtInicioDesc();
    List<Evento> findByFgExcluidoFalseAndFgAtivoTrueOrderByDtInicioDesc();
}
