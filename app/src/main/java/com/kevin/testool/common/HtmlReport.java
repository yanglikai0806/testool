package com.kevin.testool.common;

import android.annotation.SuppressLint;
import android.os.Build;
import android.util.Log;

import com.kevin.testool.MyFile;
import com.kevin.testool.logUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static java.lang.System.in;
import static java.lang.System.lineSeparator;

public class HtmlReport {

    static String HTML_TMPL = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" +
            "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
            "<head>\n" +
            "    <title>%s</title>\n" +
            "    <meta name=\"generator\" content=\"%s\"/>\n" +
            "    <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>\n" +
            "    %s\n" +
            "    <link href=\"http://cdn.bootcss.com/bootstrap/3.3.0/css/bootstrap.min.css\" rel=\"stylesheet\">\n" +
            "</head>\n" +
            "<body>\n" +
            "<script language=\"javascript\" type=\"text/javascript\"><!--\n" +
            "output_list = Array();\n" +
            "\n" +
            "/* level - 0:Summary; 1:Failed; 2:All */\n" +
            "function showCase(level) {\n" +
            "    trs = document.getElementsByTagName(\"tr\");\n" +
            "    for (var i = 0; i < trs.length; i++) {\n" +
            "        tr = trs[i];\n" +
            "        id = tr.id;\n" +
            "        if (id.substr(0,2) == 'ft') {\n" +
            "            if (level < 1) {\n" +
            "                tr.className = 'hiddenRow';\n" +
            "            }\n" +
            "            else {\n" +
            "                tr.className = '';\n" +
            "            }\n" +
            "        }\n" +
            "        if (id.substr(0,2) == 'pt') {\n" +
            "            if (level > 1) {\n" +
            "                tr.className = '';\n" +
            "            }\n" +
            "            else {\n" +
            "                tr.className = 'hiddenRow';\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "}\n" +
            "\n" +
            "\n" +
            "function showClassDetail(cid, count) {\n" +
            "    var id_list = Array(count);\n" +
            "    var toHide = 1;\n" +
            "    for (var i = 0; i < count; i++) {\n" +
            "        tid0 = 't' + cid.substr(1) + '.' + (i+1);\n" +
            "        tid = 'f' + tid0;\n" +
            "        tr = document.getElementById(tid);\n" +
            "        if (!tr) {\n" +
            "            tid = 'p' + tid0;\n" +
            "            tr = document.getElementById(tid);\n" +
            "        }\n" +
            "        id_list[i] = tid;\n" +
            "        if (tr.className) {\n" +
            "            toHide = 0;\n" +
            "        }\n" +
            "    }\n" +
            "    for (var i = 0; i < count; i++) {\n" +
            "        tid = id_list[i];\n" +
            "        if (toHide) {\n" +
            "            document.getElementById('div_'+tid).style.display = 'none'\n" +
            "            document.getElementById(tid).className = 'hiddenRow';\n" +
            "        }\n" +
            "        else {\n" +
            "            document.getElementById(tid).className = '';\n" +
            "        }\n" +
            "    }\n" +
            "}\n" +
            "\n" +
            "\n" +
            "function showTestDetail(div_id){\n" +
            "    var details_div = document.getElementById(div_id)\n" +
            "    var displayState = details_div.style.display\n" +
            "    // alert(displayState)\n" +
            "    if (displayState != 'block' ) {\n" +
            "        displayState = 'block'\n" +
            "        details_div.style.display = 'block'\n" +
            "    }\n" +
            "    else {\n" +
            "        details_div.style.display = 'none'\n" +
            "    }\n" +
            "}\n" +
            "\n" +
            "\n" +
            "function html_escape(s) {\n" +
            "    s = s.replace(/&/g,'&amp;');\n" +
            "    s = s.replace(/</g,'&lt;');\n" +
            "    s = s.replace(/>/g,'&gt;');\n" +
            "    return s;\n" +
            "}\n" +
            "\n" +
            "/* obsoleted by detail in <div>\n" +
            "function showOutput(id, name) {\n" +
            "    var w = window.open(\"\", //url\n" +
            "                    name,\n" +
            "                    \"resizable,scrollbars,status,width=800,height=450\");\n" +
            "    d = w.document;\n" +
            "    d.write(\"<pre>\");\n" +
            "    d.write(html_escape(output_list[id]));\n" +
            "    d.write(\"\\n\");\n" +
            "    d.write(\"<a href='javascript:window.close()'>close</a>\\n\");\n" +
            "    d.write(\"</pre>\\n\");\n" +
            "    d.close();\n" +
            "}\n" +
            "*/\n" +
            "--></script>\n" +
            "<div id=\"div_base\">\n" +
            "\n" +
            "%s\n" +
            "%s\n" +
            "%s\n" +
            "\n" +
            "</div>\n" +
            "</body>\n" +
            "</html>";

//    static JSONObject STATUS;


    static String STYLESHEET_TMPL = "<style type=\"text/css\" media=\"screen\">\n" +
            "body        { font-family: verdana, arial, helvetica, sans-serif; font-size: 80%; }\n" +
            "table       { font-size: 100%; }\n" +
            "pre         { white-space: pre-wrap;word-wrap: break-word; }\n" +
            "\n" +
            "/* -- heading ---------------------------------------------------------------------- */\n" +
            "h1 {\n" +
            "\tfont-size: 16pt;\n" +
            "\tcolor: gray;\n" +
            "}\n" +
            ".heading {\n" +
            "    margin-top: 0ex;\n" +
            "    margin-bottom: 1ex;\n" +
            "}\n" +
            "\n" +
            ".heading .attribute {\n" +
            "    margin-top: 1ex;\n" +
            "    margin-bottom: 0;\n" +
            "}\n" +
            "\n" +
            ".heading .description {\n" +
            "    margin-top: 2ex;\n" +
            "    margin-bottom: 3ex;\n" +
            "}\n" +
            "\n" +
            "/* -- css div popup ------------------------------------------------------------------------ */\n" +
            "a.popup_link {\n" +
            "}\n" +
            "\n" +
            "a.popup_link:hover {\n" +
            "    color: red;\n" +
            "}\n" +
            "\n" +
            ".popup_window {\n" +
            "    display: none;\n" +
            "    position: relative;\n" +
            "    left: 0px;\n" +
            "    top: 0px;\n" +
            "    /*border: solid #627173 1px; */\n" +
            "    padding: 10px;\n" +
            "    background-color: #E6E6D6;\n" +
            "    font-family: \"Lucida Console\", \"Courier New\", Courier, monospace;\n" +
            "    text-align: left;\n" +
            "    font-size: 8pt;\n" +
            "    /* width: 500px;*/\n" +
            "}\n" +
            "\n" +
            "}\n" +
            "/* -- report ------------------------------------------------------------------------ */\n" +
            "#show_detail_line {\n" +
            "    margin-top: 3ex;\n" +
            "    margin-bottom: 1ex;\n" +
            "}\n" +
            "#result_table {\n" +
            "    width: 99%;\n" +
            "}\n" +
            "#header_row {\n" +
            "    font-weight: bold;\n" +
            "    color: white;\n" +
            "    background-color: #777;\n" +
            "}\n" +
            "#total_row  { font-weight: bold; }\n" +
            ".passClass  { background-color: #74A474; }\n" +
            ".failClass  { background-color: #FDD283; }\n" +
            ".errorClass { background-color: #FF6600; }\n" +
            ".passCase   { color: #6c6; }\n" +
            ".failCase   { color: #FF6600; font-weight: bold; }\n" +
            ".errorCase  { color: #c00; font-weight: bold; }\n" +
            ".hiddenRow  { display: none; }\n" +
            ".testcase   { margin-left: 2em; }\n" +
            "\n" +
            "\n" +
            "/* -- ending ---------------------------------------------------------------------- */\n" +
            "#ending {\n" +
            "}\n" +
            "\n" +
            "#div_base {\n" +
            "            position:absolute;\n" +
            "            top:0%;\n" +
            "            left:5%;\n" +
            "            right:5%;\n" +
            "            width: auto;\n" +
            "            height: auto;\n" +
            "            margin: -15px 0 0 0;\n" +
            "}\n" +
            "</style>\n";

    static String HEADING_TMPL = "<div class='page-header'>\n" +
            "<h1>%s</h1>\n" +
            "%s\n" +
            "</div>\n" +
            "<p class='description'>%s</p>\n";

    static String HEADING_ATTRIBUTE_TMPL = "<p class='attribute'><strong>%s:</strong> %s</p>";

    static String REPORT_TMPL = "<div class=\"btn-group btn-group-sm\">\n" +
            "<button class=\"btn btn-default\" onclick='javascript:showCase(0)'>总结</button>\n" +
            "<button class=\"btn btn-default\" onclick='javascript:showCase(1)'>失败</button>\n" +
            "<button class=\"btn btn-default\" onclick='javascript:showCase(2)'>全部</button>\n" +
            "</div>\n" +
            "<p></p>\n" +
            "<table id='result_table' class=\"table table-bordered\">\n" +
            "<colgroup>\n" +
            "<col align='left' />\n" +
            "<col align='right' />\n" +
            "<col align='right' />\n" +
            "<col align='right' />\n" +
            "<col align='right' />\n" +
            "<col align='right' />\n" +
            "<col align='right' />\n" +
            "</colgroup>\n" +
            "<tr id='header_row'>\n" +
            "    <td>测试套件/测试用例</td>\n" +
            "    <td>总数</td>\n" +
            "    <td>通过</td>\n" +
            "    <td>失败</td>\n" +
            "    <td>错误</td>\n" +
            "    <td>通过率</td>\n" +
            "    <td>查看</td>\n" +
            "</tr>\n" +
            "%s\n" +
            "<tr id='total_row'>\n" +
            "    <td>总计</td>\n" +
            "    <td>%s</td>\n" +
            "    <td>%s</td>\n" +
            "    <td>%s</td>\n" +
            "    <td>%s</td>\n" +
            "    <td>%s</td>\n" +
            "    <td>&nbsp;</td>\n" +
            "</tr>\n" +
            "</table>\n";

    static String REPORT_CLASS_TMPL = "<tr class='%s'>\n" +
            "    <td>%s</td>\n" +
            "    <td>%s</td>\n" +
            "    <td>%s</td>\n" +
            "    <td>%s</td>\n" +
            "    <td>%s</td>\n" +
            "    <td>%s</td>\n" +
            "    <td><a href=\"javascript:showClassDetail('%s',%s)\">详情</a></td>\n" +
            "</tr>\n";


    static String REPORT_TEST_OUTPUT_TMPL = "\n%s: %s";
    static String ENDING_TMPL = "<div id='ending'>&nbsp;</div>";

    static String TITLE = "REPORT_TITLE";
    static String START = "START";
    static String CASEID = "CASEID";
    static String RESULT = "RESULT";
    static String ERROR = "ERROR";

    static String title = "";
    static String startTime = "";
    static String stopTime = "";
    static int success_count = 0;
    static int failure_count = 0;
    static int error_count = 0;
    static JSONObject result = new JSONObject();
    String device = "";
    String env = "";
    static String _domain;
    static String _caseid;
    static String _error;
    static String _result;
    static int start_i;
    static int end_i;


    public static JSONObject sortResult(String logFile) throws IOException, JSONException {

        JSONObject _result_dic = new JSONObject("{\"true\": 0, \"false\": 1, \"error\": 2}");

        _caseid = "";
        _result = "";
        _error = "";
        start_i = 0;
        end_i = 1;

        JSONObject r = new JSONObject();

        File file = new File(logFile);
        if (!file.exists()) {
            logUtil.d("logFile", "文件不存在");
        }
        String line;
        BufferedReader br = new BufferedReader(new FileReader(file));
        JSONArray content = new JSONArray();
//        ArrayList<String> content = new ArrayList<>();
        int i = 0;
        while((line = br.readLine()) != null){
            i++;
            content.put(line);
            if (line.contains(TITLE)){
                startTime = line.split(":")[0];
                title = line.split(TITLE)[1].trim();
            }

            if (line.contains(START)){
                _domain = line.split(START)[1].trim();
                r.put(_domain, new JSONArray());
            }

            if (line.contains(CASEID)){
                _caseid = line.split(CASEID)[1].trim();
                start_i = i;
            }

            if (line.contains(ERROR)){
                _error = line.split(ERROR)[1].trim();
                error_count += 1;
                _result = "error";
                end_i = i;
//                r.getJSONArray(_domain).put(_result_dic);
                JSONArray r_detail = new JSONArray();
                r_detail.put(_result_dic.getInt(_result));
                r_detail.put(_caseid);
                JSONArray mContent = new JSONArray();
                for (int j=start_i+1; j< end_i; j++){
                    mContent.put(content.getString(j));
                }
                r_detail.put(mContent);
                r_detail.put(_error);
                r.getJSONArray(_domain).put(r_detail);
                _error = "";
                continue;
            }

            if (line.contains(RESULT)){
                _result = line.split(RESULT)[1].trim();
                if (_result.equals("true")){
                    success_count += 1;
                }
                if (_result.equals("false")){

                    failure_count += 1;
                }
                end_i = i;
                JSONArray r_detail = new JSONArray();
                r_detail.put(_result_dic.getInt(_result));
                r_detail.put(_caseid);
                JSONArray mContent = new JSONArray();
                for (int j=start_i+1; j< end_i; j++){
                    mContent.put(content.getString(j));
                }
                r_detail.put(mContent);
                r_detail.put(_error);
                r.getJSONArray(_domain).put(r_detail);
//                Log.d("report", r_detail.toString());
            }
//            Log.d("report", line);
            stopTime = line.split(":")[0];
        }

        br.close();
        return r;
    }

    public static JSONArray getReportAttributes(){
        ArrayList<String> statusLst = new ArrayList<>();
        String status = "";
        statusLst.add("通过 " + success_count);
        statusLst.add("失败 " + failure_count);
        statusLst.add("错误 " + error_count);
        if (statusLst.size() > 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                status = String.join(" ", statusLst);
            } else {
                status = String.valueOf(statusLst).replace("[","").replace("]", "").replace(" ", "").replace(",", " ");
            }
        } else {
            status = "none";
        }
        return new JSONArray().put(new JSONArray().put("开始时间").put(startTime)).put(new JSONArray().put("结束时间").put(stopTime)).put(new JSONArray().put("状态").put(status));
    }

    public static String generateReport(String logFile) throws IOException, JSONException {
        result = sortResult(logFile);
//        Log.d("Report", result.toString());
        JSONArray report_attrs = getReportAttributes();
        String generator = "HTMLTestRunner 1.0";
//        stylesheet = self._generate_stylesheet()
        String heading = _generate_heading(report_attrs);
        String report = _generate_report();
        String ending = _generate_ending();
        String output = String.format(HTML_TMPL, title, generator, STYLESHEET_TMPL, heading,report,ending);

        File reportFile = new File(logFile.replace("log.txt", "report.html"));
        MyFile.writeFile(reportFile.toString(), output, false);
        System.out.println(logFile.replace("log.txt", "report.html"));
        return logFile.replace("log.txt", "report.html");
    }

    public static String _generate_heading(JSONArray report_attrs) throws JSONException {

        ArrayList<String> a_lines = new ArrayList<>();
        for (int i=0; i< report_attrs.length(); i++){
            report_attrs.getJSONArray(i).get(0);
            String line = String.format(HEADING_ATTRIBUTE_TMPL, report_attrs.getJSONArray(i).get(0), report_attrs.getJSONArray(i).get(1));
            a_lines.add(line);
        }
        String a_lines_t;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            a_lines_t = String.join("", a_lines);
        } else {
            a_lines_t = String.valueOf(a_lines).replace("[","").replace("]", "").replace(" ", "").replace(",", "");
        }

        String heading = String.format(HEADING_TMPL, title, a_lines_t, "App测试报告");

        return heading;

    }

    public static String _generate_report() throws JSONException {
        ArrayList<String> rows = new ArrayList<>();
        Iterator<String> keySet = result.keys();
        int cid=0;
        while (keySet.hasNext()){
            int np = 0, nf = 0, ne = 0;
            String key = keySet.next();
            JSONArray caseSet = result.getJSONArray(key);
            for (int i=0; i < caseSet.length(); i++){

                if (caseSet.getJSONArray(i).getInt(0) == 0){
                    np += 1;
                }
                if (caseSet.getJSONArray(i).getInt(0) == 1){
                    nf += 1;
                }
                if (caseSet.getJSONArray(i).getInt(0) == 2){
                    ne += 1;
                }
            }
            String desc = key + "\n";
            String style = "passClass";
            if (ne > 0) {
                style = "errorClass";
            }
            if (nf > 0){
                style= "failClass";
            }
            if ((np + nf + ne) != 0) {
                String row = String.format(REPORT_CLASS_TMPL, style, desc, np+nf+ne+"", np+"", nf+"", ne+"", String.valueOf(np / (np + nf + ne) * 100), "c"+(cid+1), np+nf+ne+"");
                rows.add(row);
            }
            for (int tid=0; tid< caseSet.length(); tid++){
                int n = caseSet.getJSONArray(tid).getInt(0);
                String t = caseSet.getJSONArray(tid).getString(1);
                JSONArray o = caseSet.getJSONArray(tid).getJSONArray(2);
                String e = caseSet.getJSONArray(tid).getString(3);
                _generate_report_test(rows, cid, tid, n, t, o, e);
            }
            cid++;
        }

        String pp = "0.00";
        if ((success_count + failure_count + error_count) > 0) {
             pp = String.valueOf(success_count / (success_count + failure_count + error_count) * 100);
        }
        String _rows;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            _rows = String.join("", rows);
        } else {
            _rows = String.valueOf(rows).replace("[","").replace("]", "").replace(",", "");
        }

        String report = String.format(REPORT_TMPL, _rows, success_count + failure_count + error_count, success_count, failure_count, error_count, pp);

        return report;
    }

    public static void  _generate_report_test(ArrayList<String> rows, int cid, int _tid, int n, String t, JSONArray o, String e) throws JSONException {
        boolean has_output = false;
        if (o.length() > 0 || e.length() > 0){
            has_output = true;
        }
        String tid = "p1.1";
        String Class = "hiddenRow";
        String style;
        if (n == 0){
            tid = String.format("pt%s.%s", cid +1, _tid+1);
            Class = "hiddenRow";
        } else {
            tid = String.format("ft%s.%s", cid +1, _tid+1);
            Class = "none";
        }
        if (n==2){
            style = "errorCase";
        } else {
            if (n==1){
                style = "failCase";
            } else {
                style = "none";
            }
        }
        String name = t;
        String desc = name;
        String tmpl;
//        if (has_output){
//            tmpl = REPORT_TEST_WITH_OUTPUT_TMPL;
//        } else {
//            tmpl = REPORT_TEST_NO_OUTPUT_TMPL;
//        }
        StringBuilder output = new StringBuilder();
        StringBuilder noutput = new StringBuilder();
        for (int i=0; i < o.length();i++){
//            output.append(o.getString(i)+"\n");
            String line = o.getString(i).trim();
            if (line.contains(".png")){
                String image = "screenshot/"+line.split(":i")[1].trim();
                line = String.format("<img src=\"%s\" alt=\"screen_shot\" height=\"320\" width=\"170\"></img>\n", image);
            }
            noutput.append(line).append("\n");
        }
        output.append(e);
//        StringBuilder noutput = new StringBuilder();
        // 插入截图
//        String[] outputLst = output.toString().split("\n");
//        for (String line: outputLst){
//            if (line.contains(".png")){
//                String image = "screenshot/"+line.split(":i")[1].trim();
//                line = String.format("<img src=\"%s\" alt=\"screen_shot\" height=\"352\" width=\"200\"></img>\n" +
//                        "        <a href=\"%s\" target=\"_blank\">点击查看原图</a>", image, image);
//            }
//            noutput.append(line).append('\n');

//        }
        JSONObject STATUS = new JSONObject("{\"0\":\"通过\", \"1\":\"失败\", \"2\":\"错误\"}");
        String script = String.format(REPORT_TEST_OUTPUT_TMPL, tid.substring(2), noutput);
        String row = tmpl(has_output, tid, Class, style, desc, script, STATUS.getString(n+""));
        rows.add(row);

    }

    public static String tmpl(boolean has_output, String tid, String Class, String style, String desc, String script, String status){
        String REPORT_TEST_WITH_OUTPUT_TMPL = "<tr id='"+tid+"' class='"+Class+"'>\n" +
                "    <td class='"+style+"'><div class='testcase'>"+desc+"</div></td>\n" +
                "    <td colspan='6' align='center'>\n" +
                "\n" +
                "    <!--css div popup start-->\n" +
                "    <a class=\"popup_link\" onfocus='this.blur();' href=\"javascript:showTestDetail('div_"+tid+"')\" >\n" +
                "        "+status+"</a>\n" +
                "\n" +
                "    <div id='div_"+tid+"' class=\"popup_window\">\n" +
                "        <div style='text-align: right; color:red;cursor:pointer'>\n" +
                "        <a onfocus='this.blur();' onclick=\"document.getElementById('div_"+tid+"').style.display = 'none' \" >\n" +
                "           [x]</a>\n" +
                "        </div>\n" +
                "        <pre>\n" +
                "        "+script+"\n" +
                "        </pre>\n" +
                "        <a onfocus='this.blur();' onclick=\"document.getElementById('div_"+tid+"').style.display = 'none' \" >\n" +
                "           [x]</a>\n" +
                "    </div>\n" +
                "    <!--css div popup end-->\n" +
                "\n" +
                "    </td>\n" +
                "</tr>\n";

        String REPORT_TEST_NO_OUTPUT_TMPL = "<tr id='"+tid+"' class='"+Class+"'>\n" +
                "    <td class='"+style+"'><div class='testcase'>"+desc+"</div></td>\n" +
                "    <td colspan='5' align='center'>"+status+"</td>\n" +
                "</tr>\n";
        if (has_output){
            return REPORT_TEST_WITH_OUTPUT_TMPL;
        } else {
            return REPORT_TEST_NO_OUTPUT_TMPL;
        }
    }

    public static String _generate_ending(){
        return ENDING_TMPL;
    }


}
