package mindustry.arcModule;

import arc.math.Mathf;
import arc.util.Strings;

import java.util.regex.Pattern;

import static arc.util.Strings.fixed;
import static arc.util.Strings.format;

public class NumberFormat {
    private static final String defaultFormat = "@[gray]@[]";
    private static final Pattern autoFixedRegex = Pattern.compile("(?<=\\.\\d{0,10})0+(?!\\d|E)|\\.0+(?!\\d|E)");

    public static String formatInteger(long number){
        return formatInteger(number, 2, defaultFormat);
    }
    public static String formatFloat(float number){
        return formatFloat(number, 2, defaultFormat);
    }
    public static String autoFixed(float number){
        return autoFixed(number, 2, defaultFormat);
    }
    public static String formatInteger(long number, int maxDeci){
        return formatInteger(number, maxDeci, defaultFormat);
    }
    public static String formatFloat(float number, int maxDeci){
        return formatFloat(number, maxDeci, defaultFormat);
    }
    public static String formatFloat(float number, String format){
        return formatFloat(number, 2, format);
    }
    public static String autoFixed(float number, int maxDeci){
        return autoFixed(number, maxDeci, defaultFormat);
    }
    public static String formatInteger(long number, int maxDeci, String format){

        //以后可能完全合并到formatFloat
        if(number == Long.MAX_VALUE) return "Inf";
        if(number == Long.MIN_VALUE) return "-Inf";

        String unit, sign = number < 0 ? "-" : "";
        number = Math.abs(number);
        double fnumber;

        if (number >= 1_000_000_000_000L) {//直接比较float和long可能有精度问题，大概?
            int exponent = (int) Math.log10(number);
            float mantissa = (float) (number / Math.pow(10, exponent));
            return format(format, sign + format("@E@", Strings.autoFixed(mantissa, 1), exponent), "");
        } else if (number >= 1_000_000_000L) {
            fnumber = number / 1E9;
            unit = "B";
        } else if (number >= 1_000_000L) {
            fnumber = number / 1E6;
            unit = "M";
        } else if (number >= 1_000L) {
            fnumber = number / 1E3;
            unit = "K";
        }
        else return format(format, sign + number, "");
        return format(format, sign + fixed((float) fnumber, maxDeci - (int) Math.log10(fnumber)), unit);
    }

    public static String formatFloat(float number, int maxDeci, String format){

        if(Float.isNaN(number)) return format(format, "NaN", "");
        if(number == Float.POSITIVE_INFINITY) return format(format, "Inf", "");
        if(number == Float.NEGATIVE_INFINITY) return format(format, "-Inf", "");

        String sign = number < 0 ? "-" : "", unit = "";
        number = Math.abs(number);

        if (number < 0.00001f) return format(format, sign + number, "");

        if (number >= Math.pow(10, maxDeci + 1)) {

            float tolernce = (float) Math.pow(10, maxDeci + 2);

            if (number >= 1E12f || Mathf.equal(1E12f, number, number / tolernce)) {

                int exponent = Mathf.floor((float) Math.log10(number));
                float mantissa = (float) (number / Math.pow(10, exponent));

                return format(format, sign + format("@E@", fixed(mantissa, 1), exponent), "");

            } else if (number >= 1E9f || Mathf.equal(1E9f, number, number / tolernce)) {
                number /= 1E9f;
                unit = "B";
            } else if (number >= 1E6f || Mathf.equal(1E6f, number, number / tolernce)) {
                number /= 1E6f;
                unit = "M";
            } else if (number >= 1E3f || Mathf.equal(1E3f, number, number / tolernce)) {
                number /= 1E3f;
                unit = "K";
            }
        }
        return format(format, sign + fixed(number, maxDeci - (int) Math.log10(number)), unit);
    }

    public static String autoFixed(float number, int maxDeci, String format) {
        return autoFixedRegex.matcher(formatFloat(number, maxDeci, format)).replaceAll("");
    }



    public static String formatPercent(String prefix, float cur, float max) {
        // 最通用的情况
        return formatPercent(prefix, cur, Math.abs(cur) > 0.001f, max,  cur / max < 0.9f, 2);
    }

    public static String formatPercent(String prefix, float cur, float max, int arcFixed) {
        // 用于处理血量等需要精细显示的
        return formatPercent(prefix, cur, cur > 0.0001f, max, cur / max < 0.9f, arcFixed);
    }

    public static String formatPercent(String prefix, float cur, boolean showMin, float max, boolean showPercent, int arcFixed) {
        return formatPercent(prefix, cur,showMin, max, showMin & showPercent ? buildPercent(cur, max) : "", arcFixed);
    }

    public static String formatPercent(String prefix, float cur, float max, String format) {
        // 用于有自定义百分比需求的，如热量相关
        return formatPercent(prefix, cur, cur > 0.0001f, max, format, 2);
    }

    public static String formatPercent(String prefix, float cur, boolean showMin, float max, String format, int arcFixed) {
        // 只不处理showMax
        return formatPercent(prefix, cur, showMin, max, Math.abs(cur/max - 1) > 0.01f, format, arcFixed);
    }

    public static String formatPercent(String prefix, float cur, boolean showMin, float max, boolean showMax, String format, int arcFixed) {
        // 用于建筑等的bar显示，保持统一格式
        StringBuilder text = new StringBuilder(prefix).append(" ");
        text.append(showMin ? autoFixed(cur, arcFixed) : "\uE815");
        if (showMax) text.append("/").append(autoFixed(max, arcFixed));
        text.append(format);
        return text.toString();
    }

    private static String buildPercent(float cur, float max) {
        return buildPercent(100 * cur / max);
    }

    public static String buildPercent(float percent) {
        //return " [lightgray]| " + UI.arcFixed(percent, 2) + "%";
        return " [lightgray]| " + (int)percent + "%";
    }
}
