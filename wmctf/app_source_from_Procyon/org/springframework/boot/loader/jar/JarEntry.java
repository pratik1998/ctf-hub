// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.boot.loader.jar;

import java.security.CodeSigner;
import java.security.cert.Certificate;
import java.io.IOException;
import java.util.jar.Manifest;
import java.util.jar.Attributes;
import java.net.MalformedURLException;
import java.net.URL;

class JarEntry extends java.util.jar.JarEntry implements FileHeader
{
    private final int index;
    private final AsciiBytes name;
    private final AsciiBytes headerName;
    private final JarFile jarFile;
    private long localHeaderOffset;
    private volatile JarEntryCertification certification;
    
    JarEntry(final JarFile jarFile, final int index, final CentralDirectoryFileHeader header, final AsciiBytes nameAlias) {
        super((nameAlias != null) ? nameAlias.toString() : header.getName().toString());
        this.index = index;
        this.name = ((nameAlias != null) ? nameAlias : header.getName());
        this.headerName = header.getName();
        this.jarFile = jarFile;
        this.localHeaderOffset = header.getLocalHeaderOffset();
        this.setCompressedSize(header.getCompressedSize());
        this.setMethod(header.getMethod());
        this.setCrc(header.getCrc());
        this.setComment(header.getComment().toString());
        this.setSize(header.getSize());
        this.setTime(header.getTime());
        if (header.hasExtra()) {
            this.setExtra(header.getExtra());
        }
    }
    
    int getIndex() {
        return this.index;
    }
    
    AsciiBytes getAsciiBytesName() {
        return this.name;
    }
    
    @Override
    public boolean hasName(final CharSequence name, final char suffix) {
        return this.headerName.matches(name, suffix);
    }
    
    URL getUrl() throws MalformedURLException {
        return new URL(this.jarFile.getUrl(), this.getName());
    }
    
    @Override
    public Attributes getAttributes() throws IOException {
        final Manifest manifest = this.jarFile.getManifest();
        return (manifest != null) ? manifest.getAttributes(this.getName()) : null;
    }
    
    @Override
    public Certificate[] getCertificates() {
        return this.getCertification().getCertificates();
    }
    
    @Override
    public CodeSigner[] getCodeSigners() {
        return this.getCertification().getCodeSigners();
    }
    
    private JarEntryCertification getCertification() {
        if (!this.jarFile.isSigned()) {
            return JarEntryCertification.NONE;
        }
        JarEntryCertification certification = this.certification;
        if (certification == null) {
            certification = this.jarFile.getCertification(this);
            this.certification = certification;
        }
        return certification;
    }
    
    @Override
    public long getLocalHeaderOffset() {
        return this.localHeaderOffset;
    }
}
