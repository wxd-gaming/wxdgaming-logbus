package run;

import lombok.extern.slf4j.Slf4j;

/**
 * 日志上报测试
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-03-12 20:48
 **/
@Slf4j
public class LogBusLocal_2_Test extends LogBusTest {

    public LogBusLocal_2_Test(String configName) {
        super(configName);
    }

    public static void main(String[] args) {
        LogBusLocal_2_Test local_2_test = new LogBusLocal_2_Test("log-boot-local2.yml");
        local_2_test.test();
    }

}
