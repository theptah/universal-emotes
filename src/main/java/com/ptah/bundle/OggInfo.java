package com.ptah.bundle;

import java.io.IOException;
import java.io.InputStream;

public final class OggInfo {
    private OggInfo() { }

    public static double durationSeconds(byte[] data) {
        if (data == null || data.length < 64) return Double.NaN;
        int id = indexOf(data, new byte[] {1, 'v', 'o', 'r', 'b', 'i', 's'}, 0, Math.min(data.length, 4096));
        if (id < 0 || id + 16 > data.length) return Double.NaN;
        long rate = le32(data, id + 12);
        if (rate <= 0) return Double.NaN;
        for (int i = data.length - 27; i >= 0; i--) {
            if (data[i] == 'O' && data[i + 1] == 'g' && data[i + 2] == 'g' && data[i + 3] == 'S') {
                long granule = le32(data, i + 6) | (le32(data, i + 10) << 32);
                if (granule > 0) return granule / (double) rate;
            }
        }
        return Double.NaN;
    }

    public static double durationSeconds(InputStream in) throws IOException {
        return durationSeconds(in.readAllBytes());
    }

    private static long le32(byte[] d, int o) {
        return (d[o] & 0xFFL) | (d[o + 1] & 0xFFL) << 8 | (d[o + 2] & 0xFFL) << 16 | (d[o + 3] & 0xFFL) << 24;
    }

    private static int indexOf(byte[] d, byte[] pat, int from, int to) {
        outer:
        for (int i = from; i <= to - pat.length; i++) {
            for (int k = 0; k < pat.length; k++) if (d[i + k] != pat[k]) continue outer;
            return i;
        }
        return -1;
    }
}
