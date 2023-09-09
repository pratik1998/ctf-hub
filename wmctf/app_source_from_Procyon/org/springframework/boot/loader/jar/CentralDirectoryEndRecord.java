// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.boot.loader.jar;

import java.io.IOException;
import org.springframework.boot.loader.data.RandomAccessData;

class CentralDirectoryEndRecord
{
    private static final int MINIMUM_SIZE = 22;
    private static final int MAXIMUM_COMMENT_LENGTH = 65535;
    private static final int MAXIMUM_SIZE = 65557;
    private static final int SIGNATURE = 101010256;
    private static final int COMMENT_LENGTH_OFFSET = 20;
    private static final int READ_BLOCK_SIZE = 256;
    private final Zip64End zip64End;
    private byte[] block;
    private int offset;
    private int size;
    
    CentralDirectoryEndRecord(final RandomAccessData data) throws IOException {
        this.block = this.createBlockFromEndOfData(data, 256);
        this.size = 22;
        this.offset = this.block.length - this.size;
        while (!this.isValid()) {
            ++this.size;
            if (this.size > this.block.length) {
                if (this.size >= 65557 || this.size > data.getSize()) {
                    throw new IOException("Unable to find ZIP central directory records after reading " + this.size + " bytes");
                }
                this.block = this.createBlockFromEndOfData(data, this.size + 256);
            }
            this.offset = this.block.length - this.size;
        }
        final long startOfCentralDirectoryEndRecord = data.getSize() - this.size;
        final Zip64Locator zip64Locator = find(data, startOfCentralDirectoryEndRecord);
        this.zip64End = ((zip64Locator != null) ? new Zip64End(data, zip64Locator) : null);
    }
    
    private byte[] createBlockFromEndOfData(final RandomAccessData data, final int size) throws IOException {
        final int length = (int)Math.min(data.getSize(), size);
        return data.read(data.getSize() - length, length);
    }
    
    private boolean isValid() {
        if (this.block.length < 22 || Bytes.littleEndianValue(this.block, this.offset + 0, 4) != 101010256L) {
            return false;
        }
        final long commentLength = Bytes.littleEndianValue(this.block, this.offset + 20, 2);
        return this.size == 22L + commentLength;
    }
    
    long getStartOfArchive(final RandomAccessData data) {
        final long length = Bytes.littleEndianValue(this.block, this.offset + 12, 4);
        final long specifiedOffset = (this.zip64End != null) ? this.zip64End.centralDirectoryOffset : Bytes.littleEndianValue(this.block, this.offset + 16, 4);
        final long zip64EndSize = (this.zip64End != null) ? this.zip64End.getSize() : 0L;
        final int zip64LocSize = (this.zip64End != null) ? 20 : 0;
        final long actualOffset = data.getSize() - this.size - length - zip64EndSize - zip64LocSize;
        return actualOffset - specifiedOffset;
    }
    
    RandomAccessData getCentralDirectory(final RandomAccessData data) {
        if (this.zip64End != null) {
            return this.zip64End.getCentralDirectory(data);
        }
        final long offset = Bytes.littleEndianValue(this.block, this.offset + 16, 4);
        final long length = Bytes.littleEndianValue(this.block, this.offset + 12, 4);
        return data.getSubsection(offset, length);
    }
    
    int getNumberOfRecords() {
        if (this.zip64End != null) {
            return this.zip64End.getNumberOfRecords();
        }
        final long numberOfRecords = Bytes.littleEndianValue(this.block, this.offset + 10, 2);
        return (int)numberOfRecords;
    }
    
    String getComment() {
        final int commentLength = (int)Bytes.littleEndianValue(this.block, this.offset + 20, 2);
        final AsciiBytes comment = new AsciiBytes(this.block, this.offset + 20 + 2, commentLength);
        return comment.toString();
    }
    
    boolean isZip64() {
        return this.zip64End != null;
    }
    
    private static final class Zip64End
    {
        private static final int ZIP64_ENDTOT = 32;
        private static final int ZIP64_ENDSIZ = 40;
        private static final int ZIP64_ENDOFF = 48;
        private final Zip64Locator locator;
        private final long centralDirectoryOffset;
        private final long centralDirectoryLength;
        private final int numberOfRecords;
        
        private Zip64End(final RandomAccessData data, final Zip64Locator locator) throws IOException {
            this.locator = locator;
            final byte[] block = data.read(locator.getZip64EndOffset(), 56L);
            this.centralDirectoryOffset = Bytes.littleEndianValue(block, 48, 8);
            this.centralDirectoryLength = Bytes.littleEndianValue(block, 40, 8);
            this.numberOfRecords = (int)Bytes.littleEndianValue(block, 32, 8);
        }
        
        private long getSize() {
            return this.locator.getZip64EndSize();
        }
        
        private RandomAccessData getCentralDirectory(final RandomAccessData data) {
            return data.getSubsection(this.centralDirectoryOffset, this.centralDirectoryLength);
        }
        
        private int getNumberOfRecords() {
            return this.numberOfRecords;
        }
    }
    
    private static final class Zip64Locator
    {
        static final int SIGNATURE = 117853008;
        static final int ZIP64_LOCSIZE = 20;
        static final int ZIP64_LOCOFF = 8;
        private final long zip64EndOffset;
        private final long offset;
        
        private Zip64Locator(final long offset, final byte[] block) throws IOException {
            this.offset = offset;
            this.zip64EndOffset = Bytes.littleEndianValue(block, 8, 8);
        }
        
        private long getZip64EndSize() {
            return this.offset - this.zip64EndOffset;
        }
        
        private long getZip64EndOffset() {
            return this.zip64EndOffset;
        }
        
        private static Zip64Locator find(final RandomAccessData data, final long centralDirectoryEndOffset) throws IOException {
            final long offset = centralDirectoryEndOffset - 20L;
            if (offset >= 0L) {
                final byte[] block = data.read(offset, 20L);
                if (Bytes.littleEndianValue(block, 0, 4) == 117853008L) {
                    return new Zip64Locator(offset, block);
                }
            }
            return null;
        }
    }
}
