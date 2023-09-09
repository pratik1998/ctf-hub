// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.boot.loader.archive;

import java.util.function.Function;
import java.util.Collections;
import java.net.URI;
import java.util.NoSuchElementException;
import java.util.LinkedList;
import java.util.Deque;
import java.util.Comparator;
import java.util.Collection;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Iterator;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.jar.Manifest;
import java.io.File;
import java.util.Set;

public class ExplodedArchive implements Archive
{
    private static final Set<String> SKIPPED_NAMES;
    private final File root;
    private final boolean recursive;
    private File manifestFile;
    private Manifest manifest;
    
    public ExplodedArchive(final File root) {
        this(root, true);
    }
    
    public ExplodedArchive(final File root, final boolean recursive) {
        if (!root.exists() || !root.isDirectory()) {
            throw new IllegalArgumentException("Invalid source directory " + root);
        }
        this.root = root;
        this.recursive = recursive;
        this.manifestFile = this.getManifestFile(root);
    }
    
    private File getManifestFile(final File root) {
        final File metaInf = new File(root, "META-INF");
        return new File(metaInf, "MANIFEST.MF");
    }
    
    @Override
    public URL getUrl() throws MalformedURLException {
        return this.root.toURI().toURL();
    }
    
    @Override
    public Manifest getManifest() throws IOException {
        if (this.manifest == null && this.manifestFile.exists()) {
            try (final FileInputStream inputStream = new FileInputStream(this.manifestFile)) {
                this.manifest = new Manifest(inputStream);
            }
        }
        return this.manifest;
    }
    
    @Override
    public Iterator<Archive> getNestedArchives(final EntryFilter searchFilter, final EntryFilter includeFilter) throws IOException {
        return new ArchiveIterator(this.root, this.recursive, searchFilter, includeFilter);
    }
    
    @Deprecated
    @Override
    public Iterator<Entry> iterator() {
        return new EntryIterator(this.root, this.recursive, null, null);
    }
    
    protected Archive getNestedArchive(final Entry entry) throws IOException {
        final File file = ((FileEntry)entry).getFile();
        return file.isDirectory() ? new ExplodedArchive(file) : new SimpleJarFileArchive((FileEntry)entry);
    }
    
    @Override
    public boolean isExploded() {
        return true;
    }
    
    @Override
    public String toString() {
        try {
            return this.getUrl().toString();
        }
        catch (Exception ex) {
            return "exploded archive";
        }
    }
    
    static {
        SKIPPED_NAMES = new HashSet<String>(Arrays.asList(".", ".."));
    }
    
    private abstract static class AbstractIterator<T> implements Iterator<T>
    {
        private static final Comparator<File> entryComparator;
        private final File root;
        private final boolean recursive;
        private final EntryFilter searchFilter;
        private final EntryFilter includeFilter;
        private final Deque<Iterator<File>> stack;
        private FileEntry current;
        private String rootUrl;
        
        AbstractIterator(final File root, final boolean recursive, final EntryFilter searchFilter, final EntryFilter includeFilter) {
            this.stack = new LinkedList<Iterator<File>>();
            this.root = root;
            this.rootUrl = this.root.toURI().getPath();
            this.recursive = recursive;
            this.searchFilter = searchFilter;
            this.includeFilter = includeFilter;
            this.stack.add(this.listFiles(root));
            this.current = this.poll();
        }
        
        @Override
        public boolean hasNext() {
            return this.current != null;
        }
        
        @Override
        public T next() {
            final FileEntry entry = this.current;
            if (entry == null) {
                throw new NoSuchElementException();
            }
            this.current = this.poll();
            return this.adapt(entry);
        }
        
        private FileEntry poll() {
            while (!this.stack.isEmpty()) {
                while (this.stack.peek().hasNext()) {
                    final File file = this.stack.peek().next();
                    if (ExplodedArchive.SKIPPED_NAMES.contains(file.getName())) {
                        continue;
                    }
                    final FileEntry entry = this.getFileEntry(file);
                    if (this.isListable(entry)) {
                        this.stack.addFirst(this.listFiles(file));
                    }
                    if (this.includeFilter == null || this.includeFilter.matches(entry)) {
                        return entry;
                    }
                }
                this.stack.poll();
            }
            return null;
        }
        
        private FileEntry getFileEntry(final File file) {
            final URI uri = file.toURI();
            final String name = uri.getPath().substring(this.rootUrl.length());
            try {
                return new FileEntry(name, file, uri.toURL());
            }
            catch (MalformedURLException ex) {
                throw new IllegalStateException(ex);
            }
        }
        
        private boolean isListable(final FileEntry entry) {
            return entry.isDirectory() && (this.recursive || entry.getFile().getParentFile().equals(this.root)) && (this.searchFilter == null || this.searchFilter.matches(entry)) && (this.includeFilter == null || !this.includeFilter.matches(entry));
        }
        
        private Iterator<File> listFiles(final File file) {
            final File[] files = file.listFiles();
            if (files == null) {
                return Collections.emptyIterator();
            }
            Arrays.sort(files, AbstractIterator.entryComparator);
            return Arrays.asList(files).iterator();
        }
        
        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove");
        }
        
        protected abstract T adapt(final FileEntry entry);
        
        static {
            entryComparator = Comparator.comparing((Function<? super File, ? extends Comparable>)File::getAbsolutePath);
        }
    }
    
    private static class EntryIterator extends AbstractIterator<Entry>
    {
        EntryIterator(final File root, final boolean recursive, final EntryFilter searchFilter, final EntryFilter includeFilter) {
            super(root, recursive, searchFilter, includeFilter);
        }
        
        @Override
        protected Entry adapt(final FileEntry entry) {
            return entry;
        }
    }
    
    private static class ArchiveIterator extends AbstractIterator<Archive>
    {
        ArchiveIterator(final File root, final boolean recursive, final EntryFilter searchFilter, final EntryFilter includeFilter) {
            super(root, recursive, searchFilter, includeFilter);
        }
        
        @Override
        protected Archive adapt(final FileEntry entry) {
            final File file = entry.getFile();
            return file.isDirectory() ? new ExplodedArchive(file) : new SimpleJarFileArchive(entry);
        }
    }
    
    private static class FileEntry implements Entry
    {
        private final String name;
        private final File file;
        private final URL url;
        
        FileEntry(final String name, final File file, final URL url) {
            this.name = name;
            this.file = file;
            this.url = url;
        }
        
        File getFile() {
            return this.file;
        }
        
        @Override
        public boolean isDirectory() {
            return this.file.isDirectory();
        }
        
        @Override
        public String getName() {
            return this.name;
        }
        
        URL getUrl() {
            return this.url;
        }
    }
    
    private static class SimpleJarFileArchive implements Archive
    {
        private final URL url;
        
        SimpleJarFileArchive(final FileEntry file) {
            this.url = file.getUrl();
        }
        
        @Override
        public URL getUrl() throws MalformedURLException {
            return this.url;
        }
        
        @Override
        public Manifest getManifest() throws IOException {
            return null;
        }
        
        @Override
        public Iterator<Archive> getNestedArchives(final EntryFilter searchFilter, final EntryFilter includeFilter) throws IOException {
            return Collections.emptyIterator();
        }
        
        @Deprecated
        @Override
        public Iterator<Entry> iterator() {
            return Collections.emptyIterator();
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
    }
}
