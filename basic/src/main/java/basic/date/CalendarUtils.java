package basic.date;

import com.fasterxml.jackson.core.type.TypeReference;
import json.jackson.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import java.io.InputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 日历工具类
 * @author pengshuaifeng
 * 2025/2/12
 */
@Slf4j
public class CalendarUtils {

    //年度节假日数据
    public static Map<String, Map<String, List<String>>> holidayData;

    //数据加载
    static {
        try {
            InputStream resourceAsStream = DateUtils.class.getResourceAsStream("/calendar.json");
            holidayData = JsonUtils.getMap(resourceAsStream, new TypeReference<Map<String, Map<String, List<String>>>>() {});
            if (resourceAsStream != null)
                resourceAsStream.close();
        } catch (Exception e) {
            if(log.isTraceEnabled()){
                log.error("加载假期数据失败",e);
            }
        }
    }

    /**是否为工作日
     * 2023/2/16 0016-20:59
     * @author pengfulin
     * @param date 本地时间
     * @param year 所在年
     */
    public static boolean isWeekday(LocalDate date, int year){
        Map<String, List<String>> yearHoliday = holidayData.get(String.valueOf(year));
        if(yearHoliday==null)
            throw new RuntimeException("未加载到"+year+"的节假日数据");
        List<String> holiday = yearHoliday.get("法定节假日");
        List<String> shift = yearHoliday.get("法定补班日");
        String ymd = DateUtils.format(date,"yyyy-MM-dd");
        //是否为节假日：false
        if(holiday.contains(ymd))
            return false;
        //是否为节假日补班：true
        if(shift.contains(ymd))
            return true;
        //是否为周末：false
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }

}