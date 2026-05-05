package es.altia.domeadapter.backend.shared.domain.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UVarIntTest {

    @Test
    void shouldCreateUVarIntForZero() {
        UVarInt uVarInt = new UVarInt(0L);

        assertThat(uVarInt.getValue()).isZero();
        assertThat(uVarInt.getBytes()).containsExactly((byte) 0x00);
        assertThat(uVarInt.getLength()).isEqualTo(1);
        assertThat(uVarInt).hasToString("0x0");
    }

    @Test
    void shouldCreateUVarIntForSingleByteValue() {
        UVarInt uVarInt = new UVarInt(127L);

        assertThat(uVarInt.getValue()).isEqualTo(127L);
        assertThat(uVarInt.getBytes()).containsExactly((byte) 0x7F);
        assertThat(uVarInt.getLength()).isEqualTo(1);
        assertThat(uVarInt).hasToString("0x7f");
    }

    @Test
    void shouldCreateUVarIntForFirstTwoByteValue() {
        UVarInt uVarInt = new UVarInt(128L);

        assertThat(uVarInt.getValue()).isEqualTo(128L);
        assertThat(uVarInt.getBytes()).containsExactly((byte) 0x80, (byte) 0x01);
        assertThat(uVarInt.getLength()).isEqualTo(2);
        assertThat(uVarInt).hasToString("0x80");
    }

    @Test
    void shouldCreateUVarIntForTwoByteValue() {
        UVarInt uVarInt = new UVarInt(300L);

        assertThat(uVarInt.getValue()).isEqualTo(300L);
        assertThat(uVarInt.getBytes()).containsExactly((byte) 0xAC, (byte) 0x02);
        assertThat(uVarInt.getLength()).isEqualTo(2);
        assertThat(uVarInt).hasToString("0x12c");
    }

    @Test
    void shouldCreateUVarIntForThreeByteValue() {
        UVarInt uVarInt = new UVarInt(16_384L);

        assertThat(uVarInt.getValue()).isEqualTo(16_384L);
        assertThat(uVarInt.getBytes()).containsExactly((byte) 0x80, (byte) 0x80, (byte) 0x01);
        assertThat(uVarInt.getLength()).isEqualTo(3);
        assertThat(uVarInt).hasToString("0x4000");
    }

    @Test
    void shouldCreateUVarIntForMaxUInt32Value() {
        UVarInt uVarInt = new UVarInt(4_294_967_295L);

        assertThat(uVarInt.getValue()).isEqualTo(4_294_967_295L);
        assertThat(uVarInt.getBytes()).containsExactly(
                (byte) 0xFF,
                (byte) 0xFF,
                (byte) 0xFF,
                (byte) 0xFF,
                (byte) 0x0F
        );
        assertThat(uVarInt.getLength()).isEqualTo(5);
        assertThat(uVarInt).hasToString("0xffffffff");
    }

    @Test
    void shouldDefensivelyKeepLengthConsistentWithBytesLength() {
        UVarInt uVarInt = new UVarInt(300L);

        assertThat(uVarInt.getLength()).isEqualTo(uVarInt.getBytes().length);
    }
}
