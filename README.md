# Android UI测试工具
# 一. 测试工具介绍
  1. 适用于Android app ui 自动化测试
  2. 不需连接usb，脱机执行测试用例
  3. 需安装 app-debug.apk, atx.apk, atx-androidTest.apk 三个应用并允许读写存储等权限
  4. 测试用例需按照指定数据结构实现方可执行
  
# 二. 使用简介：

  1. 测试case以json文件的格式执行：
  如：文件名 testDemon.json
  文件内容：
 
    [{"id":"testcase-1", 
    "case":{
     "case_tag": "monitor",
    "feature":"demo",
    "action": "demo",
    "app": "系统设置",
    "step":[{"press":"home"}, {"text":"设置"}],
    "wait_time":[1,2] },
    "check_point":{
    "text":"",
    "resource-id":"",
    "activity":"com.android.setting",
    "nd":""},
    "skip_condition":{
    }
    },
    {"id":"testcase-2", 
    "case":{
     "case_tag": "monitor",
    "feature":"demo",
    "action": "demo",
    "app": "系统设置",
    "step":[{"press":"home"}, {"text":"设置"}],
    "wait_time":[1,2] },
    "check_point":{
  
    "text":"",
    "resource-id":"",
    "activity":"com.android.setting",
    "nd":"",
    }
    }]
    
    
  2. 将写好的testDemon.json文件存储到手机跟目录下/autotest/testcases/路径下
  3. 在工具左侧导航栏里选择“重新导入”，用例会显示在主界面
  4. 选中要执行的用例集合 点击 “开始” 测试开始执行
  5. 测试完成后 点击导航栏 “日志报告” 可以查看测试报告
# 三. 功能介绍：
step 支持的操作方式可以从 MyIntentService.java 里的 execute_xa 方法里获得
check_point 支持的检测方法可以从 resultCheck 方法里查看
skipe_condition 为测试跳过条件，判断方法与check_point相似
具体功能实现以后补充～～～
