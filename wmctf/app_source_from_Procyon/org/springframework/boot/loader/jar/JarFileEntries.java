// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.boot.loader.jar;

import java.util.NoSuchElementException;
import java.util.jar.JarInputStream;
import java.util.Arrays;
import java.util.jar.Manifest;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.loader.data.RandomAccessData;
import java.util.jar.Attributes;

class JarFileEntries implements CentralDirectoryVisitor, Iterable<JarEntry>
{
    private static final Runnable NO_VALIDATION;
    private static final String META_INF_PREFIX = "META-INF/";
    private static final Attributes.Name MULTI_RELEASE;
    private static final int BASE_VERSION = 8;
    private static final int RUNTIME_VERSION;
    private static final long LOCAL_FILE_HEADER_SIZE = 30L;
    private static final char SLASH = '/';
    private static final char NO_SUFFIX = '\0';
    protected static final int ENTRY_CACHE_SIZE = 25;
    private final JarFile jarFile;
    private final JarEntryFilter filter;
    private RandomAccessData centralDirectoryData;
    private int size;
    private int[] hashCodes;
    private Offsets centralDirectoryOffsets;
    private int[] positions;
    private Boolean multiReleaseJar;
    private JarEntryCertification[] certifications;
    private final Map<Integer, FileHeader> entriesCache;
    
    JarFileEntries(final JarFile jarFile, final JarEntryFilter filter) {
        this.entriesCache = Collections.synchronizedMap((Map<Integer, FileHeader>)new LinkedHashMap<Integer, FileHeader>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(final Map.Entry<Integer, FileHeader> eldest) {
                return this.size() >= 25;
            }
        });
        this.jarFile = jarFile;
        this.filter = filter;
        if (JarFileEntries.RUNTIME_VERSION == 8) {
            this.multiReleaseJar = false;
        }
    }
    
    @Override
    public void visitStart(final CentralDirectoryEndRecord endRecord, final RandomAccessData centralDirectoryData) {
        final int maxSize = endRecord.getNumberOfRecords();
        this.centralDirectoryData = centralDirectoryData;
        this.hashCodes = new int[maxSize];
        this.centralDirectoryOffsets = Offsets.from(endRecord);
        this.positions = new int[maxSize];
    }
    
    @Override
    public void visitFileHeader(final CentralDirectoryFileHeader fileHeader, final long dataOffset) {
        final AsciiBytes name = this.applyFilter(fileHeader.getName());
        if (name != null) {
            this.add(name, dataOffset);
        }
    }
    
    private void add(final AsciiBytes name, final long dataOffset) {
        this.hashCodes[this.size] = name.hashCode();
        this.centralDirectoryOffsets.set(this.size, dataOffset);
        this.positions[this.size] = this.size;
        ++this.size;
    }
    
    @Override
    public void visitEnd() {
        this.sort(0, this.size - 1);
        final int[] positions = this.positions;
        this.positions = new int[positions.length];
        for (int i = 0; i < this.size; ++i) {
            this.positions[positions[i]] = i;
        }
    }
    
    int getSize() {
        return this.size;
    }
    
    private void sort(final int left, final int right) {
        if (left < right) {
            final int pivot = this.hashCodes[left + (right - left) / 2];
            int i;
            int j;
            for (i = left, j = right; i <= j; ++i, --j) {
                while (this.hashCodes[i] < pivot) {
                    ++i;
                }
                while (this.hashCodes[j] > pivot) {
                    --j;
                }
                if (i <= j) {
                    this.swap(i, j);
                }
            }
            if (left < j) {
                this.sort(left, j);
            }
            if (right > i) {
                this.sort(i, right);
            }
        }
    }
    
    private void swap(final int i, final int j) {
        swap(this.hashCodes, i, j);
        this.centralDirectoryOffsets.swap(i, j);
        swap(this.positions, i, j);
    }
    
    @Override
    public Iterator<JarEntry> iterator() {
        return new EntryIterator(JarFileEntries.NO_VALIDATION);
    }
    
    Iterator<JarEntry> iterator(final Runnable validator) {
        return new EntryIterator(validator);
    }
    
    boolean containsEntry(final CharSequence name) {
        return this.getEntry(name, FileHeader.class, true) != null;
    }
    
    JarEntry getEntry(final CharSequence name) {
        return this.getEntry(name, JarEntry.class, true);
    }
    
    InputStream getInputStream(final String name) throws IOException {
        final FileHeader entry = this.getEntry(name, FileHeader.class, false);
        return this.getInputStream(entry);
    }
    
    InputStream getInputStream(final FileHeader entry) throws IOException {
        if (entry == null) {
            return null;
        }
        InputStream inputStream = this.getEntryData(entry).getInputStream();
        if (entry.getMethod() == 8) {
            inputStream = new ZipInflaterInputStream(inputStream, (int)entry.getSize());
        }
        return inputStream;
    }
    
    RandomAccessData getEntryData(final String name) throws IOException {
        final FileHeader entry = this.getEntry(name, FileHeader.class, false);
        if (entry == null) {
            return null;
        }
        return this.getEntryData(entry);
    }
    
    private RandomAccessData getEntryData(final FileHeader entry) throws IOException {
        final RandomAccessData data = this.jarFile.getData();
        final byte[] localHeader = data.read(entry.getLocalHeaderOffset(), 30L);
        final long nameLength = Bytes.littleEndianValue(localHeader, 26, 2);
        final long extraLength = Bytes.littleEndianValue(localHeader, 28, 2);
        return data.getSubsection(entry.getLocalHeaderOffset() + 30L + nameLength + extraLength, entry.getCompressedSize());
    }
    
    private <T extends FileHeader> T getEntry(final CharSequence name, final Class<T> type, final boolean cacheEntry) {
        final T entry = this.doGetEntry(name, type, cacheEntry, null);
        if (!this.isMetaInfEntry(name) && this.isMultiReleaseJar()) {
            int version = JarFileEntries.RUNTIME_VERSION;
            final AsciiBytes nameAlias = (entry instanceof JarEntry) ? ((JarEntry)entry).getAsciiBytesName() : new AsciiBytes(name.toString());
            while (version > 8) {
                final T versionedEntry = this.doGetEntry("META-INF/versions/" + version + "/" + (Object)name, type, cacheEntry, nameAlias);
                if (versionedEntry != null) {
                    return versionedEntry;
                }
                --version;
            }
        }
        return entry;
    }
    
    private boolean isMetaInfEntry(final CharSequence name) {
        return name.toString().startsWith("META-INF/");
    }
    
    private boolean isMultiReleaseJar() {
        Boolean multiRelease = this.multiReleaseJar;
        if (multiRelease != null) {
            return multiRelease;
        }
        try {
            final Manifest manifest = this.jarFile.getManifest();
            if (manifest == null) {
                multiRelease = false;
            }
            else {
                final Attributes attributes = manifest.getMainAttributes();
                multiRelease = attributes.containsKey(JarFileEntries.MULTI_RELEASE);
            }
        }
        catch (IOException ex) {
            multiRelease = false;
        }
        this.multiReleaseJar = multiRelease;
        return multiRelease;
    }
    
    private <T extends FileHeader> T doGetEntry(final CharSequence name, final Class<T> type, final boolean cacheEntry, final AsciiBytes nameAlias) {
        int hashCode = AsciiBytes.hashCode(name);
        T entry = this.getEntry(hashCode, name, '\0', type, cacheEntry, nameAlias);
        if (entry == null) {
            hashCode = AsciiBytes.hashCode(hashCode, '/');
            entry = this.getEntry(hashCode, name, '/', type, cacheEntry, nameAlias);
        }
        return entry;
    }
    
    private <T extends FileHeader> T getEntry(final int hashCode, final CharSequence name, final char suffix, final Class<T> type, final boolean cacheEntry, final AsciiBytes nameAlias) {
        for (int index = this.getFirstIndex(hashCode); index >= 0 && index < this.size && this.hashCodes[index] == hashCode; ++index) {
            final T entry = this.getEntry(index, type, cacheEntry, nameAlias);
            if (entry.hasName(name, suffix)) {
                return entry;
            }
        }
        return null;
    }
    
    private <T extends FileHeader> T getEntry(final int index, final Class<T> type, final boolean cacheEntry, final AsciiBytes nameAlias) {
        try {
            final long offset = this.centralDirectoryOffsets.get(index);
            final FileHeader cached = this.entriesCache.get(index);
            FileHeader entry = (cached != null) ? cached : CentralDirectoryFileHeader.fromRandomAccessData(this.centralDirectoryData, offset, this.filter);
            if (CentralDirectoryFileHeader.class.equals(entry.getClass()) && type.equals(JarEntry.class)) {
                entry = new JarEntry(this.jarFile, index, (CentralDirectoryFileHeader)entry, nameAlias);
            }
            if (cacheEntry && cached != entry) {
                this.entriesCache.put(index, entry);
            }
            return (T)entry;
        }
        catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
    
    private int getFirstIndex(final int hashCode) {
        int index = Arrays.binarySearch(this.hashCodes, 0, this.size, hashCode);
        if (index < 0) {
            return -1;
        }
        while (index > 0 && this.hashCodes[index - 1] == hashCode) {
            --index;
        }
        return index;
    }
    
    void clearCache() {
        this.entriesCache.clear();
    }
    
    private AsciiBytes applyFilter(final AsciiBytes name) {
        return (this.filter != null) ? this.filter.apply(name) : name;
    }
    
    JarEntryCertification getCertification(final JarEntry entry) throws IOException {
        JarEntryCertification[] certifications = this.certifications;
        if (certifications == null) {
            certifications = new JarEntryCertification[this.size];
            try (final JarInputStream certifiedJarStream = new JarInputStream(this.jarFile.getData().getInputStream())) {
                java.util.jar.JarEntry certifiedEntry = null;
                while ((certifiedEntry = certifiedJarStream.getNextJarEntry()) != null) {
                    certifiedJarStream.closeEntry();
                    final int index = this.getEntryIndex(certifiedEntry.getName());
                    if (index != -1) {
                        certifications[index] = JarEntryCertification.from(certifiedEntry);
                    }
                }
            }
            this.certifications = certifications;
        }
        final JarEntryCertification certification = certifications[entry.getIndex()];
        return (certification != null) ? certification : JarEntryCertification.NONE;
    }
    
    private int getEntryIndex(final CharSequence name) {
        for (int hashCode = AsciiBytes.hashCode(name), index = this.getFirstIndex(hashCode); index >= 0 && index < this.size && this.hashCodes[index] == hashCode; ++index) {
            final FileHeader candidate = this.getEntry(index, FileHeader.class, false, null);
            if (candidate.hasName(name, '\0')) {
                return index;
            }
        }
        return -1;
    }
    
    private static void swap(final int[] array, final int i, final int j) {
        final int temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }
    
    private static void swap(final long[] array, final int i, final int j) {
        final long temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }
    
    static {
        NO_VALIDATION = (() -> {});
        MULTI_RELEASE = new Attributes.Name("Multi-Release");
        int version;
        try {
            final Object runtimeVersion = Runtime.class.getMethod("version", (Class<?>[])new Class[0]).invoke(null, new Object[0]);
            version = (int)runtimeVersion.getClass().getMethod("major", (Class<?>[])new Class[0]).invoke(runtimeVersion, new Object[0]);
        }
        catch (Throwable ex) {
            version = 8;
        }
        RUNTIME_VERSION = version;
    }
    
    private final class EntryIterator implements Iterator<JarEntry>
    {
        private final Runnable validator;
        private int index;
        
        private EntryIterator(final Runnable validator) {
            this.index = 0;
            (this.validator = validator).run();
        }
        
        @Override
        public boolean hasNext() {
            this.validator.run();
            return this.index < JarFileEntries.this.size;
        }
        
        @Override
        public JarEntry next() {
            this.validator.run();
            if (!this.hasNext()) {
                throw new NoSuchElementException();
            }
            final int entryIndex = JarFileEntries.this.positions[this.index];
            ++this.index;
            return (JarEntry)JarFileEntries.this.getEntry(entryIndex, (Class<FileHeader>)JarEntry.class, false, null);
        }
    }
    
    private interface Offsets
    {
        void set(final int index, final long value);
        
        long get(final int index);
        
        void swap(final int i, final int j);
        
        default Offsets from(final CentralDirectoryEndRecord endRecord) {
            final int size = endRecord.getNumberOfRecords();
            return endRecord.isZip64() ? new Zip64Offsets(size) : new ZipOffsets(size);
        }
    }
    
    private static final class ZipOffsets implements Offsets
    {
        private final int[] offsets;
        
        private ZipOffsets(final int size) {
            this.offsets = new int[size];
        }
        
        @Override
        public void swap(final int i, final int j) {
            swap(this.offsets, i, j);
        }
        
        @Override
        public void set(final int index, final long value) {
            this.offsets[index] = (int)value;
        }
        
        @Override
        public long get(final int index) {
            return this.offsets[index];
        }
    }
    
    private static final class Zip64Offsets implements Offsets
    {
        private final long[] offsets;
        
        private Zip64Offsets(final int size) {
            this.offsets = new long[size];
        }
        
        @Override
        public void swap(final int i, final int j) {
            swap(this.offsets, i, j);
        }
        
        @Override
        public void set(final int index, final long value) {
            this.offsets[index] = value;
        }
        
        @Override
        public long get(final int index) {
            return this.offsets[index];
        }
    }
}
