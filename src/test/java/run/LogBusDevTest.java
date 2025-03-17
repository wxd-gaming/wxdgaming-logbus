package run;

import lombok.extern.slf4j.Slf4j;

/**
 * 日志上报测试
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-03-12 20:48
 **/
@Slf4j
public class LogBusDevTest extends LogBusTest {


    public LogBusDevTest(String configName) {
        super(configName);
    }

    public static void main(String[] args) {
        LogBusDevTest logBusDevTest = new LogBusDevTest("log-boot-dev.yml");
        logBusDevTest.test();
    }

}
