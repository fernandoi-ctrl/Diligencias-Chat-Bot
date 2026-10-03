package com.legal.bot.repository;

import com.legal.bot.model.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByTelefono(String telefono);
    boolean existsByTelefono(String telefono);

    /**
     * Sella la ultima interaccion por id, con un UPDATE directo.
     *
     * <p>A proposito NO se hace {@code cliente.setFechaUltimaInteraccion(...) + save()}:
     * el Cliente que cuelga de una Conversacion es un proxy LAZY creado en la transaccion
     * de {@code obtenerOCrearConversacion}, que ya esta cerrada. Tocar cualquier campo de
     * ese proxy lanza LazyInitializationException ("no session"). Aqui solo se usa el id,
     * que el proxy sí puede dar sin inicializarse.</p>
     */
    @Modifying
    @Query("UPDATE Cliente c SET c.fechaUltimaInteraccion = :ahora WHERE c.id = :id")
    void actualizarUltimaInteraccion(@Param("id") Long id, @Param("ahora") LocalDateTime ahora);
}