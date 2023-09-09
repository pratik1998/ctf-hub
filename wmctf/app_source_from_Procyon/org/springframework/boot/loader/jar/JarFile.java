// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.boot.loader.jar;

import java.net.URLStreamHandlerFactory;
import java.net.URLStreamHandler;
import java.net.MalformedURLException;
import java.util.zip.ZipEntry;
import java.util.Spliterator;
import java.util.stream.StreamSupport;
import java.util.Iterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.Enumeration;
import java.io.FilePermission;
import java.security.Permission;
import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.lang.ref.SoftReference;
import java.util.jar.Manifest;
import java.util.function.Supplier;
import java.net.URL;
import org.springframework.boot.loader.data.RandomAccessData;
import org.springframework.boot.loader.data.RandomAccessDataFile;
import java.util.jar.JarEntry;

public class JarFile extends AbstractJarFile implements Iterable<JarEntry>
{
    private static final String MANIFEST_NAME = "META-INF/MANIFEST.MF";
    private static final String PROTOCOL_HANDLER = "java.protocol.handler.pkgs";
    private static final String HANDLERS_PACKAGE = "org.springframework.boot.loader";
    private static final AsciiBytes META_INF;
    private static final AsciiBytes SIGNATURE_FILE_EXTENSION;
    private static final String READ_ACTION = "read";
    private final RandomAccessDataFile rootFile;
    private final String pathFromRoot;
    private final RandomAccessData data;
    private final JarFileType type;
    private URL url;
    private String urlString;
    private JarFileEntries entries;
    private Supplier<Manifest> manifestSupplier;
    private SoftReference<Manifest> manifest;
    private boolean signed;
    private String comment;
    private volatile boolean closed;
    private volatile JarFileWrapper wrapper;
    
    public JarFile(final File file) throws IOException {
        this(new RandomAccessDataFile(file));
    }
    
    JarFile(final RandomAccessDataFile file) throws IOException {
        this(file, "", file, JarFileType.DIRECT);
    }
    
    private JarFile(final RandomAccessDataFile rootFile, final String pathFromRoot, final RandomAccessData data, final JarFileType type) throws IOException {
        this(rootFile, pathFromRoot, data, null, type, null);
    }
    
    private JarFile(final RandomAccessDataFile rootFile, final String pathFromRoot, final RandomAccessData data, final JarEntryFilter filter, final JarFileType type, final Supplier<Manifest> manifestSupplier) throws IOException {
        super(rootFile.getFile());
        if (System.getSecurityManager() == null) {
            super.close();
        }
        this.rootFile = rootFile;
        this.pathFromRoot = pathFromRoot;
        final CentralDirectoryParser parser = new CentralDirectoryParser();
        this.entries = parser.addVisitor(new JarFileEntries(this, filter));
        this.type = type;
        parser.addVisitor(this.centralDirectoryVisitor());
        try {
            this.data = parser.parse(data, filter == null);
        }
        catch (RuntimeException ex) {
            try {
                this.rootFile.close();
                super.close();
            }
            catch (IOException ex3) {}
            throw ex;
        }
        InputStream inputStream;
        final Object o;
        Manifest manifest;
        final Throwable t2;
        this.manifestSupplier = ((manifestSupplier != null) ? manifestSupplier : (() -> {
            try {
                inputStream = this.getInputStream("META-INF/MANIFEST.MF");
                try {
                    if (inputStream == null) {
                        return (Manifest)o;
                    }
                    else {
                        manifest = new Manifest(inputStream);
                        return manifest;
                    }
                }
                catch (Throwable t) {
                    throw t;
                }
                finally {
                    if (inputStream != null) {
                        if (t2 != null) {
                            try {
                                inputStream.close();
                            }
                            catch (Throwable exception) {
                                t2.addSuppressed(exception);
                            }
                        }
                        else {
                            inputStream.close();
                        }
                    }
                }
            }
            catch (IOException ex2) {
                throw new RuntimeException(ex2);
            }
        }));
    }
    
    private CentralDirectoryVisitor centralDirectoryVisitor() {
        return new CentralDirectoryVisitor() {
            @Override
            public void visitStart(final CentralDirectoryEndRecord endRecord, final RandomAccessData centralDirectoryData) {
                JarFile.this.comment = endRecord.getComment();
            }
            
            @Override
            public void visitFileHeader(final CentralDirectoryFileHeader fileHeader, final long dataOffset) {
                final AsciiBytes name = fileHeader.getName();
                if (name.startsWith(JarFile.META_INF) && name.endsWith(JarFile.SIGNATURE_FILE_EXTENSION)) {
                    JarFile.this.signed = true;
                }
            }
            
            @Override
            public void visitEnd() {
            }
        };
    }
    
    JarFileWrapper getWrapper() throws IOException {
        JarFileWrapper wrapper = this.wrapper;
        if (wrapper == null) {
            wrapper = new JarFileWrapper(this);
            this.wrapper = wrapper;
        }
        return wrapper;
    }
    
    @Override
    Permission getPermission() {
        return new FilePermission(this.rootFile.getFile().getPath(), "read");
    }
    
    protected final RandomAccessDataFile getRootJarFile() {
        return this.rootFile;
    }
    
    RandomAccessData getData() {
        return this.data;
    }
    
    @Override
    public Manifest getManifest() throws IOException {
        Manifest manifest = (this.manifest != null) ? this.manifest.get() : null;
        if (manifest == null) {
            try {
                manifest = this.manifestSupplier.get();
            }
            catch (RuntimeException ex) {
                throw new IOException(ex);
            }
            this.manifest = new SoftReference<Manifest>(manifest);
        }
        return manifest;
    }
    
    @Override
    public Enumeration<JarEntry> entries() {
        return new JarEntryEnumeration(this.entries.iterator());
    }
    
    @Override
    public Stream<JarEntry> stream() {
        final Spliterator<JarEntry> spliterator = Spliterators.spliterator((Iterator<? extends JarEntry>)this.iterator(), (long)this.size(), 1297);
        return StreamSupport.stream(spliterator, false);
    }
    
    @Override
    public Iterator<JarEntry> iterator() {
        return (Iterator<JarEntry>)this.entries.iterator(this::ensureOpen);
    }
    
    public org.springframework.boot.loader.jar.JarEntry getJarEntry(final CharSequence name) {
        return this.entries.getEntry(name);
    }
    
    @Override
    public org.springframework.boot.loader.jar.JarEntry getJarEntry(final String name) {
        return (org.springframework.boot.loader.jar.JarEntry)this.getEntry(name);
    }
    
    public boolean containsEntry(final String name) {
        return this.entries.containsEntry(name);
    }
    
    @Override
    public ZipEntry getEntry(final String name) {
        this.ensureOpen();
        return this.entries.getEntry(name);
    }
    
    @Override
    InputStream getInputStream() throws IOException {
        return this.data.getInputStream();
    }
    
    @Override
    public synchronized InputStream getInputStream(final ZipEntry entry) throws IOException {
        this.ensureOpen();
        if (entry instanceof org.springframework.boot.loader.jar.JarEntry) {
            return this.entries.getInputStream((FileHeader)entry);
        }
        return this.getInputStream((entry != null) ? entry.getName() : null);
    }
    
    InputStream getInputStream(final String name) throws IOException {
        return this.entries.getInputStream(name);
    }
    
    public synchronized JarFile getNestedJarFile(final ZipEntry entry) throws IOException {
        return this.getNestedJarFile((org.springframework.boot.loader.jar.JarEntry)entry);
    }
    
    public synchronized JarFile getNestedJarFile(final org.springframework.boot.loader.jar.JarEntry entry) throws IOException {
        try {
            return this.createJarFileFromEntry(entry);
        }
        catch (Exception ex) {
            throw new IOException("Unable to open nested jar file '" + entry.getName() + "'", ex);
        }
    }
    
    private JarFile createJarFileFromEntry(final org.springframework.boot.loader.jar.JarEntry entry) throws IOException {
        if (entry.isDirectory()) {
            return this.createJarFileFromDirectoryEntry(entry);
        }
        return this.createJarFileFromFileEntry(entry);
    }
    
    private JarFile createJarFileFromDirectoryEntry(final org.springframework.boot.loader.jar.JarEntry entry) throws IOException {
        final AsciiBytes name = entry.getAsciiBytesName();
        final AsciiBytes asciiBytes;
        final JarEntryFilter filter = candidate -> {
            if (candidate.startsWith(asciiBytes) && !candidate.equals(asciiBytes)) {
                return candidate.substring(asciiBytes.length());
            }
            else {
                return null;
            }
        };
        return new JarFile(this.rootFile, this.pathFromRoot + "!/" + entry.getName().substring(0, name.length() - 1), this.data, filter, JarFileType.NESTED_DIRECTORY, this.manifestSupplier);
    }
    
    private JarFile createJarFileFromFileEntry(final org.springframework.boot.loader.jar.JarEntry entry) throws IOException {
        if (entry.getMethod() != 0) {
            throw new IllegalStateException("Unable to open nested entry '" + entry.getName() + "'. It has been compressed and nested jar files must be stored without compression. Please check the mechanism used to create your executable jar file");
        }
        final RandomAccessData entryData = this.entries.getEntryData(entry.getName());
        return new JarFile(this.rootFile, this.pathFromRoot + "!/" + entry.getName(), entryData, JarFileType.NESTED_JAR);
    }
    
    @Override
    public String getComment() {
        this.ensureOpen();
        return this.comment;
    }
    
    @Override
    public int size() {
        this.ensureOpen();
        return this.entries.getSize();
    }
    
    @Override
    public void close() throws IOException {
        if (this.closed) {
            return;
        }
        super.close();
        if (this.type == JarFileType.DIRECT) {
            this.rootFile.close();
        }
        this.closed = true;
    }
    
    private void ensureOpen() {
        if (this.closed) {
            throw new IllegalStateException("zip file closed");
        }
    }
    
    boolean isClosed() {
        return this.closed;
    }
    
    String getUrlString() throws MalformedURLException {
        if (this.urlString == null) {
            this.urlString = this.getUrl().toString();
        }
        return this.urlString;
    }
    
    public URL getUrl() throws MalformedURLException {
        if (this.url == null) {
            String file = this.rootFile.getFile().toURI() + this.pathFromRoot + "!/";
            file = file.replace("file:////", "file://");
            this.url = new URL("jar", "", -1, file, new Handler(this));
        }
        return this.url;
    }
    
    @Override
    public String toString() {
        return this.getName();
    }
    
    @Override
    public String getName() {
        return this.rootFile.getFile() + this.pathFromRoot;
    }
    
    boolean isSigned() {
        return this.signed;
    }
    
    JarEntryCertification getCertification(final org.springframework.boot.loader.jar.JarEntry entry) {
        try {
            return this.entries.getCertification(entry);
        }
        catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
    
    public void clearCache() {
        this.entries.clearCache();
    }
    
    protected String getPathFromRoot() {
        return this.pathFromRoot;
    }
    
    @Override
    JarFileType getType() {
        return this.type;
    }
    
    public static void registerUrlProtocolHandler() {
        Handler.captureJarContextUrl();
        final String handlers = System.getProperty("java.protocol.handler.pkgs", "");
        System.setProperty("java.protocol.handler.pkgs", (handlers == null || handlers.isEmpty()) ? "org.springframework.boot.loader" : (handlers + "|" + "org.springframework.boot.loader"));
        resetCachedUrlHandlers();
    }
    
    private static void resetCachedUrlHandlers() {
        try {
            URL.setURLStreamHandlerFactory(null);
        }
        catch (Error error) {}
    }
    
    static {
        META_INF = new AsciiBytes("META-INF/");
        SIGNATURE_FILE_EXTENSION = new AsciiBytes(".SF");
    }
    
    private static class JarEntryEnumeration implements Enumeration<JarEntry>
    {
        private final Iterator<org.springframework.boot.loader.jar.JarEntry> iterator;
        
        JarEntryEnumeration(final Iterator<org.springframework.boot.loader.jar.JarEntry> iterator) {
            this.iterator = iterator;
        }
        
        @Override
        public boolean hasMoreElements() {
            return this.iterator.hasNext();
        }
        
        @Override
        public JarEntry nextElement() {
            return this.iterator.next();
        }
    }
}
