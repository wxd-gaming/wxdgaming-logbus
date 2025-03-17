package wxdgaming.logbus;

import lombok.Getter;
import lombok.Setter;
import wxdgaming.logbus.http.HttpClientConfig;
import wxdgaming.logbus.util.YamlUtil;

/**
 * 启动配置
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-14 15:55
 **/
@Getter
@Setter
public class BootConfig {

    @Getter private static BootConfig ins;

    static void init(String configName) {
        ins = YamlUtil.loadYaml(configName);
    }

    private int sid;
    private int appId;
    private String appToken;
    private String logToken;
    private String portUrl;
    private boolean batch = true;
    private int batchSize = 100;

    private int executorCoreSize;

    private HttpClientConfig httpClient;

}
