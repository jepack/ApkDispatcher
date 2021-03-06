package com.jepack.dispatcher;

import android.content.Context;
import android.graphics.Paint;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by zhanghaihai on 2017/3/13.
 */

public class TextUtil {

    private static Pattern humpPattern = Pattern.compile("[A-Z]");
    public static SpannableStringBuilder getMultiColorText(String[] strings, int[] colors){

        return getMultiStyleText(strings, colors, null, null);
    }

    public static String getChineseNumber(long number){
        if(number > Math.pow(10, 9)){
            return  new BigDecimal((number * 1.0f / Math.pow(10, 9))).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue() + "亿";
        }else if(number > Math.pow(10, 8)){
            return new BigDecimal((number * 1.0f / Math.pow(10, 8))).intValue() + "千万";
        }else if(number > Math.pow(10, 7)){
            return new BigDecimal((number * 1.0f / Math.pow(10, 7))).intValue() + "百万";
        }else if(number > Math.pow(10, 5)){
            return new BigDecimal((number * 1.0f / Math.pow(10, 5))).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue() + "万";
        }
        return "" + number;
    }

    public static SpannableStringBuilder getTextWithTip(String tip, String content,
                                                        @ColorInt int tipColor, @ColorInt int contentColor){

        String[] strings = {tip, content};
        int[] colors = {tipColor, contentColor};
        return getMultiStyleText(strings, colors, null, null);
    }
    /**
     * 用于一个TextView显示复杂样式文字
     * @param strings 切分的文字
     * @param colors 字体颜色
     * @param useBold 是否加粗
     * @param relativeFontSize 设置相对size
     * @return 具有指定样式的文字
     */
    public static SpannableStringBuilder getMultiStyleText(String[] strings, int[] colors, @Nullable boolean useBold[], @Nullable float relativeFontSize[]){
        int smallSize = Math.min(strings.length, colors.length);
        if(useBold != null){
            smallSize = Math.min(smallSize, useBold.length);
        }

        if(relativeFontSize != null){
            smallSize = Math.min(smallSize, relativeFontSize.length);
        }

        String str = "";
        for (String string : strings) {
            str += string;
        }
        SpannableStringBuilder builder = new SpannableStringBuilder(str);
        int fromIndex = 0;
        for(int i = 0; i < smallSize; i++){
            ForegroundColorSpan span = new ForegroundColorSpan(colors[i]);
            builder.setSpan(span, fromIndex, fromIndex + strings[i].length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            if(useBold != null && useBold[i]) {
                StyleSpan bss = new StyleSpan(android.graphics.Typeface.BOLD);
                builder.setSpan(bss, fromIndex, fromIndex + strings[i].length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            if(relativeFontSize != null){
                RelativeSizeSpan rss = new RelativeSizeSpan(relativeFontSize[i]);
                builder.setSpan(rss, fromIndex, fromIndex + strings[i].length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            //下一组样式
            fromIndex += strings[i].length();

        }
        return builder;
    }

    public static String intercept(String str, String reg, int position){
        List<String> matches = intercept(str, reg);
        String match = null;
        if(matches.size() > position){
            match = matches.get(position);
        }
        return match;
    }
    public static List<String> intercept(String str, String reg){
        //String regex=">(([^\\x00-\\xff]|[0-9])+?)</span>";
        List<String> matches = new ArrayList<>();
        Pattern p = Pattern.compile(reg);

        Matcher m = p.matcher(str);

        while(m.find()){
            matches.add(m.group());
        }

        return matches;
    }


    public static String convertASCIIToString(byte[] bytes) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            sb.append((char) bytes[i]);
        }
        return sb.toString();
    }

    public static String toString(Map<String, Object> map) {
        String str = "";
        for (String key : map.keySet()) {
            str = str + key + "|" + map.get(key) + "; ";
        }

        return str;
    }

    /**
     *
     * @param map
     * @param key
     * @return 若不包含该字段或字段值为空 返回 "" 值
     */
    public static String getDefaultEmpty(Map<String, Object> map, String key){
        return (map != null && map.containsKey(key) && !isEmpty(map.get(key)))? map.get(key).toString() : "";
    }

    /**
     *
     * @param map
     * @param key
     * @return 若不包含该字段或字段值为空 返回 "" 值
     */
    public static String getWithDefault(Map<String, Object> map, String key, String defaultValue){
        return map.containsKey(key) && !isEmpty(map.get(key))? map.get(key).toString() : defaultValue;
    }

    /**
     *
     * @param map
     * @param key
     * @return 若不包含该字段或字段值为空 返回 "" 值
     */
    public static int getDefaultZero(Map<String, Object> map, String key){
        try {
            return map.containsKey(key) && !isEmpty(map.get(key)) && map.get(key) != "null"? Integer.parseInt(map.get(key).toString()) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }

    }

    /**
     *
     * @param old 旧的数据格式
     * @param fresh 新的数据格式
     * @param text 数据
     * @return old if it is failed to convert
     */
    @Nullable
    public static String convertDateFormat(@NotNull String old, @NotNull String fresh, @NotNull String text){
        SimpleDateFormat oldFormat = new SimpleDateFormat(old, Locale.getDefault());
        SimpleDateFormat freshFormat = new SimpleDateFormat(fresh, Locale.getDefault());
        try {
            Date date = oldFormat.parse(text);
            return freshFormat.format(date);
        } catch (ParseException e) {
            return null;
        }
    }


    @Nullable
    public static String convertDateFormat(@NotNull String old, @NotNull String fresh, @NotNull String text, String defaultText){
        String result = convertDateFormat(old, fresh, text);
        return result == null? defaultText: result;
    }

    public static String formatStageDateName(@NotNull String date){
        String[] formats = {"yyyyMMdd", "yyyy-MM-dd"};
        for (String format: formats) {
            String formatted = convertDateFormat(format, "MM-dd期", date);
            if (formatted != null) {
                return formatted;
            }
        }
        return date;
    }
    /**
     *
     * @param map
     * @param key
     * @return 若不包含该字段或字段值为空 返回 "" 值
     */
    public static long getLongDefaultZero(Map<String, Object> map, String key){
        try{
            return map.containsKey(key) && !isEmpty(map.get(key)) && map.get(key) != "null"? Long.parseLong(map.get(key).toString()) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     *
     * @param map
     * @param key
     * @return 若不包含该字段或字段值为空 返回 "" 值
     */
    public static float getFloatDefaultZero(Map<String, Object> map, String key){
        try {
            return map.containsKey(key) && !TextUtil.isEmpty(map.get(key)) && map.get(key) != "null"? Float.parseFloat(map.get(key).toString()) : 0;
        } catch (NumberFormatException e) {
            return 0f;
        }

    }
    /**
     * 转换对应单位的大小
     * @param fileSize
     * @return
     */
    public static String formatSize(long fileSize) {
        String fileSizeStr = "";
        DecimalFormat df = new DecimalFormat("#0.0");
        if (fileSize < 1024) {
            fileSizeStr = fileSize + "B";
        } else if (fileSize < (1024 * 1024)) {
            double fileSizeF = fileSize * 1.0 / 1024;
            fileSizeStr = df.format(fileSizeF) + "KB";
        } else if(fileSize < (1024 * 1024 * 1024)){
            double fileSizeF = fileSize * 1.0 / (1024 * 1024);
            fileSizeStr = df.format(fileSizeF) + "MB";
        }else {
            double fileSizeF = fileSize * 1.0 / (1024 * 1024 * 1024);
            fileSizeStr = df.format(fileSizeF) + "GB";
        }
        return fileSizeStr.equals("B") ? "0B" : fileSizeStr;
    }

    public static String forMatSpeed(long size){
        return formatSize(size) + "/S";
    }

    /**
     * 转换为百分比字符串
     * @param num
     * @return
     */
    public static String getPercentText(float num) {

        return getPercentText(100 * num);
    }

    /**
     * 转换为百分比字符串
     * @param percent
     * @return
     */
    public static String getPercentText(int percent) {

        return percent + "%";
    }
    public static String toString(byte[] b) {
        StringBuffer sb = new StringBuffer();
        String stmp = "";
        for (int n = 0; n < b.length; n++) {
            stmp = Integer.toHexString(b[n] & 0XFF);
            if (stmp.length() == 1) {
                sb.append("0" + stmp);
            } else {
                sb.append(stmp);
            }

        }
        return sb.toString();
    }

    public static boolean wildcardMatch(String pattern, String str) {
        pattern = pattern.replaceAll("\\*", ".*");
        pattern = pattern.replaceAll("\\?", "");
        return str.matches(pattern);
    }

    public static String getMD5Hash(String str) {
        return getMD5Hash(str.getBytes());
    }

    /**
     * 获取MD5
     *
     * @param bytes
     * @return
     */
    public static String getMD5Hash(byte[] bytes) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("md5");
            md.update(bytes);
            byte[] mdbytes = md.digest();
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < mdbytes.length; i++) {
                sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16)
                        .substring(1));
            }
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < mdbytes.length; i++) {
                String hex = Integer.toHexString(0xff & mdbytes[i]);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public static String mergeResStrings(int ...args){
        Context context = AIApplication.getAppCtx();
        String tmp ="";
        for (int stringID : args) {
            tmp += context.getString(stringID);
        }
        return tmp;
    }

    public static String convertToString(Object object){
        if(object == null) return "";
        else return "" + object;
    }

    public static String getString(int stringID){
        Context context = AIApplication.getAppCtx();
        if(context != null) {
            return context.getString(stringID);
        }else{
            return "";
        }
    }

    public static String getString(int stringID, Object...obj){
        Context context = AIApplication.getAppCtx();
        if(context != null) {
            return context.getString(stringID, obj);
        }else{
            return "";
        }
    }

    public static SpannableStringBuilder getMutiColorText(String[] strs, int[] colors){
        int smallSize = strs.length > colors.length? colors.length : strs.length;
        int strSize = strs.length;
        String str = "";
        for(int i = 0; i < strSize; i++){
            str += strs[i];
        }
        SpannableStringBuilder builder = new SpannableStringBuilder(str);
        int fromIndex = 0;
        for(int i = 0; i < smallSize; i++){
            ForegroundColorSpan span = new ForegroundColorSpan(colors[i]);
            builder.setSpan(span, fromIndex, fromIndex + strs[i].length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            fromIndex += strs[i].length();
        }
        return builder;
    }
    /**
     * 返回yyyy-MM-dd hh-mm-ss格式时间
     * @param time
     * @return
     */
    public static String formatTime(long time){
        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh-mm-ss",
                Locale.getDefault());

        return sdf.format(time);
    }

    public static String formatPlayTime(long time){
        return  patchZero(time / 3600000) + ":" +
                patchZero((time % 3600000) / 60000) + ":" +
                patchZero((time % 60000) / 1000);
    }

    public static String patchZero(long num){
        return num < 10?  "0" + num : num + "";
    }

    /**
     * 返回yyyy-MM-dd hh-mm-ss格式时间
     * @param time
     * @return
     */
    public static String formatTime(long time, String format){
        DateFormat sdf = new SimpleDateFormat(format,
                Locale.getDefault());
        return sdf.format(time);
    }

    public static Date parserDateStr(String dateStr) throws ParseException {
        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_hh-mm-ss",
                Locale.getDefault());
        return sdf.parse(dateStr);
    }

    public static String parserDate(Date date) throws ParseException {
        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_hh-mm-ss",
                Locale.getDefault());
        return sdf.format(date);
    }

    public static boolean isEmpty(String str){
        return str == null || str.length() == 0 || str.equals(" ");
    }

    public static boolean isEmpty(Object strObject){
        return strObject == null || isEmpty(strObject.toString());
    }

    //ignoreCase是否忽略大小写
    public static boolean contains(String src, String dest, boolean ignoreCase){
        if(ignoreCase){
            src = src.toLowerCase(Locale.getDefault());
            dest = dest.toLowerCase(Locale.getDefault());
        }

        return src.contains(dest);
    }

    public static String getString(String StringResName){
        String str = null;
        try{
            int id = AIApplication.getAppCtx().getResources().getIdentifier(StringResName,"string", AIApplication.getAppCtx().getPackageName());
            str = AIApplication.getAppCtx().getString(id);
        }catch(Exception e){
            // MLog.e(new Exception(new Throwable("未能获取到字符串资源：" + StringResName )));
        }

        return str == null ? StringResName: str;
    }

    public static float formatSize(String sizeStr) {
        sizeStr = sizeStr.toLowerCase(Locale.getDefault()).trim();

        float size = 0;
        if (sizeStr.endsWith("gb") || sizeStr.endsWith("g")) {
            size = Float.parseFloat(sizeStr.subSequence(0, sizeStr.length() - 1)
                    .toString()) * (long) (1024 * 1024 * 1024);
        } else {
            if (sizeStr.endsWith("mb") || sizeStr.endsWith("m")) {
                size = Float.parseFloat(sizeStr.subSequence(0,
                        sizeStr.length() - 1).toString())
                        * (long) (1024 * 1024);
            } else {
                if (sizeStr.endsWith("kb") || sizeStr.endsWith("k")) {
                    size = Float.parseFloat(sizeStr.subSequence(0,
                            sizeStr.length() - 1).toString())
                            * (long) 1024;
                }
            }
        }

        return size;
    }


    public static String humpToLine(String str){
        Matcher matcher = humpPattern.matcher(str);
        StringBuffer sb = new StringBuffer();
        while(matcher.find()){
            matcher.appendReplacement(sb, "_" + matcher.group(0).toLowerCase());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 获取按照 separator 分割的字符串
     * @param elements
     * @param separator
     * @return
     */
    public static String join(Iterable<? extends Object> elements, CharSequence separator) {
        StringBuilder builder = new StringBuilder();

        if (elements != null) {
            Iterator<? extends Object> iter = elements.iterator();
            if (iter.hasNext()) {
                builder.append(String.valueOf(iter.next()));
                while (iter.hasNext()) {
                    builder.append(separator).append(String.valueOf(iter.next()));
                }
            }
        }

        return builder.toString();
    }

    public static String join(Object[] elements, CharSequence separator) {
        return join(Arrays.asList(elements), separator);
    }

    public static String pickDomain(String fullUrl){
        String[] part1 = fullUrl.split("//");
        if(part1.length > 1){
            String[] part2 = part1[1].split("/");
            if(part2.length > 0){
                return part2[0];
            }
        }
        return "";
    }

    public static String autoSplitText(final TextView tv, String rawText) {
        final Paint tvPaint = tv.getPaint(); //paint，包含字体等信息
        final float tvWidth = tv.getWidth() - tv.getPaddingLeft() - tv.getPaddingRight(); //控件可用宽度
        //将原始文本按行拆分
        String[] rawTextLines = rawText.replaceAll("\r", "").split("\n");
        StringBuilder sbNewText = new StringBuilder();
        for (String rawTextLine : rawTextLines) {
            if (tvPaint.measureText(rawTextLine) <= tvWidth) {
                //如果整行宽度在控件可用宽度之内，就不处理了
                sbNewText.append(rawTextLine);
            } else {
                //如果整行宽度超过控件可用宽度，则按字符测量，在超过可用宽度的前一个字符处手动换行
                float lineWidth = 0;
                for (int cnt = 0; cnt != rawTextLine.length(); ++cnt) {
                    char ch = rawTextLine.charAt(cnt);
                    lineWidth += tvPaint.measureText(String.valueOf(ch));
                    if (lineWidth <= tvWidth) {
                        sbNewText.append(ch);
                    } else {
                        sbNewText.append("\n");
                        lineWidth = 0;
                        --cnt;
                    }
                }
            }
            sbNewText.append("\n");
        }
        //把结尾多余的\n去掉
        if (!rawText.endsWith("\n")) {
            sbNewText.deleteCharAt(sbNewText.length() - 1);
        }
        return sbNewText.toString();
    }
    /**
     * fyy 2018-11-6 增加获取当前年份
     */
    public static int getCurrentYear(){
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Calendar date = Calendar.getInstance( Locale.CHINA);
           return  date.get(Calendar.YEAR);
        }else{
            Date date = new Date(System.currentTimeMillis());
            return  (date.getYear() + 1900);
        }
    }



}
