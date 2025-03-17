package wxdgaming.logbus;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

/**
 * id算法
 * <p>因为无符号 所以每一秒的id最大值是52万
 * <p>hexId 取值范围 1 ~ 16500
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2024-09-24 15:08
 */
public class HexId implements Serializable {

    static final String max_exception_format = "每秒钟生成的最大值 %d";
    static final String exception_format = "id=%d, hexId=%d, secondByDay=%d, seed=%d";

    /** 相当于1970年1月1日，到2024年9月24日 经过了这么多天 */
    public static final int OffSetDays = 19990;
    /** 19位的最大值 */
    public static final int Offset12 = 0xFFF;
    public static final int Offset14 = 0x3FFF;
    public static final int Offset17 = 0x1FFFF;
    public static final long Offset20 = 0XFFFFF;

    final long hexId;
    volatile long lastDays = 0;
    volatile long lastSecondByDay = 0;
    volatile long seed = 0;

    public HexId(long hexId) {
        AssertUtil.assertTrueFmt(0 < hexId && hexId < Offset14, "取值范围 1 ~ %s", Offset14);
        this.hexId = hexId;
    }

    /** 得到当前本地系统时间 周期天数 1970 年开始 */
    static int __days(long millis) {
        Instant instant = Instant.ofEpochMilli(millis);
        LocalDate localDate = LocalDate.ofInstant(instant, ZoneId.systemDefault());
        return (int) localDate.toEpochDay();
    }

    /** 用秒表示今天日期 */
    static int dayOfSecond(long millis) {
        Instant instant = Instant.ofEpochMilli(millis);
        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        return (int) (TimeUnit.HOURS.toSeconds(localDateTime.getHour())
                      + TimeUnit.MINUTES.toSeconds(localDateTime.getMinute())
                      + localDateTime.getSecond());
    }

    /** 因为无符号 所以每一秒的id最大值是52万 */
    public synchronized long newId() {
        long nanoTime = System.currentTimeMillis();
        final long days = __days(nanoTime) - OffSetDays;
        final long secondByDay = dayOfSecond(nanoTime);
        if (lastDays != days || secondByDay != lastSecondByDay) {
            seed = 0;
            lastDays = days;
            lastSecondByDay = secondByDay;
        }

        /*因为无符号 所以每一秒的id最大值是52万*/
        seed++;
        AssertUtil.assertTrueFmt(
                seed < Offset20,
                max_exception_format,
                Offset20
        );
        //   hexId 占14位     day 占用12位  second 占17位       seed 占用19位
        long lid = hexId << 49;
        lid |= days << 37;
        lid |= secondByDay << 20;
        lid |= seed;
        int i = secondByDay(lid);
        long idValue = idValue(lid);

        AssertUtil.assertTrueFmt(
                i == secondByDay && idValue == seed,
                exception_format,
                lid, hexId, secondByDay, seed
        );
        return lid;
    }

    public static int hexId(long value) {
        return (int) (value >> 49 & Offset14);
    }

    public static int days(long value) {
        return (int) (value >> 37 & Offset12);
    }

    public static int secondByDay(long value) {
        return (int) (value >> 20 & Offset17);
    }

    public static long idValue(long value) {
        return (value & Offset20);
    }

}
