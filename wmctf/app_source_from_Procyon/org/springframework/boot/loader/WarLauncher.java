// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.boot.loader;

import org.springframework.boot.loader.archive.Archive;

public class WarLauncher extends ExecutableArchiveLauncher
{
    public WarLauncher() {
    }
    
    protected WarLauncher(final Archive archive) {
        super(archive);
    }
    
    @Override
    protected boolean isPostProcessingClassPathArchives() {
        return false;
    }
    
    public boolean isNestedArchive(final Archive.Entry entry) {
        if (entry.isDirectory()) {
            return entry.getName().equals("WEB-INF/classes/");
        }
        return entry.getName().startsWith("WEB-INF/lib/") || entry.getName().startsWith("WEB-INF/lib-provided/");
    }
    
    @Override
    protected String getArchiveEntryPathPrefix() {
        return "WEB-INF/";
    }
    
    public static void main(final String[] args) throws Exception {
        new WarLauncher().launch(args);
    }
}
