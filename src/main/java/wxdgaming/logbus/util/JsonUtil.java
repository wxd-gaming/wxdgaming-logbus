package wxdgaming.logbus.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.serializer.SerializerFeature;

/**
 * json
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-05-18 10:27
 **/
public class JsonUtil {

    /** 默认值 */
    public static final SerializerFeature[] Writer_Features;

    /** 默认值 */
    public static final Feature[] Reader_Features;

    static {
        /*fast json 启动类型自动推断*/
        Writer_Features = new SerializerFeature[]{
                SerializerFeature.QuoteFieldNames,                  /*给字段加引号*/
                SerializerFeature.WriteMapNullValue,                /*map字段如果为null,输出为null*/
                SerializerFeature.WriteNullListAsEmpty,             /*List字段如果为null,输出为[],而非null*/
                SerializerFeature.WriteNullNumberAsZero,            /*数值字段如果为null,输出为0,而非null*/
                SerializerFeature.WriteNullBooleanAsFalse,          /*Boolean字段如果为null,输出为false,而非null*/
                SerializerFeature.WriteNullStringAsEmpty,           /*String字段如果为null,输出为"",而非null*/
                SerializerFeature.SkipTransientField,               /*忽律 transient*/
                SerializerFeature.WriteEnumUsingName,               /*枚举用 toString() */
                // SerializerFeature.IgnoreNonFieldGetter,             /*忽略 没有 get 属性 继续写入*/
                SerializerFeature.DisableCircularReferenceDetect,   /*屏蔽循环引用*/
                SerializerFeature.SortField,                        /*排序*/
                SerializerFeature.MapSortField                      /*排序*/
        };


        Reader_Features = new Feature[]{
                // Feature.OrderedField,
                Feature.SupportAutoType
        };

    }

    public static String toJSONString(Object obj) {
        return JSON.toJSONString(obj, Writer_Features);
    }


    public static JSONObject parseObject(String obj) {
        return JSON.parseObject(obj, JSONObject.class, Reader_Features);
    }

}
