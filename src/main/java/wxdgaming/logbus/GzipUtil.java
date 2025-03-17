package wxdgaming.logbus;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2020-12-30 20:33
 */
public class GzipUtil {

    /**
     * 使用gzip进行压缩
     *
     * @param primStr
     */
    public static String gzip2String(String primStr) {
        return gzip2String(primStr.getBytes(StandardCharsets.UTF_8));
    }

    public static String gzip2String(byte[] prim) {
        byte[] gzip = gzip(prim);
        return new String(gzip, StandardCharsets.ISO_8859_1);
    }

    public static byte[] gzip2Bytes(String primStr) {
        return gzip(primStr.getBytes(StandardCharsets.UTF_8));
    }


    /**
     * @param prim
     * @return
     */
    public static byte[] gzip(byte[] prim) {
        if (prim == null || prim.length == 0) {
            return prim;
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
                gzip.write(prim);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 从字符串解压
     *
     * @param primStr
     * @return
     */
    public static String unGzip2String(String primStr) {
        try {
            final byte[] bytes = primStr.getBytes(StandardCharsets.ISO_8859_1);
            return unGzip2String(bytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 还原普通字符串
     *
     * @param uncompress
     * @return
     */
    public static String unGzip2String(byte[] uncompress) {
        byte[] bytes = unGZip(uncompress);
        return new String(bytes, StandardCharsets.UTF_8);
    }


    /**
     * @param uncompress
     * @return
     */
    public static byte[] unGZip(byte[] uncompress) {
        if (uncompress == null) {
            return null;
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            try (ByteArrayInputStream input = new ByteArrayInputStream(uncompress)) {
                try (GZIPInputStream gzip = new GZIPInputStream(input)) {
                    byte[] buffer = new byte[256];
                    int offset = -1;
                    while ((offset = gzip.read(buffer)) != -1) {
                        out.write(buffer, 0, offset);
                    }
                }
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
