package wxdgaming.logbus.util;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * md5
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2020-07-29 10:33
 */
@Slf4j
public class Md5Util {

    private final static String[] hexDigits = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"};

    /**
     * 空白字符
     */
    public static final String Null = "";

    /**
     * 转换字节数组为16进制字串
     *
     * @param b 字节数组
     * @return 16进制字串
     */
    private static String byteArrayToHexString(byte[] b) {
        StringBuilder resultSb = new StringBuilder();
        for (byte value : b) {
            resultSb.append(byteToHexString(value));
        }
        return resultSb.toString();
    }

    private static String byteToHexString(byte b) {
        int n = b;
        if (n < 0) {
            n = 256 + n;
        }
        int d1 = n / 16;
        int d2 = n % 16;
        return hexDigits[d1] + hexDigits[d2];
    }

    /**
     * 会把参数拼接起来，
     * <p>
     * 采用md5的 Digest 插值算法
     *
     * @param origin
     * @return
     */
    public static String md5DigestEncode(String... origin) {
        return Md5Util.md5DigestEncode0(Md5Util.Null, origin);
    }

    /**
     * 会把参数用 joinStr 间隔字符 拼接起来
     * <p>
     * 采用md5的 Digest 插值算法
     *
     * @param joinStr 链接字符串 空白字符为 '\b'
     * @param origins 需要验证的字符组合
     * @return
     */
    public static String md5DigestEncode0(String joinStr, String... origins) {
        String resultString = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            if (joinStr == null) {
                joinStr = Md5Util.Null;
            }
            String join = String.join(joinStr, origins);
            resultString = byteArrayToHexString(md.digest(join.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new UnsupportedOperationException(ex);
        }
        return resultString;
    }

    public static String sign(JSONObject jsonObject, String key) {
        String rawToken = jsonObject.entrySet().stream()
                .filter(v -> !v.getKey().equals("token"))
                .sorted(Map.Entry.comparingByKey())
                .map(v -> String.valueOf(v.getKey()) + "=" + JsonUtil.toJSONString(v.getValue()))
                .collect(Collectors.joining());

        log.info("rawToken: {}", rawToken);

        return Md5Util.md5DigestEncode(rawToken, key);
    }


}
