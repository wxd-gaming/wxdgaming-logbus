package run;

import lombok.extern.slf4j.Slf4j;

/**
 * 日志上报测试
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-03-12 20:48
 **/
@Slf4j
public class LogBusLocal_1_Test extends LogBusTest {

    public LogBusLocal_1_Test(String configName) {
        super(configName);
    }

    public static void main(String[] args) {
        LogBusLocal_1_Test logBusLocal1Test = new LogBusLocal_1_Test("log-boot-local.yml");
        logBusLocal1Test.test();
        // for (int i = 0; i < 1000; i++) {
        //     for (int j = 0; j < 1000; j++) {
        //         logBusLocalTest.pushItem();
        //     }
        //     try {
        //         Thread.sleep(1000);
        //     } catch (InterruptedException e) {
        //         throw new RuntimeException(e);
        //     }
        // }
    }

}
