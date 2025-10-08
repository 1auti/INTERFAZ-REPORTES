package org.zkoss.reporte;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.zkoss.zk.au.http.DHtmlUpdateServlet;
import org.zkoss.zk.ui.http.DHtmlLayoutServlet;
import org.zkoss.zk.ui.http.HttpSessionListener;
import org.zkoss.zk.ui.http.RichletFilter;

import javax.servlet.DispatcherType;
import java.util.EnumSet;

@SpringBootApplication
@ComponentScan(basePackages = {
        "org.zkoss.reporte",
        "org.zkoss.reporte.viewmodel",
        "org.zkoss.reporte.service"
})
public class AdminApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
    }


    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(AdminApplication.class);
    }

    // Registrar el listener de sesión HTTP de ZK explícitamente
    @Bean
    public ServletListenerRegistrationBean<HttpSessionListener> httpSessionListener() {
        return new ServletListenerRegistrationBean<>(new HttpSessionListener());
    }

    // Registro del servlet principal de ZK
    @Bean
    public ServletRegistrationBean<DHtmlLayoutServlet> zkLayoutServlet() {
        ServletRegistrationBean<DHtmlLayoutServlet> reg = new ServletRegistrationBean<>(
                new DHtmlLayoutServlet(), "*.zul", "*.zhtml");
        reg.setLoadOnStartup(1);
        reg.addInitParameter("update-uri", "/zkau");
        return reg;
    }

    // Registro del servlet de actualización asíncrona de ZK
    @Bean
    public ServletRegistrationBean<DHtmlUpdateServlet> zkUpdateServlet() {
        ServletRegistrationBean<DHtmlUpdateServlet> reg = new ServletRegistrationBean<>(
                new DHtmlUpdateServlet(), "/zkau/*");
        reg.setLoadOnStartup(2);
        return reg;
    }

    // Registro del CometAsyncServlet para ZK EE
//        @Bean
//        public ServletRegistrationBean<CometAsyncServlet> cometAsyncServlet() {
//            ServletRegistrationBean<CometAsyncServlet> reg = new ServletRegistrationBean<>(
//                    new CometAsyncServlet(), "/zkcomet/*");
//            reg.setLoadOnStartup(3);
//            reg.setAsyncSupported(true);
//            return reg;
//        }

    // Filtro para richlets
    @Bean
    public FilterRegistrationBean<RichletFilter> richletFilter() {
        FilterRegistrationBean<RichletFilter> reg = new FilterRegistrationBean<>(new RichletFilter());
        reg.addUrlPatterns("/richlet/*");
        reg.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST));
        return reg;
    }

    // Filtro de codificación
    @Bean
    public FilterRegistrationBean<CharacterEncodingFilter> characterEncodingFilter() {
        FilterRegistrationBean<CharacterEncodingFilter> reg = new FilterRegistrationBean<>(
                new CharacterEncodingFilter());
        reg.addUrlPatterns("/*");
        reg.setInitParameters(java.util.Collections.singletonMap("encoding", "UTF-8"));
        return reg;
    }
}