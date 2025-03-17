package wxdgaming.logbus;

import com.alibaba.fastjson.JSONObject;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * yaml文件读取
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2024-04-23 17:23
 **/
public class YamlUtil {

    public static BootConfig loadYaml(String path) {
        return loadYaml(Thread.currentThread().getContextClassLoader(), path);
    }

    public static BootConfig loadYaml(ClassLoader classLoader, String path) {
        try {
            if (new File(path).exists()) {
                return loadYaml(new FileInputStream(path));
            } else {
                InputStream resourceAsStream = classLoader.getResourceAsStream(path);
                return loadYaml(resourceAsStream);
            }
        } catch (Exception e) {
            throw new RuntimeException("读取文件：" + path, e);
        }
    }

    public static BootConfig loadYaml(InputStream inputStream) {
        DumperOptions dumperOptions = new DumperOptions();
        Representer representer = new Representer(dumperOptions);
        representer.getPropertyUtils().setSkipMissingProperties(true);
        Yaml yaml = new Yaml(representer, dumperOptions);
        return yaml.loadAs(inputStream, BootConfig.class);
    }

    /** 把指定类型转换成 yaml 文件 */
    public static String dumpYaml(Object source) {
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
        Representer representer = new Representer(dumperOptions);
        representer.getPropertyUtils().setSkipMissingProperties(true);
        Yaml yaml = new Yaml(representer, dumperOptions);
        return yaml.dumpAsMap(source);
    }

}
