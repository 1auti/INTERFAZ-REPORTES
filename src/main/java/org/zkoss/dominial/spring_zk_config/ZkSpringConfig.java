package org.zkoss.dominial.spring_zk_config;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zkoss.zk.ui.http.HttpSessionListener;

/*@Configuration*/
public class ZkSpringConfig {

    private final ApplicationContext applicationContext;

    public ZkSpringConfig(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Bean
    public HttpSessionListener httpSessionListener() {
        return new HttpSessionListener();
    }

    // Este es un bean clave para asegurar la correcta integración
    @Bean
    public org.zkoss.zkplus.spring.DelegatingVariableResolver delegatingVariableResolver() {
        return new org.zkoss.zkplus.spring.DelegatingVariableResolver();
    }
}