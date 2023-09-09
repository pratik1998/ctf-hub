// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.boot.loader.jarmode;

public interface JarMode
{
    boolean accepts(final String mode);
    
    void run(final String mode, final String[] args);
}
