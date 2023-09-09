// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.boot.loader.jar;

import java.time.temporal.ValueRange;
import java.time.temporal.TemporalUnit;
import java.time.temporal.ChronoUnit;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.io.IOException;
import org.springframework.boot.loader.data.RandomAccessData;

final class CentralDirectoryFileHeader implements FileHeader
{
    private static final AsciiBytes SLASH;
    private static final byte[] NO_EXTRA;
    private static final AsciiBytes NO_COMMENT;
    private byte[] header;
    private int headerOffset;
    private AsciiBytes name;
    private byte[] extra;
    private AsciiBytes comment;
    private long localHeaderOffset;
    
    CentralDirectoryFileHeader() {
    }
    
    CentralDirectoryFileHeader(final byte[] header, final int headerOffset, final AsciiBytes name, final byte[] extra, final AsciiBytes comment, final long localHeaderOffset) {
        this.header = header;
        this.headerOffset = headerOffset;
        this.name = name;
        this.extra = extra;
        this.comment = comment;
        this.localHeaderOffset = localHeaderOffset;
    }
    
    void load(byte[] data, int dataOffset, final RandomAccessData variableData, final long variableOffset, final JarEntryFilter filter) throws IOException {
        this.header = data;
        this.headerOffset = dataOffset;
        final long compressedSize = Bytes.littleEndianValue(data, dataOffset + 20, 4);
        final long uncompressedSize = Bytes.littleEndianValue(data, dataOffset + 24, 4);
        final long nameLength = Bytes.littleEndianValue(data, dataOffset + 28, 2);
        final long extraLength = Bytes.littleEndianValue(data, dataOffset + 30, 2);
        final long commentLength = Bytes.littleEndianValue(data, dataOffset + 32, 2);
        final long localHeaderOffset = Bytes.littleEndianValue(data, dataOffset + 42, 4);
        dataOffset += 46;
        if (variableData != null) {
            data = variableData.read(variableOffset + 46L, nameLength + extraLength + commentLength);
            dataOffset = 0;
        }
        this.name = new AsciiBytes(data, dataOffset, (int)nameLength);
        if (filter != null) {
            this.name = filter.apply(this.name);
        }
        this.extra = CentralDirectoryFileHeader.NO_EXTRA;
        this.comment = CentralDirectoryFileHeader.NO_COMMENT;
        if (extraLength > 0L) {
            this.extra = new byte[(int)extraLength];
            System.arraycopy(data, (int)(dataOffset + nameLength), this.extra, 0, this.extra.length);
        }
        this.localHeaderOffset = this.getLocalHeaderOffset(compressedSize, uncompressedSize, localHeaderOffset, this.extra);
        if (commentLength > 0L) {
            this.comment = new AsciiBytes(data, (int)(dataOffset + nameLength + extraLength), (int)commentLength);
        }
    }
    
    private long getLocalHeaderOffset(final long compressedSize, final long uncompressedSize, final long localHeaderOffset, final byte[] extra) throws IOException {
        if (localHeaderOffset != 4294967295L) {
            return localHeaderOffset;
        }
        int length;
        for (int extraOffset = 0; extraOffset < extra.length - 2; extraOffset += length) {
            final int id = (int)Bytes.littleEndianValue(extra, extraOffset, 2);
            length = (int)Bytes.littleEndianValue(extra, extraOffset, 2);
            extraOffset += 4;
            if (id == 1) {
                int localHeaderExtraOffset = 0;
                if (compressedSize == 4294967295L) {
                    localHeaderExtraOffset += 4;
                }
                if (uncompressedSize == 4294967295L) {
                    localHeaderExtraOffset += 4;
                }
                return Bytes.littleEndianValue(extra, extraOffset + localHeaderExtraOffset, 8);
            }
        }
        throw new IOException("Zip64 Extended Information Extra Field not found");
    }
    
    AsciiBytes getName() {
        return this.name;
    }
    
    @Override
    public boolean hasName(final CharSequence name, final char suffix) {
        return this.name.matches(name, suffix);
    }
    
    boolean isDirectory() {
        return this.name.endsWith(CentralDirectoryFileHeader.SLASH);
    }
    
    @Override
    public int getMethod() {
        return (int)Bytes.littleEndianValue(this.header, this.headerOffset + 10, 2);
    }
    
    long getTime() {
        final long datetime = Bytes.littleEndianValue(this.header, this.headerOffset + 12, 4);
        return this.decodeMsDosFormatDateTime(datetime);
    }
    
    private long decodeMsDosFormatDateTime(final long datetime) {
        final int year = getChronoValue((datetime >> 25 & 0x7FL) + 1980L, ChronoField.YEAR);
        final int month = getChronoValue(datetime >> 21 & 0xFL, ChronoField.MONTH_OF_YEAR);
        final int day = getChronoValue(datetime >> 16 & 0x1FL, ChronoField.DAY_OF_MONTH);
        final int hour = getChronoValue(datetime >> 11 & 0x1FL, ChronoField.HOUR_OF_DAY);
        final int minute = getChronoValue(datetime >> 5 & 0x3FL, ChronoField.MINUTE_OF_HOUR);
        final int second = getChronoValue(datetime << 1 & 0x3EL, ChronoField.SECOND_OF_MINUTE);
        return ZonedDateTime.of(year, month, day, hour, minute, second, 0, ZoneId.systemDefault()).toInstant().truncatedTo(ChronoUnit.SECONDS).toEpochMilli();
    }
    
    long getCrc() {
        return Bytes.littleEndianValue(this.header, this.headerOffset + 16, 4);
    }
    
    @Override
    public long getCompressedSize() {
        return Bytes.littleEndianValue(this.header, this.headerOffset + 20, 4);
    }
    
    @Override
    public long getSize() {
        return Bytes.littleEndianValue(this.header, this.headerOffset + 24, 4);
    }
    
    byte[] getExtra() {
        return this.extra;
    }
    
    boolean hasExtra() {
        return this.extra.length > 0;
    }
    
    AsciiBytes getComment() {
        return this.comment;
    }
    
    @Override
    public long getLocalHeaderOffset() {
        return this.localHeaderOffset;
    }
    
    public CentralDirectoryFileHeader clone() {
        final byte[] header = new byte[46];
        System.arraycopy(this.header, this.headerOffset, header, 0, header.length);
        return new CentralDirectoryFileHeader(header, 0, this.name, header, this.comment, this.localHeaderOffset);
    }
    
    static CentralDirectoryFileHeader fromRandomAccessData(final RandomAccessData data, final long offset, final JarEntryFilter filter) throws IOException {
        final CentralDirectoryFileHeader fileHeader = new CentralDirectoryFileHeader();
        final byte[] bytes = data.read(offset, 46L);
        fileHeader.load(bytes, 0, data, offset, filter);
        return fileHeader;
    }
    
    private static int getChronoValue(final long value, final ChronoField field) {
        final ValueRange range = field.range();
        return Math.toIntExact(Math.min(Math.max(value, range.getMinimum()), range.getMaximum()));
    }
    
    static {
        SLASH = new AsciiBytes("/");
        NO_EXTRA = new byte[0];
        NO_COMMENT = new AsciiBytes("");
    }
}
