package wxdgaming.logbus.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 文件辅助
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-03-17 17:38
 **/
public class FileUtil {

    /** 把内容覆盖到文件 */
    public static void writeString2File(Path path, String content) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, content);
        } catch (Exception ex) {
            throw new RuntimeException("写入文件失败：" + path, ex);
        }
    }

    public static void writeLog2File(Path path, String url, List<JSONObject> content) {
        writeString2File(path, "0\n%s\n%s".formatted(url, JSON.toJSONString(content)));
    }

    public static String readLog4File(Path path) {
        try {
            return Files.readString(path);
        } catch (Exception ex) {
            throw new RuntimeException("写入文件失败：" + path, ex);
        }
    }

}
