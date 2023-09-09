// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.boot.loader.jar;

import java.nio.charset.StandardCharsets;

final class AsciiBytes
{
    private static final String EMPTY_STRING = "";
    private static final int[] INITIAL_BYTE_BITMASK;
    private static final int SUBSEQUENT_BYTE_BITMASK = 63;
    private final byte[] bytes;
    private final int offset;
    private final int length;
    private String string;
    private int hash;
    
    AsciiBytes(final String string) {
        this(string.getBytes(StandardCharsets.UTF_8));
        this.string = string;
    }
    
    AsciiBytes(final byte[] bytes) {
        this(bytes, 0, bytes.length);
    }
    
    AsciiBytes(final byte[] bytes, final int offset, final int length) {
        if (offset < 0 || length < 0 || offset + length > bytes.length) {
            throw new IndexOutOfBoundsException();
        }
        this.bytes = bytes;
        this.offset = offset;
        this.length = length;
    }
    
    int length() {
        return this.length;
    }
    
    boolean startsWith(final AsciiBytes prefix) {
        if (this == prefix) {
            return true;
        }
        if (prefix.length > this.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; ++i) {
            if (this.bytes[i + this.offset] != prefix.bytes[i + prefix.offset]) {
                return false;
            }
        }
        return true;
    }
    
    boolean endsWith(final AsciiBytes postfix) {
        if (this == postfix) {
            return true;
        }
        if (postfix.length > this.length) {
            return false;
        }
        for (int i = 0; i < postfix.length; ++i) {
            if (this.bytes[this.offset + (this.length - 1) - i] != postfix.bytes[postfix.offset + (postfix.length - 1) - i]) {
                return false;
            }
        }
        return true;
    }
    
    AsciiBytes substring(final int beginIndex) {
        return this.substring(beginIndex, this.length);
    }
    
    AsciiBytes substring(final int beginIndex, final int endIndex) {
        final int length = endIndex - beginIndex;
        if (this.offset + length > this.bytes.length) {
            throw new IndexOutOfBoundsException();
        }
        return new AsciiBytes(this.bytes, this.offset + beginIndex, length);
    }
    
    boolean matches(final CharSequence name, final char suffix) {
        int charIndex = 0;
        final int nameLen = name.length();
        final int totalLen = nameLen + ((suffix != '\0') ? 1 : 0);
        for (int i = this.offset; i < this.offset + this.length; ++i) {
            int b = this.bytes[i];
            final int remainingUtfBytes = this.getNumberOfUtfBytes(b) - 1;
            b &= AsciiBytes.INITIAL_BYTE_BITMASK[remainingUtfBytes];
            for (int j = 0; j < remainingUtfBytes; ++j) {
                b = (b << 6) + (this.bytes[++i] & 0x3F);
            }
            char c = this.getChar(name, suffix, charIndex++);
            if (b <= 65535) {
                if (c != b) {
                    return false;
                }
            }
            else {
                if (c != (b >> 10) + 55232) {
                    return false;
                }
                c = this.getChar(name, suffix, charIndex++);
                if (c != (b & 0x3FF) + 56320) {
                    return false;
                }
            }
        }
        return charIndex == totalLen;
    }
    
    private char getChar(final CharSequence name, final char suffix, final int index) {
        if (index < name.length()) {
            return name.charAt(index);
        }
        if (index == name.length()) {
            return suffix;
        }
        return '\0';
    }
    
    private int getNumberOfUtfBytes(int b) {
        if ((b & 0x80) == 0x0) {
            return 1;
        }
        int numberOfUtfBytes;
        for (numberOfUtfBytes = 0; (b & 0x80) != 0x0; b <<= 1, ++numberOfUtfBytes) {}
        return numberOfUtfBytes;
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (obj.getClass() == AsciiBytes.class) {
            final AsciiBytes other = (AsciiBytes)obj;
            if (this.length == other.length) {
                for (int i = 0; i < this.length; ++i) {
                    if (this.bytes[this.offset + i] != other.bytes[other.offset + i]) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        int hash = this.hash;
        if (hash == 0 && this.bytes.length > 0) {
            for (int i = this.offset; i < this.offset + this.length; ++i) {
                int b = this.bytes[i];
                final int remainingUtfBytes = this.getNumberOfUtfBytes(b) - 1;
                b &= AsciiBytes.INITIAL_BYTE_BITMASK[remainingUtfBytes];
                for (int j = 0; j < remainingUtfBytes; ++j) {
                    b = (b << 6) + (this.bytes[++i] & 0x3F);
                }
                if (b <= 65535) {
                    hash = 31 * hash + b;
                }
                else {
                    hash = 31 * hash + ((b >> 10) + 55232);
                    hash = 31 * hash + ((b & 0x3FF) + 56320);
                }
            }
            this.hash = hash;
        }
        return hash;
    }
    
    @Override
    public String toString() {
        if (this.string == null) {
            if (this.length == 0) {
                this.string = "";
            }
            else {
                this.string = new String(this.bytes, this.offset, this.length, StandardCharsets.UTF_8);
            }
        }
        return this.string;
    }
    
    static String toString(final byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }
    
    static int hashCode(final CharSequence charSequence) {
        if (charSequence instanceof StringSequence) {
            return charSequence.hashCode();
        }
        return charSequence.toString().hashCode();
    }
    
    static int hashCode(final int hash, final char suffix) {
        return (suffix != '\0') ? (31 * hash + suffix) : hash;
    }
    
    static {
        INITIAL_BYTE_BITMASK = new int[] { 127, 31, 15, 7 };
    }
}
