package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.Marca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarcaRepository extends JpaRepository<Marca, Long> {
    List<Marca> findByFgExcluidoFalseAndFgAtivoTrueOrderByOrOrdemAscNmMarcaAsc();
}
