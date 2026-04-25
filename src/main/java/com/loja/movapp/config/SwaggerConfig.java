package com.loja.movapp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
                Info info = new Info();
                info.setTitle("MovApp API");
                info.setVersion("1.0.0");
                info.setDescription("API REST para gerenciamento de produtos Loja");

                return new OpenAPI().info(info);

    }
}
