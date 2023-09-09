// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.boot.loader.jar;

import java.net.URLStreamHandlerFactory;
import java.util.concurrent.ConcurrentHashMap;
import java.net.URI;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.net.MalformedURLException;
import java.io.IOException;
import java.net.URLConnection;
import java.io.File;
import java.util.Map;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.regex.Pattern;
import java.net.URLStreamHandler;

public class Handler extends URLStreamHandler
{
    private static final String JAR_PROTOCOL = "jar:";
    private static final String FILE_PROTOCOL = "file:";
    private static final String TOMCAT_WARFILE_PROTOCOL = "war:file:";
    private static final String SEPARATOR = "!/";
    private static final Pattern SEPARATOR_PATTERN;
    private static final String CURRENT_DIR = "/./";
    private static final Pattern CURRENT_DIR_PATTERN;
    private static final String PARENT_DIR = "/../";
    private static final String PROTOCOL_HANDLER = "java.protocol.handler.pkgs";
    private static final String[] FALLBACK_HANDLERS;
    private static URL jarContextUrl;
    private static SoftReference<Map<File, JarFile>> rootFileCache;
    private final JarFile jarFile;
    private URLStreamHandler fallbackHandler;
    
    public Handler() {
        this(null);
    }
    
    public Handler(final JarFile jarFile) {
        this.jarFile = jarFile;
    }
    
    @Override
    protected URLConnection openConnection(final URL url) throws IOException {
        if (this.jarFile != null && this.isUrlInJarFile(url, this.jarFile)) {
            return JarURLConnection.get(url, this.jarFile);
        }
        try {
            return JarURLConnection.get(url, this.getRootJarFileFromUrl(url));
        }
        catch (Exception ex) {
            return this.openFallbackConnection(url, ex);
        }
    }
    
    private boolean isUrlInJarFile(final URL url, final JarFile jarFile) throws MalformedURLException {
        return url.getPath().startsWith(jarFile.getUrl().getPath()) && url.toString().startsWith(jarFile.getUrlString());
    }
    
    private URLConnection openFallbackConnection(final URL url, final Exception reason) throws IOException {
        try {
            URLConnection connection = this.openFallbackTomcatConnection(url);
            connection = ((connection != null) ? connection : this.openFallbackContextConnection(url));
            return (connection != null) ? connection : this.openFallbackHandlerConnection(url);
        }
        catch (Exception ex) {
            if (reason instanceof IOException) {
                this.log(false, "Unable to open fallback handler", ex);
                throw (IOException)reason;
            }
            this.log(true, "Unable to open fallback handler", ex);
            if (reason instanceof RuntimeException) {
                throw (RuntimeException)reason;
            }
            throw new IllegalStateException(reason);
        }
    }
    
    private URLConnection openFallbackTomcatConnection(final URL url) {
        String file = url.getFile();
        if (this.isTomcatWarUrl(file)) {
            file = file.substring("war:file:".length());
            file = file.replaceFirst("\\*/", "!/");
            try {
                final URLConnection connection = this.openConnection(new URL("jar:file:" + file));
                connection.getInputStream().close();
                return connection;
            }
            catch (IOException ex) {}
        }
        return null;
    }
    
    private boolean isTomcatWarUrl(final String file) {
        if (!file.startsWith("war:file:")) {
            if (file.contains("*/")) {
                return false;
            }
        }
        try {
            final URLConnection connection = new URL(file).openConnection();
            if (connection.getClass().getName().startsWith("org.apache.catalina")) {
                return true;
            }
        }
        catch (Exception ex) {}
        return false;
    }
    
    private URLConnection openFallbackContextConnection(final URL url) {
        try {
            if (Handler.jarContextUrl != null) {
                return new URL(Handler.jarContextUrl, url.toExternalForm()).openConnection();
            }
        }
        catch (Exception ex) {}
        return null;
    }
    
    private URLConnection openFallbackHandlerConnection(final URL url) throws Exception {
        final URLStreamHandler fallbackHandler = this.getFallbackHandler();
        return new URL(null, url.toExternalForm(), fallbackHandler).openConnection();
    }
    
    private URLStreamHandler getFallbackHandler() {
        if (this.fallbackHandler != null) {
            return this.fallbackHandler;
        }
        final String[] fallback_HANDLERS = Handler.FALLBACK_HANDLERS;
        final int length = fallback_HANDLERS.length;
        int i = 0;
        while (i < length) {
            final String handlerClassName = fallback_HANDLERS[i];
            try {
                final Class<?> handlerClass = Class.forName(handlerClassName);
                return this.fallbackHandler = (URLStreamHandler)handlerClass.getDeclaredConstructor((Class<?>[])new Class[0]).newInstance(new Object[0]);
            }
            catch (Exception ex) {
                ++i;
                continue;
            }
            break;
        }
        throw new IllegalStateException("Unable to find fallback handler");
    }
    
    private void log(final boolean warning, final String message, final Exception cause) {
        try {
            final Level level = warning ? Level.WARNING : Level.FINEST;
            Logger.getLogger(this.getClass().getName()).log(level, message, cause);
        }
        catch (Exception ex) {
            if (warning) {
                System.err.println("WARNING: " + message);
            }
        }
    }
    
    @Override
    protected void parseURL(final URL context, final String spec, final int start, final int limit) {
        if (spec.regionMatches(true, 0, "jar:", 0, "jar:".length())) {
            this.setFile(context, this.getFileFromSpec(spec.substring(start, limit)));
        }
        else {
            this.setFile(context, this.getFileFromContext(context, spec.substring(start, limit)));
        }
    }
    
    private String getFileFromSpec(final String spec) {
        final int separatorIndex = spec.lastIndexOf("!/");
        if (separatorIndex == -1) {
            throw new IllegalArgumentException("No !/ in spec '" + spec + "'");
        }
        try {
            new URL(spec.substring(0, separatorIndex));
            return spec;
        }
        catch (MalformedURLException ex) {
            throw new IllegalArgumentException("Invalid spec URL '" + spec + "'", ex);
        }
    }
    
    private String getFileFromContext(final URL context, final String spec) {
        final String file = context.getFile();
        if (spec.startsWith("/")) {
            return this.trimToJarRoot(file) + "!/" + spec.substring(1);
        }
        if (file.endsWith("/")) {
            return file + spec;
        }
        final int lastSlashIndex = file.lastIndexOf(47);
        if (lastSlashIndex == -1) {
            throw new IllegalArgumentException("No / found in context URL's file '" + file + "'");
        }
        return file.substring(0, lastSlashIndex + 1) + spec;
    }
    
    private String trimToJarRoot(final String file) {
        final int lastSeparatorIndex = file.lastIndexOf("!/");
        if (lastSeparatorIndex == -1) {
            throw new IllegalArgumentException("No !/ found in context URL's file '" + file + "'");
        }
        return file.substring(0, lastSeparatorIndex);
    }
    
    private void setFile(final URL context, final String file) {
        String path = this.normalize(file);
        String query = null;
        final int queryIndex = path.lastIndexOf(63);
        if (queryIndex != -1) {
            query = path.substring(queryIndex + 1);
            path = path.substring(0, queryIndex);
        }
        this.setURL(context, "jar:", null, -1, null, null, path, query, context.getRef());
    }
    
    private String normalize(final String file) {
        if (!file.contains("/./") && !file.contains("/../")) {
            return file;
        }
        final int afterLastSeparatorIndex = file.lastIndexOf("!/") + "!/".length();
        String afterSeparator = file.substring(afterLastSeparatorIndex);
        afterSeparator = this.replaceParentDir(afterSeparator);
        afterSeparator = this.replaceCurrentDir(afterSeparator);
        return file.substring(0, afterLastSeparatorIndex) + afterSeparator;
    }
    
    private String replaceParentDir(String file) {
        int parentDirIndex;
        while ((parentDirIndex = file.indexOf("/../")) >= 0) {
            final int precedingSlashIndex = file.lastIndexOf(47, parentDirIndex - 1);
            if (precedingSlashIndex >= 0) {
                file = file.substring(0, precedingSlashIndex) + file.substring(parentDirIndex + 3);
            }
            else {
                file = file.substring(parentDirIndex + 4);
            }
        }
        return file;
    }
    
    private String replaceCurrentDir(final String file) {
        return Handler.CURRENT_DIR_PATTERN.matcher(file).replaceAll("/");
    }
    
    @Override
    protected int hashCode(final URL u) {
        return this.hashCode(u.getProtocol(), u.getFile());
    }
    
    private int hashCode(final String protocol, final String file) {
        int result = (protocol != null) ? protocol.hashCode() : 0;
        final int separatorIndex = file.indexOf("!/");
        if (separatorIndex == -1) {
            return result + file.hashCode();
        }
        final String source = file.substring(0, separatorIndex);
        final String entry = this.canonicalize(file.substring(separatorIndex + 2));
        try {
            result += new URL(source).hashCode();
        }
        catch (MalformedURLException ex) {
            result += source.hashCode();
        }
        result += entry.hashCode();
        return result;
    }
    
    @Override
    protected boolean sameFile(final URL u1, final URL u2) {
        if (!u1.getProtocol().equals("jar") || !u2.getProtocol().equals("jar")) {
            return false;
        }
        final int separator1 = u1.getFile().indexOf("!/");
        final int separator2 = u2.getFile().indexOf("!/");
        if (separator1 == -1 || separator2 == -1) {
            return super.sameFile(u1, u2);
        }
        final String nested1 = u1.getFile().substring(separator1 + "!/".length());
        final String nested2 = u2.getFile().substring(separator2 + "!/".length());
        if (!nested1.equals(nested2)) {
            final String canonical1 = this.canonicalize(nested1);
            final String canonical2 = this.canonicalize(nested2);
            if (!canonical1.equals(canonical2)) {
                return false;
            }
        }
        final String root1 = u1.getFile().substring(0, separator1);
        final String root2 = u2.getFile().substring(0, separator2);
        try {
            return super.sameFile(new URL(root1), new URL(root2));
        }
        catch (MalformedURLException ex) {
            return super.sameFile(u1, u2);
        }
    }
    
    private String canonicalize(final String path) {
        return Handler.SEPARATOR_PATTERN.matcher(path).replaceAll("/");
    }
    
    public JarFile getRootJarFileFromUrl(final URL url) throws IOException {
        final String spec = url.getFile();
        final int separatorIndex = spec.indexOf("!/");
        if (separatorIndex == -1) {
            throw new MalformedURLException("Jar URL does not contain !/ separator");
        }
        final String name = spec.substring(0, separatorIndex);
        return this.getRootJarFile(name);
    }
    
    private JarFile getRootJarFile(final String name) throws IOException {
        try {
            if (!name.startsWith("file:")) {
                throw new IllegalStateException("Not a file URL");
            }
            final File file = new File(URI.create(name));
            final Map<File, JarFile> cache = Handler.rootFileCache.get();
            JarFile result = (cache != null) ? cache.get(file) : null;
            if (result == null) {
                result = new JarFile(file);
                addToRootFileCache(file, result);
            }
            return result;
        }
        catch (Exception ex) {
            throw new IOException("Unable to open root Jar file '" + name + "'", ex);
        }
    }
    
    static void addToRootFileCache(final File sourceFile, final JarFile jarFile) {
        Map<File, JarFile> cache = Handler.rootFileCache.get();
        if (cache == null) {
            cache = new ConcurrentHashMap<File, JarFile>();
            Handler.rootFileCache = new SoftReference<Map<File, JarFile>>(cache);
        }
        cache.put(sourceFile, jarFile);
    }
    
    static void captureJarContextUrl() {
        if (canResetCachedUrlHandlers()) {
            final String handlers = System.getProperty("java.protocol.handler.pkgs", "");
            try {
                System.clearProperty("java.protocol.handler.pkgs");
                try {
                    resetCachedUrlHandlers();
                    Handler.jarContextUrl = new URL("jar:file:context.jar!/");
                    final URLConnection connection = Handler.jarContextUrl.openConnection();
                    if (connection instanceof JarURLConnection) {
                        Handler.jarContextUrl = null;
                    }
                }
                catch (Exception ex) {}
            }
            finally {
                if (handlers == null) {
                    System.clearProperty("java.protocol.handler.pkgs");
                }
                else {
                    System.setProperty("java.protocol.handler.pkgs", handlers);
                }
            }
            resetCachedUrlHandlers();
        }
    }
    
    private static boolean canResetCachedUrlHandlers() {
        try {
            resetCachedUrlHandlers();
            return true;
        }
        catch (Error ex) {
            return false;
        }
    }
    
    private static void resetCachedUrlHandlers() {
        URL.setURLStreamHandlerFactory(null);
    }
    
    public static void setUseFastConnectionExceptions(final boolean useFastConnectionExceptions) {
        JarURLConnection.setUseFastExceptions(useFastConnectionExceptions);
    }
    
    static {
        SEPARATOR_PATTERN = Pattern.compile("!/", 16);
        CURRENT_DIR_PATTERN = Pattern.compile("/./", 16);
        FALLBACK_HANDLERS = new String[] { "sun.net.www.protocol.jar.Handler" };
        Handler.rootFileCache = new SoftReference<Map<File, JarFile>>(null);
    }
}
