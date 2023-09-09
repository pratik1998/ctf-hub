// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.boot.loader;

import org.springframework.boot.loader.archive.Archive;

public class JarLauncher extends ExecutableArchiveLauncher
{
    static final Archive.EntryFilter NESTED_ARCHIVE_ENTRY_FILTER;
    
    public JarLauncher() {
    }
    
    protected JarLauncher(final Archive archive) {
        super(archive);
    }
    
    @Override
    protected boolean isPostProcessingClassPathArchives() {
        return false;
    }
    
    @Override
    protected boolean isNestedArchive(final Archive.Entry entry) {
        return JarLauncher.NESTED_ARCHIVE_ENTRY_FILTER.matches(entry);
    }
    
    @Override
    protected String getArchiveEntryPathPrefix() {
        return "BOOT-INF/";
    }
    
    public static void main(final String[] args) throws Exception {
        new JarLauncher().launch(args);
    }
    
    static {
        NESTED_ARCHIVE_ENTRY_FILTER = (entry -> {
            if (entry.isDirectory()) {
                return entry.getName().equals("BOOT-INF/classes/");
            }
            else {
                return entry.getName().startsWith("BOOT-INF/lib/");
            }
        });
    }
}
