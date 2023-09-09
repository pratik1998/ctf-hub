// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.boot.loader.jar;

import java.util.Objects;

final class StringSequence implements CharSequence
{
    private final String source;
    private final int start;
    private final int end;
    private int hash;
    
    StringSequence(final String source) {
        this(source, 0, (source != null) ? source.length() : -1);
    }
    
    StringSequence(final String source, final int start, final int end) {
        Objects.requireNonNull(source, "Source must not be null");
        if (start < 0) {
            throw new StringIndexOutOfBoundsException(start);
        }
        if (end > source.length()) {
            throw new StringIndexOutOfBoundsException(end);
        }
        this.source = source;
        this.start = start;
        this.end = end;
    }
    
    StringSequence subSequence(final int start) {
        return this.subSequence(start, this.length());
    }
    
    @Override
    public StringSequence subSequence(final int start, final int end) {
        final int subSequenceStart = this.start + start;
        final int subSequenceEnd = this.start + end;
        if (subSequenceStart > this.end) {
            throw new StringIndexOutOfBoundsException(start);
        }
        if (subSequenceEnd > this.end) {
            throw new StringIndexOutOfBoundsException(end);
        }
        if (start == 0 && subSequenceEnd == this.end) {
            return this;
        }
        return new StringSequence(this.source, subSequenceStart, subSequenceEnd);
    }
    
    public boolean isEmpty() {
        return this.length() == 0;
    }
    
    @Override
    public int length() {
        return this.end - this.start;
    }
    
    @Override
    public char charAt(final int index) {
        return this.source.charAt(this.start + index);
    }
    
    int indexOf(final char ch) {
        return this.source.indexOf(ch, this.start) - this.start;
    }
    
    int indexOf(final String str) {
        return this.source.indexOf(str, this.start) - this.start;
    }
    
    int indexOf(final String str, final int fromIndex) {
        return this.source.indexOf(str, this.start + fromIndex) - this.start;
    }
    
    boolean startsWith(final String prefix) {
        return this.startsWith(prefix, 0);
    }
    
    boolean startsWith(final String prefix, final int offset) {
        final int prefixLength = prefix.length();
        final int length = this.length();
        return length - prefixLength - offset >= 0 && this.source.startsWith(prefix, this.start + offset);
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CharSequence)) {
            return false;
        }
        final CharSequence other = (CharSequence)obj;
        int n = this.length();
        if (n != other.length()) {
            return false;
        }
        int i = 0;
        while (n-- != 0) {
            if (this.charAt(i) != other.charAt(i)) {
                return false;
            }
            ++i;
        }
        return true;
    }
    
    @Override
    public int hashCode() {
        int hash = this.hash;
        if (hash == 0 && this.length() > 0) {
            for (int i = this.start; i < this.end; ++i) {
                hash = 31 * hash + this.source.charAt(i);
            }
            this.hash = hash;
        }
        return hash;
    }
    
    @Override
    public String toString() {
        return this.source.substring(this.start, this.end);
    }
}
