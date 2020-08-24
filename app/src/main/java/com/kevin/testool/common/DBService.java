package com.kevin.testool.common;

import com.kevin.share.Common;
import com.kevin.share.utils.logUtil;
import com.kevin.testool.MyApplication;
import com.kevin.share.utils.FileUtils;
import com.kevin.share.utils.ToastUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
//import net.sf.json.JSONObject;

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

    public static boolean createTable(String table){
        String sql = String.format("CREATE TABLE IF NOT EXISTS `%s` (`case_id` INT NOT NULL, `id` VARCHAR(255), `case` VARCHAR(1024) NOT NULL, `check_point` VARCHAR(600) NOT NULL, `skip_condition` VARCHAR(600) NOT NULL,`submission_date` DATETIME, `result` VARCHAR(1024) NOT NULL,PRIMARY KEY (`case_id`)) ENGINE= InnoDB CHARSET=utf8", table);
        conn = MysqlHelper.getConn();
        try{
            if(conn!=null&&(!conn.isClosed())){
                ps = (PreparedStatement) conn.prepareStatement(sql);
                if(ps!=null){
                    ps.executeUpdate();
                }
            } else {
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        MysqlHelper.closeAll(conn,ps,rs);
        return true;
    }

    public static boolean dropTable(String table){
        String sql = String.format("DROP TABLE `%s`", table);
        conn = MysqlHelper.getConn();
        try{
            if(conn!=null&&(!conn.isClosed())){
                ps = (PreparedStatement) conn.prepareStatement(sql);
                if(ps!=null){
                    ps.executeUpdate();
                }

            } else {
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        MysqlHelper.closeAll(conn,ps,null);
        return true;
    }

    public static boolean insertCase(String table, JSONObject testcase, int case_id) throws JSONException {
        logUtil.d("上传case", testcase.toString());
        String id = "";
        if (!testcase.isNull("id")){
            id = testcase.getString("id");
        }
        JSONObject caseInfo = new JSONObject();
        if (!testcase.isNull("case")){
            caseInfo = testcase.getJSONObject("case");
        }
        JSONObject check_point = new JSONObject();
        if (!testcase.isNull("check_point")){
            check_point = testcase.getJSONObject("check_point");
        }
        JSONObject skip_condition = new JSONObject();
        if (!testcase.isNull("skip_condition")){
            skip_condition = testcase.getJSONObject("skip_condition");
        }

        String sql = String.format("INSERT IGNORE INTO `%s` (`case_id`,`id`,`case`,`check_point`,`skip_condition`,`submission_date`) VALUES ('%s','%s', '%s', '%s', '%s',NOW())",
                table, case_id, id, caseInfo, check_point, skip_condition);
        conn = MysqlHelper.getConn();
        try{
            if(conn!=null&&(!conn.isClosed())){
                ps = (PreparedStatement) conn.prepareStatement(sql);
                if(ps!=null){
                    ps.executeUpdate();
                }

            } else {
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            logUtil.d("insert case", e.toString());
            return false;
        }
        MysqlHelper.closeAll(conn,ps,null);
        return true;
    }

    public static void updateCase(String table, JSONObject testcase, int case_id) throws JSONException {
        String sql = String.format("UPDATE `%s` SET `id`='%s',`case`='%s',`check_point`='%s',`skip_condition`='%s',`submission_date`=NOW() WHERE `case_id`=%s",
        table, testcase.getString("id"), testcase.getJSONObject("case"),testcase.getJSONObject("check_point"), testcase.getJSONObject("skip_condition"), case_id);
        conn = MysqlHelper.getConn();
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

    public static void updateCaseResult(String table, JSONObject result, int case_id){
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


    public static JSONArray showTables(String database){

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
                            tables.put(rs.getString("Tables_in_"+database));
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

    /**
     * 同步数据库全部用例
     */
    public static void syncTestcases(){
        String database = "test_mp";
        try {
            JSONObject mysql = Common.CONFIG().getJSONObject("MYSQL");
            database = mysql.getString("database");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSONArray _tables = DBService.showTables(database);
//        ToastUtils.showLongByHandler(MyApplication.getContext(), _tables.toString());
                for (int i=0; i < _tables.length(); i++){
                    try {
                        JSONArray list_cases = DBService.selectAllCases(_tables.getString(i));
                        FileUtils.writeCaseJsonFile(_tables.getString(i), list_cases);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
        ToastUtils.showShortByHandler(MyApplication.getContext(), "用例同步完成");
            }

    public static void syncTestcase(String table){
        JSONArray list_cases = DBService.selectAllCases(table);
        if (list_cases.length() > 0) {
            FileUtils.writeCaseJsonFile(table, list_cases);
            ToastUtils.showShortByHandler(MyApplication.getContext(), "用例更新完成");
        } else {
            ToastUtils.showShortByHandler(MyApplication.getContext(), "用例更新失败");
        }
    }
}
