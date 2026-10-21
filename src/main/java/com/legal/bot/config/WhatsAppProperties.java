package com.legal.bot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "whatsapp")
public class WhatsAppProperties {
    private String accessToken;
    private String phoneNumberId;
    private String verifyToken;
    private String apiUrl;

    private List<String> numerosPermitidos = new ArrayList<>();

    // ---------- Métodos de conveniencia que usa WhatsAppConfig ----------

    /** true si hay token y phone-number-id cargados. */
    public boolean estaConfigurado() {
        return accessToken != null && !accessToken.isBlank()
                && phoneNumberId != null && !phoneNumberId.isBlank();
    }

    /** URL completa para enviar mensajes (POST). */
    public String messagesUrl() {
        return String.format("%s/%s/messages", apiUrl, phoneNumberId);
    }

    /** Alias del getter, por si WhatsAppConfig llama accessToken(). */
    public String accessToken() {
        return accessToken;
    }

    // ---------- Whitelist de prueba ----------

    public boolean numeroEstaPermitido(String telefono) {
        if (numerosPermitidos == null || numerosPermitidos.isEmpty()) {
            return true;
        }
        String limpio = soloDigitos(telefono);
        if (limpio.isEmpty()) {
            return false;
        }
        return numerosPermitidos.stream()
                .map(WhatsAppProperties::soloDigitos)
                .anyMatch(permitido -> mismoNumero(permitido, limpio));
    }

    /**
     * Compara por los ultimos 10 digitos (el numero nacional) en lugar de exigir una
     * igualdad literal.
     *
     * <p>Es necesario por Mexico: el mismo telefono llega de Meta unas veces como
     * {@code 521XXXXXXXXXX} (con el 1 de movil) y otras como {@code 52XXXXXXXXXX} sin el.
     * Con la comparacion exacta anterior, una de las dos formas se caia de la whitelist y
     * el mensaje se descartaba con un log de "no esta en la whitelist".</p>
     */
    private static boolean mismoNumero(String permitido, String entrante) {
        if (permitido.equals(entrante)) {
            return true;
        }
        if (permitido.length() < 10 || entrante.length() < 10) {
            return false;
        }
        return permitido.substring(permitido.length() - 10)
                .equals(entrante.substring(entrante.length() - 10));
    }

    private static String soloDigitos(String valor) {
        return valor == null ? "" : valor.replaceAll("[^0-9]", "");
    }

    // ---------- Getters y setters ----------

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getPhoneNumberId() { return phoneNumberId; }
    public void setPhoneNumberId(String phoneNumberId) { this.phoneNumberId = phoneNumberId; }

    public String getVerifyToken() { return verifyToken; }
    public void setVerifyToken(String verifyToken) { this.verifyToken = verifyToken; }

    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }

    public List<String> getNumerosPermitidos() { return numerosPermitidos; }
    public void setNumerosPermitidos(List<String> numerosPermitidos) { this.numerosPermitidos = numerosPermitidos; }
}