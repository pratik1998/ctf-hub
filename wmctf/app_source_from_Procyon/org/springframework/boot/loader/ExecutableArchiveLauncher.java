// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.boot.loader;

import java.util.List;
import java.util.Collection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.io.IOException;
import org.springframework.boot.loader.archive.ExplodedArchive;
import org.springframework.boot.loader.archive.Archive;

public abstract class ExecutableArchiveLauncher extends Launcher
{
    private static final String START_CLASS_ATTRIBUTE = "Start-Class";
    protected static final String BOOT_CLASSPATH_INDEX_ATTRIBUTE = "Spring-Boot-Classpath-Index";
    protected static final String DEFAULT_CLASSPATH_INDEX_FILE_NAME = "classpath.idx";
    private final Archive archive;
    private final ClassPathIndexFile classPathIndex;
    
    public ExecutableArchiveLauncher() {
        try {
            this.archive = this.createArchive();
            this.classPathIndex = this.getClassPathIndex(this.archive);
        }
        catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
    
    protected ExecutableArchiveLauncher(final Archive archive) {
        try {
            this.archive = archive;
            this.classPathIndex = this.getClassPathIndex(this.archive);
        }
        catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
    
    protected ClassPathIndexFile getClassPathIndex(final Archive archive) throws IOException {
        if (archive instanceof ExplodedArchive) {
            final String location = this.getClassPathIndexFileLocation(archive);
            return ClassPathIndexFile.loadIfPossible(archive.getUrl(), location);
        }
        return null;
    }
    
    private String getClassPathIndexFileLocation(final Archive archive) throws IOException {
        final Manifest manifest = archive.getManifest();
        final Attributes attributes = (manifest != null) ? manifest.getMainAttributes() : null;
        final String location = (attributes != null) ? attributes.getValue("Spring-Boot-Classpath-Index") : null;
        return (location != null) ? location : (this.getArchiveEntryPathPrefix() + "classpath.idx");
    }
    
    @Override
    protected String getMainClass() throws Exception {
        final Manifest manifest = this.archive.getManifest();
        String mainClass = null;
        if (manifest != null) {
            mainClass = manifest.getMainAttributes().getValue("Start-Class");
        }
        if (mainClass == null) {
            throw new IllegalStateException("No 'Start-Class' manifest entry specified in " + this);
        }
        return mainClass;
    }
    
    @Override
    protected ClassLoader createClassLoader(final Iterator<Archive> archives) throws Exception {
        final List<URL> urls = new ArrayList<URL>(this.guessClassPathSize());
        while (archives.hasNext()) {
            urls.add(archives.next().getUrl());
        }
        if (this.classPathIndex != null) {
            urls.addAll(this.classPathIndex.getUrls());
        }
        return this.createClassLoader(urls.toArray(new URL[0]));
    }
    
    private int guessClassPathSize() {
        if (this.classPathIndex != null) {
            return this.classPathIndex.size() + 10;
        }
        return 50;
    }
    
    @Override
    protected Iterator<Archive> getClassPathArchivesIterator() throws Exception {
        final Archive.EntryFilter searchFilter = this::isSearchCandidate;
        Iterator<Archive> archives = this.archive.getNestedArchives(searchFilter, entry -> this.isNestedArchive(entry) && !this.isEntryIndexed(entry));
        if (this.isPostProcessingClassPathArchives()) {
            archives = this.applyClassPathArchivePostProcessing(archives);
        }
        return archives;
    }
    
    private boolean isEntryIndexed(final Archive.Entry entry) {
        return this.classPathIndex != null && this.classPathIndex.containsEntry(entry.getName());
    }
    
    private Iterator<Archive> applyClassPathArchivePostProcessing(final Iterator<Archive> archives) throws Exception {
        final List<Archive> list = new ArrayList<Archive>();
        while (archives.hasNext()) {
            list.add(archives.next());
        }
        this.postProcessClassPathArchives(list);
        return list.iterator();
    }
    
    protected boolean isSearchCandidate(final Archive.Entry entry) {
        return this.getArchiveEntryPathPrefix() == null || entry.getName().startsWith(this.getArchiveEntryPathPrefix());
    }
    
    protected abstract boolean isNestedArchive(final Archive.Entry entry);
    
    protected boolean isPostProcessingClassPathArchives() {
        return true;
    }
    
    protected void postProcessClassPathArchives(final List<Archive> archives) throws Exception {
    }
    
    protected String getArchiveEntryPathPrefix() {
        return null;
    }
    
    @Override
    protected boolean isExploded() {
        return this.archive.isExploded();
    }
    
    @Override
    protected final Archive getArchive() {
        return this.archive;
    }
}
