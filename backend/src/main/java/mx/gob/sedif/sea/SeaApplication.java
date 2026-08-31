package mx.gob.sedif.sea;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Punto de arranque del Sistema de Empadronamiento Animal (SEA).
 *
 * <p>Padron Estatal de Perros y Gatos del Instituto de Bienestar Animal (IBA),
 * operado por el Sistema Estatal DIF Puebla.
 */
@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorAware",
        dateTimeProviderRef = "auditoriaDateTimeProvider")
@EnableCaching
@EnableAsync
public class SeaApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeaApplication.class, args);
    }
}
