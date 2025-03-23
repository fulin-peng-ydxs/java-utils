package basic.date;

import lombok.extern.slf4j.Slf4j;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 日期时间工具类，提供日期时间的格式化、转换、计算等功能
 *
 * @author pengshuaifeng
 * @since 2023/12/27
 */
@Slf4j
public class DateUtils {

    private DateUtils() {
        // Utility class should not be instantiated
    }

    /** 常用日期时间格式 */
    public static final String DEFAULT_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String TIME_FORMAT = "HH:mm:ss";
    public static final String DATETIME_FORMAT_NO_SEC = "yyyy-MM-dd HH:mm";
    public static final String DATETIME_FORMAT_COMPACT = "yyyyMMddHHmmss";
    public static final String DATE_FORMAT_COMPACT = "yyyyMMdd";
    public static final String TIME_FORMAT_COMPACT = "HHmmss";
    public static final String DATETIME_FORMAT_WITH_MILLIS = "yyyy-MM-dd HH:mm:ss.SSS";
    public static final String DATETIME_FORMAT_ISO = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String DATETIME_FORMAT_ISO_WITH_ZONE = "yyyy-MM-dd'T'HH:mm:ssXXX";

    /** 默认时区 */
    private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();

    /** 格式化器缓存 */
    private static final Map<String, DateTimeFormatter> FORMATTER_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, DateFormat> DATE_FORMAT_CACHE = new ConcurrentHashMap<>();

    /** 默认格式化器 */
    public static final DateTimeFormatter DEFAULT_FORMATTER = getFormatter(DEFAULT_FORMAT);
    public static final DateTimeFormatter DATE_FORMATTER = getFormatter(DATE_FORMAT);
    public static final DateTimeFormatter TIME_FORMATTER = getFormatter(TIME_FORMAT);
    public static final DateTimeFormatter ISO_FORMATTER = getFormatter(DATETIME_FORMAT_ISO);
    public static final DateTimeFormatter ISO_ZONE_FORMATTER = getFormatter(DATETIME_FORMAT_ISO_WITH_ZONE);
    /**
     * 获取DateTimeFormatter（从缓存中）
     * @param pattern 日期格式
     * @return DateTimeFormatter实例
     */
    private static DateTimeFormatter getFormatter(String pattern) {
        return FORMATTER_CACHE.computeIfAbsent(pattern, 
            DateTimeFormatter::ofPattern);
    }

    /**
     * 获取DateFormat（从缓存中）
     * @param pattern 日期格式
     * @return DateFormat实例
     */
    private static DateFormat getDateFormatFromCache(String pattern) {
        return DATE_FORMAT_CACHE.computeIfAbsent(pattern, 
            SimpleDateFormat::new);
    }

    /**
     * 获取指定时区的当前时间
     * @param zoneId 时区ID
     * @return 指定时区的当前时间
     */
    public static LocalDateTime getCurrentTimeInZone(ZoneId zoneId) {
        return LocalDateTime.now(zoneId != null ? zoneId : DEFAULT_ZONE);
    }

    /**
     * 转换时间到指定时区
     * @param dateTime 日期时间
     * @param sourceZone 源时区
     * @param targetZone 目标时区
     * @return 转换后的时间
     */
    public static LocalDateTime convertTimeZone(LocalDateTime dateTime, 
            ZoneId sourceZone, ZoneId targetZone) {
        if (dateTime == null || sourceZone == null || targetZone == null) {
            return dateTime;
        }
        ZonedDateTime source = dateTime.atZone(sourceZone);
        return source.withZoneSameInstant(targetZone).toLocalDateTime();
    }

    /**
     * 获取指定日期是一年中的第几周
     * @param date 日期
     * @return 周数，如果date为null则返回0
     */
    public static int getWeekOfYear(Date date) {
        if (date == null) {
            return 0;
        }
        LocalDateTime localDateTime = dateToLocalDateTime(date);
        return localDateTime.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear());
    }

    /**
     * 获取指定日期是星期几
     * @param date 日期
     * @return 星期几（1-7，1表示星期一），如果date为null则返回0
     */
    public static int getDayOfWeek(Date date) {
        if (date == null) {
            return 0;
        }
        return dateToLocalDateTime(date).getDayOfWeek().getValue();
    }

    /**
     * 获取指定日期所在季度的第一天
     * @param date 日期
     * @return 季度第一天
     */
    public static Date getFirstDayOfQuarter(Date date) {
        if (date == null) {
            return null;
        }
        LocalDateTime localDateTime = dateToLocalDateTime(date);
        int month = localDateTime.getMonthValue();
        int quarterMonth = ((month - 1) / 3) * 3 + 1;
        return localDateTimeToDate(localDateTime
            .withMonth(quarterMonth)
            .withDayOfMonth(1)
            .withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0));
    }

    /**
     * 获取指定日期所在季度的最后一天
     * @param date 日期
     * @return 季度最后一天
     */
    public static Date getLastDayOfQuarter(Date date) {
        if (date == null) {
            return null;
        }
        LocalDateTime localDateTime = dateToLocalDateTime(date);
        int month = localDateTime.getMonthValue();
        int quarterMonth = ((month - 1) / 3) * 3 + 3;
        return localDateTimeToDate(localDateTime
            .withMonth(quarterMonth)
            .with(TemporalAdjusters.lastDayOfMonth())
            .withHour(23)
            .withMinute(59)
            .withSecond(59)
            .withNano(999999999));
    }

    /**
     * 获取指定日期所在年份的第一天
     * @param date 日期
     * @return 年份第一天
     */
    public static Date getFirstDayOfYear(Date date) {
        if (date == null) {
            return null;
        }
        LocalDateTime localDateTime = dateToLocalDateTime(date);
        return localDateTimeToDate(localDateTime
            .withMonth(1)
            .withDayOfMonth(1)
            .withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0));
    }

    /**
     * 获取指定日期所在年份的最后一天
     * @param date 日期
     * @return 年份最后一天
     */
    public static Date getLastDayOfYear(Date date) {
        if (date == null) {
            return null;
        }
        LocalDateTime localDateTime = dateToLocalDateTime(date);
        return localDateTimeToDate(localDateTime
            .withMonth(12)
            .withDayOfMonth(31)
            .withHour(23)
            .withMinute(59)
            .withSecond(59)
            .withNano(999999999));
    }

    /**
     * 判断是否为周末
     * @param date 日期
     * @return true如果是周末
     */
    public static boolean isWeekend(Date date) {
        if (date == null) {
            return false;
        }
        DayOfWeek dayOfWeek = dateToLocalDateTime(date).getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    /**
     * 获取两个日期之间的工作日数量
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 工作日数量
     */
    public static long getWorkingDaysBetween(Date startDate, Date endDate) {
        if (startDate == null || endDate == null || startDate.after(endDate)) {
            return 0;
        }
        LocalDate start = dateToLocalDateTime(startDate).toLocalDate();
        LocalDate end = dateToLocalDateTime(endDate).toLocalDate();
        
        long days = 0;
        LocalDate current = start;
        while (!current.isAfter(end)) {
            if (!isWeekend(localDateToDate(current))) {
                days++;
            }
            current = current.plusDays(1);
        }
        return days;
    }

    /**
     * 获取日期范围内的所有日期
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 日期列表
     */
    public static List<Date> getDateRange(Date startDate, Date endDate) {
        if (startDate == null || endDate == null || startDate.after(endDate)) {
            return Collections.emptyList();
        }
        
        List<Date> dates = new ArrayList<>();
        LocalDate start = dateToLocalDateTime(startDate).toLocalDate();
        LocalDate end = dateToLocalDateTime(endDate).toLocalDate();
        
        LocalDate current = start;
        while (!current.isAfter(end)) {
            dates.add(localDateToDate(current));
            current = current.plusDays(1);
        }
        return dates;
    }

    /** 线程本地DateFormat缓存，确保线程安全 */
    private static final ThreadLocal<DateFormat> DEFAULT_THREAD_LOCAL_FORMATTER = 
        ThreadLocal.withInitial(() -> new SimpleDateFormat(DEFAULT_FORMAT));
    
    private static final ThreadLocal<Map<String, DateFormat>> PATTERN_FORMATTERS = 
        ThreadLocal.withInitial(HashMap::new);

    /**
     * 获取指定格式的DateFormat
     * @param pattern 日期格式
     * @return DateFormat实例
     */
    private static DateFormat getDateFormat(String pattern) {
        if (pattern == null || pattern.equals(DEFAULT_FORMAT)) {
            return DEFAULT_THREAD_LOCAL_FORMATTER.get();
        }
        Map<String, DateFormat> formatters = PATTERN_FORMATTERS.get();
        return formatters.computeIfAbsent(pattern, p -> new SimpleDateFormat(p));
    }

    /** 时间常量 */
    private static final int HOURS_PER_DAY = 24;
    private static final int MINUTES_PER_HOUR = 60;
    private static final int SECONDS_PER_MINUTE = 60;
    private static final int MILLIS_PER_SECOND = 1000;

    /** 时间差类型 */
    private static final int DIFF_TYPE_DAY = 1;
    private static final int DIFF_TYPE_HOUR = 2;
    private static final int DIFF_TYPE_MINUTE = 3;
    private static final int DIFF_TYPE_SECOND = 4;

    /** 错误消息 */
    private static final String PARSE_ERROR = "日期解析失败: ";
    private static final String INVALID_FORMAT = "无效的日期格式: ";
    private static final String NULL_DATE = "日期不能为null";
    private static final String INVALID_HOUR = "小时索引必须在0-24之间";
    private static final String START_AFTER_END = "开始时间不能大于结束时间";
    private static final String INVALID_DIFF_TYPE = "时间差类型必须在1-4之间";
    private static final String NULL_UNIT_TYPE = "时间单位和操作类型不能为null";

    /**
     * 格式化Date对象
     * 
     * @param date 待格式化的日期
     * @param pattern 日期格式，为null时使用默认格式
     * @return 格式化后的字符串，date为null时返回null
     * @author pengshuaifeng
     * @since 2023/12/27
     */
    public static String format(Date date, String pattern) {
        if (date == null) {
            return null;
        }
        DateFormat dateFormat = getDateFormat(pattern);
        return dateFormat.format(date);
    }

    /**
     * 格式化LocalDate对象
     * 
     * @param localDate 待格式化的日期
     * @param pattern 日期格式，为null时使用默认格式
     * @return 格式化后的字符串，localDate为null时返回null
     */
    public static String format(LocalDate localDate, String pattern) {
        if (localDate == null) {
            return null;
        }
        DateTimeFormatter formatter = pattern == null ? 
            DEFAULT_FORMATTER : 
            DateTimeFormatter.ofPattern(pattern);
        return localDate.format(formatter);
    }

    /**
     * 格式化LocalDateTime对象
     * 
     * @param localDateTime 待格式化的日期时间
     * @param pattern 日期格式，为null时使用默认格式
     * @return 格式化后的字符串，localDateTime为null时返回null
     */
    public static String format(LocalDateTime localDateTime, String pattern) {
        if (localDateTime == null) {
            return null;
        }
        DateTimeFormatter formatter = pattern == null ? 
            DEFAULT_FORMATTER : 
            DateTimeFormatter.ofPattern(pattern);
        return localDateTime.format(formatter);
    }

    /**
     * 使用默认格式格式化Date对象
     * 
     * @param date 待格式化的日期
     * @return 格式化后的字符串，date为null时返回null
     */
    public static String format(Date date) {
        return format(date, null);
    }

    /**
     * 调整时间
     * 
     * @param now 操作时间，为null时使用当前时间
     * @param dateUnitType 时间单位
     * @param operateType 操作类型
     * @param count 操作量
     * @return 调整后的时间
     * @throws IllegalArgumentException 如果时间单位或操作类型为null
     * @author fulin-peng
     * @since 2023/12/11
     */
    public static LocalDateTime operateTime(LocalDateTime now, DateUnitType dateUnitType, 
            DateOperateType operateType, int count) {
        if (dateUnitType == null || operateType == null) {
            throw new IllegalArgumentException(NULL_UNIT_TYPE);
        }
        
        LocalDateTime currentTime = now == null ? LocalDateTime.now() : now;
        int sign = operateType == DateOperateType.INCREASE ? 1 : -1;
        long amount = sign * count;

        switch (dateUnitType) {
            case SECOND:
                return currentTime.plusSeconds(amount);
            case MINUTE:
                return currentTime.plusMinutes(amount);
            case HOUR:
                return currentTime.plusHours(amount);
            case DAY:
                return currentTime.plusDays(amount);
            case MONTH:
                return currentTime.plusMonths(amount);
            case YEAR:
                return currentTime.plusYears(amount);
            default:
                throw new IllegalArgumentException("不支持的时间单位: " + dateUnitType);
        }
    }

    /**
     * 调整时间
     * 
     * @param now 操作时间，为null时使用当前时间
     * @param dateUnitType 时间单位
     * @param operateType 操作类型
     * @param count 操作量
     * @return 调整后的时间
     * @throws IllegalArgumentException 如果时间单位或操作类型为null
     * @author fulin-peng
     * @since 2023/12/11
     */
    public static Date operateTime(Date now, DateUnitType dateUnitType, DateOperateType operateType, 
            int count) {
        if (dateUnitType == null || operateType == null) {
            throw new IllegalArgumentException(NULL_UNIT_TYPE);
        }
        Date currentTime = now == null ? new Date() : now;
        return localDateTimeToDate(operateTime(dateToLocalDateTime(currentTime), dateUnitType, 
            operateType, count));
    }

    /**
     * 获取时间差
     * 
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @param startHourIndex 每日开始时间（全天设置为0）
     * @param endHourIndex 每日结束时间（全天设置为24）
     * @param type 时间差类型：1为天、2为小时、3为分钟、4为秒
     * @param onlyWorkDay 只计算工作日
     * @param containPointTime 包含起止时间
     * @return 时间差值
     * @throws IllegalArgumentException 如果参数无效
     * @author pengfulin
     * @since 2023/2/16
     */
    public static int timeDifference(Date startDate, Date endDate, int startHourIndex, 
            int endHourIndex, int type, boolean onlyWorkDay, boolean containPointTime) {
        validateTimeDifferenceParams(startDate, endDate, startHourIndex, endHourIndex, type);

        int unit = endHourIndex - startHourIndex;  // 一天的标准
        boolean isNotAllTime = startHourIndex != 0 || endHourIndex != HOURS_PER_DAY;

        // 转换为LocalDate便于计算
        LocalDate startLocalDate = dateToLocalDateTime(startDate).toLocalDate();
        LocalDate endLocalDate = dateToLocalDateTime(endDate).toLocalDate();

        // 计算工作日
        int dayNum = calculateWorkDays(startLocalDate, endLocalDate, onlyWorkDay, containPointTime);

        // 获取时间信息
        TimeInfo startTimeInfo = getTimeInfo(startDate);
        TimeInfo endTimeInfo = getTimeInfo(endDate);

        // 处理非24小时制
        dayNum = adjustDayCount(dayNum, startTimeInfo, endTimeInfo, startHourIndex, endHourIndex, 
            isNotAllTime, onlyWorkDay, containPointTime, startLocalDate, endLocalDate);

        // 根据类型计算结果
        return calculateResult(type, dayNum, unit, startTimeInfo, endTimeInfo, isNotAllTime, 
            containPointTime);
    }

    /**
     * 验证时间差参数
     */
    private static void validateTimeDifferenceParams(Date startDate, Date endDate, int startHourIndex, 
            int endHourIndex, int type) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException(NULL_DATE);
        }
        if (startHourIndex < 0 || startHourIndex > HOURS_PER_DAY || 
            endHourIndex < 0 || endHourIndex > HOURS_PER_DAY) {
            throw new IllegalArgumentException(INVALID_HOUR);
        }
        if (startHourIndex >= endHourIndex) {
            throw new IllegalArgumentException("开始时间必须小于结束时间");
        }
        if (type < DIFF_TYPE_DAY || type > DIFF_TYPE_SECOND) {
            throw new IllegalArgumentException(INVALID_DIFF_TYPE);
        }
        if (startDate.after(endDate)) {
            throw new IllegalArgumentException(START_AFTER_END);
        }
    }

    /**
     * 计算工作日天数
     */
    private static int calculateWorkDays(LocalDate startDate, LocalDate endDate, 
            boolean onlyWorkDay, boolean containPointTime) {
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        daysBetween = daysBetween == 0 ? 0 : daysBetween + 1;

        int dayNum = 0;
        for (int i = 0; i < daysBetween; i++) {
            LocalDate currentDate = startDate.plusDays(i);
            if ((CalendarUtils.isWeekday(currentDate, currentDate.getYear()) || !onlyWorkDay) &&
                    ((currentDate != startDate && !currentDate.toString()
                        .equals(endDate.toString())) || containPointTime)) {
                dayNum++;
            }
        }
        return dayNum;
    }

    /**
     * 调整天数
     */
    private static int adjustDayCount(int dayNum, TimeInfo startTime, TimeInfo endTime, 
            int startHourIndex, int endHourIndex, boolean isNotAllTime, boolean onlyWorkDay, 
            boolean containPointTime, LocalDate startDate, LocalDate endDate) {
        boolean startDateWeekday = (CalendarUtils.isWeekday(startDate, startDate.getYear()) || 
            !onlyWorkDay) && containPointTime;
        boolean endDateWeekday = (CalendarUtils.isWeekday(endDate, endDate.getYear()) || 
            !onlyWorkDay) && containPointTime;

        if (isNotAllTime) {
            if (startDateWeekday && startTime.hour >= endHourIndex && dayNum > 0) {
                dayNum--;
            }
            if (endDateWeekday && endTime.hour < startHourIndex && dayNum > 0) {
                dayNum--;
            }
        } else if (endDateWeekday && endTime.hour == 0 && dayNum > 0) {
            dayNum--;
        }

        return dayNum;
    }

    /**
     * 计算最终结果
     */
    private static int calculateResult(int type, int dayNum, int unit, TimeInfo startTime, 
            TimeInfo endTime, boolean isNotAllTime, boolean containPointTime) {
        if (type == DIFF_TYPE_DAY) {
            return dayNum;
        }

        int totalHours = calculateTotalHours(dayNum, unit, startTime, endTime, isNotAllTime, 
            containPointTime);
        if (type == DIFF_TYPE_HOUR) {
            return totalHours;
        }

        int totalMinutes = calculateTotalMinutes(totalHours, startTime, endTime, containPointTime);
        if (type == DIFF_TYPE_MINUTE) {
            return totalMinutes;
        }

        return totalMinutes * SECONDS_PER_MINUTE;
    }

    /**
     * 计算总小时数
     */
    private static int calculateTotalHours(int dayNum, int unit, TimeInfo startTime, 
            TimeInfo endTime, boolean isNotAllTime, boolean containPointTime) {
        int totalHours = dayNum >= 1 ? dayNum * unit : 0;

        if (containPointTime) {
            if (isNotAllTime) {
                if (startTime.hour > startTime.startHourIndex && 
                    startTime.hour < startTime.endHourIndex) {
                    totalHours = totalHours == 0 ? unit : totalHours;
                    totalHours -= (startTime.hour - startTime.startHourIndex);
                }
                if (endTime.hour > endTime.startHourIndex && 
                    endTime.hour < endTime.endHourIndex) {
                    totalHours = totalHours == 0 ? unit : totalHours;
                    totalHours -= (endTime.endHourIndex - endTime.hour);
                }
            } else {
                if (startTime.hour > 0) {
                    totalHours = totalHours == 0 ? HOURS_PER_DAY : totalHours;
                    totalHours -= startTime.hour;
                }
                if (endTime.hour > 0) {
                    totalHours = totalHours == 0 ? HOURS_PER_DAY : totalHours;
                    totalHours -= (HOURS_PER_DAY - endTime.hour);
                }
            }
        }

        return totalHours;
    }

    /**
     * 计算总分钟数
     */
    private static int calculateTotalMinutes(int totalHours, TimeInfo startTime, TimeInfo endTime, 
            boolean containPointTime) {
        int totalMinutes = totalHours * MINUTES_PER_HOUR;

        if (containPointTime) {
            if (startTime.minute > 0) {
                totalMinutes = totalMinutes == 0 ? MINUTES_PER_HOUR : totalMinutes;
                totalMinutes -= startTime.minute;
            }

            if (endTime.minute > 0) {
                totalMinutes = totalMinutes == 0 ? MINUTES_PER_HOUR : totalMinutes;
                if (startTime.isSameHour(endTime)) {
                    totalMinutes -= (MINUTES_PER_HOUR - endTime.minute);
                } else {
                    totalMinutes += endTime.minute;
                }
            }
        }

        return totalMinutes;
    }

    /**
     * 获取时间信息
     * @param date 日期
     * @return 时间信息对象
     */
    private static TimeInfo getTimeInfo(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return new TimeInfo(cal);
    }

    /**
     * 时间信息类，提供更多时间相关的功能
     */
    private static class TimeInfo {
        final int hour;
        final int minute;
        final int second;
        final int month;
        final int year;
        final int day;
        final int startHourIndex;
        final int endHourIndex;

        TimeInfo(Calendar cal) {
            this.hour = cal.get(Calendar.HOUR_OF_DAY);
            this.minute = cal.get(Calendar.MINUTE);
            this.second = cal.get(Calendar.SECOND);
            this.month = cal.get(Calendar.MONTH);
            this.year = cal.get(Calendar.YEAR);
            this.day = cal.get(Calendar.DAY_OF_MONTH);
            this.startHourIndex = 0;
            this.endHourIndex = HOURS_PER_DAY;
        }

        boolean isSameHour(TimeInfo other) {
            return year == other.year && month == other.month && 
                   day == other.day && hour == other.hour;
        }

        boolean isSameDay(TimeInfo other) {
            return year == other.year && month == other.month && day == other.day;
        }

        boolean isBeforeHour(TimeInfo other) {
            if (year != other.year) return year < other.year;
            if (month != other.month) return month < other.month;
            if (day != other.day) return day < other.day;
            return hour < other.hour;
        }

        int getTotalMinutes() {
            return hour * MINUTES_PER_HOUR + minute;
        }

        int getTotalSeconds() {
            return getTotalMinutes() * SECONDS_PER_MINUTE + second;
        }
    }

    /**
     * 获取指定日期的开始时间
     * @param date 指定日期
     * @return 该日期的开始时间（00:00:00.000）
     */
    public static Date getStartOfDay(Date date) {
        if (date == null) {
            return null;
        }
        LocalDateTime localDateTime = dateToLocalDateTime(date);
        return localDateTimeToDate(localDateTime.withHour(0).withMinute(0).withSecond(0).withNano(0));
    }

    /**
     * 获取指定日期的结束时间
     * @param date 指定日期
     * @return 该日期的结束时间（23:59:59.999）
     */
    public static Date getEndOfDay(Date date) {
        if (date == null) {
            return null;
        }
        LocalDateTime localDateTime = dateToLocalDateTime(date);
        return localDateTimeToDate(localDateTime.withHour(23).withMinute(59).withSecond(59).withNano(999999999));
    }

    /**
     * 获取指定日期所在月份的第一天
     * @param date 指定日期
     * @return 所在月份的第一天
     */
    public static Date getFirstDayOfMonth(Date date) {
        if (date == null) {
            return null;
        }
        LocalDateTime localDateTime = dateToLocalDateTime(date);
        return localDateTimeToDate(localDateTime.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0));
    }

    /**
     * 获取指定日期所在月份的最后一天
     * @param date 指定日期
     * @return 所在月份的最后一天
     */
    public static Date getLastDayOfMonth(Date date) {
        if (date == null) {
            return null;
        }
        LocalDateTime localDateTime = dateToLocalDateTime(date);
        return localDateTimeToDate(localDateTime.withDayOfMonth(localDateTime.toLocalDate().lengthOfMonth())
            .withHour(23).withMinute(59).withSecond(59).withNano(999999999));
    }

    /**
     * Date转LocalDateTime
     * 
     * @param date Date对象
     * @return LocalDateTime对象，date为null时返回null
     * @author pengshuaifeng
     * @since 2023/12/27
     */
    public static LocalDateTime dateToLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime();
    }

    /**
     * LocalDateTime转Date
     * 
     * @param localDateTime LocalDateTime对象
     * @return Date对象，localDateTime为null时返回null
     * @author pengshuaifeng
     * @since 2023/12/27
     */
    public static Date localDateTimeToDate(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return Date.from(localDateTime.atZone(ZoneId.systemDefault())
            .toInstant());
    }

    /**
     * LocalDate转Date
     * 
     * @param localDate LocalDate对象
     * @return Date对象，localDate为null时返回null
     */
    public static Date localDateToDate(LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        return Date.from(localDate.atStartOfDay()
            .atZone(ZoneId.systemDefault())
            .toInstant());
    }

    /**
     * 字符串转Date
     * 
     * @param value 时间字符串
     * @param dateFormat DateFormat对象
     * @return Date对象，如果参数为null则返回null
     * @throws RuntimeException 如果解析失败
     * @author fulin-peng
     * @since 2024/3/6
     */
    public static Date stringToDate(String value, DateFormat dateFormat) {
        if (value == null || dateFormat == null) {
            return null;
        }
        try {
            return dateFormat.parse(value);
        } catch (ParseException e) {
            throw new RuntimeException(PARSE_ERROR + value, e);
        }
    }

    /**
     * 使用默认格式将字符串转换为Date
     * 
     * @param value 时间字符串
     * @return Date对象，value为null时返回null
     * @author fulin-peng
     * @since 2024/3/6
     */
    public static Date stringToDate(String value) {
        if (value == null) {
            return null;
        }
        return stringToDate(value, DEFAULT_THREAD_LOCAL_FORMATTER.get());
    }

    /**
     * 使用指定格式将字符串转换为Date
     * 
     * @param value 时间字符串
     * @param pattern 日期格式
     * @return Date对象，如果参数为null则返回null
     * @throws RuntimeException 如果格式无效或解析失败
     * @author fulin-peng
     * @since 2024/3/6
     */
    public static Date stringToDate(String value, String pattern) {
        if (value == null || pattern == null) {
            return null;
        }
        try {
            return stringToDate(value, new SimpleDateFormat(pattern));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(INVALID_FORMAT + pattern, e);
        }
    }

    /**
     * 时间戳转Date
     * 
     * @param timestamp 时间戳（毫秒）
     * @return Date对象
     * @author pengshuaifeng
     * @since 2024/3/27
     */
    public static Date timestampToDate(long timestamp) {
        return new Date(timestamp);
    }

    /**
     * 时间戳转LocalDateTime
     * 
     * @param timestamp 时间戳（毫秒）
     * @return LocalDateTime对象
     * @author pengshuaifeng
     * @since 2024/3/27
     */
    public static LocalDateTime timestampToLocalDateTime(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), 
            ZoneId.systemDefault());
    }

    /**
     * LocalDateTime转时间戳
     * 
     * @param localDateTime LocalDateTime对象
     * @return 时间戳（毫秒），localDateTime为null时返回0
     */
    public static long localDateTimeToTimestamp(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return 0L;
        }
        return localDateTime.atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli();
    }

    /**
     * 比较两个日期是否相等（忽略时分秒）
     * 
     * @param date1 第一个日期
     * @param date2 第二个日期
     * @return 如果日期相等返回true，否则返回false
     */
    public static boolean isSameDay(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
            return false;
        }
        return dateToLocalDateTime(date1).toLocalDate().isEqual(dateToLocalDateTime(date2).toLocalDate());
    }

    /**
     * 判断日期是否在指定范围内
     * 
     * @param date 待判断的日期
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 如果在范围内返回true，否则返回false
     */
    public static boolean isDateInRange(Date date, Date startDate, Date endDate) {
        if (date == null || startDate == null || endDate == null) {
            return false;
        }
        return !date.before(startDate) && !date.after(endDate);
    }

    /**
     * 获取当前时间戳（毫秒）
     * 
     * @return 当前时间戳
     */
    public static long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * 获取当前时间戳(秒)
     * @return 当前时间戳
     */
    public static long currentTimestamp() {
        return Instant.now().getEpochSecond();
    }

    /**
     * 获取当前日期时间
     * 
     * @return 当前日期时间
     */
    public static Date currentDate() {
        return new Date();
    }

    /**
     * 获取当前LocalDateTime
     * 
     * @return 当前LocalDateTime
     */
    public static LocalDateTime currentLocalDateTime() {
        return LocalDateTime.now();
    }

    /**
     * 时间单位枚举
     */
    public enum DateUnitType {
        /** 秒 */
        SECOND,
        /** 分钟 */
        MINUTE,
        /** 小时 */
        HOUR,
        /** 天 */
        DAY,
        /** 月 */
        MONTH,
        /** 年 */
        YEAR
    }

    /**
     * 时间操作类型枚举
     */
    public enum DateOperateType {
        /** 增加 */
        INCREASE,
        /** 减少 */
        REDUCE
    }

    /**
     * 清理ThreadLocal资源
     */
    public static void cleanThreadLocal() {
        DEFAULT_THREAD_LOCAL_FORMATTER.remove();
        PATTERN_FORMATTERS.remove();
    }

    /**
     * 判断是否为闰年
     * @param year 年份
     * @return true如果是闰年
     */
    public static boolean isLeapYear(int year) {
        return Year.isLeap(year);
    }

    /**
     * 获取两个日期之间的天数
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 天数差值，如果任一参数为null则返回0
     */
    public static long getDaysBetween(Date startDate, Date endDate) {
        if (startDate == null || endDate == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(
            dateToLocalDateTime(startDate).toLocalDate(),
            dateToLocalDateTime(endDate).toLocalDate()
        );
    }

    /**
     * 获取两个日期之间的月数
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 月数差值，如果任一参数为null则返回0
     */
    public static long getMonthsBetween(Date startDate, Date endDate) {
        if (startDate == null || endDate == null) {
            return 0;
        }
        return ChronoUnit.MONTHS.between(
            dateToLocalDateTime(startDate).toLocalDate(),
            dateToLocalDateTime(endDate).toLocalDate()
        );
    }

    /**
     * 获取两个日期之间的年数
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 年数差值，如果任一参数为null则返回0
     */
    public static long getYearsBetween(Date startDate, Date endDate) {
        if (startDate == null || endDate == null) {
            return 0;
        }
        return ChronoUnit.YEARS.between(
            dateToLocalDateTime(startDate).toLocalDate(),
            dateToLocalDateTime(endDate).toLocalDate()
        );
    }
}
