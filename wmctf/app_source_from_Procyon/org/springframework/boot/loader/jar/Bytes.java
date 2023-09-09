// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.boot.loader.jar;

final class Bytes
{
    private Bytes() {
    }
    
    static long littleEndianValue(final byte[] bytes, final int offset, final int length) {
        long value = 0L;
        for (int i = length - 1; i >= 0; --i) {
            value = (value << 8 | (long)(bytes[offset + i] & 0xFF));
        }
        return value;
    }
}
