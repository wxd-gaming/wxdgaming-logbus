package wxdgaming.logbus;

/**
 * 启动器
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-03-11 20:09
 **/
public class LogMain {

    public static void main(String[] args) {
        launch();
    }

    public static void launch() {
        launch("log-boot.yml");
    }

    public static void launch(String configName) {
        BootConfig.init(configName);
        LogBus.getInstance().init();
    }

}
