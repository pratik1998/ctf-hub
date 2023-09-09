// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.boot.loader.jarmode;

import java.util.Iterator;
import java.util.List;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.ClassUtils;

public final class JarModeLauncher
{
    static final String DISABLE_SYSTEM_EXIT;
    
    private JarModeLauncher() {
    }
    
    public static void main(final String[] args) {
        final String mode = System.getProperty("jarmode");
        final List<JarMode> candidates = (List<JarMode>)SpringFactoriesLoader.loadFactories((Class)JarMode.class, ClassUtils.getDefaultClassLoader());
        for (final JarMode candidate : candidates) {
            if (candidate.accepts(mode)) {
                candidate.run(mode, args);
                return;
            }
        }
        System.err.println("Unsupported jarmode '" + mode + "'");
        if (!Boolean.getBoolean(JarModeLauncher.DISABLE_SYSTEM_EXIT)) {
            System.exit(1);
        }
    }
    
    static {
        DISABLE_SYSTEM_EXIT = JarModeLauncher.class.getName() + ".DISABLE_SYSTEM_EXIT";
    }
}
