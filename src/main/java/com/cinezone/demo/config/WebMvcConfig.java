package com.cinezone.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.lang.NonNull;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // Sirve los archivos de la carpeta /uploads en la raíz del proyecto
        // Accesible vía http://localhost:8080/api/v1/uploads/nombre-archivo.jpg
        // Construcción a prueba de fallos para Windows
        String absolutePath = "file:///" + System.getProperty("user.dir").replace("\\", "/") + "/uploads/";
        registry.addResourceHandler("/api/v1/uploads/**")
                .addResourceLocations(absolutePath);
    }
}
