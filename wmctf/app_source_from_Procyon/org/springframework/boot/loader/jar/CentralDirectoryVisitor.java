// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.boot.loader.jar;

import org.springframework.boot.loader.data.RandomAccessData;

interface CentralDirectoryVisitor
{
    void visitStart(final CentralDirectoryEndRecord endRecord, final RandomAccessData centralDirectoryData);
    
    void visitFileHeader(final CentralDirectoryFileHeader fileHeader, final long dataOffset);
    
    void visitEnd();
}
