// 
// Decompiled by Procyon v0.5.36
// 

package cc.saferoad.config;

import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.context.annotation.Bean;
import cc.saferoad.firewall.CustomHttpFirewall;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity
public class SpringSecurityConfig extends WebSecurityConfigurerAdapter
{
    @Bean
    public HttpFirewall httpFirewall() {
        return (HttpFirewall)new CustomHttpFirewall();
    }
    
    protected void configure(final HttpSecurity httpSecurity) throws Exception {
        ((ExpressionUrlAuthorizationConfigurer.AuthorizedUrl)httpSecurity.authorizeRequests().antMatchers(new String[] { "/admin/**" })).authenticated();
    }
}
