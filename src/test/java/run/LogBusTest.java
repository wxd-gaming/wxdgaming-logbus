package run;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.logbus.HexId;
import wxdgaming.logbus.LogBus;
import wxdgaming.logbus.LogMain;
import wxdgaming.logbus.util.RandomUtils;
import wxdgaming.logbus.util.StringUtils;

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
            record.fluentPut("account", j + "-" + StringUtils.randomString(6));
            record.fluentPut("uid", hexId.newId());
            record.fluentPut("sid", RandomUtils.random(1, 20));
            recordMap.add(record);
        }
        Files.createDirectories(path.getParent());
        Files.writeString(path, JSON.toJSONString(recordMap, SerializerFeature.PrettyFormat));
    }

    public LogBusTest(String configName) {
        LogMain.launch(configName);
        LogBus.getInstance().addRoleLogType("role_copy_success", "副本通关");
        LogBus.getInstance().addServerLogType("server_rank_lv", "排行榜");
        try {
            String string = Files.readString(path);
            recordMap = JSON.parseArray(string, JSONObject.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        addServer();
        pushServer();

        /*模拟推送本服排行榜*/
        for (int i = 0; i < recordMap.size(); i++) {
            JSONObject jsonObject = recordMap.get(i);
            long uid = jsonObject.getLongValue("uid");
            String account = jsonObject.getString("account");

            int sid = jsonObject.getIntValue("sid");

            JSONObject rank = new JSONObject()
                    .fluentPut("rank", i + 1)
                    .fluentPut("uid", uid)
                    .fluentPut("account", account);

            LogBus.getInstance().pushServerLog(
                    "server_rank_lv",
                    sid * 10000L + i + 1 /*通过固定uid形式选择覆盖日志记录*/,
                    rank
            );
            if (i >= 99) break;
        }

    }

    public void addServer() {
        for (int i = 1; i <= 20; i++) {
            LogBus.getInstance().addServer(
                    i,
                    "1-100", 1, "new",
                    "正式" + i + "服",
                    System.currentTimeMillis(),
                    "127.0.0.1", "127.0.0.1", 19000, 19000
            );
        }
    }

    public void pushServer() {
        for (int i = 1; i <= 20; i++) {
            LogBus.getInstance().pushServer(
                    i,
                    0,
                    RandomUtils.random(100, 1000),
                    new JSONObject()
                            .fluentPut("version", "v1.0.1")
                            .fluentPut("client-version", "1001"));
        }
    }

    public void test() {

        for (JSONObject jsonObject : recordMap) {
            long uid = jsonObject.getLongValue("uid");
            String account = jsonObject.getString("account");
            long createTime = jsonObject.getLongValue("createTime");
            int sid = jsonObject.getIntValue("sid");
            /*创建账号*/
            LogBus.getInstance().registerAccount(account, new JSONObject().fluentPut("os", "xiaomi"));

            /*推送角色信息*/
            LogBus.getInstance().pushRole(
                    sid, account, createTime,
                    uid, account,
                    "战士", "女", 1,
                    new JSONObject().fluentPut("os", "xiaomi")
            );

            if (RandomUtils.randomBoolean(3000)) continue;

            LogBus.getInstance().pushLogin(sid, account, uid, account, 1, new JSONObject().fluentPut("os", "xiaomi"));

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
                        LogBus.getInstance().pushLogout(sid, account, uid, account, 1, new JSONObject().fluentPut("os", "xiaomi"));
                    },
                    RandomUtils.random(2, 5),
                    TimeUnit.MINUTES
            );

            LogBus.getInstance().pushRoleLv(account, uid, 2);

            pushRecharge(sid, account, uid);
            /*3星通关副本*/
            LogBus.getInstance().pushRoleLog(
                    "role_copy_success",
                    sid,
                    account, uid, account, 1,
                    new JSONObject()
                            .fluentPut("copyId", RandomUtils.random(1001, 1102))
                            .fluentPut("star", RandomUtils.random(1, 3))
            );

        }
    }

    public void pushItem() {
        for (JSONObject jsonObject : recordMap) {
            long uid = jsonObject.getLongValue("uid");
            String account = jsonObject.getString("account");
            long createTime = jsonObject.getLongValue("createTime");
            int sid = jsonObject.getIntValue("sid");
            /*上线奖励*/
            testItem(sid, account, uid);
        }
    }

    public void testItem(int sid, String account, long roleId) {

        /*上线奖励*/
        int itemId = RandomUtils.random(1000, 1110);
        int change = RandomUtils.random(100, 1000);
        LogBus.getInstance().pushRoleItem(
                sid, account, roleId, account, 2,
                RandomUtils.random(LogBus.ChangeTypeEnum.values()),
                itemId, "货币", false,
                change + 200,
                change,
                "货币", "货币", "上线奖励", "上线奖励",
                new JSONObject().fluentPut("shopId", RandomUtils.random(1, 10))
        );

    }

    public void pushRecharge() {
        for (JSONObject jsonObject : recordMap) {
            long uid = jsonObject.getLongValue("uid");
            String account = jsonObject.getString("account");
            long createTime = jsonObject.getLongValue("createTime");
            int sid = jsonObject.getIntValue("sid");
            pushRecharge(sid, account, uid);
        }
    }

    public void pushRecharge(int sid, String account, long roleId) {
        List<Integer> integers = List.of(600, 1200, 6400, 9800, 12800, 25600, 48800, 64800);
        for (Integer amount : integers) {
            if (RandomUtils.randomBoolean(5500)) {/*35%概率会充值*/
                /*充值日志*/
                LogBus.getInstance().pushRecharge(
                        sid, account, roleId, account, 2,
                        "huawei", amount/*单位分*/,
                        StringUtils.randomString(18), StringUtils.randomString(18),
                        new JSONObject().fluentPut("shopId", RandomUtils.random(1, 10)).fluentPut("comment", "首充奖励")
                );
            }
        }
    }
}
