// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.boot.loader;

import java.util.Collection;
import org.springframework.boot.loader.archive.JarFileArchive;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.jar.Manifest;
import org.springframework.boot.loader.archive.ExplodedArchive;
import java.lang.reflect.Constructor;
import java.util.Set;
import java.util.LinkedHashSet;
import java.net.URLConnection;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import org.springframework.boot.loader.util.SystemPropertyUtils;
import java.util.Collections;
import java.io.InputStream;
import java.util.Iterator;
import java.util.ArrayList;
import org.springframework.boot.loader.archive.Archive;
import java.util.Properties;
import java.util.List;
import java.io.File;
import java.util.regex.Pattern;
import java.net.URL;

public class PropertiesLauncher extends Launcher
{
    private static final Class<?>[] PARENT_ONLY_PARAMS;
    private static final Class<?>[] URLS_AND_PARENT_PARAMS;
    private static final Class<?>[] NO_PARAMS;
    private static final URL[] NO_URLS;
    private static final String DEBUG = "loader.debug";
    public static final String MAIN = "loader.main";
    public static final String PATH = "loader.path";
    public static final String HOME = "loader.home";
    public static final String ARGS = "loader.args";
    public static final String CONFIG_NAME = "loader.config.name";
    public static final String CONFIG_LOCATION = "loader.config.location";
    public static final String SET_SYSTEM_PROPERTIES = "loader.system";
    private static final Pattern WORD_SEPARATOR;
    private static final String NESTED_ARCHIVE_SEPARATOR;
    private final File home;
    private List<String> paths;
    private final Properties properties;
    private final Archive parent;
    private volatile ClassPathArchives classPathArchives;
    
    public PropertiesLauncher() {
        this.paths = new ArrayList<String>();
        this.properties = new Properties();
        try {
            this.home = this.getHomeDirectory();
            this.initializeProperties();
            this.initializePaths();
            this.parent = this.createArchive();
        }
        catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
    
    protected File getHomeDirectory() {
        try {
            return new File(this.getPropertyWithDefault("loader.home", "${user.dir}"));
        }
        catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
    
    private void initializeProperties() throws Exception {
        final List<String> configs = new ArrayList<String>();
        if (this.getProperty("loader.config.location") != null) {
            configs.add(this.getProperty("loader.config.location"));
        }
        else {
            final String[] split;
            final String[] names = split = this.getPropertyWithDefault("loader.config.name", "loader").split(",");
            for (final String name : split) {
                configs.add("file:" + this.getHomeDirectory() + "/" + name + ".properties");
                configs.add("classpath:" + name + ".properties");
                configs.add("classpath:BOOT-INF/classes/" + name + ".properties");
            }
        }
        for (final String config : configs) {
            try (final InputStream resource = this.getResource(config)) {
                if (resource != null) {
                    this.debug("Found: " + config);
                    this.loadResource(resource);
                    return;
                }
                this.debug("Not found: " + config);
            }
        }
    }
    
    private void loadResource(final InputStream resource) throws Exception {
        this.properties.load(resource);
        for (final Object key : Collections.list(this.properties.propertyNames())) {
            final String text = this.properties.getProperty((String)key);
            final String value = SystemPropertyUtils.resolvePlaceholders(this.properties, text);
            if (value != null) {
                this.properties.put(key, value);
            }
        }
        if ("true".equals(this.getProperty("loader.system"))) {
            this.debug("Adding resolved properties to System properties");
            for (final Object key : Collections.list(this.properties.propertyNames())) {
                final String value2 = this.properties.getProperty((String)key);
                System.setProperty((String)key, value2);
            }
        }
    }
    
    private InputStream getResource(String config) throws Exception {
        if (config.startsWith("classpath:")) {
            return this.getClasspathResource(config.substring("classpath:".length()));
        }
        config = this.handleUrl(config);
        if (this.isUrl(config)) {
            return this.getURLResource(config);
        }
        return this.getFileResource(config);
    }
    
    private String handleUrl(String path) throws UnsupportedEncodingException {
        if (path.startsWith("jar:file:") || path.startsWith("file:")) {
            path = URLDecoder.decode(path, "UTF-8");
            if (path.startsWith("file:")) {
                path = path.substring("file:".length());
                if (path.startsWith("//")) {
                    path = path.substring(2);
                }
            }
        }
        return path;
    }
    
    private boolean isUrl(final String config) {
        return config.contains("://");
    }
    
    private InputStream getClasspathResource(String config) {
        while (config.startsWith("/")) {
            config = config.substring(1);
        }
        config = "/" + config;
        this.debug("Trying classpath: " + config);
        return this.getClass().getResourceAsStream(config);
    }
    
    private InputStream getFileResource(final String config) throws Exception {
        final File file = new File(config);
        this.debug("Trying file: " + config);
        if (file.canRead()) {
            return new FileInputStream(file);
        }
        return null;
    }
    
    private InputStream getURLResource(final String config) throws Exception {
        final URL url = new URL(config);
        if (this.exists(url)) {
            final URLConnection con = url.openConnection();
            try {
                return con.getInputStream();
            }
            catch (IOException ex) {
                if (con instanceof HttpURLConnection) {
                    ((HttpURLConnection)con).disconnect();
                }
                throw ex;
            }
        }
        return null;
    }
    
    private boolean exists(final URL url) throws IOException {
        final URLConnection connection = url.openConnection();
        try {
            connection.setUseCaches(connection.getClass().getSimpleName().startsWith("JNLP"));
            if (connection instanceof HttpURLConnection) {
                final HttpURLConnection httpConnection = (HttpURLConnection)connection;
                httpConnection.setRequestMethod("HEAD");
                final int responseCode = httpConnection.getResponseCode();
                if (responseCode == 200) {
                    return true;
                }
                if (responseCode == 404) {
                    return false;
                }
            }
            return connection.getContentLength() >= 0;
        }
        finally {
            if (connection instanceof HttpURLConnection) {
                ((HttpURLConnection)connection).disconnect();
            }
        }
    }
    
    private void initializePaths() throws Exception {
        final String path = this.getProperty("loader.path");
        if (path != null) {
            this.paths = this.parsePathsProperty(path);
        }
        this.debug("Nested archive paths: " + this.paths);
    }
    
    private List<String> parsePathsProperty(final String commaSeparatedPaths) {
        final List<String> paths = new ArrayList<String>();
        for (String path : commaSeparatedPaths.split(",")) {
            path = this.cleanupPath(path);
            path = ((path == null || path.isEmpty()) ? "/" : path);
            paths.add(path);
        }
        if (paths.isEmpty()) {
            paths.add("lib");
        }
        return paths;
    }
    
    protected String[] getArgs(String... args) throws Exception {
        final String loaderArgs = this.getProperty("loader.args");
        if (loaderArgs != null) {
            final String[] defaultArgs = loaderArgs.split("\\s+");
            final String[] additionalArgs = args;
            args = new String[defaultArgs.length + additionalArgs.length];
            System.arraycopy(defaultArgs, 0, args, 0, defaultArgs.length);
            System.arraycopy(additionalArgs, 0, args, defaultArgs.length, additionalArgs.length);
        }
        return args;
    }
    
    @Override
    protected String getMainClass() throws Exception {
        final String mainClass = this.getProperty("loader.main", "Start-Class");
        if (mainClass == null) {
            throw new IllegalStateException("No 'loader.main' or 'Start-Class' specified");
        }
        return mainClass;
    }
    
    @Override
    protected ClassLoader createClassLoader(final Iterator<Archive> archives) throws Exception {
        final String customLoaderClassName = this.getProperty("loader.classLoader");
        if (customLoaderClassName == null) {
            return super.createClassLoader(archives);
        }
        final Set<URL> urls = new LinkedHashSet<URL>();
        while (archives.hasNext()) {
            urls.add(archives.next().getUrl());
        }
        ClassLoader loader = new LaunchedURLClassLoader(urls.toArray(PropertiesLauncher.NO_URLS), this.getClass().getClassLoader());
        this.debug("Classpath for custom loader: " + urls);
        loader = this.wrapWithCustomClassLoader(loader, customLoaderClassName);
        this.debug("Using custom class loader: " + customLoaderClassName);
        return loader;
    }
    
    private ClassLoader wrapWithCustomClassLoader(final ClassLoader parent, final String className) throws Exception {
        final Class<ClassLoader> type = (Class<ClassLoader>)Class.forName(className, true, parent);
        ClassLoader classLoader = this.newClassLoader(type, PropertiesLauncher.PARENT_ONLY_PARAMS, parent);
        if (classLoader == null) {
            classLoader = this.newClassLoader(type, PropertiesLauncher.URLS_AND_PARENT_PARAMS, PropertiesLauncher.NO_URLS, parent);
        }
        if (classLoader == null) {
            classLoader = this.newClassLoader(type, PropertiesLauncher.NO_PARAMS, new Object[0]);
        }
        if (classLoader == null) {
            throw new IllegalArgumentException("Unable to create class loader for " + className);
        }
        return classLoader;
    }
    
    private ClassLoader newClassLoader(final Class<ClassLoader> loaderClass, final Class<?>[] parameterTypes, final Object... initargs) throws Exception {
        try {
            final Constructor<ClassLoader> constructor = loaderClass.getDeclaredConstructor(parameterTypes);
            constructor.setAccessible(true);
            return constructor.newInstance(initargs);
        }
        catch (NoSuchMethodException ex) {
            return null;
        }
    }
    
    private String getProperty(final String propertyKey) throws Exception {
        return this.getProperty(propertyKey, null, null);
    }
    
    private String getProperty(final String propertyKey, final String manifestKey) throws Exception {
        return this.getProperty(propertyKey, manifestKey, null);
    }
    
    private String getPropertyWithDefault(final String propertyKey, final String defaultValue) throws Exception {
        return this.getProperty(propertyKey, null, defaultValue);
    }
    
    private String getProperty(final String propertyKey, String manifestKey, final String defaultValue) throws Exception {
        if (manifestKey == null) {
            manifestKey = propertyKey.replace('.', '-');
            manifestKey = toCamelCase(manifestKey);
        }
        final String property = SystemPropertyUtils.getProperty(propertyKey);
        if (property != null) {
            final String value = SystemPropertyUtils.resolvePlaceholders(this.properties, property);
            this.debug("Property '" + propertyKey + "' from environment: " + value);
            return value;
        }
        if (this.properties.containsKey(propertyKey)) {
            final String value = SystemPropertyUtils.resolvePlaceholders(this.properties, this.properties.getProperty(propertyKey));
            this.debug("Property '" + propertyKey + "' from properties: " + value);
            return value;
        }
        try {
            if (this.home != null) {
                try (final ExplodedArchive archive = new ExplodedArchive(this.home, false)) {
                    final Manifest manifest = archive.getManifest();
                    if (manifest != null) {
                        final String value2 = manifest.getMainAttributes().getValue(manifestKey);
                        if (value2 != null) {
                            this.debug("Property '" + manifestKey + "' from home directory manifest: " + value2);
                            return SystemPropertyUtils.resolvePlaceholders(this.properties, value2);
                        }
                    }
                }
            }
        }
        catch (IllegalStateException ex) {}
        final Manifest manifest2 = this.createArchive().getManifest();
        if (manifest2 != null) {
            final String value3 = manifest2.getMainAttributes().getValue(manifestKey);
            if (value3 != null) {
                this.debug("Property '" + manifestKey + "' from archive manifest: " + value3);
                return SystemPropertyUtils.resolvePlaceholders(this.properties, value3);
            }
        }
        return (defaultValue != null) ? SystemPropertyUtils.resolvePlaceholders(this.properties, defaultValue) : defaultValue;
    }
    
    @Override
    protected Iterator<Archive> getClassPathArchivesIterator() throws Exception {
        ClassPathArchives classPathArchives = this.classPathArchives;
        if (classPathArchives == null) {
            classPathArchives = new ClassPathArchives();
            this.classPathArchives = classPathArchives;
        }
        return classPathArchives.iterator();
    }
    
    public static void main(String[] args) throws Exception {
        final PropertiesLauncher launcher = new PropertiesLauncher();
        args = launcher.getArgs(args);
        launcher.launch(args);
    }
    
    public static String toCamelCase(final CharSequence string) {
        if (string == null) {
            return null;
        }
        final StringBuilder builder = new StringBuilder();
        final Matcher matcher = PropertiesLauncher.WORD_SEPARATOR.matcher(string);
        int pos = 0;
        while (matcher.find()) {
            builder.append(capitalize(string.subSequence(pos, matcher.end()).toString()));
            pos = matcher.end();
        }
        builder.append(capitalize(string.subSequence(pos, string.length()).toString()));
        return builder.toString();
    }
    
    private static String capitalize(final String str) {
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
    
    private void debug(final String message) {
        if (Boolean.getBoolean("loader.debug")) {
            System.out.println(message);
        }
    }
    
    private String cleanupPath(String path) {
        path = path.trim();
        if (path.startsWith("./")) {
            path = path.substring(2);
        }
        final String lowerCasePath = path.toLowerCase(Locale.ENGLISH);
        if (lowerCasePath.endsWith(".jar") || lowerCasePath.endsWith(".zip")) {
            return path;
        }
        if (path.endsWith("/*")) {
            path = path.substring(0, path.length() - 1);
        }
        else if (!path.endsWith("/") && !path.equals(".")) {
            path += "/";
        }
        return path;
    }
    
    void close() throws Exception {
        if (this.classPathArchives != null) {
            this.classPathArchives.close();
        }
        if (this.parent != null) {
            this.parent.close();
        }
    }
    
    static {
        PARENT_ONLY_PARAMS = new Class[] { ClassLoader.class };
        URLS_AND_PARENT_PARAMS = new Class[] { URL[].class, ClassLoader.class };
        NO_PARAMS = new Class[0];
        NO_URLS = new URL[0];
        WORD_SEPARATOR = Pattern.compile("\\W+");
        NESTED_ARCHIVE_SEPARATOR = "!" + File.separator;
    }
    
    private class ClassPathArchives implements Iterable<Archive>
    {
        private final List<Archive> classPathArchives;
        private final List<JarFileArchive> jarFileArchives;
        
        ClassPathArchives() throws Exception {
            this.jarFileArchives = new ArrayList<JarFileArchive>();
            this.classPathArchives = new ArrayList<Archive>();
            for (final String path : PropertiesLauncher.this.paths) {
                for (final Archive archive : this.getClassPathArchives(path)) {
                    this.addClassPathArchive(archive);
                }
            }
            this.addNestedEntries();
        }
        
        private void addClassPathArchive(final Archive archive) throws IOException {
            if (!(archive instanceof ExplodedArchive)) {
                this.classPathArchives.add(archive);
                return;
            }
            this.classPathArchives.add(archive);
            this.classPathArchives.addAll(this.asList(archive.getNestedArchives(null, new ArchiveEntryFilter())));
        }
        
        private List<Archive> getClassPathArchives(final String path) throws Exception {
            final String root = PropertiesLauncher.this.cleanupPath(PropertiesLauncher.this.handleUrl(path));
            final List<Archive> lib = new ArrayList<Archive>();
            File file = new File(root);
            if (!"/".equals(root)) {
                if (!this.isAbsolutePath(root)) {
                    file = new File(PropertiesLauncher.this.home, root);
                }
                if (file.isDirectory()) {
                    PropertiesLauncher.this.debug("Adding classpath entries from " + file);
                    final Archive archive = new ExplodedArchive(file, false);
                    lib.add(archive);
                }
            }
            final Archive archive = this.getArchive(file);
            if (archive != null) {
                PropertiesLauncher.this.debug("Adding classpath entries from archive " + archive.getUrl() + root);
                lib.add(archive);
            }
            final List<Archive> nestedArchives = this.getNestedArchives(root);
            if (nestedArchives != null) {
                PropertiesLauncher.this.debug("Adding classpath entries from nested " + root);
                lib.addAll(nestedArchives);
            }
            return lib;
        }
        
        private boolean isAbsolutePath(final String root) {
            return root.contains(":") || root.startsWith("/");
        }
        
        private Archive getArchive(final File file) throws IOException {
            if (this.isNestedArchivePath(file)) {
                return null;
            }
            final String name = file.getName().toLowerCase(Locale.ENGLISH);
            if (name.endsWith(".jar") || name.endsWith(".zip")) {
                return this.getJarFileArchive(file);
            }
            return null;
        }
        
        private boolean isNestedArchivePath(final File file) {
            return file.getPath().contains(PropertiesLauncher.NESTED_ARCHIVE_SEPARATOR);
        }
        
        private List<Archive> getNestedArchives(final String path) throws Exception {
            Archive parent = PropertiesLauncher.this.parent;
            String root = path;
            if ((!root.equals("/") && root.startsWith("/")) || parent.getUrl().toURI().equals(PropertiesLauncher.this.home.toURI())) {
                return null;
            }
            final int index = root.indexOf(33);
            if (index != -1) {
                File file = new File(PropertiesLauncher.this.home, root.substring(0, index));
                if (root.startsWith("jar:file:")) {
                    file = new File(root.substring("jar:file:".length(), index));
                }
                parent = this.getJarFileArchive(file);
                for (root = root.substring(index + 1); root.startsWith("/"); root = root.substring(1)) {}
            }
            if (root.endsWith(".jar")) {
                final File file = new File(PropertiesLauncher.this.home, root);
                if (file.exists()) {
                    parent = this.getJarFileArchive(file);
                    root = "";
                }
            }
            if (root.equals("/") || root.equals("./") || root.equals(".")) {
                root = "";
            }
            final Archive.EntryFilter filter = new PrefixMatchingArchiveFilter(root);
            final List<Archive> archives = this.asList(parent.getNestedArchives(null, filter));
            if ((root == null || root.isEmpty() || ".".equals(root)) && !path.endsWith(".jar") && parent != PropertiesLauncher.this.parent) {
                archives.add(parent);
            }
            return archives;
        }
        
        private void addNestedEntries() {
            try {
                final Iterator<Archive> archives = PropertiesLauncher.this.parent.getNestedArchives(null, JarLauncher.NESTED_ARCHIVE_ENTRY_FILTER);
                while (archives.hasNext()) {
                    this.classPathArchives.add(archives.next());
                }
            }
            catch (IOException ex) {}
        }
        
        private List<Archive> asList(final Iterator<Archive> iterator) {
            final List<Archive> list = new ArrayList<Archive>();
            while (iterator.hasNext()) {
                list.add(iterator.next());
            }
            return list;
        }
        
        private JarFileArchive getJarFileArchive(final File file) throws IOException {
            final JarFileArchive archive = new JarFileArchive(file);
            this.jarFileArchives.add(archive);
            return archive;
        }
        
        @Override
        public Iterator<Archive> iterator() {
            return this.classPathArchives.iterator();
        }
        
        void close() throws IOException {
            for (final JarFileArchive archive : this.jarFileArchives) {
                archive.close();
            }
        }
    }
    
    private static final class PrefixMatchingArchiveFilter implements Archive.EntryFilter
    {
        private final String prefix;
        private final ArchiveEntryFilter filter;
        
        private PrefixMatchingArchiveFilter(final String prefix) {
            this.filter = new ArchiveEntryFilter();
            this.prefix = prefix;
        }
        
        @Override
        public boolean matches(final Archive.Entry entry) {
            if (entry.isDirectory()) {
                return entry.getName().equals(this.prefix);
            }
            return entry.getName().startsWith(this.prefix) && this.filter.matches(entry);
        }
    }
    
    private static final class ArchiveEntryFilter implements Archive.EntryFilter
    {
        private static final String DOT_JAR = ".jar";
        private static final String DOT_ZIP = ".zip";
        
        @Override
        public boolean matches(final Archive.Entry entry) {
            return entry.getName().endsWith(".jar") || entry.getName().endsWith(".zip");
        }
    }
}
