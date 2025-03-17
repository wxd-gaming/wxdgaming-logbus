package run;

import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot2.core.RunApplication;
import wxdgaming.logbus.LogMain;

/**
 * 日志上报测试
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-03-12 20:48
 **/
@Slf4j
public class LogBusLocalTest extends LogBusTest {

    public LogBusLocalTest(String configName) {
        super(configName);
    }

    public static void main(String[] args) {
        RunApplication launch = LogMain.launch("log-boot-local.yml");
        test(launch);
    }

}
