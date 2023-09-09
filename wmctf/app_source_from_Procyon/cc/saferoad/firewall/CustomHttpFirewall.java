// 
// Decompiled by Procyon v0.5.36
// 

package cc.saferoad.firewall;

import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.security.web.firewall.DefaultHttpFirewall;

public class CustomHttpFirewall extends DefaultHttpFirewall
{
    protected void configure(final StrictHttpFirewall firewalledRequest) {
        firewalledRequest.setAllowUrlEncodedSlash(true);
        firewalledRequest.setAllowUrlEncodedDoubleSlash(true);
        firewalledRequest.setAllowUrlEncodedPeriod(true);
    }
}
