// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.boot.loader.jarmode;

import java.util.Arrays;

class TestJarMode implements JarMode
{
    @Override
    public boolean accepts(final String mode) {
        return "test".equals(mode);
    }
    
    @Override
    public void run(final String mode, final String[] args) {
        System.out.println("running in " + mode + " jar mode " + Arrays.asList(args));
    }
}
