package br.com.caelum;

import java.beans.PropertyVetoException;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.mchange.v2.c3p0.ComboPooledDataSource;

@Configuration
@EnableTransactionManagement
public class JpaConfigurator {

	@Bean
	public DataSource getDataSource() throws PropertyVetoException {
		/* Data source c3p0 com limitador de pool, mt bom e dahora */
		ComboPooledDataSource dataSource = new ComboPooledDataSource();

		dataSource.setDriverClass("com.mysql.jdbc.Driver");
		dataSource.setJdbcUrl("jdbc:mysql://localhost/projeto_jpa");
		dataSource.setUser("root");
		dataSource.setPassword("");

		dataSource.setMinPoolSize(5);
		dataSource.setMaxPoolSize(10);
		dataSource.setNumHelperThreads(15);

		/*
		 * Precisamos ensinar o pool a matar as conexões que ficam ociosas por muito
		 * tempo, eliminando o risco de escolher uma conexão quebrada, pq pode acontecer
		 * de o bd cair por alguns momentos e as conexões que estavam abertas com o db
		 * quebrarem
		 */
		dataSource.setIdleConnectionTestPeriod(1); // a cada um segundo testamos as conexões ociosas

		return dataSource;
	}

	@Bean
	public Statistics statistics(EntityManagerFactory emf) {
		SessionFactory factory = emf.unwrap(SessionFactory.class);
		return factory.getStatistics();
	}

	@Bean
	public LocalContainerEntityManagerFactoryBean getEntityManagerFactory(DataSource dataSource) {
		LocalContainerEntityManagerFactoryBean entityManagerFactory = new LocalContainerEntityManagerFactoryBean();

		entityManagerFactory.setPackagesToScan("br.com.caelum");
		entityManagerFactory.setDataSource(dataSource);

		entityManagerFactory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

		Properties props = new Properties();

		props.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQL5InnoDBDialect");
		props.setProperty("hibernate.show_sql", "true");
		props.setProperty("hibernate.hbm2ddl.auto", "create-drop");
		// propriedade para o hibernate usar um cache de segundo nivel
		props.setProperty("hibernate.cache.use_second_level_cache", "true");
		// armazenar em cache o resultado de uma query feita para determinados
		// parâmetros e, com isso, poderemos buscar no cache ao em vez de ir diretamente
		// ao banco.
		props.setProperty("hibernate.cache.use_query_cache", "true");
		// provedor de cache que usaremos.
		props.setProperty("hibernate.cache.region.factory_class",
				"org.hibernate.cache.ehcache.SingletonEhCacheRegionFactory");
		// acionando recurso de estatistica do hibernade
		props.setProperty("hibernate.generate_statistics", "true");

		entityManagerFactory.setJpaProperties(props);
		return entityManagerFactory;
	}

	@Bean
	public JpaTransactionManager getTransactionManager(EntityManagerFactory emf) {
		JpaTransactionManager transactionManager = new JpaTransactionManager();
		transactionManager.setEntityManagerFactory(emf);

		return transactionManager;
	}

}
