package io.konig.privacy.deidentification.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;


@Configuration
public class SpringSecConfig extends WebSecurityConfigurerAdapter {

    @Autowired
   private BasicAuthenticationPoint basicAuthenticationPoint;
	
    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.authorizeRequests().antMatchers("/","/swagger-resources").permitAll()
       .anyRequest().authenticated();
        httpSecurity.csrf().disable();
        httpSecurity.headers().frameOptions().disable();

        httpSecurity.httpBasic().authenticationEntryPoint(basicAuthenticationPoint);
    }
    
}
