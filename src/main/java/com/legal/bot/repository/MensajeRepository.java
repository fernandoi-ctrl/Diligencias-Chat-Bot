package com.legal.bot.repository;

import com.legal.bot.model.entity.Mensaje;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

    boolean existsByWaMessageId(String waMessageId);

    /** Últimos N mensajes de la conversación, ordenados del más viejo al más reciente. */
    @Query("""
        SELECT m FROM Mensaje m
        WHERE m.conversacion.id = :conversacionId
        ORDER BY m.fechaCreacion DESC
    """)
    List<Mensaje> findUltimosPorConversacion(@Param("conversacionId") Long conversacionId,
                                             Pageable pageable);
    List<Mensaje> findByConversacionIdOrderByFechaCreacionAsc(Long id);
}