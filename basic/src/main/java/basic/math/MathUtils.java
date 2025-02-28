package basic.math;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Random;

/**
 * 数学工具类，提供数值格式化、随机数生成、数学计算等功能
 *
 * @author fulin-peng
 * @since 2024-04-24 18:38
 */
public class MathUtils {

    private MathUtils() {
        // Utility class should not be instantiated
    }

    /** 默认数字格式化模式：两位小数 */
    private static final String DEFAULT_DECIMAL_PATTERN = "0.00";

    /** 亿的数值 */
    private static final long BILLION = 100000000L;
    
    /** 万的数值 */
    private static final long TEN_THOUSAND = 10000L;
    
    /** 千的数值 */
    private static final long THOUSAND = 1000L;

    /** 随机数生成器 */
    private static final Random RANDOM = new Random();

    /**
     * 将数字转换为中文计量单位表示
     * 
     * @param number 待转换的数字
     * @param billionPattern 亿的格式化模式（如"0.00"）
     * @param tenThousandPattern 万的格式化模式（如"0.0"）
     * @param thousandPattern 千的格式化模式（如"0.0"）
     * @param containUnit 是否包含单位
     * @param unitSplit 单位分隔符
     * @return 格式化后的字符串
     * @throws IllegalArgumentException 如果number为null或格式化模式无效
     * @author fulin-peng
     * @since 2024/4/24 18:48
     */
    public static String numberToCnUnit(Long number, String billionPattern, String tenThousandPattern,
            String thousandPattern, boolean containUnit, String unitSplit) {
        if (number == null) {
            throw new IllegalArgumentException("数字不能为null");
        }
        if (unitSplit == null) {
            unitSplit = "";
        }

        try {
            if (number >= BILLION) {
                return formatWithUnit(number / (double) BILLION, billionPattern, "亿", 
                        containUnit, unitSplit);
            } else if (number >= TEN_THOUSAND) {
                return formatWithUnit(number / (double) TEN_THOUSAND, tenThousandPattern, "万", 
                        containUnit, unitSplit);
            } else if (number >= THOUSAND) {
                return formatWithUnit(number / (double) THOUSAND, thousandPattern, "千", 
                        containUnit, unitSplit);
            }
            return number.toString();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("无效的格式化模式", e);
        }
    }

    /**
     * 将数字转换为中文计量单位表示（使用默认格式）
     * @param number 待转换的数字
     * @param containUnit 是否包含单位
     * @param unitSplit 单位分隔符
     * @return 格式化后的字符串
     */
    public static String numberToCnUnit(Long number, boolean containUnit, String unitSplit) {
        return numberToCnUnit(number, "0.00", "0.0", "0.0", containUnit, unitSplit);
    }

    /**
     * 将数字转换为中文计量单位表示（使用默认格式和设置）
     * @param number 待转换的数字
     * @return 格式化后的字符串
     */
    public static String numberToCnUnit(Long number) {
        return numberToCnUnit(number, "0.00", "0.0", "0.0", true, "");
    }

    /**
     * 获取指定范围内的随机整数 [0, bound)
     * @param bound 上界（不包含）
     * @return 随机整数
     * @throws IllegalArgumentException 如果bound小于等于0
     * @author pengshuaifeng
     * @since 2024/9/1 22:11
     */
    public static int getRandom(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("上界必须大于0");
        }
        return RANDOM.nextInt(bound);
    }

    /**
     * 获取指定范围内的随机整数 [min, max)
     * @param min 下界（包含）
     * @param max 上界（不包含）
     * @return 随机整数
     * @throws IllegalArgumentException 如果min大于等于max
     * @author pengshuaifeng
     * @since 2024/9/1 22:11
     */
    public static int getRandom(int min, int max) {
        if (min >= max) {
            throw new IllegalArgumentException("下界必须小于上界");
        }
        return min + RANDOM.nextInt(max - min);
    }

    /**
     * 获取指定范围内的随机浮点数 [min, max)
     * @param min 下界（包含）
     * @param max 上界（不包含）
     * @return 随机浮点数
     * @throws IllegalArgumentException 如果min大于等于max
     */
    public static double getRandomDouble(double min, double max) {
        if (min >= max) {
            throw new IllegalArgumentException("下界必须小于上界");
        }
        return min + (max - min) * RANDOM.nextDouble();
    }

    /**
     * 保留指定位数的小数（四舍五入）
     * @param value 待处理的数值
     * @param scale 小数位数
     * @return 处理后的数值
     * @throws IllegalArgumentException 如果scale小于0
     */
    public static double round(double value, int scale) {
        if (scale < 0) {
            throw new IllegalArgumentException("小数位数不能为负数");
        }
        BigDecimal bd = BigDecimal.valueOf(value);
        return bd.setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * 将数值格式化为指定格式的字符串
     * @param value 待格式化的数值
     * @param pattern 格式化模式
     * @return 格式化后的字符串
     * @throws IllegalArgumentException 如果格式化模式无效
     */
    public static String format(double value, String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            pattern = DEFAULT_DECIMAL_PATTERN;
        }
        try {
            return new DecimalFormat(pattern).format(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("无效的格式化模式: " + pattern, e);
        }
    }

    /**
     * 判断一个数是否为偶数
     * @param number 待判断的数
     * @return true如果是偶数，false如果是奇数
     */
    public static boolean isEven(int number) {
        return number % 2 == 0;
    }

    /**
     * 判断一个数是否为奇数
     * @param number 待判断的数
     * @return true如果是奇数，false如果是偶数
     */
    public static boolean isOdd(int number) {
        return number % 2 != 0;
    }

    /**
     * 计算百分比（保留两位小数）
     * @param numerator 分子
     * @param denominator 分母
     * @return 百分比字符串
     * @throws IllegalArgumentException 如果分母为0
     */
    public static String percentage(double numerator, double denominator) {
        if (denominator == 0) {
            throw new IllegalArgumentException("分母不能为0");
        }
        double percentage = (numerator / denominator) * 100;
        return format(percentage, "0.00") + "%";
    }

    /**
     * 格式化数值并添加单位
     */
    private static String formatWithUnit(double value, String pattern, String unit, 
            boolean containUnit, String unitSplit) {
        DecimalFormat formatter = new DecimalFormat(pattern);
        return formatter.format(value) + unitSplit + (containUnit ? unit : "");
    }
}
