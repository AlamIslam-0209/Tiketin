package com.tiketin.util;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Pure Java ISO/IEC 18004 QR Code encoder (Byte mode, ECC Level M, Versions 1-6)
 * for rendering scannable QRIS payment codes without external dependencies.
 */
public final class QrCodeGenerator {

    private static final int[] EXP = new int[512];
    private static final int[] LOG = new int[256];

    // Versions 1..6 under ECC Level M: {dataPerBlock, ecPerBlock, numBlocks, alignCenter}
    private static final int[][] VERSION_M = {
        {}, // 0 unused
        {16, 10, 1, -1},
        {28, 16, 1, 18},
        {44, 26, 1, 22},
        {32, 18, 2, 26},
        {43, 24, 2, 30},
        {27, 16, 4, 34}
    };

    static {
        int x = 1;
        for (int i = 0; i < 255; i++) {
            EXP[i] = x;
            LOG[x] = i;
            x <<= 1;
            if ((x & 0x100) != 0) {
                x ^= 0x11D;
            }
        }
        for (int i = 255; i < 512; i++) {
            EXP[i] = EXP[i - 255];
        }
    }

    private QrCodeGenerator() {}

    public static Canvas createQrCanvas(String payload, double canvasSize) {
        String safePayload = (payload == null || payload.isBlank()) ? "TIKETIN-QRIS" : payload;
        byte[] bytes = safePayload.getBytes(StandardCharsets.UTF_8);
        // Cap cleanly to V6 ECC-M capacity (108 bytes total -> 106 usable payload bytes)
        if (bytes.length > 106) {
            bytes = Arrays.copyOf(bytes, 106);
        }

        boolean[][] matrix = encode(bytes);
        int n = matrix.length;
        int quiet = 3;
        double totalModules = n + quiet * 2;
        double modulePx = canvasSize / totalModules;

        Canvas canvas = new Canvas(canvasSize, canvasSize);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvasSize, canvasSize);

        gc.setFill(Color.web("#20252A"));
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                if (matrix[r][c]) {
                    double x = Math.floor((c + quiet) * modulePx);
                    double y = Math.floor((r + quiet) * modulePx);
                    double w = Math.ceil((c + quiet + 1) * modulePx) - x;
                    double h = Math.ceil((r + quiet + 1) * modulePx) - y;
                    gc.fillRect(x, y, w, h);
                }
            }
        }
        return canvas;
    }

    public static boolean[][] encode(byte[] data) {
        int version = 1;
        while (version <= 6) {
            int dataBytesCap = VERSION_M[version][0] * VERSION_M[version][2];
            int neededBits = 4 + 8 + data.length * 8;
            if ((neededBits + 7) / 8 <= dataBytesCap) {
                break;
            }
            version++;
        }
        if (version > 6) {
            version = 6;
        }

        int dataPerBlock = VERSION_M[version][0];
        int ecPerBlock = VERSION_M[version][1];
        int numBlocks = VERSION_M[version][2];
        int totalDataBytes = dataPerBlock * numBlocks;

        // Encode bitstream: mode (0100), byte count (8 bits), data, terminator, pad bytes
        byte[] dataCodewords = new byte[totalDataBytes];
        int bitPos = 0;
        bitPos = writeBits(dataCodewords, bitPos, 0b0100, 4);
        int maxPayload = Math.min(data.length, totalDataBytes - 2);
        bitPos = writeBits(dataCodewords, bitPos, maxPayload, 8);
        for (int i = 0; i < maxPayload; i++) {
            bitPos = writeBits(dataCodewords, bitPos, data[i] & 0xFF, 8);
        }
        int totalBits = totalDataBytes * 8;
        int termLen = Math.min(4, totalBits - bitPos);
        bitPos = writeBits(dataCodewords, bitPos, 0, termLen);
        if (bitPos % 8 != 0) {
            bitPos += 8 - (bitPos % 8);
        }
        int byteIdx = bitPos / 8;
        boolean alt = false;
        while (byteIdx < totalDataBytes) {
            dataCodewords[byteIdx++] = (byte) (alt ? 0x11 : 0xEC);
            alt = !alt;
        }

        // Split into blocks & compute Reed-Solomon EC codewords
        byte[][] blocksData = new byte[numBlocks][dataPerBlock];
        byte[][] blocksEc = new byte[numBlocks][ecPerBlock];
        int[] gen = rsGenerator(ecPerBlock);
        for (int b = 0; b < numBlocks; b++) {
            System.arraycopy(dataCodewords, b * dataPerBlock, blocksData[b], 0, dataPerBlock);
            blocksEc[b] = rsRemainder(blocksData[b], gen, ecPerBlock);
        }

        // Interleave data and EC codewords
        byte[] allCodewords = new byte[(dataPerBlock + ecPerBlock) * numBlocks];
        int outIdx = 0;
        for (int i = 0; i < dataPerBlock; i++) {
            for (int b = 0; b < numBlocks; b++) {
                allCodewords[outIdx++] = blocksData[b][i];
            }
        }
        for (int i = 0; i < ecPerBlock; i++) {
            for (int b = 0; b < numBlocks; b++) {
                allCodewords[outIdx++] = blocksEc[b][i];
            }
        }

        // Initialize matrix and function patterns
        int size = 17 + 4 * version;
        boolean[][] isFunction = new boolean[size][size];
        boolean[][] base = new boolean[size][size];

        drawFinderPattern(base, isFunction, 0, 0);
        drawFinderPattern(base, isFunction, 0, size - 7);
        drawFinderPattern(base, isFunction, size - 7, 0);

        // Separators
        for (int i = 0; i < 8; i++) {
            setFunc(base, isFunction, 7, i, false);
            setFunc(base, isFunction, i, 7, false);
            setFunc(base, isFunction, 7, size - 1 - i, false);
            setFunc(base, isFunction, i, size - 8, false);
            setFunc(base, isFunction, size - 8, i, false);
            setFunc(base, isFunction, size - 1 - i, 7, false);
        }

        // Timing patterns
        for (int i = 8; i < size - 8; i++) {
            boolean dark = (i % 2 == 0);
            setFunc(base, isFunction, 6, i, dark);
            setFunc(base, isFunction, i, 6, dark);
        }

        // Alignment pattern (Versions 2..6 have single center at alignCenter)
        int alignCenter = VERSION_M[version][3];
        if (alignCenter > 0) {
            drawAlignmentPattern(base, isFunction, alignCenter, alignCenter);
        }

        // Dark module
        setFunc(base, isFunction, 4 * version + 9, 8, true);

        // Reserve format info modules
        for (int i = 0; i < 9; i++) {
            if (i != 6) {
                isFunction[8][i] = true;
                isFunction[i][8] = true;
            }
        }
        for (int i = 0; i < 8; i++) {
            isFunction[8][size - 1 - i] = true;
            if (i < 7) {
                isFunction[size - 1 - i][8] = true;
            }
        }

        // Place data bits in zigzag column pairs
        int bitIdx = 0;
        int totalCodewordBits = allCodewords.length * 8;
        boolean upward = true;
        for (int right = size - 1; right >= 1; right -= 2) {
            if (right == 6) {
                right = 5;
            }
            for (int vert = 0; vert < size; vert++) {
                int r = upward ? (size - 1 - vert) : vert;
                for (int j = 0; j < 2; j++) {
                    int c = right - j;
                    if (!isFunction[r][c]) {
                        boolean bit = false;
                        if (bitIdx < totalCodewordBits) {
                            int bVal = allCodewords[bitIdx >>> 3] & 0xFF;
                            bit = ((bVal >>> (7 - (bitIdx & 7))) & 1) != 0;
                            bitIdx++;
                        }
                        base[r][c] = bit;
                    }
                }
            }
            upward = !upward;
        }

        // Evaluate all 8 masks and choose lowest penalty score
        int bestMask = 0;
        int bestPenalty = Integer.MAX_VALUE;
        boolean[][] bestMatrix = null;
        for (int mask = 0; mask < 8; mask++) {
            boolean[][] cand = applyMask(base, isFunction, mask);
            applyFormatInfo(cand, mask);
            int penalty = computePenalty(cand);
            if (penalty < bestPenalty) {
                bestPenalty = penalty;
                bestMask = mask;
                bestMatrix = cand;
            }
        }
        return bestMatrix != null ? bestMatrix : applyMask(base, isFunction, bestMask);
    }

    private static void drawFinderPattern(boolean[][] mat, boolean[][] func, int row, int col) {
        for (int dr = 0; dr < 7; dr++) {
            for (int dc = 0; dc < 7; dc++) {
                boolean border = dr == 0 || dr == 6 || dc == 0 || dc == 6;
                boolean inner = dr >= 2 && dr <= 4 && dc >= 2 && dc <= 4;
                setFunc(mat, func, row + dr, col + dc, border || inner);
            }
        }
    }

    private static void drawAlignmentPattern(boolean[][] mat, boolean[][] func, int cr, int cc) {
        for (int dr = -2; dr <= 2; dr++) {
            for (int dc = -2; dc <= 2; dc++) {
                boolean dark = Math.max(Math.abs(dr), Math.abs(dc)) != 1;
                setFunc(mat, func, cr + dr, cc + dc, dark);
            }
        }
    }

    private static void setFunc(boolean[][] mat, boolean[][] func, int r, int c, boolean val) {
        mat[r][c] = val;
        func[r][c] = true;
    }

    private static boolean[][] applyMask(boolean[][] base, boolean[][] isFunc, int mask) {
        int n = base.length;
        boolean[][] out = new boolean[n][n];
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                boolean invert = false;
                if (!isFunc[r][c]) {
                    invert = switch (mask) {
                        case 0 -> (r + c) % 2 == 0;
                        case 1 -> r % 2 == 0;
                        case 2 -> c % 3 == 0;
                        case 3 -> (r + c) % 3 == 0;
                        case 4 -> (r / 2 + c / 3) % 2 == 0;
                        case 5 -> ((r * c) % 2 + (r * c) % 3) == 0;
                        case 6 -> (((r * c) % 2 + (r * c) % 3) % 2) == 0;
                        case 7 -> (((r + c) % 2 + (r * c) % 3) % 2) == 0;
                        default -> false;
                    };
                }
                out[r][c] = base[r][c] ^ invert;
            }
        }
        return out;
    }

    private static void applyFormatInfo(boolean[][] mat, int mask) {
        int n = mat.length;
        // ECC-M indicator = 00 -> 5-bit data is mask (0..7)
        int data = mask & 0x7;
        int rem = data << 10;
        int poly = 0b10100110111;
        for (int i = 4; i >= 0; i--) {
            if (((rem >>> (i + 10)) & 1) != 0) {
                rem ^= (poly << i);
            }
        }
        int bits = ((data << 10) | rem) ^ 0x5412;

        for (int i = 0; i <= 5; i++) mat[8][i] = ((bits >>> (14 - i)) & 1) != 0;
        mat[8][7] = ((bits >>> 8) & 1) != 0;
        mat[8][8] = ((bits >>> 7) & 1) != 0;
        mat[7][8] = ((bits >>> 6) & 1) != 0;
        for (int i = 0; i <= 5; i++) mat[5 - i][8] = ((bits >>> (5 - i)) & 1) != 0;

        for (int i = 0; i < 8; i++) mat[8][n - 1 - i] = ((bits >>> i) & 1) != 0;
        for (int i = 0; i < 7; i++) mat[n - 1 - i][8] = ((bits >>> (14 - i)) & 1) != 0;
    }

    private static int computePenalty(boolean[][] m) {
        int n = m.length;
        int penalty = 0;
        for (int r = 0; r < n; r++) {
            int runRow = 1, runCol = 1;
            for (int i = 1; i < n; i++) {
                if (m[r][i] == m[r][i - 1]) runRow++;
                else {
                    if (runRow >= 5) penalty += 3 + (runRow - 5);
                    runRow = 1;
                }
                if (m[i][r] == m[i - 1][r]) runCol++;
                else {
                    if (runCol >= 5) penalty += 3 + (runCol - 5);
                    runCol = 1;
                }
            }
            if (runRow >= 5) penalty += 3 + (runRow - 5);
            if (runCol >= 5) penalty += 3 + (runCol - 5);
        }
        for (int r = 0; r < n - 1; r++) {
            for (int c = 0; c < n - 1; c++) {
                boolean b = m[r][c];
                if (m[r][c + 1] == b && m[r + 1][c] == b && m[r + 1][c + 1] == b) {
                    penalty += 3;
                }
            }
        }
        int dark = 0;
        for (boolean[] row : m) for (boolean v : row) if (v) dark++;
        int k = Math.abs(dark * 20 - n * n * 10) / (n * n);
        penalty += k * 10;
        return penalty;
    }

    private static int writeBits(byte[] buf, int bitPos, int value, int numBits) {
        for (int i = numBits - 1; i >= 0; i--) {
            if (((value >>> i) & 1) != 0) {
                buf[bitPos >>> 3] |= (byte) (1 << (7 - (bitPos & 7)));
            }
            bitPos++;
        }
        return bitPos;
    }

    private static int gfMul(int a, int b) {
        if (a == 0 || b == 0) return 0;
        return EXP[LOG[a] + LOG[b]];
    }

    private static int[] rsGenerator(int degree) {
        int[] poly = new int[]{1};
        for (int i = 0; i < degree; i++) {
            int root = EXP[i];
            int[] next = new int[poly.length + 1];
            for (int j = 0; j < poly.length; j++) {
                next[j] ^= poly[j];
                next[j + 1] ^= gfMul(poly[j], root);
            }
            poly = next;
        }
        return poly;
    }

    private static byte[] rsRemainder(byte[] data, int[] gen, int degree) {
        int[] rem = new int[data.length + degree];
        for (int i = 0; i < data.length; i++) {
            rem[i] = data[i] & 0xFF;
        }
        for (int i = 0; i < data.length; i++) {
            int factor = rem[i];
            if (factor != 0) {
                for (int j = 0; j < gen.length; j++) {
                    rem[i + j] ^= gfMul(gen[j], factor);
                }
            }
        }
        byte[] ec = new byte[degree];
        for (int i = 0; i < degree; i++) {
            ec[i] = (byte) rem[data.length + i];
        }
        return ec;
    }
}
