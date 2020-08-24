package com.kevin.testool.common;

import com.kevin.share.CONST;
import com.kevin.share.Common;
import com.kevin.testool.MyApplication;
import com.kevin.share.utils.ToastUtils;
import com.kevin.share.utils.logUtil;

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
    /**
     * 连接数据库
     * */

    public static Connection getConn(){
        Connection conn = null;
        try {
            Class.forName(driver);//获取MYSQL驱动
            try {
                JSONObject mysql = Common.CONFIG().getJSONObject("MYSQL");
                String url = "jdbc:mysql://"+ mysql.getString("server_ip") + ":" + mysql.getString("port") + "/"+mysql.getString("database")+"?useUnicode=true&characterEncoding=UTF-8";//MYSQL数据库连接Url
                conn = (Connection) DriverManager.getConnection(url, mysql.getString("user"), mysql.getString("password"));//获取连接
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
        if (conn == null){
            ToastUtils.showLongByHandler(MyApplication.getContext(), "数据库连接失败");
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

    public static List<String> getTestcase(String Table, int id){
        return DBService.mSelect(Table, id);
    }

}


