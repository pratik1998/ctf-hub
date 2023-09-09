// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.boot.loader;

import java.util.function.Supplier;
import java.util.jar.Manifest;
import java.util.jar.JarFile;
import java.net.URLConnection;
import java.security.PrivilegedActionException;
import java.security.AccessController;
import java.net.JarURLConnection;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import org.springframework.boot.loader.jar.Handler;
import java.net.URL;
import org.springframework.boot.loader.archive.Archive;
import java.net.URLClassLoader;

public class LaunchedURLClassLoader extends URLClassLoader
{
    private static final int BUFFER_SIZE = 4096;
    private final boolean exploded;
    private final Archive rootArchive;
    private final Object packageLock;
    private volatile DefinePackageCallType definePackageCallType;
    
    public LaunchedURLClassLoader(final URL[] urls, final ClassLoader parent) {
        this(false, urls, parent);
    }
    
    public LaunchedURLClassLoader(final boolean exploded, final URL[] urls, final ClassLoader parent) {
        this(exploded, null, urls, parent);
    }
    
    public LaunchedURLClassLoader(final boolean exploded, final Archive rootArchive, final URL[] urls, final ClassLoader parent) {
        super(urls, parent);
        this.packageLock = new Object();
        this.exploded = exploded;
        this.rootArchive = rootArchive;
    }
    
    @Override
    public URL findResource(final String name) {
        if (this.exploded) {
            return super.findResource(name);
        }
        Handler.setUseFastConnectionExceptions(true);
        try {
            return super.findResource(name);
        }
        finally {
            Handler.setUseFastConnectionExceptions(false);
        }
    }
    
    @Override
    public Enumeration<URL> findResources(final String name) throws IOException {
        if (this.exploded) {
            return super.findResources(name);
        }
        Handler.setUseFastConnectionExceptions(true);
        try {
            return new UseFastConnectionExceptionsEnumeration(super.findResources(name));
        }
        finally {
            Handler.setUseFastConnectionExceptions(false);
        }
    }
    
    @Override
    protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
        if (name.startsWith("org.springframework.boot.loader.jarmode.")) {
            try {
                final Class<?> result = this.loadClassInLaunchedClassLoader(name);
                if (resolve) {
                    this.resolveClass(result);
                }
                return result;
            }
            catch (ClassNotFoundException ex2) {}
        }
        if (this.exploded) {
            return super.loadClass(name, resolve);
        }
        Handler.setUseFastConnectionExceptions(true);
        try {
            try {
                this.definePackageIfNecessary(name);
            }
            catch (IllegalArgumentException ex) {
                if (this.getPackage(name) == null) {
                    throw new AssertionError((Object)("Package " + name + " has already been defined but it could not be found"));
                }
            }
            return super.loadClass(name, resolve);
        }
        finally {
            Handler.setUseFastConnectionExceptions(false);
        }
    }
    
    private Class<?> loadClassInLaunchedClassLoader(final String name) throws ClassNotFoundException {
        final String internalName = name.replace('.', '/') + ".class";
        final InputStream inputStream = this.getParent().getResourceAsStream(internalName);
        if (inputStream == null) {
            throw new ClassNotFoundException(name);
        }
        try {
            try {
                final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                final byte[] buffer = new byte[4096];
                int bytesRead = -1;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                inputStream.close();
                final byte[] bytes = outputStream.toByteArray();
                final Class<?> definedClass = this.defineClass(name, bytes, 0, bytes.length);
                this.definePackageIfNecessary(name);
                return definedClass;
            }
            finally {
                inputStream.close();
            }
        }
        catch (IOException ex) {
            throw new ClassNotFoundException("Cannot load resource for class [" + name + "]", ex);
        }
    }
    
    private void definePackageIfNecessary(final String className) {
        final int lastDot = className.lastIndexOf(46);
        if (lastDot >= 0) {
            final String packageName = className.substring(0, lastDot);
            if (this.getPackage(packageName) == null) {
                try {
                    this.definePackage(className, packageName);
                }
                catch (IllegalArgumentException ex) {
                    if (this.getPackage(packageName) == null) {
                        throw new AssertionError((Object)("Package " + packageName + " has already been defined but it could not be found"));
                    }
                }
            }
        }
    }
    
    private void definePackage(final String className, final String packageName) {
        try {
            final String packageEntryName;
            final String classEntryName;
            final URL[] array;
            int length;
            int i = 0;
            URL url;
            URLConnection connection;
            JarFile jarFile;
            AccessController.doPrivileged(() -> {
                packageEntryName = packageName.replace('.', '/') + "/";
                classEntryName = className.replace('.', '/') + ".class";
                this.getURLs();
                for (length = array.length; i < length; ++i) {
                    url = array[i];
                    try {
                        connection = url.openConnection();
                        if (connection instanceof JarURLConnection) {
                            jarFile = ((JarURLConnection)connection).getJarFile();
                            if (jarFile.getEntry(classEntryName) != null && jarFile.getEntry(packageEntryName) != null && jarFile.getManifest() != null) {
                                this.definePackage(packageName, jarFile.getManifest(), url);
                                return null;
                            }
                        }
                    }
                    catch (IOException ex) {}
                }
                return null;
            }, AccessController.getContext());
        }
        catch (PrivilegedActionException ex2) {}
    }
    
    @Override
    protected Package definePackage(final String name, final Manifest man, final URL url) throws IllegalArgumentException {
        if (!this.exploded) {
            return super.definePackage(name, man, url);
        }
        synchronized (this.packageLock) {
            return this.doDefinePackage(DefinePackageCallType.MANIFEST, () -> super.definePackage(name, man, url));
        }
    }
    
    @Override
    protected Package definePackage(final String name, final String specTitle, final String specVersion, final String specVendor, final String implTitle, final String implVersion, final String implVendor, final URL sealBase) throws IllegalArgumentException {
        if (!this.exploded) {
            return super.definePackage(name, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, sealBase);
        }
        synchronized (this.packageLock) {
            if (this.definePackageCallType == null) {
                final Manifest manifest = this.getManifest(this.rootArchive);
                if (manifest != null) {
                    return this.definePackage(name, manifest, sealBase);
                }
            }
            return this.doDefinePackage(DefinePackageCallType.ATTRIBUTES, () -> super.definePackage(name, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, sealBase));
        }
    }
    
    private Manifest getManifest(final Archive archive) {
        try {
            return (archive != null) ? archive.getManifest() : null;
        }
        catch (IOException ex) {
            return null;
        }
    }
    
    private <T> T doDefinePackage(final DefinePackageCallType type, final Supplier<T> call) {
        final DefinePackageCallType existingType = this.definePackageCallType;
        try {
            this.definePackageCallType = type;
            return call.get();
        }
        finally {
            this.definePackageCallType = existingType;
        }
    }
    
    public void clearCache() {
        if (this.exploded) {
            return;
        }
        for (final URL url : this.getURLs()) {
            try {
                final URLConnection connection = url.openConnection();
                if (connection instanceof JarURLConnection) {
                    this.clearCache(connection);
                }
            }
            catch (IOException ex) {}
        }
    }
    
    private void clearCache(final URLConnection connection) throws IOException {
        final Object jarFile = ((JarURLConnection)connection).getJarFile();
        if (jarFile instanceof org.springframework.boot.loader.jar.JarFile) {
            ((org.springframework.boot.loader.jar.JarFile)jarFile).clearCache();
        }
    }
    
    static {
        ClassLoader.registerAsParallelCapable();
    }
    
    private static class UseFastConnectionExceptionsEnumeration implements Enumeration<URL>
    {
        private final Enumeration<URL> delegate;
        
        UseFastConnectionExceptionsEnumeration(final Enumeration<URL> delegate) {
            this.delegate = delegate;
        }
        
        @Override
        public boolean hasMoreElements() {
            Handler.setUseFastConnectionExceptions(true);
            try {
                return this.delegate.hasMoreElements();
            }
            finally {
                Handler.setUseFastConnectionExceptions(false);
            }
        }
        
        @Override
        public URL nextElement() {
            Handler.setUseFastConnectionExceptions(true);
            try {
                return this.delegate.nextElement();
            }
            finally {
                Handler.setUseFastConnectionExceptions(false);
            }
        }
    }
    
    private enum DefinePackageCallType
    {
        MANIFEST, 
        ATTRIBUTES;
    }
}
