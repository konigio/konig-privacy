package io.konig.privacy.deidentification;

import java.io.IOException;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.FailureMode;
import net.spy.memcached.MemcachedClient;



@SpringBootApplication 
@ComponentScan({"io.konig.privacy.deidentification"})
public class Application  {
	
	@Autowired
	private Environment env;
	
	
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
	
	 @Bean
	 public MemcachedClient memcachedCloudConfiguration() throws IOException {
		 return new MemcachedClient(
					new ConnectionFactoryBuilder().setDaemon(true).setFailureMode(FailureMode.Retry).build(),
					AddrUtil.getAddresses(env.getProperty("aws.memcache.endpoint")));
	 }
	
	 @Bean(value = "datasource")
	    @ConfigurationProperties("spring.datasource")
	    public DataSource dataSource() {
		 System.out.println("Inside datasource");
	        return DataSourceBuilder.create().build();
	    }
}
