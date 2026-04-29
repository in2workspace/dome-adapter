package es.altia.domeadapter.backend.shared.domain.util;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class UVarInt {

    private static final long MSB    = 0x80L;
    private static final long MSBALL = 0xFFFFFF80L;

    private final long value;
    private final byte[] bytes;
    private final int length;

    public UVarInt(long value) {
        this.value  = value;
        this.bytes  = bytesFromUInt(value);
        this.length = bytes.length;
    }

    private byte[] bytesFromUInt(long num) {
        List<Byte> varInt = new ArrayList<>();
        long rest = num;
        while ((rest & MSBALL) != 0) {
            varInt.add((byte) ((rest & 0xFF) | MSB));
            rest = rest >>> 7;
        }
        varInt.add((byte) rest);
        byte[] result = new byte[varInt.size()];
        for (int i = 0; i < varInt.size(); i++) {
            result[i] = varInt.get(i);
        }
        return result;
    }

    @Override
    public String toString() {
        return "0x" + Long.toHexString(value);
    }
}
