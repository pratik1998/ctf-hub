// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.boot.loader.jar;

import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.stream.Stream;
import java.util.jar.JarEntry;
import java.util.Enumeration;
import java.util.jar.Manifest;
import java.security.Permission;
import java.net.MalformedURLException;
import java.net.URL;
import java.io.IOException;

class JarFileWrapper extends AbstractJarFile
{
    private final org.springframework.boot.loader.jar.JarFile parent;
    
    JarFileWrapper(final org.springframework.boot.loader.jar.JarFile parent) throws IOException {
        super(parent.getRootJarFile().getFile());
        this.parent = parent;
        if (System.getSecurityManager() == null) {
            super.close();
        }
    }
    
    @Override
    URL getUrl() throws MalformedURLException {
        return this.parent.getUrl();
    }
    
    @Override
    JarFileType getType() {
        return this.parent.getType();
    }
    
    @Override
    Permission getPermission() {
        return this.parent.getPermission();
    }
    
    @Override
    public Manifest getManifest() throws IOException {
        return this.parent.getManifest();
    }
    
    @Override
    public Enumeration<JarEntry> entries() {
        return this.parent.entries();
    }
    
    @Override
    public Stream<JarEntry> stream() {
        return this.parent.stream();
    }
    
    @Override
    public JarEntry getJarEntry(final String name) {
        return this.parent.getJarEntry(name);
    }
    
    @Override
    public ZipEntry getEntry(final String name) {
        return this.parent.getEntry(name);
    }
    
    @Override
    InputStream getInputStream() throws IOException {
        return this.parent.getInputStream();
    }
    
    @Override
    public synchronized InputStream getInputStream(final ZipEntry ze) throws IOException {
        return this.parent.getInputStream(ze);
    }
    
    @Override
    public String getComment() {
        return this.parent.getComment();
    }
    
    @Override
    public int size() {
        return this.parent.size();
    }
    
    @Override
    public String toString() {
        return this.parent.toString();
    }
    
    @Override
    public String getName() {
        return this.parent.getName();
    }
    
    static org.springframework.boot.loader.jar.JarFile unwrap(final JarFile jarFile) {
        if (jarFile instanceof org.springframework.boot.loader.jar.JarFile) {
            return (org.springframework.boot.loader.jar.JarFile)jarFile;
        }
        if (jarFile instanceof JarFileWrapper) {
            return unwrap(((JarFileWrapper)jarFile).parent);
        }
        throw new IllegalStateException("Not a JarFile or Wrapper");
    }
}
