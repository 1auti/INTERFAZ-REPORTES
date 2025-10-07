package org.zkoss.reporte.spring_zk_config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.zkoss.zk.au.http.DHtmlUpdateServlet;
import org.zkoss.zk.ui.WebApp;
import org.zkoss.zk.ui.http.DHtmlLayoutServlet;
import org.zkoss.zk.ui.http.HttpSessionListener;
import org.zkoss.zk.ui.util.WebAppInit;

import javax.servlet.ServletContext;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class ZkSpringIntegrator {

    private static final Logger logger = LoggerFactory.getLogger(ZkSpringIntegrator.class);
    private final ApplicationContext applicationContext;

    public ZkSpringIntegrator(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        logger.info("✅ ZkSpringIntegrator creado con ApplicationContext: {}", applicationContext);
    }

    @Bean
    public ServletListenerRegistrationBean<HttpSessionListener> zkHttpSessionListener() {
        logger.info("✅ Registrando HttpSessionListener de ZK");
        return new ServletListenerRegistrationBean<>(new HttpSessionListener());
    }

    @Bean
    public WebAppInit zkSpringWebAppInitializer() {
        logger.info("✅ Creando WebAppInit para integración ZK-Spring");

        return webApp -> {
            logger.info("🔧 Inicializando integración ZK-Spring...");

            // Obtener ServletContext
            ServletContext servletContext = webApp.getServletContext();

            // Registrar ApplicationContext en TODOS los lugares donde ZK lo busca
            String[] attributeNames = {
                    "org.springframework.web.context.WebApplicationContext.ROOT",
                    "springApplicationContext",
                    "SpringUtil.applicationContext"
            };

            for (String name : attributeNames) {
                webApp.setAttribute(name, applicationContext);
                servletContext.setAttribute(name, applicationContext);
                logger.info("   ✓ Registrado ApplicationContext como: {}", name);
            }

            setupZkAutowiring(webApp);

            // Verificar que se registró correctamente
            Object ctx = servletContext.getAttribute(
                    "org.springframework.web.context.WebApplicationContext.ROOT"
            );

            if (ctx != null) {
                logger.info("✅ ApplicationContext registrado correctamente en ServletContext");
                logger.info("   Beans disponibles: {}", applicationContext.getBeanDefinitionCount());

                // Listar algunos beans importantes
                String[] beanNames = applicationContext.getBeanNamesForType(Object.class);
                logger.info("   Ejemplo de beans: ");
                for (int i = 0; i < Math.min(10, beanNames.length); i++) {
                    logger.info("      - {}", beanNames[i]);
                }
            } else {
                logger.error("❌ FALLO: ApplicationContext NO se registró en ServletContext");
            }
        };
    }

    private void setupZkAutowiring(WebApp webApp) {
        logger.info("🔧 Configurando autowiring de ZK...");

        org.zkoss.zk.ui.util.Configuration zkConfig = webApp.getConfiguration();

        zkConfig.setPreference("org.zkoss.zk.ui.composer.autowire.enabled", "true");
        zkConfig.setPreference("org.zkoss.zk.ui.composer.autowire.type", "byType");
        zkConfig.setPreference("org.zkoss.bind.composer.autowire.enabled", "true");
        zkConfig.setPreference("org.zkoss.bind.composer.autowire.type", "byType");

        logger.info("   ✓ Autowiring configurado (byType)");
    }

    @Bean
    public ServletRegistrationBean<DHtmlLayoutServlet> zkLayoutServlet() {
        logger.info("✅ Registrando ZK Layout Servlet");

        Map<String, String> params = new HashMap<>();
        params.put("update-uri", "/zkau");

        ServletRegistrationBean<DHtmlLayoutServlet> reg = new ServletRegistrationBean<>(
                new DHtmlLayoutServlet(), "*.zul", "*.zhtml");
        reg.setInitParameters(params);
        reg.setLoadOnStartup(1);
        reg.setName("zkLoader");
        return reg;
    }

    @Bean
    public ServletRegistrationBean<DHtmlUpdateServlet> zkUpdateServlet() {
        logger.info("✅ Registrando ZK Update Servlet");

        ServletRegistrationBean<DHtmlUpdateServlet> reg = new ServletRegistrationBean<>(
                new DHtmlUpdateServlet(), "/zkau/*");
        reg.setLoadOnStartup(2);
        reg.setName("auEngine");
        return reg;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}