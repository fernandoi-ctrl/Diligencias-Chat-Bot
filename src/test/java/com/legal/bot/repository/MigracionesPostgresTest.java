package com.legal.bot.repository;

import com.legal.bot.model.entity.Lead;
import com.legal.bot.model.entity.SolicitudServicio;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Corre las migraciones reales en un PostgreSQL embebido y arranca JPA con
 * ddl-auto=validate, igual que en produccion: si una entidad no cuadra con el SQL,
 * este test falla en vez de tumbar el bot en Render.
 */
@DataJpaTest(properties = {
		"spring.flyway.enabled=true",
		"spring.flyway.baseline-on-migrate=true",
		"spring.flyway.baseline-version=2",
		"spring.jpa.hibernate.ddl-auto=validate" })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MigracionesPostgresTest {

	private static final EmbeddedPostgres POSTGRES = iniciar();

	private static EmbeddedPostgres iniciar() {
		try {
			return EmbeddedPostgres.start();
		}
		catch (IOException ex) {
			throw new IllegalStateException("No se pudo iniciar PostgreSQL embebido", ex);
		}
	}

	@AfterAll
	static void detener() throws IOException {
		POSTGRES.close();
	}

	@TestConfiguration
	static class BaseDeDatos {
		@Bean
		DataSource dataSource() {
			return POSTGRES.getPostgresDatabase();
		}
	}

	@Autowired
	private LeadRepository leadRepository;

	@Autowired
	private SolicitudServicioRepository solicitudRepository;

	@Test
	void lasEntidadesCuadranConLasMigracionesYSeGuardaUnaSolicitud() {
		Lead lead = new Lead();
		lead.setFullName("Despacho de Prueba");
		lead.setEmail("contacto@example.com");
		lead.setSource("web");
		lead.setConsentAt(Instant.now());
		leadRepository.saveAndFlush(lead);

		SolicitudServicio solicitud = new SolicitudServicio();
		solicitud.setLead(lead);
		solicitud.setServiceCode("EXHORTOS");
		solicitud.setRequestedDate(LocalDate.of(2026, 12, 1));
		solicitudRepository.saveAndFlush(solicitud);

		SolicitudServicio leida = solicitudRepository.findById(solicitud.getId()).orElseThrow();
		assertThat(leida.getStatus()).isEqualTo("RECIBIDA");
		assertThat(leida.getLead().getStatus()).isEqualTo("NUEVO");
		assertThat(leida.getCreatedAt()).isNotNull();
	}

	/**
	 * Simula Supabase hoy: tablas creadas a mano con V1, V2 y citas, sin historial de
	 * Flyway. Con la configuracion de application.yaml solo deben correr V3 y V4.
	 */
	@Test
	void enUnaBaseExistenteSinHistorialSoloAplicaV3yV4() throws Exception {
		DataSource ds = POSTGRES.getDatabase("postgres", "postgres");
		try (Connection c = ds.getConnection()) {
			c.createStatement().execute("CREATE DATABASE simulacion_supabase");
		}
		DataSource supabase = POSTGRES.getDatabase("postgres", "simulacion_supabase");
		try (Connection c = supabase.getConnection()) {
			c.createStatement().execute(sql("db/migration/V1__esquema_inicial.sql"));
			c.createStatement().execute(sql("db/migration/V2__esquema_actual.sql"));
			c.createStatement().execute(sql("db/migration/V3__citas.sql"));
			c.createStatement().execute("INSERT INTO clientes (telefono, nombre) VALUES ('5210000000000', 'Existente')");
		}

		Flyway flyway = Flyway.configure()
				.dataSource(supabase)
				.baselineOnMigrate(true)
				.baselineVersion("2")
				.load();
		flyway.migrate();

		assertThat(Arrays.stream(flyway.info().applied()).map(MigrationInfo::getVersion).map(Object::toString))
				.containsExactly("2", "3", "4");
		try (Connection c = supabase.getConnection();
				var rs = c.createStatement().executeQuery("SELECT count(*) FROM clientes")) {
			rs.next();
			assertThat(rs.getInt(1)).as("los datos existentes se conservan").isEqualTo(1);
		}
	}

	private static String sql(String ruta) throws IOException {
		return StreamUtils.copyToString(new ClassPathResource(ruta).getInputStream(), StandardCharsets.UTF_8);
	}
}
