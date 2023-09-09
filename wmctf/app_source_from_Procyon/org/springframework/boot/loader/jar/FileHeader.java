// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.boot.loader.jar;

interface FileHeader
{
    boolean hasName(final CharSequence name, final char suffix);
    
    long getLocalHeaderOffset();
    
    long getCompressedSize();
    
    long getSize();
    
    int getMethod();
}
