// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.boot.loader.archive;

import java.util.Spliterators;
import java.util.Spliterator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.List;
import java.util.Iterator;
import java.io.IOException;
import java.util.jar.Manifest;
import java.net.MalformedURLException;
import java.net.URL;

public interface Archive extends Iterable<Entry>, AutoCloseable
{
    URL getUrl() throws MalformedURLException;
    
    Manifest getManifest() throws IOException;
    
    default Iterator<Archive> getNestedArchives(final EntryFilter searchFilter, final EntryFilter includeFilter) throws IOException {
        final EntryFilter combinedFilter = entry -> (searchFilter == null || searchFilter.matches(entry)) && (includeFilter == null || includeFilter.matches(entry));
        final List<Archive> nestedArchives = this.getNestedArchives(combinedFilter);
        return nestedArchives.iterator();
    }
    
    @Deprecated
    default List<Archive> getNestedArchives(final EntryFilter filter) throws IOException {
        throw new IllegalStateException("Unexpected call to getNestedArchives(filter)");
    }
    
    @Deprecated
    Iterator<Entry> iterator();
    
    @Deprecated
    default void forEach(final Consumer<? super Entry> action) {
        Objects.requireNonNull(action);
        for (final Entry entry : this) {
            action.accept(entry);
        }
    }
    
    @Deprecated
    default Spliterator<Entry> spliterator() {
        return Spliterators.spliteratorUnknownSize((Iterator<? extends Entry>)this.iterator(), 0);
    }
    
    default boolean isExploded() {
        return false;
    }
    
    default void close() throws Exception {
    }
    
    @FunctionalInterface
    public interface EntryFilter
    {
        boolean matches(final Entry entry);
    }
    
    public interface Entry
    {
        boolean isDirectory();
        
        String getName();
    }
}
