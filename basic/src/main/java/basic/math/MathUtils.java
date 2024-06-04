package basic.math;

import java.text.DecimalFormat;

/**
 * 数学工具类
 *
 * @author fulin-peng
 * 2024-04-24  18:38
 */
public class MathUtils {


    /**
     * 整数计量中文单位输出
     * 2024/4/24 0024 18:48
     * @author fulin-peng
     * @param  number 整数
     * @param  y 亿计量格式
     * @param  w 万计量格式
     * @param  q 千计量格式
     * @param  containUnit 是否包含单位
     * @param  unitSplit 单位分隔符：默认为空
     */
    public static String numberToCnUnit(Long number,String y,String w,String q,boolean containUnit,String unitSplit) {
        DecimalFormat formatter;
        // 1 亿 = 100,000,000
        if (number >= 100000000) {
            formatter = new DecimalFormat(y);
            // 将数字转换为亿
            double billions = number / 100000000.0;
            return formatter.format(billions)+unitSplit+(containUnit?"亿":"");
        } else if (number >= 10000) {
            formatter = new DecimalFormat(w);
            // 将数字转换为万
            double tenThousands = number / 10000.0;
            return formatter.format(tenThousands)+unitSplit+(containUnit?"万":"");
        } else if(number>=1000){
            formatter = new DecimalFormat(q);
            return formatter.format(number)+unitSplit+(containUnit?"千":"");
        }else{
            return number.toString();
        }
    }

    public static String numberToCnUnit(Long number,boolean containUnit,String unitSplit) {
        return numberToCnUnit(number,"0.00","0.0","0.0",containUnit,unitSplit);
    }

    public static String numberToCnUnit(Long number) {
        return numberToCnUnit(number,"0.00","0.0","0.0",true,"");
    }


}
