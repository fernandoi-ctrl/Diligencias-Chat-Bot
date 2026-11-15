package com.legal.bot.repository;

import com.legal.bot.model.entity.SolicitudServicio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SolicitudServicioRepository extends JpaRepository<SolicitudServicio, UUID> {
}
