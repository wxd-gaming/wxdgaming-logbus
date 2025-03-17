package wxdgaming.logbus.http;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;

/**
 * 配置
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-18 10:26
 **/
@Getter
@Setter
public class HttpClientConfig {

    @JSONField(ordinal = 1)
    private int core;
    @JSONField(ordinal = 2)
    private int max;
    @JSONField(ordinal = 3)
    private int resetTimeM;
    @JSONField(ordinal = 4)
    private int connectionRequestTimeout;
    @JSONField(ordinal = 5)
    private int connectTimeOut;
    @JSONField(ordinal = 6)
    private int readTimeout;
    @JSONField(ordinal = 7)
    private int keepAliveTimeout;
    @JSONField(ordinal = 8)
    private String sslProtocol;
    @JSONField(ordinal = 9)
    private boolean autoUseGzip;


}
