package gov.prajadisha.backend.config;

import gov.prajadisha.backend.storage.StorageService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Opens CORS for the frontend clients (Citizen App + Org Admin Dashboard) and serves locally
 * stored uploads (photos, videos, voice) from the {@code /files/**} URL space.
 * Tighten allowedOrigins for production deployments.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final StorageService storage;

    public WebConfig(StorageService storage) {
        this.storage = storage;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
        // Allow the frontends to <img>/<video> directly from the file server across origins.
        registry.addMapping("/files/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "OPTIONS");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve uploaded files straight off the local disk directory.
        String location = storage.getRoot().toUri().toString(); // file:/.../uploads/
        registry.addResourceHandler(StorageService.PUBLIC_PREFIX + "/**")
                .addResourceLocations(location);
    }
}
