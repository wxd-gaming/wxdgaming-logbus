package run;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot2.core.RunApplication;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.chatset.json.FastJsonUtil;
import wxdgaming.boot2.core.collection.MapOf;
import wxdgaming.boot2.core.format.HexId;
import wxdgaming.boot2.core.io.FileWriteUtil;
import wxdgaming.boot2.core.threading.ExecutorUtil;
import wxdgaming.boot2.core.threading.TimerJob;
import wxdgaming.boot2.core.timer.MyClock;
import wxdgaming.boot2.core.util.RandomUtils;
import wxdgaming.logbus.LogBus;
import wxdgaming.logbus.LogMain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 日志上报测试
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-03-12 20:48
 **/
@Slf4j
public class LogBusTest {

    public static void main(String[] args) {
        HashMap<Integer, List<JSONObject>> recordMap = new HashMap<>();
        int days = 30;
        LocalDateTime localDateTime = LocalDateTime.now().plusDays(-(days));
        HexId hexId = new HexId(1);
        for (int i = 1; i <= days; i++) {
            /*模拟每天注册的人数*/
            int random = RandomUtils.random(50, 200);
            LocalDateTime time = localDateTime.plusDays(i);
            long time2Milli = MyClock.time2Milli(time);
            System.out.println(time);
            List<JSONObject> list = recordMap.computeIfAbsent(i, k -> new ArrayList<>());
            for (int j = 0; j < random; j++) {
                JSONObject record = new JSONObject();
                record.fluentPut("createTime", time2Milli);
                record.fluentPut("account", i + "-" + j + "-" + StringUtils.randomString(6));
                record.fluentPut("uid", hexId.newId());
                list.add(record);
            }
        }
        FileWriteUtil.writeString("src/test/resources/account.json", FastJsonUtil.toJsonFmt(recordMap));
    }

    final RunApplication application;
    final LogBus logBus;

    public LogBusTest(String configName) {
        application = LogMain.launch(configName);
        logBus = application.getInstance(LogBus.class);
    }

    public void test() {

        logBus.addRoleLogType("role_copy_success", "副本通关");

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
            record.fluentPut("other", MapOf.newJSONObject("version", "v1.0.1"));
            logBus.push("", "server/pushList", record);
        }

        for (int i = 0; i < 180; i++) {
            String account = StringUtils.randomString(6);
            /*创建账号*/
            logBus.registerAccount(account, MapOf.newJSONObject("os", "xiaomi"));

            int sid = RandomUtils.random(1, 20);
            long roleId = logBus.getHexId().newId();

            /*推送角色信息*/
            logBus.pushRole(
                    account, System.currentTimeMillis(),
                    sid, sid,
                    roleId, account,
                    "战士", "女", 1,
                    MapOf.newJSONObject("os", "xiaomi")
            );

            if (RandomUtils.randomBoolean(3000)) continue;

            logBus.pushLogin(account, roleId, account, 1, MapOf.newJSONObject("os", "xiaomi"));

            /*同步在线状态*/
            TimerJob timerJob = ExecutorUtil.getInstance().getLogicExecutor().scheduleAtFixedDelay(
                    () -> logBus.online(account, roleId),
                    10,
                    10,
                    TimeUnit.SECONDS
            );

            /*2分钟之后下线*/
            ExecutorUtil.getInstance().getLogicExecutor().schedule(
                    () -> {
                        timerJob.cancel();
                        logBus.pushLogout(account, roleId, account, 1, MapOf.newJSONObject("os", "xiaomi"));
                    },
                    RandomUtils.random(2, 5),
                    TimeUnit.MINUTES
            );

            logBus.pushRoleLv(account, roleId, 2);

            if (RandomUtils.randomBoolean(5500)) {/*35%概率会充值*/
                List<Integer> integers = List.of(600, 1200, 6400, 9800, 12800, 25600, 48800, 64800);
                /*充值日志*/
                logBus.pushRecharge(
                        account, roleId, account, 2,
                        "huawei", RandomUtils.randomItem(integers)/*单位分*/, StringUtils.randomString(18), StringUtils.randomString(18),
                        MapOf.newJSONObject("comment", "首充奖励")
                );
            }

            /*3星通关副本*/
            logBus.pushRoleLog(
                    "role_copy_success",
                    account, roleId, account, 1,
                    MapOf.newJSONObject("copyId", RandomUtils.random(1001, 1102))
                            .fluentPut("star", RandomUtils.random(1, 3))
            );

            /*上线奖励*/
            int random = RandomUtils.random(100, 1000);
            logBus.pushRoleItem(
                    account, roleId, account, 2,
                    RandomUtils.random(LogBus.ChangeTypeEnum.values()),
                    RandomUtils.random(1000, 1010), "货币", false,
                    random + 200,
                    random,
                    "货币", "货币", "上线奖励", "上线奖励",
                    MapOf.newJSONObject()
            );

        }
    }

    public static void testItem(LogBus logBus) {

        /*上线奖励*/
        int random = RandomUtils.random(100, 1000);
        logBus.pushRoleItem(
                account, roleId, account, 2,
                RandomUtils.random(LogBus.ChangeTypeEnum.values()),
                RandomUtils.random(1000, 1010), "货币", false,
                random + 200,
                random,
                "货币", "货币", "上线奖励", "上线奖励",
                MapOf.newJSONObject()
        );

    }
}
