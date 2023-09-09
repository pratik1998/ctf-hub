// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.boot.loader;

import java.lang.reflect.Method;

public class MainMethodRunner
{
    private final String mainClassName;
    private final String[] args;
    
    public MainMethodRunner(final String mainClass, final String[] args) {
        this.mainClassName = mainClass;
        this.args = (String[])((args != null) ? ((String[])args.clone()) : null);
    }
    
    public void run() throws Exception {
        final Class<?> mainClass = Class.forName(this.mainClassName, false, Thread.currentThread().getContextClassLoader());
        final Method mainMethod = mainClass.getDeclaredMethod("main", String[].class);
        mainMethod.setAccessible(true);
        mainMethod.invoke(null, this.args);
    }
}
