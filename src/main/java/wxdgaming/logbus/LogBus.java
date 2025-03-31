package wxdgaming.logbus;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.logbus.collection.LinkedTable;
import wxdgaming.logbus.collection.SplitCollection;
import wxdgaming.logbus.http.HttpClientPool;
import wxdgaming.logbus.http.PostJson;
import wxdgaming.logbus.http.Response;
import wxdgaming.logbus.util.FileUtil;
import wxdgaming.logbus.util.StringUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 日志载体
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-03-11 20:15
 **/
@Slf4j
@Getter
public class LogBus {

    @Getter private static final LogBus instance = new LogBus();

    private ReentrantLock lock = new ReentrantLock();
    private LinkedTable<String, String, SplitCollection<JSONObject>> logMap = new LinkedTable<>();
    private ScheduledExecutorService scheduledExecutorService;
    private HexId hexId;
    private AtomicLong postHash = new AtomicLong(0);
    private HashMap<Path, PostRunnable> postRunnableMap = new HashMap(0);


    public void init() {
        this.hexId = new HexId(BootConfig.getIns().getSid());
        this.scheduledExecutorService = Executors.newScheduledThreadPool(BootConfig.getIns().getExecutorCoreSize() + 2);
        Path pathBase = Paths.get("target", "post", String.valueOf(BootConfig.getIns().getAppId()));

        scheduledExecutorService.scheduleWithFixedDelay(
                () -> {
                    LinkedTable<String, String, SplitCollection<JSONObject>> tmpMap;
                    lock.lock();
                    try {
                        if (logMap.isEmpty()) return;
                        tmpMap = logMap;
                        logMap = new LinkedTable<>();
                    } finally {
                        lock.unlock();
                    }
                    for (Map.Entry<String, LinkedHashMap<String, SplitCollection<JSONObject>>> mapEntry : tmpMap.entrySet()) {
                        for (Map.Entry<String, SplitCollection<JSONObject>> entry : mapEntry.getValue().entrySet()) {
                            String postUrl = entry.getKey();
                            SplitCollection<JSONObject> value = entry.getValue();
                            while (!value.isEmpty()) {
                                List<JSONObject> jsonObjects = value.removeFirst();
                                long l = postHash.incrementAndGet();
                                long hash = l % BootConfig.getIns().getExecutorCoreSize();
                                Path path = pathBase.resolve(String.valueOf(hash)).resolve("data").resolve(System.nanoTime() + "-" + StringUtils.randomString(4) + ".dat");
                                FileUtil.writeLog2File(path, postUrl, jsonObjects);
                            }
                        }
                    }
                },
                3_000,
                33,
                TimeUnit.MILLISECONDS
        );

        for (int i = 0; i < BootConfig.getIns().getExecutorCoreSize(); i++) {
            final Path path = pathBase.resolve(String.valueOf(i));
            /*本次没执行完成不执行下一个*/
            PostRunnable postRunnable = new PostRunnable(path);
            postRunnableMap.put(path, postRunnable);
        }

        scheduledExecutorService.scheduleWithFixedDelay(
                () -> {
                    for (Map.Entry<Path, PostRunnable> entry : postRunnableMap.entrySet()) {
                        PostRunnable postRunnable = entry.getValue();
                        /*本次没执行完成不执行下一个*/
                        if (postRunnable.running.get()) continue;
                        String[] files = postRunnable.file.list();
                        /*当前目录不为空*/
                        if (files == null || files.length < 1) continue;
                        if (postRunnable.running.compareAndSet(false, true)) {
                            scheduledExecutorService.execute(postRunnable);
                        }
                    }
                },
                3_000,
                33,
                TimeUnit.MILLISECONDS
        );

    }

    public void addRoleLogType(String logType, String logComment) {
        JSONObject postData = new JSONObject();
        postData.put("gameId", BootConfig.getIns().getAppId());
        postData.put("token", BootConfig.getIns().getAppToken());
        postData.put("logType", logType);
        postData.put("logComment", logComment);
        String uriPath = BootConfig.getIns().getPortUrl() + "/game/addRoleLogType";
        try {
            Response<PostJson> request = new PostJson(HttpClientPool.getDefault(), uriPath)
                    .setParams(postData.toJSONString())
                    .readTimeout(130000)
                    .retry(2)
                    .request();
            log.info("logbus push {} result {}", uriPath, request.bodyString());
        } catch (Exception e) {
            log.error("logbus push {} fail", uriPath, e);
        }
    }

    public void addServerLogType(String logType, String logComment) {
        JSONObject postData = new JSONObject();
        postData.put("gameId", BootConfig.getIns().getAppId());
        postData.put("token", BootConfig.getIns().getAppToken());
        postData.put("logType", logType);
        postData.put("logComment", logComment);
        String uriPath = BootConfig.getIns().getPortUrl() + "/game/addServerLogType";
        try {
            Response<PostJson> request = new PostJson(HttpClientPool.getDefault(), uriPath)
                    .setParams(postData.toJSONString())
                    .readTimeout(130000)
                    .retry(2)
                    .request();
            log.info("logbus push {} result {}", uriPath, request.bodyString());
        } catch (Exception e) {
            log.error("logbus push {} fail", uriPath, e);
        }
    }

    /**
     * 开新服配置
     *
     * @param sid      服务器id
     * @param group    分组信息，比如1-100，101-200
     * @param ordinal  显示顺序
     * @param label    标签 比如new-新服，recommend-推荐服
     * @param name     名字
     * @param openTime 开服时间
     * @param wlan     外网IP 或者域名
     * @param lan      内网IP 或者域名
     * @param port     端口
     * @param webPort  web端口
     * @author: wxd-gaming(無心道, 15388152619)
     * @version: 2025-03-19 10:05
     */
    public void addServer(int sid, String group, int ordinal, String label, String name, long openTime, String wlan, String lan, int port, int webPort, boolean enabled) {
        JSONObject record = new JSONObject();
        record.fluentPut("uid", sid);
        record.fluentPut("group", group);
        record.fluentPut("ordinal", ordinal);
        record.fluentPut("label", label);
        record.fluentPut("name", name);
        record.fluentPut("openTime", openTime);
        record.fluentPut("wlan", wlan);
        record.fluentPut("lan", lan);
        record.fluentPut("port", port);
        record.fluentPut("webPort", webPort);
        record.fluentPut("enabled", enabled);

        push("", "server/addList", record);
    }

    /** 同步游戏数据，比如版本号等 */
    public void pushServer(int sid, int mainSid, boolean enabled, int roleCount, JSONObject other) {
        JSONObject record = new JSONObject();
        record.fluentPut("uid", sid);
        record.fluentPut("mainSid", mainSid);
        record.fluentPut("enabled", enabled);
        record.fluentPut("registerRoleCount", roleCount);
        record.fluentPut("other", other);
        push("", "server/pushList", record);
    }

    /** 创建一个账号 */
    public void registerAccount(String account, JSONObject other) {
        registerAccount(account, System.currentTimeMillis(), other);
    }

    /** 创建一个账号 */
    public void registerAccount(String account, long createTime, JSONObject other) {
        JSONObject jsonObject = new JSONObject()
                .fluentPut("account", account)
                .fluentPut("createTime", createTime)
                .fluentPut("other", other);
        push(account, "log/account/pushList", jsonObject);
    }

    /** 创建角色或者修改角色数据 */
    public void pushRole(int sid, String account, long createTime, long roleId, String roleName, String job, String sex, int lv, JSONObject other) {

        JSONObject record = new JSONObject();
        record.put("uid", roleId);
        record.put("account", account);
        record.put("createTime", createTime);
        record.put("sid", sid);
        record.put("roleName", roleName);
        record.put("Job", job);
        record.put("sex", sex);
        record.put("lv", lv);
        record.put("other", other);/*附加参数，本身也是一个json*/

        push(account, "log/role/pushList", record);
    }

    /** 修改角色等级 */
    public void pushRoleLv(String account, long roleId, int lv) {

        JSONObject record = new JSONObject();
        record.put("account", account);
        record.put("roleId", roleId);
        record.put("lv", lv);

        push(account, "log/role/lvList", record);
    }

    /** 同步在线状态 建议每分钟一次 */
    public void online(String account, long roleId) {
        JSONObject jsonObject = new JSONObject()
                .fluentPut("account", account)
                .fluentPut("roleId", roleId)
                .fluentPut("time", System.currentTimeMillis());
        push(account, "log/role/login/onlineList", jsonObject);
    }

    public void pushLogin(String account, long roleId, String roleName, int lv, JSONObject other) {
        pushLogin(BootConfig.getIns().getSid(), account, roleId, roleName, lv, other);
    }

    public void pushLogin(int sid, String account, long roleId, String roleName, int lv, JSONObject other) {
        JSONObject jsonObject = new JSONObject()
                .fluentPut("logEnum", "LOGIN")
                .fluentPut("uid", hexId.newId())/*指定一个唯一id，这样可以避免因为网络重复提交导致出现重复数据*/
                .fluentPut("createTime", System.currentTimeMillis())
                .fluentPut("sid", sid)
                .fluentPut("account", account)
                .fluentPut("roleId", roleId)
                .fluentPut("roleName", roleName)
                .fluentPut("lv", lv)
                .fluentPut("other", other);
        push(account, "log/role/login/pushList", jsonObject);
    }

    public void pushLogout(String account, long roleId, String roleName, int lv, JSONObject other) {
        pushLogout(BootConfig.getIns().getSid(), account, roleId, roleName, lv, other);
    }

    public void pushLogout(int sid, String account, long roleId, String roleName, int lv, JSONObject other) {
        JSONObject jsonObject = new JSONObject()
                .fluentPut("logEnum", "LOGOUT")
                .fluentPut("uid", hexId.newId())/*指定一个唯一id，这样可以避免因为网络重复提交导致出现重复数据*/
                .fluentPut("createTime", System.currentTimeMillis())
                .fluentPut("sid", sid)
                .fluentPut("account", account)
                .fluentPut("roleId", roleId)
                .fluentPut("roleName", roleName)
                .fluentPut("lv", lv)
                .fluentPut("other", other);
        push(account, "log/role/login/pushList", jsonObject);
    }

    public void pushRecharge(String account, long roleId, String roleName, int lv,
                             String channel, long amount, String spOrder, String cpOrder,
                             JSONObject other) {
        pushRecharge(BootConfig.getIns().getSid(), account, roleId, roleName, lv, channel, amount, spOrder, cpOrder, other);
    }

    public void pushRecharge(int sid, String account, long roleId, String roleName, int lv,
                             String channel, long amount, String spOrder, String cpOrder,
                             JSONObject other) {
        JSONObject jsonObject = new JSONObject()
                .fluentPut("uid", hexId.newId())/*指定一个唯一id，这样可以避免因为网络重复提交导致出现重复数据*/
                .fluentPut("createTime", System.currentTimeMillis())
                .fluentPut("sid", sid)
                .fluentPut("account", account)
                .fluentPut("roleId", roleId)
                .fluentPut("roleName", roleName)
                .fluentPut("lv", lv)
                .fluentPut("channel", channel)
                .fluentPut("amount", amount)
                .fluentPut("spOrder", spOrder)
                .fluentPut("cpOrder", cpOrder)
                .fluentPut("other", other);
        push(account, "log/recharge/pushList", jsonObject);
    }

    /**
     * 根据角色相关的通用的日志
     *
     * @param logType  日志类型
     * @param account  玩家账号
     * @param roleId   玩家的id
     * @param roleName 玩家名字
     * @param lv       玩家等级
     * @param other    附加的信息
     * @author: wxd-gaming(無心道, 15388152619)
     * @version: 2025-03-13 09:03
     */
    public void pushRoleLog(String logType, String account, long roleId, String roleName, int lv, JSONObject other) {
        pushRoleLog(logType, BootConfig.getIns().getSid(), account, roleId, roleName, lv, other);
    }

    public void pushRoleLog(String logType, int sid, String account, long roleId, String roleName, int lv, JSONObject other) {
        JSONObject jsonObject = new JSONObject()
                .fluentPut("logType", logType)
                .fluentPut("uid", hexId.newId())/*指定一个唯一id，这样可以避免因为网络重复提交导致出现重复数据*/
                .fluentPut("createTime", System.currentTimeMillis())
                .fluentPut("sid", sid)
                .fluentPut("account", account)
                .fluentPut("roleId", roleId)
                .fluentPut("roleName", roleName)
                .fluentPut("lv", lv)
                .fluentPut("other", other);
        push(account, "log/role/slog/pushList", jsonObject);
    }

    /**
     * @param account     账号
     * @param roleId      角色id
     * @param roleName    角色名称
     * @param lv          角色等级
     * @param changeType  变更类型，是获得还是消耗
     * @param itemId      道具第
     * @param itemBind    是否绑定
     * @param itemCount   当前数量
     * @param change      本次变更的数量
     * @param itemType    道具类型
     * @param itemSubType 道具子类型
     * @param source      来源。比如是某系统
     * @param comment     备注。比如来源是任务系统，具体的任务id
     * @param other       其它备注的记录，比如交易，谁交易给谁
     * @author: wxd-gaming(無心道, 15388152619)
     * @version: 2025-03-12 20:38
     */
    public void pushRoleItem(String account, long roleId, String roleName, int lv,
                             ChangeTypeEnum changeType,
                             int itemId, String itemName, boolean itemBind, long itemCount, long change,
                             String itemType, String itemSubType,
                             String source, String comment,
                             JSONObject other) {
        pushRoleItem(
                BootConfig.getIns().getSid(),
                account, roleId, roleName, lv,
                changeType, itemId, itemName, itemBind, itemCount, change,
                itemType, itemSubType, source, comment,
                other
        );
    }

    public void pushRoleItem(int sid, String account, long roleId, String roleName, int lv,
                             ChangeTypeEnum changeType,
                             int itemId, String itemName, boolean itemBind, long itemCount, long change,
                             String itemType, String itemSubType,
                             String source, String comment,
                             JSONObject other) {
        JSONObject jsonObject = new JSONObject()
                .fluentPut("uid", hexId.newId())/*指定一个唯一id，这样可以避免因为网络重复提交导致出现重复数据*/
                .fluentPut("createTime", System.currentTimeMillis())
                .fluentPut("sid", sid)
                .fluentPut("account", account)
                .fluentPut("roleId", roleId)
                .fluentPut("roleName", roleName)
                .fluentPut("lv", lv)
                .fluentPut("changeType", changeType)
                .fluentPut("itemId", itemId)
                .fluentPut("itemName", itemName)
                .fluentPut("itemBind", itemBind)
                .fluentPut("itemCount", itemCount)
                .fluentPut("change", change)
                .fluentPut("itemType", itemType)
                .fluentPut("itemSubType", itemSubType)
                .fluentPut("source", source)
                .fluentPut("comment", comment)
                .fluentPut("other", other);
        push(account, "log/role/item/pushList", jsonObject);
    }

    /**
     * 根据角色相关的通用的日志
     *
     * @param logType 日志类型
     * @param uid     日志id
     * @param other   附加的信息
     * @author: wxd-gaming(無心道, 15388152619)
     * @version: 2025-03-13 09:03
     */
    public void pushServerLog(String logType, long uid, JSONObject other) {
        pushServerLog(logType, BootConfig.getIns().getSid(), uid, other);
    }

    public void pushServerLog(String logType, int sid, long uid, JSONObject other) {
        JSONObject jsonObject = new JSONObject()
                .fluentPut("logType", logType)
                .fluentPut("uid", uid)/*指定一个唯一id，这样可以避免因为网络重复提交导致出现重复数据*/
                .fluentPut("sid", sid)
                .fluentPut("other", other);
        push("", "log/server/slog/pushList", jsonObject);
    }

    private void push(String key, String url, JSONObject data) {
        lock.lock();
        try {
            key = java.lang.String.valueOf(key.hashCode() % 20);
            SplitCollection<JSONObject> collection = logMap.computeIfAbsent("", url, k -> new SplitCollection<>(BootConfig.getIns().getBatchSize()));
            collection.add(data);
        } finally {
            lock.unlock();
        }
    }

    public enum ChangeTypeEnum {
        /** 获取 */
        GET,
        /** 消耗 */
        COST
    }

}
