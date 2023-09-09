// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.boot.loader.jar;

import java.io.InputStream;
import java.security.Permission;
import java.net.MalformedURLException;
import java.net.URL;
import java.io.IOException;
import java.io.File;
import java.util.jar.JarFile;

abstract class AbstractJarFile extends JarFile
{
    AbstractJarFile(final File file) throws IOException {
        super(file);
    }
    
    abstract URL getUrl() throws MalformedURLException;
    
    abstract JarFileType getType();
    
    abstract Permission getPermission();
    
    abstract InputStream getInputStream() throws IOException;
    
    enum JarFileType
    {
        DIRECT, 
        NESTED_DIRECTORY, 
        NESTED_JAR;
    }
}
