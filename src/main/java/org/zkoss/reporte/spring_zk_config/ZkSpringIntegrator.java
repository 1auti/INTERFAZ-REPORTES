package org.zkoss.reporte.spring_zk_config;

import org.springframework.boot.web.servlet.ServletRegistrationBean;



import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zkoss.zk.au.http.DHtmlUpdateServlet;
import org.zkoss.zk.ui.WebApp;
import org.zkoss.zk.ui.http.DHtmlLayoutServlet;
import org.zkoss.zk.ui.http.HttpSessionListener;
import org.zkoss.zk.ui.util.WebAppInit;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ZkSpringIntegrator {
    private final ApplicationContext applicationContext;

    public ZkSpringIntegrator(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Bean
    public WebAppInit zkSpringWebAppInitializer() {
        return webApp -> {
            // Registrar el contexto de Spring con los nombres que ZK espera
            webApp.setAttribute("springApplicationContext", applicationContext);
            webApp.setAttribute("SpringUtil.applicationContext", applicationContext);
            webApp.setAttribute("WebApplicationContext.ROOT", applicationContext);
            webApp.setAttribute("org.springframework.web.context.WebApplicationContext.ROOT", applicationContext);
            webApp.setAttribute("org.zkoss.zkplus.spring.DelegatingVariableResolver.applicationContext", applicationContext);

            setupZkAutowiring(webApp);
        };
    }

    private void setupZkAutowiring(WebApp webApp) {
        org.zkoss.zk.ui.util.Configuration zkConfig = webApp.getConfiguration();
        zkConfig.setPreference("org.zkoss.zk.ui.composer.autowire.enabled", "true");
        zkConfig.setPreference("org.zkoss.zk.ui.composer.autowire.type", "byType");
        zkConfig.setPreference("org.zkoss.bind.composer.autowire.enabled", "true");
        zkConfig.setPreference("org.zkoss.bind.composer.autowire.type", "byType");
    }


    // Registra el Servlet principal de ZK
    @Bean
    public ServletRegistrationBean<DHtmlLayoutServlet> zkLayoutServlet() {
        Map<String, String> params = new HashMap<>();
        params.put("update-uri", "/zkau");

        ServletRegistrationBean<DHtmlLayoutServlet> reg = new ServletRegistrationBean<>(
                new DHtmlLayoutServlet(), "*.zul", "*.zhtml");
        reg.setInitParameters(params);
        reg.setLoadOnStartup(1);
        return reg;
    }

    // Registra el Servlet de actualización asincrónica de ZK
    @Bean
    public ServletRegistrationBean<DHtmlUpdateServlet> zkUpdateServlet() {
        ServletRegistrationBean<DHtmlUpdateServlet> reg = new ServletRegistrationBean<>(
                new DHtmlUpdateServlet(), "/zkau/*");
        reg.setLoadOnStartup(2);
        return reg;
    }

    // Registra el HttpSessionListener para limpiar recursos ZK cuando caduca la sesión
    @Bean
    public HttpSessionListener httpSessionListener() {
        return new HttpSessionListener();
    }
}