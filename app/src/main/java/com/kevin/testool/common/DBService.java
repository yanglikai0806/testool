package com.kevin.testool.common;

import com.kevin.testool.utils.FileUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DBService {

    private static Connection conn=null; //打开数据库对象
    private static PreparedStatement ps=null;//操作整合sql语句的对象
    private static ResultSet rs=null;//查询结果的集合

    //DBService 对象
    public static DBService dbService=null;

    /**
     * 构造方法 私有化
     * */

    private DBService(){
    }

    /**
     * 获取MySQL数据库单例类对象
     * */

    public DBService getDbService(){
        if(dbService==null){
            dbService=new DBService();
        }
        return dbService;
    }

    public static List<String> mSelect(String table, int id){
        JSONObject _case = new JSONObject();
        List<String> list=new ArrayList<>();
        String sql = "SELECT * FROM "+ table + " WHERE case_id=?";
        conn = MysqlHelper.getConn();
        try{
            if(conn!=null&&(!conn.isClosed())){
                ps = (PreparedStatement) conn.prepareStatement(sql);
                ps.setInt(1,id);
                if(ps!=null){
                    rs= ps.executeQuery();
                    if(rs!=null){
                        while (rs.next()){
                            _case.put("id", rs.getString("id"));
                            _case.put("case", new JSONObject(rs.getString("case")));
                            _case.put("check_point", new JSONObject(rs.getString("check_point")));
                            _case.put("skip_condition", new JSONObject(rs.getString("skip_condition")));
                            list.add(String.valueOf(_case));
                        }
                    }

                }

            }
        } catch (SQLException | JSONException e) {
            e.printStackTrace();
        }
        MysqlHelper.closeAll(conn,ps,rs);
        return list;
    }

    public static JSONObject selectCaseResult(String table, int id){
        JSONObject result = new JSONObject();
        String sql = "SELECT result FROM "+ table + " WHERE case_id=?";
        conn = MysqlHelper.getConn();
        try{
            if(conn!=null&&(!conn.isClosed())){
                ps = (PreparedStatement) conn.prepareStatement(sql);
                ps.setInt(1,id);
                if(ps!=null){
                    rs= ps.executeQuery();
                    if(rs!=null){
                        while (rs.next()){
                            result = new JSONObject(rs.getString("result"));
                        }
                    }

                }

            }
        } catch (SQLException | JSONException e) {
//            e.printStackTrace();
            result = new JSONObject();
        }
        MysqlHelper.closeAll(conn,ps,rs);
        return result;
    }

    public static void updateCaseResult(String table, JSONObject result, int case_id) throws JSONException {
//        String sql = String.format("UPDATE `%s` SET `id`='%s',`case`='%s',`check_point`='%s',`skip_condition`='%s',`submission_date`=NOW(),`result`='%s' WHERE `case_id`=%s",
//                table, testcase.getString("id"), testcase.getJSONObject("case"),
        String sql = String.format("UPDATE `%s` SET `result`='%s' WHERE `case_id`=%s",
                table, result, case_id);
        conn = MysqlHelper.getConn();
//        System.out.println(sql);
        try{
            if (conn!=null&&(!conn.isClosed())){
                ps = (PreparedStatement) conn.prepareStatement(sql);
                if(ps!=null) {
                    ps.executeUpdate();
                }
            }
        }catch (SQLException e) {
            e.printStackTrace();
        }
        MysqlHelper.closeAll(conn,ps,rs);
    }

    public static JSONArray selectAllCases(String table){

        JSONArray list = new JSONArray();
        String sql = "SELECT * FROM `"+ table + "`";
        conn = MysqlHelper.getConn();
        try{
            if(conn!=null&&(!conn.isClosed())){
                ps = (PreparedStatement) conn.prepareStatement(sql);
                if(ps!=null){
                    rs= ps.executeQuery();
                    if(rs!=null){
                        while (rs.next()){
                            JSONObject _case = new JSONObject();
                            _case.put("id", rs.getString("id"));
                            _case.put("case", new JSONObject(rs.getString("case")));
                            _case.put("check_point", new JSONObject(rs.getString("check_point")));
                            _case.put("skip_condition", new JSONObject(rs.getString("skip_condition")));
//                            System.out.println(_case);
                            list.put( _case);
                        }
                    }

                }

            }
        } catch (SQLException | JSONException e) {
            e.printStackTrace();
        }
        MysqlHelper.closeAll(conn,ps,rs);
        return list;
    }


    public static JSONArray showTables(){

        JSONArray tables = new JSONArray();
        String sql = "SHOW TABLES";
        conn = MysqlHelper.getConn();
        try{
            if(conn!=null&&(!conn.isClosed())){
                ps = (PreparedStatement) conn.prepareStatement(sql);
                if(ps!=null){
                    rs= ps.executeQuery();
                    if(rs!=null){
                        while (rs.next()){
                            tables.put(rs.getString("Tables_in_test_mp"));
                        }
                    }

                }

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        MysqlHelper.closeAll(conn,ps,rs);
        return tables;
    }

    public static void syncTestcases(){

        JSONArray _tables = DBService.showTables();
                for (int i=0; i < _tables.length(); i++){
                    try {
                        JSONArray list_cases = DBService.selectAllCases(_tables.getString(i));
                        FileUtils.writeCaseJsonFile(_tables.getString(i), list_cases);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }



}
