package run;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.logbus.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 日志上报测试
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-03-12 20:48
 **/
@Slf4j
public class LogBusTest {

    static Path path = Path.of("src/test/resources/account.json");
    static List<JSONObject> recordMap = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        HexId hexId = new HexId(1);
        /*模拟每天注册的人数*/
        int random = RandomUtils.random(50, 200);
        for (int j = 0; j < random; j++) {
            JSONObject record = new JSONObject();
            record.fluentPut("createTime", System.currentTimeMillis());
            record.fluentPut("account", j + "-" + LogBus.randomString(6));
            record.fluentPut("uid", hexId.newId());
            recordMap.add(record);
        }
        Files.createDirectories(path.getParent());
        Files.writeString(path, JSON.toJSONString(recordMap));
    }

    public LogBusTest(String configName) {
        LogMain.launch(configName);
        LogBus.getInstance().addRoleLogType("role_copy_success", "副本通关");
        try {
            String string = Files.readString(path);
            recordMap = JSON.parseArray(string, JSONObject.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void postServer() {
        for (int i = 1; i <= 20; i++) {
            JSONObject record = new JSONObject();
            record.fluentPut("uid", i);
            record.fluentPut("mainSid", 0);
            record.fluentPut("name", "测试服-" + i);
            record.fluentPut("showName", "测试服-" + i);
            record.fluentPut("openTime", "2025-01-24 14:02");
            record.fluentPut("maintainTime", "2025-01-24 14:02");
            record.fluentPut("wlan", "wxd-gaming");
            record.fluentPut("lan", "192.168.137.10");
            record.fluentPut("port", 19000);
            record.fluentPut("webPort", 19001);
            record.fluentPut("status", "online");
            record.fluentPut("other", new JSONObject().fluentPut("version", "v1.0.1"));
            LogBus.getInstance().push("", "server/pushList", record);
        }
    }

    public void test() {

        for (JSONObject jsonObject : recordMap) {
            long uid = jsonObject.getLongValue("uid");
            String account = jsonObject.getString("account");
            long createTime = jsonObject.getLongValue("createTime");
            /*创建账号*/
            LogBus.getInstance().registerAccount(account, new JSONObject().fluentPut("os", "xiaomi"));
            int sid = RandomUtils.random(1, 20);

            /*推送角色信息*/
            LogBus.getInstance().pushRole(
                    account, createTime,
                    sid, sid,
                    uid, account,
                    "战士", "女", 1,
                    new JSONObject().fluentPut("os", "xiaomi")
            );

            if (RandomUtils.randomBoolean(3000)) continue;

            LogBus.getInstance().pushLogin(account, uid, account, 1, new JSONObject().fluentPut("os", "xiaomi"));

            /*同步在线状态*/
            ScheduledFuture<?> scheduledFuture = LogBus.getInstance().getScheduledExecutorService().scheduleWithFixedDelay(
                    () -> LogBus.getInstance().online(account, uid),
                    10,
                    10,
                    TimeUnit.SECONDS
            );

            /*2分钟之后下线*/
            LogBus.getInstance().getScheduledExecutorService().schedule(
                    () -> {
                        scheduledFuture.cancel(false);
                        LogBus.getInstance().pushLogout(account, uid, account, 1, new JSONObject().fluentPut("os", "xiaomi"));
                    },
                    RandomUtils.random(2, 5),
                    TimeUnit.MINUTES
            );

            LogBus.getInstance().pushRoleLv(account, uid, 2);

            /*3星通关副本*/
            LogBus.getInstance().pushRoleLog(
                    "role_copy_success",
                    account, uid, account, 1,
                    new JSONObject().fluentPut("copyId", RandomUtils.random(1001, 1102))
                            .fluentPut("star", RandomUtils.random(1, 3))
            );

        }
    }

    public void pushItem() {
        for (JSONObject jsonObject : recordMap) {
            long uid = jsonObject.getLongValue("uid");
            String account = jsonObject.getString("account");
            long createTime = jsonObject.getLongValue("createTime");

            /*上线奖励*/
            testItem(account, uid);
        }
    }

    public void testItem(String account, long roleId) {

        /*上线奖励*/
        int itemId = RandomUtils.random(1000, 1110);
        int change = RandomUtils.random(100, 1000);
        LogBus.getInstance().pushRoleItem(
                account, roleId, account, 2,
                RandomUtils.random(LogBus.ChangeTypeEnum.values()),
                itemId, "货币", false,
                change + 200,
                change,
                "货币", "货币", "上线奖励", "上线奖励",
                new JSONObject()
        );

    }

    public void pushRecharge() {
        for (JSONObject jsonObject : recordMap) {
            long uid = jsonObject.getLongValue("uid");
            String account = jsonObject.getString("account");
            long createTime = jsonObject.getLongValue("createTime");
            pushRecharge(account, uid);
        }
    }

    public void pushRecharge(String account, long roleId) {
        List<Integer> integers = List.of(600, 1200, 6400, 9800, 12800, 25600, 48800, 64800);
        for (Integer amount : integers) {
            if (RandomUtils.randomBoolean(5500)) {/*35%概率会充值*/
                /*充值日志*/
                LogBus.getInstance().pushRecharge(
                        account, roleId, account, 2,
                        "huawei", amount/*单位分*/,
                        StringUtils.randomString(18), StringUtils.randomString(18),
                        new JSONObject().fluentPut("comment", "首充奖励")
                );
            }
        }
    }
}
