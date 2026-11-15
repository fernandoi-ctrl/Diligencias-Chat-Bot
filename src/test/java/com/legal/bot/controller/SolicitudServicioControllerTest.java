package com.legal.bot.controller;

import com.legal.bot.config.SecurityConfig;
import com.legal.bot.config.WebConfig;
import com.legal.bot.model.dto.SolicitudServicioRequest;
import com.legal.bot.service.LimiteDeSolicitudes;
import com.legal.bot.service.SolicitudServicioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = SolicitudServicioController.class,
		properties = "cors.allowed-origins=https://landing.example")
@Import({ SecurityConfig.class, WebConfig.class })
class SolicitudServicioControllerTest {

	private static final String VALIDA = """
			{"nombre":"Despacho de Prueba","email":"contacto@example.com","servicio":"EXHORTOS",
			 "municipio":"Aguascalientes","aceptaPrivacidad":true}""";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SolicitudServicioService service;

	@MockitoBean
	private LimiteDeSolicitudes limite;

	@BeforeEach
	void permitirPorDefecto() {
		when(limite.permitir(anyString())).thenReturn(true);
	}

	private org.springframework.test.web.servlet.ResultActions enviar(String json) throws Exception {
		return this.mockMvc.perform(post("/api/v1/service-requests")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json));
	}

	@Test
	void registraUnaSolicitudValidaSinPedirAutenticacion() throws Exception {
		enviar(VALIDA)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.estado").value("RECIBIDA"));
		verify(service).registrar(any(SolicitudServicioRequest.class));
	}

	@Test
	void exigeCorreoOTelefono() throws Exception {
		enviar("""
				{"nombre":"Sin contacto","servicio":"EXHORTOS","aceptaPrivacidad":true}""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos.contactoPresente").value("Indique un correo o un teléfono"));
		verify(service, never()).registrar(any());
	}

	@Test
	void exigeAceptarElAvisoDePrivacidad() throws Exception {
		enviar("""
				{"nombre":"X","email":"x@example.com","servicio":"EXHORTOS","aceptaPrivacidad":false}""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos.aceptaPrivacidad").exists());
	}

	@Test
	void rechazaUnServicioQueNoEstaEnElCatalogo() throws Exception {
		enviar("""
				{"nombre":"X","email":"x@example.com","servicio":"DIVORCIO","aceptaPrivacidad":true}""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Solicitud con formato inválido."));
	}

	@Test
	void elCampoTrampaSimulaExitoPeroNoGuarda() throws Exception {
		enviar(VALIDA.replace("\"aceptaPrivacidad\":true", "\"aceptaPrivacidad\":true,\"website\":\"http://spam\""))
				.andExpect(status().isCreated());
		verify(service, never()).registrar(any());
	}

	@Test
	void respondeTooManyRequestsAlAgotarElLimite() throws Exception {
		when(limite.permitir(anyString())).thenReturn(false);
		enviar(VALIDA).andExpect(status().isTooManyRequests());
		verify(service, never()).registrar(any());
	}

	@Test
	void corsSoloPermiteLosOrigenesConfigurados() throws Exception {
		this.mockMvc.perform(options("/api/v1/service-requests")
						.header("Origin", "https://landing.example")
						.header("Access-Control-Request-Method", "POST"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", "https://landing.example"));

		this.mockMvc.perform(options("/api/v1/service-requests")
						.header("Origin", "https://otro-sitio.example")
						.header("Access-Control-Request-Method", "POST"))
				.andExpect(status().isForbidden());
	}
}
