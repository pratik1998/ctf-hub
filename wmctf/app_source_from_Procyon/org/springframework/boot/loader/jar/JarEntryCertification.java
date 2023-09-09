// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.boot.loader.jar;

import java.util.jar.JarEntry;
import java.security.CodeSigner;
import java.security.cert.Certificate;

class JarEntryCertification
{
    static final JarEntryCertification NONE;
    private final Certificate[] certificates;
    private final CodeSigner[] codeSigners;
    
    JarEntryCertification(final Certificate[] certificates, final CodeSigner[] codeSigners) {
        this.certificates = certificates;
        this.codeSigners = codeSigners;
    }
    
    Certificate[] getCertificates() {
        return (Certificate[])((this.certificates != null) ? ((Certificate[])this.certificates.clone()) : null);
    }
    
    CodeSigner[] getCodeSigners() {
        return (CodeSigner[])((this.codeSigners != null) ? ((CodeSigner[])this.codeSigners.clone()) : null);
    }
    
    static JarEntryCertification from(final JarEntry certifiedEntry) {
        final Certificate[] certificates = (Certificate[])((certifiedEntry != null) ? certifiedEntry.getCertificates() : null);
        final CodeSigner[] codeSigners = (CodeSigner[])((certifiedEntry != null) ? certifiedEntry.getCodeSigners() : null);
        if (certificates == null && codeSigners == null) {
            return JarEntryCertification.NONE;
        }
        return new JarEntryCertification(certificates, codeSigners);
    }
    
    static {
        NONE = new JarEntryCertification(null, null);
    }
}
