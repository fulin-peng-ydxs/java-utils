package basic.date;

import com.fasterxml.jackson.core.type.TypeReference;
import json.jackson.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 日历工具类，用于处理工作日、节假日等日历相关操作
 *
 * @author pengshuaifeng
 * @since 2025/2/12
 */
@Slf4j
public class CalendarUtils {

    private CalendarUtils() {
        // Utility class should not be instantiated
    }

    /** 日历数据文件路径 */
    private static final String CALENDAR_FILE = "/calendar.json";
    
    /** 法定节假日键名 */
    private static final String HOLIDAY_KEY = "法定节假日";
    
    /** 法定补班日键名 */
    private static final String WORKDAY_KEY = "法定补班日";

    /** 年度节假日数据 */
    private static Map<String, Map<String, List<String>>> holidayData;

    static {
        initializeHolidayData();
    }

    /**
     * 初始化节假日数据
     */
    private static void initializeHolidayData() {
        try (InputStream resourceAsStream = DateUtils.class.getResourceAsStream(CALENDAR_FILE)) {
            if (resourceAsStream == null) {
                log.error("未找到日历数据文件: {}", CALENDAR_FILE);
                holidayData = Collections.emptyMap();
                return;
            }
            
            holidayData = JsonUtils.getMap(
                resourceAsStream, 
                new TypeReference<Map<String, Map<String, List<String>>>>() {}
            );
            
            if (holidayData == null) {
                log.error("解析日历数据失败");
                holidayData = Collections.emptyMap();
            }
        } catch (Exception e) {
            log.error("加载假期数据失败: {}", e.getMessage(), e);
            holidayData = Collections.emptyMap();
        }
    }

    /**
     * 获取节假日数据
     * @return 节假日数据Map
     */
    public static Map<String, Map<String, List<String>>> getHolidayData() {
        return Collections.unmodifiableMap(holidayData);
    }

    /**
     * 判断指定日期是否为工作日
     * 工作日判定规则：
     * 1. 如果是法定节假日，则不是工作日
     * 2. 如果是法定补班日，则是工作日
     * 3. 如果是周末（周六日），则不是工作日
     * 4. 其他情况为工作日
     *
     * @param date 待判定的日期
     * @param year 所在年份
     * @return true如果是工作日，false如果是节假日
     * @throws IllegalArgumentException 如果参数为null或未加载到对应年份的节假日数据
     * @author pengfulin
     * @since 2023/2/16 20:59
     */
    public static boolean isWeekday(LocalDate date, int year) {
        if (date == null) {
            throw new IllegalArgumentException("日期不能为null");
        }

        Map<String, List<String>> yearHoliday = holidayData.get(String.valueOf(year));
        if (yearHoliday == null) {
            throw new IllegalArgumentException(String.format("未加载到%d年的节假日数据", year));
        }

        String dateStr = DateUtils.format(date, "yyyy-MM-dd");
        
        // 检查是否为法定节假日
        List<String> holidays = yearHoliday.get(HOLIDAY_KEY);
        if (holidays != null && holidays.contains(dateStr)) {
            return false;
        }

        // 检查是否为补班日
        List<String> workdays = yearHoliday.get(WORKDAY_KEY);
        if (workdays != null && workdays.contains(dateStr)) {
            return true;
        }

        // 检查是否为周末
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }

    /**
     * 重新加载节假日数据
     * @return true如果加载成功，false如果加载失败
     */
    public static boolean reloadHolidayData() {
        try {
            initializeHolidayData();
            return !holidayData.isEmpty();
        } catch (Exception e) {
            log.error("重新加载假期数据失败: {}", e.getMessage(), e);
            return false;
        }
    }
}
