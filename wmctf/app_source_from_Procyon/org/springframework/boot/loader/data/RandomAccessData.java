// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.boot.loader.data;

import java.io.IOException;
import java.io.InputStream;

public interface RandomAccessData
{
    InputStream getInputStream() throws IOException;
    
    RandomAccessData getSubsection(final long offset, final long length);
    
    byte[] read() throws IOException;
    
    byte[] read(final long offset, final long length) throws IOException;
    
    long getSize();
}
