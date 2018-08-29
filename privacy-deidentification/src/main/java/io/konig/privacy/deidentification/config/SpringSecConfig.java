package io.konig.privacy.deidentification.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.password.MessageDigestPasswordEncoder;



@Configuration
@EnableWebSecurity
public class SpringSecConfig extends WebSecurityConfigurerAdapter {

	@Autowired
    @Qualifier("datasource")
    private DataSource dataSource;
	
    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.httpBasic().and().csrf().disable()
        .authorizeRequests().antMatchers("/","/swagger-resources").permitAll()
       .anyRequest().authenticated();
    }

    @Autowired
    protected void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
    
    	   auth.jdbcAuthentication().dataSource(dataSource).passwordEncoder(new MessageDigestPasswordEncoder("SHA-256"))
    	   .usersByUsernameQuery(
    			   "select USERNAME,SHA2PASSWORD, ENABLED from USERS where USERNAME=?")
    			  .authoritiesByUsernameQuery(
    			   "select USERNAME, ROLE from USER_ROLES where USERNAME=?");             }

}
