package code;

import com.alibaba.fastjson.JSONObject;
import org.junit.Test;
import wxdgaming.logbus.util.JsonUtil;
import wxdgaming.logbus.util.Md5Util;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-05-18 11:13
 **/
public class JsonTest {

    @Test
    public void test1() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("gameId", "1");
        jsonObject.put("name", "<UNK>");

        List<JSONObject> dataList = new ArrayList<>();

        JSONObject data = new JSONObject();
        data.put("uid", 10001);
        data.put("sid", 1);
        data.put("logType", "server_rank_lv");
        data.put("ohter", new JSONObject().fluentPut("rank", 1).fluentPut("account", "a"));
        dataList.add(data);

        jsonObject.put("data", dataList);
        Md5Util.sign(jsonObject,"123");
        System.out.println(JsonUtil.toJSONString(jsonObject));
        JSONObject jsonObject1 = JsonUtil.parseObject(jsonObject.toJSONString());
        Md5Util.sign(jsonObject1,"123");
        System.out.println(JsonUtil.toJSONString(jsonObject1));
    }

}
