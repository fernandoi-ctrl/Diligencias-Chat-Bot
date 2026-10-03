package com.legal.bot.repository;

import com.legal.bot.model.entity.Conversacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConversacionRepository extends JpaRepository<Conversacion, Long> {

    Optional<Conversacion> findFirstByClienteIdAndActivaTrueOrderByFechaInicioDesc(Long clienteId);
}