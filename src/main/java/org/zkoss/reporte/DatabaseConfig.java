package org.zkoss.reporte;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.ClassUtils;
import org.zkoss.reporte.exception.BusinessException;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import static org.hibernate.cfg.AvailableSettings.DIALECT;
import static org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO;

@Configuration

public class DatabaseConfig {
    //  @Value("/home/administrador/Escritorio/REPOSREMOTOS/documentacion_zk/dashboard/src/main/resources")
    private String PROPERTIES_DIRECTORY;

    //@Bean(name = "dataSource")
    //@Primary
    public DataSource dataSource() throws BusinessException {
        final String DIALECT = "";
        final String HBM2DDL_AUTO = "";
        Properties databaseProperties = this.loadDatabaseProperties();
        Properties secureProperties = this.loadSecuredProperties();


        String url = databaseProperties.getProperty("DB.URL");
        String user = databaseProperties.getProperty("DB.USER");
        String encryptedPassword = secureProperties.getProperty("DB.PASSWORD");
        String encryptorKey = secureProperties.getProperty("DB.PASSWORD_ENCRYPTOR");

        //String password = DatabaseConfig.passwordEncryptor(encryptorKey).decrypt(encryptedPassword);

        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.postgresql.Driver");
        config.setJdbcUrl(url);
        config.setUsername(user);
        //config.setPassword(password);

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        return new HikariDataSource(config);
    }


    private Properties loadDatabaseProperties() throws BusinessException {
        Properties props = new Properties();

        String pathProperties = String.format("%s/database.properties", PROPERTIES_DIRECTORY);
        URI uri = null;
        try {
            uri = new URI("file://" + pathProperties);
        } catch (URISyntaxException e) {
            throw new BusinessException(e.getMessage());
        }

        try (FileInputStream fis = new FileInputStream(new File(uri))) {
            props.load(fis);
        } catch (IOException e) {
            throw new BusinessException(e.getMessage());
        }

        return props;
    }

    private Properties loadSecuredProperties() throws BusinessException {
        Properties props = new Properties();

        String pathProperties = String.format("%s/SECURITY/app_security.properties", PROPERTIES_DIRECTORY);
        URI uri = null;
        try {
            uri = new URI("file://" + pathProperties);
            //uri = new URI("file:///C:" + pathProperties);
        } catch (URISyntaxException e) {
            throw new BusinessException(e.getMessage());
        }

        try (FileInputStream fis = new FileInputStream(new File(uri))) {
            props.load(fis);
        } catch (IOException e) {
            throw new BusinessException(e.getMessage());
        }

        return props;
    }

    //@Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
        entityManagerFactoryBean.setDataSource(dataSource);

        String entities = "org.zkoss.admin.radar_system";
        String converters = ClassUtils.getPackageName(Jsr310JpaConverters.class);
        String repos = "org.zkoss.admin.radar_system";
        entityManagerFactoryBean.setPackagesToScan(entities, converters, repos);

        entityManagerFactoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        Properties jpaProperties = new Properties();
        jpaProperties.put(DIALECT, "");
        jpaProperties.put(HBM2DDL_AUTO, "");
        jpaProperties.put(AvailableSettings.SHOW_SQL, Boolean.FALSE);
        jpaProperties.put(AvailableSettings.FORMAT_SQL, Boolean.FALSE);
        jpaProperties.put(AvailableSettings.USE_SQL_COMMENTS, Boolean.FALSE);
        entityManagerFactoryBean.setJpaProperties(jpaProperties);

        return entityManagerFactoryBean;
    }

    //@Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
