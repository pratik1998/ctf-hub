// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.boot.loader.archive;

import java.util.Set;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.FileSystem;
import java.io.OutputStream;
import java.io.InputStream;
import java.nio.file.StandardOpenOption;
import java.nio.file.OpenOption;
import java.util.UUID;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;
import java.util.Iterator;
import java.util.jar.Manifest;
import java.net.MalformedURLException;
import java.io.IOException;
import java.io.File;
import java.nio.file.Path;
import java.net.URL;
import org.springframework.boot.loader.jar.JarFile;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.nio.file.attribute.FileAttribute;

public class JarFileArchive implements Archive
{
    private static final String UNPACK_MARKER = "UNPACK:";
    private static final int BUFFER_SIZE = 32768;
    private static final FileAttribute<?>[] NO_FILE_ATTRIBUTES;
    private static final EnumSet<PosixFilePermission> DIRECTORY_PERMISSIONS;
    private static final EnumSet<PosixFilePermission> FILE_PERMISSIONS;
    private final JarFile jarFile;
    private URL url;
    private Path tempUnpackDirectory;
    
    public JarFileArchive(final File file) throws IOException {
        this(file, file.toURI().toURL());
    }
    
    public JarFileArchive(final File file, final URL url) throws IOException {
        this(new JarFile(file));
        this.url = url;
    }
    
    public JarFileArchive(final JarFile jarFile) {
        this.jarFile = jarFile;
    }
    
    @Override
    public URL getUrl() throws MalformedURLException {
        if (this.url != null) {
            return this.url;
        }
        return this.jarFile.getUrl();
    }
    
    @Override
    public Manifest getManifest() throws IOException {
        return this.jarFile.getManifest();
    }
    
    @Override
    public Iterator<Archive> getNestedArchives(final EntryFilter searchFilter, final EntryFilter includeFilter) throws IOException {
        return new NestedArchiveIterator(this.jarFile.iterator(), searchFilter, includeFilter);
    }
    
    @Deprecated
    @Override
    public Iterator<Entry> iterator() {
        return new EntryIterator(this.jarFile.iterator(), null, null);
    }
    
    @Override
    public void close() throws IOException {
        this.jarFile.close();
    }
    
    protected Archive getNestedArchive(final Entry entry) throws IOException {
        final JarEntry jarEntry = ((JarFileEntry)entry).getJarEntry();
        if (jarEntry.getComment().startsWith("UNPACK:")) {
            return this.getUnpackedNestedArchive(jarEntry);
        }
        try {
            final JarFile jarFile = this.jarFile.getNestedJarFile(jarEntry);
            return new JarFileArchive(jarFile);
        }
        catch (Exception ex) {
            throw new IllegalStateException("Failed to get nested archive for entry " + entry.getName(), ex);
        }
    }
    
    private Archive getUnpackedNestedArchive(final JarEntry jarEntry) throws IOException {
        String name = jarEntry.getName();
        if (name.lastIndexOf(47) != -1) {
            name = name.substring(name.lastIndexOf(47) + 1);
        }
        final Path path = this.getTempUnpackDirectory().resolve(name);
        if (!Files.exists(path, new LinkOption[0]) || Files.size(path) != jarEntry.getSize()) {
            this.unpack(jarEntry, path);
        }
        return new JarFileArchive(path.toFile(), path.toUri().toURL());
    }
    
    private Path getTempUnpackDirectory() {
        if (this.tempUnpackDirectory == null) {
            final Path tempDirectory = Paths.get(System.getProperty("java.io.tmpdir"), new String[0]);
            this.tempUnpackDirectory = this.createUnpackDirectory(tempDirectory);
        }
        return this.tempUnpackDirectory;
    }
    
    private Path createUnpackDirectory(final Path parent) {
        int attempts = 0;
        while (attempts++ < 1000) {
            final String fileName = Paths.get(this.jarFile.getName(), new String[0]).getFileName().toString();
            final Path unpackDirectory = parent.resolve(fileName + "-spring-boot-libs-" + UUID.randomUUID());
            try {
                this.createDirectory(unpackDirectory);
                return unpackDirectory;
            }
            catch (IOException ex) {
                continue;
            }
            break;
        }
        throw new IllegalStateException("Failed to create unpack directory in directory '" + parent + "'");
    }
    
    private void unpack(final JarEntry entry, final Path path) throws IOException {
        this.createFile(path);
        path.toFile().deleteOnExit();
        try (final InputStream inputStream = this.jarFile.getInputStream(entry);
             final OutputStream outputStream = Files.newOutputStream(path, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            final byte[] buffer = new byte[32768];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
        }
    }
    
    private void createDirectory(final Path path) throws IOException {
        Files.createDirectory(path, this.getFileAttributes(path.getFileSystem(), JarFileArchive.DIRECTORY_PERMISSIONS));
    }
    
    private void createFile(final Path path) throws IOException {
        Files.createFile(path, this.getFileAttributes(path.getFileSystem(), JarFileArchive.FILE_PERMISSIONS));
    }
    
    private FileAttribute<?>[] getFileAttributes(final FileSystem fileSystem, final EnumSet<PosixFilePermission> ownerReadWrite) {
        if (!fileSystem.supportedFileAttributeViews().contains("posix")) {
            return JarFileArchive.NO_FILE_ATTRIBUTES;
        }
        return (FileAttribute<?>[])new FileAttribute[] { PosixFilePermissions.asFileAttribute(ownerReadWrite) };
    }
    
    @Override
    public String toString() {
        try {
            return this.getUrl().toString();
        }
        catch (Exception ex) {
            return "jar archive";
        }
    }
    
    static {
        NO_FILE_ATTRIBUTES = new FileAttribute[0];
        DIRECTORY_PERMISSIONS = EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE);
        FILE_PERMISSIONS = EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
    }
    
    private abstract static class AbstractIterator<T> implements Iterator<T>
    {
        private final Iterator<JarEntry> iterator;
        private final EntryFilter searchFilter;
        private final EntryFilter includeFilter;
        private Entry current;
        
        AbstractIterator(final Iterator<JarEntry> iterator, final EntryFilter searchFilter, final EntryFilter includeFilter) {
            this.iterator = iterator;
            this.searchFilter = searchFilter;
            this.includeFilter = includeFilter;
            this.current = this.poll();
        }
        
        @Override
        public boolean hasNext() {
            return this.current != null;
        }
        
        @Override
        public T next() {
            final T result = this.adapt(this.current);
            this.current = this.poll();
            return result;
        }
        
        private Entry poll() {
            while (this.iterator.hasNext()) {
                final JarFileEntry candidate = new JarFileEntry(this.iterator.next());
                if ((this.searchFilter == null || this.searchFilter.matches(candidate)) && (this.includeFilter == null || this.includeFilter.matches(candidate))) {
                    return candidate;
                }
            }
            return null;
        }
        
        protected abstract T adapt(final Entry entry);
    }
    
    private static class EntryIterator extends AbstractIterator<Entry>
    {
        EntryIterator(final Iterator<JarEntry> iterator, final EntryFilter searchFilter, final EntryFilter includeFilter) {
            super(iterator, searchFilter, includeFilter);
        }
        
        @Override
        protected Entry adapt(final Entry entry) {
            return entry;
        }
    }
    
    private class NestedArchiveIterator extends AbstractIterator<Archive>
    {
        NestedArchiveIterator(final Iterator<JarEntry> iterator, final EntryFilter searchFilter, final EntryFilter includeFilter) {
            super(iterator, searchFilter, includeFilter);
        }
        
        @Override
        protected Archive adapt(final Entry entry) {
            try {
                return JarFileArchive.this.getNestedArchive(entry);
            }
            catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }
    
    private static class JarFileEntry implements Entry
    {
        private final JarEntry jarEntry;
        
        JarFileEntry(final JarEntry jarEntry) {
            this.jarEntry = jarEntry;
        }
        
        JarEntry getJarEntry() {
            return this.jarEntry;
        }
        
        @Override
        public boolean isDirectory() {
            return this.jarEntry.isDirectory();
        }
        
        @Override
        public String getName() {
            return this.jarEntry.getName();
        }
    }
}
