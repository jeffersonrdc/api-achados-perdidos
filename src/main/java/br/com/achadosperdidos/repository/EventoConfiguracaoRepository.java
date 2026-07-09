package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.EventoConfiguracao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EventoConfiguracaoRepository extends JpaRepository<EventoConfiguracao, Long> {
    Optional<EventoConfiguracao> findByEvento_IdAndFgExcluidoFalse(Long eventoId);
}
