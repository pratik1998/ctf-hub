// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.boot.loader;

import java.net.URI;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import org.springframework.boot.loader.archive.JarFileArchive;
import org.springframework.boot.loader.archive.ExplodedArchive;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import org.springframework.boot.loader.archive.Archive;
import java.util.List;
import org.springframework.boot.loader.jar.JarFile;

public abstract class Launcher
{
    private static final String JAR_MODE_LAUNCHER = "org.springframework.boot.loader.jarmode.JarModeLauncher";
    
    protected void launch(final String[] args) throws Exception {
        if (!this.isExploded()) {
            JarFile.registerUrlProtocolHandler();
        }
        final ClassLoader classLoader = this.createClassLoader(this.getClassPathArchivesIterator());
        final String jarMode = System.getProperty("jarmode");
        final String launchClass = (jarMode != null && !jarMode.isEmpty()) ? "org.springframework.boot.loader.jarmode.JarModeLauncher" : this.getMainClass();
        this.launch(args, launchClass, classLoader);
    }
    
    @Deprecated
    protected ClassLoader createClassLoader(final List<Archive> archives) throws Exception {
        return this.createClassLoader(archives.iterator());
    }
    
    protected ClassLoader createClassLoader(final Iterator<Archive> archives) throws Exception {
        final List<URL> urls = new ArrayList<URL>(50);
        while (archives.hasNext()) {
            urls.add(archives.next().getUrl());
        }
        return this.createClassLoader(urls.toArray(new URL[0]));
    }
    
    protected ClassLoader createClassLoader(final URL[] urls) throws Exception {
        return new LaunchedURLClassLoader(this.isExploded(), this.getArchive(), urls, this.getClass().getClassLoader());
    }
    
    protected void launch(final String[] args, final String launchClass, final ClassLoader classLoader) throws Exception {
        Thread.currentThread().setContextClassLoader(classLoader);
        this.createMainMethodRunner(launchClass, args, classLoader).run();
    }
    
    protected MainMethodRunner createMainMethodRunner(final String mainClass, final String[] args, final ClassLoader classLoader) {
        return new MainMethodRunner(mainClass, args);
    }
    
    protected abstract String getMainClass() throws Exception;
    
    protected Iterator<Archive> getClassPathArchivesIterator() throws Exception {
        return this.getClassPathArchives().iterator();
    }
    
    @Deprecated
    protected List<Archive> getClassPathArchives() throws Exception {
        throw new IllegalStateException("Unexpected call to getClassPathArchives()");
    }
    
    protected final Archive createArchive() throws Exception {
        final ProtectionDomain protectionDomain = this.getClass().getProtectionDomain();
        final CodeSource codeSource = protectionDomain.getCodeSource();
        final URI location = (codeSource != null) ? codeSource.getLocation().toURI() : null;
        final String path = (location != null) ? location.getSchemeSpecificPart() : null;
        if (path == null) {
            throw new IllegalStateException("Unable to determine code source archive");
        }
        final File root = new File(path);
        if (!root.exists()) {
            throw new IllegalStateException("Unable to determine code source archive from " + root);
        }
        return root.isDirectory() ? new ExplodedArchive(root) : new JarFileArchive(root);
    }
    
    protected boolean isExploded() {
        return false;
    }
    
    protected Archive getArchive() {
        return null;
    }
}
