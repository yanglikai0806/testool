package com.kevin.testool;

import android.util.Log;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class DateTimeUtils {
    // 获取当前时间n[]之后的时间的日期时间字符串（N的单位为Calendar的那些表示时间的常量）
    public static String getNLaterDateTimeString(int nType, int n) {
        Date date = new Date();
        Calendar c = new GregorianCalendar();
        c.setTime(date);
        c.add(nType, n);

        return CalendarToString(c);
    }

    // millis to datetime
    public static String getDateTimeStringFromMillis(long millis) {
        Date date = new Date(millis);
        return (DateToString(date));
    }

    // 把日期时间字符串的时间转换成毫秒值（RTC）
    public static long stringToMillis(String dateTime) {
        Calendar c = StringToGregorianCalendar(dateTime);

        return c.getTimeInMillis();
    }

    // 获取两个日期时间字符串表示的时间之间的毫秒差值
    public static long getDifMillis(String dateTime1, String dateTime2) {
        return (stringToMillis(dateTime1) - stringToMillis(dateTime2));
    }

    // 输入一个表示日期或时间的整型数，输出"01"或"23"这样格式的字符串
    public static String getDoubleNumString(int n) {
        int num = n % 60;

        if (num < 10) {
            return "0" + num;
        } else {
            return num + "";
        }
    }

    // 获取标准日期时间字符串的整型的日期值，如："2015-01-21 00:00:00"，返回：21
    public static int getDayOfMonth(String dateTime) {
        Calendar c = StringToGregorianCalendar(dateTime);
        int day = c.get(Calendar.DAY_OF_MONTH);

        return day;
    }

    // 获取当前时间的日期时间字符串，格式："yyyy-MM-dd HH:mm:ss"
    public static String getCurrentDateTimeString() {
        Date date = new Date();
        return DateToString(date);
    }

    // 获取当前的"yyyy-MM-dd"日期格式字符串
    public static String getCurrentDateString() {
        Date date = new Date();
        return DateToString(date).substring(0, 10);
    }

    // 获取当前的"yyyy-MM"日期格式字符串
    public static String getCurrentMonthString() {
        Date date = new Date();
        return DateToString(date).substring(0, 7);
    }

    // 获取当前的"HH:mm"时间格式字符串
    public static String getCurrentTimeString() {
        Date date = new Date();
        return DateToString(date).substring(11, 16);
    }

    // 获取当前的"HH:mm:ss"时间格式字符串
    public static String getCurrentTimeLongString() {
        Date date = new Date();
        return DateToString(date).substring(11, 19);
    }

    // 由日期时间字符串生成“11月1日 星期一”这样格式的字符串
    public static String getShortDateTimeOfWeek(String dateTime) {
        Calendar c = StringToGregorianCalendar(dateTime);

        int month = c.get(Calendar.MONTH) + 1;
        int day = c.get(Calendar.DAY_OF_MONTH);

        String[] weekStr = new String[] { "星期日", "星期一", "星期二", "星期三", "星期四",
                "星期五", "星期六" };
        String week = weekStr[c.get(Calendar.DAY_OF_WEEK) - 1];

        String result = month + "月" + day + "日" + "  " + week;

        return result;
    }

    // 由日期时间字符串生成“2015年11月1日 星期一”这样格式的字符串
    public static String getDateTimeOfWeek(String dateTime) {
        Calendar c = StringToGregorianCalendar(dateTime);

        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH) + 1;
        int day = c.get(Calendar.DAY_OF_MONTH);

        String[] weekStr = new String[] { "星期日", "星期一", "星期二", "星期三", "星期四",
                "星期五", "星期六" };
        String week = weekStr[c.get(Calendar.DAY_OF_WEEK) - 1];

        String result = year + "年" + month + "月" + day + "日" + "  " + week;

        return result;
    }

    // 由日期时间字符串生成“2015年11月1日 05:43”这样格式的字符串
    public static String getDateTimeOfHourMinute(String dateTime) {
        String result = "";
        String date = dateTime.split(" ")[0];
        String time = dateTime.split(" ")[1];
        String[] dateArr = date.split("-");
        String[] timeArr = time.split(":");

        int year = Integer.parseInt(dateArr[0]);
        int month = Integer.parseInt(dateArr[1]);
        int day = Integer.parseInt(dateArr[2]);

        result = year + "年" + month + "月" + day + "日" + "  " + timeArr[0] + ":"
                + timeArr[1];

        return result;
    }

    // 用年月日生成日期字符串，month取值范围：[0,13]
    public static String getDateString(int year, int month, int day) {
        String m;
        String d;

        if (month >= 9) {
            m = (month + 1) + "";
        } else {
            m = "0" + (month + 1);
        }

        if (day >= 10) {
            d = day + "";
        } else {
            d = "0" + day;
        }

        String dateString = year + "-" + m + "-" + d;
        return dateString;
    }

    // 用年月生成年月日期字符串，month取值范围：[0,13]
    public static String getDateString(int year, int month) {
        String m;
        String d;

        if (month >= 9) {
            m = (month + 1) + "";
        } else {
            m = "0" + (month + 1);
        }

        String dateString = year + "-" + m;
        return dateString;
    }

    // 用时、分生成时间字符串
    public static String getTimeString(int hour, int minute) {
        String h;
        String m;

        if (hour >= 10) {
            h = hour + "";
        } else {
            h = "0" + hour;
        }

        if (minute >= 10) {
            m = minute + "";
        } else {
            m = "0" + minute;
        }

        return h + ":" + m;
    }

    // 用时、分、秒生成时间字符串
    public static String getTimeString(int hour, int minute, int second) {
        String h;
        String m;
        String c;

        if (hour >= 10) {
            h = hour + "";
        } else {
            h = "0" + hour;
        }

        if (minute >= 10) {
            m = minute + "";
        } else {
            m = "0" + minute;
        }

        if (second >= 10) {
            c = second + "";
        } else {
            c = "0" + second;
        }

        return h + ":" + m + ":" + c;
    }

    // 该内部类是用于对日期时间字符串数组进行排序的
    public class DateTimeString implements Comparable<DateTimeString> {
        private String mDateTimeStr;

        public DateTimeString(String dateTimeStr) {
            mDateTimeStr = dateTimeStr;
        }

        @Override
        public int compareTo(DateTimeString another) {
            return compareDateTimeString(mDateTimeStr.toString(),
                    another.toString());
        }

        @Override
        public String toString() {
            return mDateTimeStr;
        }

    }

    // 对日期时间字符串数组进行排序,返回排序后的数组（排序后的顺序是从小到大）
    public static String[] sortDateTimeStringArray(String[] dateTimeStringArray) {
        // 将日期时间字符串数组转换成DateTimeString类型数组，DateTimeString实现了Comparable接口，可以进行排序
        DateTimeString[] tmpArray = new DateTimeString[dateTimeStringArray.length];

        // 生成tmpArray数组
        int i = 0;
        DateTimeUtils tmpUtil = new DateTimeUtils();
        for (String str : dateTimeStringArray) {
            tmpArray[i++] = tmpUtil.new DateTimeString(str);
        }

        // 对tmpArray进行排序
        Arrays.sort(tmpArray);

        String[] result = new String[tmpArray.length];
        i = 0;
        for (DateTimeString str : tmpArray) {
            result[i++] = str.toString();
        }
        return result;
    }

    // 比较两个日期时间字符串的大小，如果str1比str2早，则返回-1，如果相等返回0，否则返回1
    public static int compareDateTimeString(String str1, String str2) {
        Date d1 = StringToDate(str1);
        Date d2 = StringToDate(str2);
        if (d1.getTime() - d2.getTime() < 0) {
            return -1;
        } else if (d1.getTime() - d2.getTime() > 0) {
            return 1;
        } else {
            return 0;
        }

    }

    // 时间日期字符串转换成Date对象
    // 注：dateTimeStr带不带前导0都是可以的，比如"2011-01-01 01:02:03"和"2011-1-1 1:2:3"都是合法的
    public static Date StringToDate(String dateTimeStr) {
        Date date = new Date();
        // DateFormat fmt = DateFormat.getDateTimeInstance();
        DateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            date = fmt.parse(dateTimeStr);
            return date;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    // Date对象转换成日期时间字符串
    public static String DateToString(Date date) {
        String dateTimeStr = null;
        // DateFormat fmt = DateFormat.getDateTimeInstance();
        DateFormat fmt = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        dateTimeStr = fmt.format(date);
        return dateTimeStr;
    }

    // 字符串转换成Calendar
    public static Calendar StringToGregorianCalendar(String dateTimeStr) {
        Date date = StringToDate(dateTimeStr);
        Calendar calendar = new GregorianCalendar();

        calendar.setTime(date);
        return calendar;
    }

    // Calendar转换成String
    public static String CalendarToString(Calendar calendar) {
        Date date = ((GregorianCalendar) calendar).getTime();
        return DateToString(date);
    }

    // 获取日期时间格式字符串表示的两日期时间之间相隔的天数（天数可为浮点型） AC
    public static double getDayNumDif(String str1, String str2) {
        // DateFormat fmt = DateFormat.getDateTimeInstance();
        DateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date d1 = fmt.parse(str1);
            Date d2 = fmt.parse(str2);
            long dif = Math.abs(d1.getTime() - d2.getTime());
            double dayDif = (double) (dif) / 1000 / (24 * 60 * 60);

            // 保留两位小数
            DecimalFormat df = new DecimalFormat("0.00");
            return Double.parseDouble(df.format(dayDif));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return -1;
    }

    // 求算术平均值函数，保留2位小数
    public static double getAverage(double[] data) {
        double sum = 0;
        for (int i = 0; i < data.length; i++) {
            sum += data[i];
        }

        DecimalFormat df = new DecimalFormat("0.00");
        return Double.parseDouble(df.format(sum / data.length));
    }

    // 输入一个时间日期字符串（格式：“yyyy-MM-dd HH:mm:ss”），输出num天后的时间日期字符串（num可为浮点数）
    public static String getNDayLatterDateTime(String str, double num) {
        // 创建日期时间格式对象fmt
        // DateFormat fmt = DateFormat.getDateTimeInstance();
        DateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date curDate = fmt.parse(str);
            GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTime(curDate);

            calendar.add(Calendar.SECOND, (int) (num * (24 * 60 * 60)));

            Date newDate = calendar.getTime();
            String newDateStr = fmt.format(newDate);
            return newDateStr;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return "";
    }

}
