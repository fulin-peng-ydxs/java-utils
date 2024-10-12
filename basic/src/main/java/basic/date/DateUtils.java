package basic.date;

import com.fasterxml.jackson.core.type.TypeReference;
import json.jackson.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
/**
 * 时间工具类
 *
 * @author pengshuaifeng
 * 2023/12/27
 */
@Slf4j
public class DateUtils {

    //默认format
    public static final String defaultFormat="yyyy-MM-dd HH:mm:ss";

    //默认DateFormat
    public static final DateFormat format = new SimpleDateFormat(defaultFormat);

    public static final DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern(defaultFormat);


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

    /**
     * 格式化时间
     * @param date 时间对象
     * @param pattern 格式化模型
     * 2023/12/27 22:08
     * @author pengshuaifeng
     */
    public static String format(Date date,String pattern){
        DateFormat dateFormat = pattern==null?format:new SimpleDateFormat(pattern);
        return dateFormat.format(date);
    }

    public static String format(LocalDate localDate,String pattern){
        DateTimeFormatter dateTimeFormatter= pattern==null?dateTimeFormat:DateTimeFormatter.ofPattern(pattern);
        return dateTimeFormatter.format(localDate);
    }

    public static String format(LocalDateTime localDate,String pattern){
        DateTimeFormatter dateTimeFormatter=pattern==null?dateTimeFormat:DateTimeFormatter.ofPattern(pattern);
        return dateTimeFormatter.format(localDate);
    }

    public static String format(Date date){
        return format(date,null);
    }

    /**
     * 调整时间
     * @param now 操作时间
     * @param dateUnitType 时间单位
     * @param operateType 操作类型
     * @param count 操作量
     * 2023/12/11 0011 15:04
     * @author fulin-peng
     */
    public static LocalDateTime operateTime(LocalDateTime now, DateUnitType dateUnitType, DateOperateType operateType, int count){
        LocalDateTime currentTime =now==null?LocalDateTime.now():now;
        LocalDateTime newTime=null;
        if (operateType==DateOperateType.INCREASE) {
            switch (dateUnitType){
                case SECOND:
                    newTime = currentTime.plusSeconds(count);
                    break;
                case MINUTE:
                    newTime = currentTime.plusMinutes(count);
                    break;
                case HOUR:
                    newTime = currentTime.plusHours(count);
                    break;
                case DAY:
                    newTime = currentTime.plusDays(count);
                    break;
                case MOUTH:
                    newTime = currentTime.plusMonths(count);
                    break;
                case YEAR:
                    newTime = currentTime.plusYears(count);
                    break;
            }
        }else{
            switch (dateUnitType){
                case SECOND:
                    newTime = currentTime.minusSeconds(count);
                    break;
                case MINUTE:
                    newTime = currentTime.minusMinutes(count);
                    break;
                case HOUR:
                    newTime = currentTime.minusHours(count);
                    break;
                case DAY:
                    newTime = currentTime.minusDays(count);
                    break;
                case MOUTH:
                    newTime = currentTime.minusMonths(count);
                    break;
                case YEAR:
                    newTime = currentTime.minusYears(count);
                    break;
            }
        }
        return newTime;
    }

    public static Date operateTime(Date now,DateUnitType dateUnitType,DateOperateType operateType,int count){
        now = now == null ? new Date() : now;
        return localDateTimeToDate(operateTime(dateToLocalDateTime(now), dateUnitType, operateType, count));
    }


    /**获取时间差
     * 2023/2/16 0016-20:33
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @param startHourIndex 每日开始时间:全天设置为0
     * @param endHourIndex 每日结束时间：全天设置为24
     * @param type 时间差类型：1为天、2为小时、3为分钟、4为秒
     * @param onlyWorkDay 只计算工作日
     * @param containPointTime 包含起止时间
     * @author pengfulin
     */
    public static int timeDifference(Date startDate,Date endDate,int startHourIndex,int endHourIndex,int type,boolean onlyWorkDay,boolean containPointTime){
        //1--参数校验
        if(startDate.after(endDate))
            throw new RuntimeException("起始时间不能大于结束时间");
        int unit=endHourIndex-startHourIndex;  //一天的标准
        boolean isNotAllTime= startHourIndex != 0 || endHourIndex != 24;   //是否24小时制
        //开始&结束时间对象转换
        LocalDate startLocalDate = startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate endLocalDate = endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        //2--时间间隔天数计算
        long daysBetween = ChronoUnit.DAYS.between(startLocalDate, endLocalDate);
        daysBetween=daysBetween==0?0:daysBetween+1;
        //2.1：遍历间隔中的每一天：移除掉节假日，如果需要的话,移除掉起止时间
        int dayNum=0;
        for (int i = 0; i < daysBetween; i++) {
            LocalDate currentDate = startLocalDate.plusDays(i);
            if ((isWeekday(currentDate, currentDate.getYear()) || !onlyWorkDay) &&
                    ( (currentDate!=startLocalDate && !currentDate.toString().equals(endLocalDate.toString())) || containPointTime) )
                dayNum++;
        }
        //2.2：是否也需要计算起止时间
        boolean startDateWeekday = (isWeekday(startLocalDate, startLocalDate.getYear()) ||(!onlyWorkDay)) && containPointTime;
        boolean endDateWeekday = (isWeekday(endLocalDate, endLocalDate.getYear()) ||(!onlyWorkDay)) && containPointTime;
        //起止时间的年、月、日、分
        Calendar cal = Calendar.getInstance();
        cal.setTime(startDate);
        int startHour= cal.get(Calendar.HOUR_OF_DAY);
        int startMinute=cal.get(Calendar.MINUTE);
        int startMonth = cal.get(Calendar.MONTH);
        int startYear = cal.get(Calendar.YEAR);
        int startDay = cal.get(Calendar.DAY_OF_MONTH);
        cal.setTime(endDate);
        int endHour=cal.get(Calendar.HOUR_OF_DAY);
        int endMinute=cal.get(Calendar.MINUTE);
        int endMonth = cal.get(Calendar.MONTH);
        int endYear = cal.get(Calendar.YEAR);
        int endDay = cal.get(Calendar.DAY_OF_MONTH);
        //2.3：每天有效时间处理
        if(isNotAllTime){  //不在24小时制有效时间
            if(startDateWeekday && startHour >= endHourIndex) {
                if (dayNum > 0) {
                    --dayNum;
                }
            }
            if(endDateWeekday && endHour < startHourIndex){
                if (dayNum > 0) {
                    --dayNum;
                }
            }
        }else{  //在24小时制有效时间
            if(endDateWeekday && endHour==0){
                if (dayNum > 0) {
                    --dayNum;
                }
            }
        }
        //3--开始计算：根据传入类型
        int result;
        if(type==1)  //天的计算
            result=dayNum;
        else{
            //时的计算
            int totalHour=0;
            if(dayNum>=1){
                totalHour= dayNum * unit;
            }
            if(startDateWeekday){
                if(isNotAllTime){
                    if(startHour>startHourIndex&&startHour<endHourIndex){
                        totalHour=totalHour==0?unit:totalHour;
                        totalHour=totalHour-(startHour-startHourIndex);
                    }
                }else{
                    totalHour=totalHour==0?unit:totalHour;
                    totalHour=totalHour-startHour;
                }
            }
            if(endDateWeekday){
                if(isNotAllTime){
                    if(endHour>startHourIndex&&endHour<endHourIndex){
                        totalHour=totalHour==0?unit:totalHour;
                        totalHour=totalHour-(endHourIndex-endHour);
                    }
                }else{
                    if(endHour>0){
                        totalHour=totalHour==0?unit:totalHour;
                        totalHour=totalHour-(unit-endHour);
                    }
                }
            }
            result=totalHour;
            if(type==3||type==4){
                //分钟的计算
                boolean isInAnHour= startYear == endYear && startMonth == endMonth && startDay == endDay && startHour == endHour;
                int totalMinutes=result*60;
                if(startDateWeekday){
                    if(startMinute>0){
                        totalMinutes=totalMinutes==0?60:totalMinutes;
                        totalMinutes=totalMinutes-startMinute;
                    }
                }
                if(endDateWeekday){
                    if(endMinute>0){
                        totalMinutes=totalMinutes==0?60:totalMinutes;
                        if(isInAnHour){
                            totalMinutes=totalMinutes-(60-endMinute);
                        }else{
                            totalMinutes=totalMinutes+endMinute;
                        }
                    }
                }
                result=totalMinutes;
                //秒计算
                if(type==4){
                    result=result*60;
                }
            }
        }
        return result;
    }

    /**
     * 工作日时间差
     * 2023/11/15 0015 16:20
     * @author fulin-peng
     */
    public static int timeDifference(Date startDate,Date endDate,int startHourIndex,int endHourIndex,int type){
        return timeDifference(startDate,endDate,startHourIndex,endHourIndex,type,true,true);
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
        String ymd = format(date,"yyyy-MM-dd");
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


    /**
     * Date转LocalDateTime
     * 2023/12/27 22:37
     * @author pengshuaifeng
     */
    public static LocalDateTime dateToLocalDateTime(Date date){
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     * LocalDateTime转Date
     * 2023/12/27 22:48
     * @author pengshuaifeng
     */
    public static Date localDateTimeToDate(LocalDateTime localDate){
        return Date.from(localDate.atZone(ZoneId.systemDefault())
                .toInstant());
    }

    /**
     * 字符串转Date
     * 2024/3/6 0006 16:10
     * @author fulin-peng
     */
    public static Date stringToDate(String value,DateFormat dateFormat) {
        try {
            return dateFormat.parse(value);
        } catch (ParseException e) {
            throw new RuntimeException("字符串转Date异常",e);
        }
    }

    /**
     * 字符串转Date
     * <p>使用默认格式</p>
     * 2024/3/6 0006 16:16
     * @author fulin-peng
     */
    public static Date stringToDate(String value) {
        return stringToDate(value,format);
    }

    /**
     * 字符串转Date
     * 2024/3/6 0006 16:16
     * @param value 时间字符串
     * @param dateFormat 时间格式
     * @author fulin-peng
     */
    public static Date stringToDate(String value,String dateFormat){
        return stringToDate(value,new SimpleDateFormat(dateFormat));
    }

    /**
     * 时间戳转Date
     * 2024/3/27 21:09
     * @author pengshuaifeng
     */
    public static Date timestampToDate(long timestamp){
        return new Date(timestamp);
    }

    /**
     * 时间戳转LocalDateTime
     * 2024/3/27 21:10
     * @author pengshuaifeng
     */
    public static LocalDateTime timestampToLocalDateTime(long timestamp){
        return Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     * LocalDateTime转时间戳
     */
    public static long localDateTimeToTimestamp(LocalDateTime localDateTime){
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }


    public enum DateUnitType {
        SECOND,
        MINUTE,
        HOUR,
        DAY,
        MOUTH,
        YEAR
    }

    public enum DateOperateType{
        INCREASE,
        REDUCE
    }
}
