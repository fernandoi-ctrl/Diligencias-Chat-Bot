package com.legal.bot.model.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Cuerpo de POST /api/v1/service-requests. Solo pide lo minimo para cotizar; la
 * descripcion no debe llevar datos sensibles del expediente.
 *
 * @param website campo trampa (honeypot): oculto en el formulario, solo lo llenan bots.
 */
public record SolicitudServicioRequest(
        @NotBlank(message = "Escriba su nombre") @Size(max = 160) String nombre,
        @Size(max = 160) String despacho,
        @Email(message = "Correo no válido") @Size(max = 254) String email,
        @Pattern(regexp = "^\\+?[0-9 ()-]{10,20}$", message = "Teléfono no válido") String telefono,
        @NotNull(message = "Elija un servicio") CodigoServicio servicio,
        @Size(max = 80) String materia,
        @Size(max = 160) String juzgado,
        @Size(max = 80) String municipio,
        @FutureOrPresent(message = "La fecha no puede ser pasada") LocalDate fechaTentativa,
        @Size(max = 1000) String descripcion,
        @NotNull @AssertTrue(message = "Debe aceptar el aviso de privacidad") Boolean aceptaPrivacidad,
        @Size(max = 100) String utmSource,
        @Size(max = 100) String utmCampaign,
        String website) {

    @AssertTrue(message = "Indique un correo o un teléfono")
    public boolean isContactoPresente() {
        return (email != null && !email.isBlank()) || (telefono != null && !telefono.isBlank());
    }
}
