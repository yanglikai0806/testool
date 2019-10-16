package com.kevin.testool.common;

import com.kevin.testool.logUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;


public class MysqlHelper {

    private static String driver = "com.mysql.jdbc.Driver";//MySQL 驱动
    private static String url = "jdbc:mysql://xx.xx.xx.xx:3306/xx?useUnicode=true&characterEncoding=UTF-8";//MYSQL数据库连接Url xx 为数据库ip， 以及表名
    private static String user = "xxxx";//用户名
    private static String password = "xxxx";//密码

    /**
     * 连接数据库
     * */

    public static Connection getConn(){
        Connection conn = null;
        try {
            Class.forName(driver);//获取MYSQL驱动
            try {
                JSONObject mysql = Common.CONFIG().getJSONObject("MYSQL");
                conn = (Connection) DriverManager.getConnection(mysql.getString("url"), mysql.getString("user"), mysql.getString("password"));//获取连接
            } catch (JSONException e) {
                logUtil.d("MYSQL",url +"|user:"+ user + "|password:"+password);
                conn = (Connection) DriverManager.getConnection(url, user, password);//获取连接
            }
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
        return conn;
    }

//    /**
//     * 关闭数据库
//     * */
//
//    public static void closeAll(Connection conn, PreparedStatement ps){
//        if (conn != null) {
//            try {
//                conn.close();
//            } catch (SQLException e) {
//                e.printStackTrace();
//            }
//        }
//        if (ps != null) {
//            try {
//                ps.close();
//            } catch (SQLException e) {
//                e.printStackTrace();
//            }
//        }
//
//    }

    /**
     * 关闭数据库
     * */

    public static void closeAll(Connection conn, PreparedStatement ps, ResultSet rs){
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (ps != null) {
            try {
                ps.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

}


