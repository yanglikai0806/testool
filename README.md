Testool 介绍
---
[![GitHub stars](https://img.shields.io/github/stars/yanglikai0806/testool.svg)](https://github.com/yanglikai0806/testool/stargazers)
[![GitHub forks](https://img.shields.io/github/forks/yanglikai0806/testool.svg)](https://github.com/yanglikai0806/testool/forkgazers)
[![GitHub release](https://img.shields.io/github/release/yanglikai0806/testool.svg)](https://github.com/yanglikai0806/testool/releases)
>* Testool主要适用于Android端应用的UI自动化测试
>* Testool可直接安装在Android设备中，无需通过USB连接adb，所以Testool可以在完全**脱机**的情况下使用
>* Testool的测试用例实现是通过Json的形式来实现的

开发环境
---
* Android Studio Bumblebee | 2021.1.1 Patch 3
* Gradle 6.5
* CMake 3.10
* minSdkVersion 18
* targetSdkVersion 28

首次使用
---
  - 安装Testool后, 确保网络连接，初次使用会自动下载依赖并提示用户安装，根据提示安装即可。
  - 进入App权限管理页面，将开启WLAN：始终允许，自启动：开启，获取手机信息:始终允许（最好将能给的权限全部赋予始终允许）
  - 进入设备的电池管理功能（如有）->省电优化->锁屏后断开数据：从不；锁屏后清理内存：从不；应用智能省电：无限制
  - 配置shell执行模式

**[Installation](#用例格式)**

配置文件
---
> * 为方便Testool应对不同的使用场景，很多功能给予配置文件进行设置
> * Testool安装完成后，首次启动会自动生成config.json文件（文件路径：/sdcard/autotest/config.json）

配置文件说明
```text
{  
 "RETRY": 2,             # 重试次数，表示case失败后的重试次数
 "CASE_TAG": "monitor",  # 用例标签, 如例，代表执行case_tag为“monitor”的测试case 
 "LOG": "true",          # log开关，表示失败case是否抓取bugreprot
 "SCREENSHOT": "true",   # 截图开关，表示失败用例是否截图
 "ALARM_MSG": "false",   # 报警开关，表示是否发送报警短信
 "SCREEN_LOCK_PW": "",   # 锁屏密码，表示执行设备的解锁密码
 "OFFLINE": "false",   
 "CHECK_TYPE"：1,        # 设置三种检测级别，0：检查稳定性问题fc & anr，1 : 界面检查元素 , 2： 0 & 1
 "POST_RESULT": "true",  # 数据上传开关 
}
```    

用例格式
---
测试case以json文件的格式执行： 如：文件名 testDemon.json 文件，用例主要包括四个部分    
id,  case,  check_point,  skip_condition 

    
```json
[{"id":"set_alarm",
 "case":{  
    "app": "时钟", 
    "action": "设置闹钟", 
    "step":[{"text":"闹钟"}], 
    "wait_time":[6]},
"check_point":{ 
    "text":"08:00", 
    "resource-id":"android:id/checkbox",
    "status": {"index": 2, "checked": "false"}, 
    "activity":"", 
    "img": {"text": "text", "language": "chi_sim"}, 
    "nd":"" 
  },
"skip_condition": {
    "scope": "single",
    "app": {"pkg":"com.xxx.xxx", "version_code":[0, 30400500]} 
    }
  }]
```
### 1. "id"
主要标识测试用例，根据测试用例功能特点命名即可，主要用于报告展示，方便查找定位

### 2. "case"
case 是测试用例的主体，执行测试用例的核心部分。

* __"app"__    
类型 _String_，值 _app名称_，如“微信”。 会从config.json 中根据APP 配置的名称对应其package name。表示测试执行依赖此app，会判断其是否安装，未安装则跳过测试。

* __"case_tag"__    
类型 _String_, 值 _标签名称_，如 “monitor”。执行时会根据所选择的case_tag 过滤测试用例

* __"step"__    
类型 _list_, 值 _dict_, 如 [{"text":"天气"}], dict元素表示执行的操作，目前支持的字段如下    

```
{"text":"string", "nex":0, "index":0） 根据界面元素text属性点击界面控件,"nex" 表示查找上下关系控件例如:1表示下一个元素，-1表示上一个元素，"index"当前界面有多个符合条件元素时，第几个元素。0均代表当前元素。"nex","index"缺省默认均为0。
{"id"："string"} 根据界面元素resource-id属性点击界面控件，"nex","index"用法同上。
{"content":"string"} 根据界面元素content-desc属性点击界面控件，"nex","index"用法同上。
{"class":"string"} 根据界面元素class属性点击界面控件，"nex","index"用法同上。
{"click": [x, y]} 根据x，y坐标点击操作。
{"swipe":[xs, ys, xe, ye, step]} 根据起始点 xs, ys 滑动界面到 xe，ye；step 为滑动步数，控制滑动快慢。
{"drag":[{元素1},{元素2}]}，根据元素位置拖动例如{"drag":[{"text":"设置"},{"text":"相册"}]}表示从元素"设置"拖动到元素"相册的位置"
{"activity":"string"/list} 支持string/list两种数据类型，list内多个activity 会随机启动一个，实现方式为adb命令。
{"launchApp":"string"} 支持activity启动，支持package name 启动应用，Android方法实现。
{"kill":"string"} 根据应用package name 结束应用进程。
{"uninstall":"string"} 根据应用 package name 卸载应用。
{"notification":""} 无参数，下滑打开通知栏。
{"lock":""} 无参数，锁屏。
{"unlock":"string"} 根据锁屏密码解锁屏幕，参数为空则执行上滑解锁。
{"press":"string"} 根据参数执行按键操作，支持：home, recent, back, power, AIkey
{"wait":int} 等待，单位 秒。
{"shell":"string"} 执行shell命令。
{"wifi":"string"} on/off 开关wifi。
{"if": {}} 执行过程判断，参数与check_point 用法一致。通过"true","false" 字段执行相应操作【参考check_point用法】
{"check_point":{}} 用法同check_point, 用于重写(覆盖)原检测点
{"check_add":{}} 用法同check_point, 根据条件增加检测点。

```
* 其他字段均为描述性字段可缺省

### 3. check_point
**check_point 对测试执行后的结果检测字段如下：**

* __"text"__  
```
    {"text":[]} / {"text":"string"}
```
检测当前界面**(xml布局文件)**是否存在文本属性（text 、content-desc、recource-id...），list 元素之间为 _与_ 的关系，元素中 "|" 分割 为 _或_ 的关系，如{"text":["今天天气|空气","度"]}

* __"resource-id__" / __"id"__  
```
    {"resource-id":"string"} 
```
是否存在某个控件id，与"text"实现方式相似，只支持string参数

* __"nd"__  
```
    {"nd": [ ]/ "string"} 
```

与 __"text"__ 用法一致，结果取反

* __"activity"__  
```
    {"activity":"string"} 
```
检查当前activity,元素中 "|" 分割 为 _或_ 的关系

* __"toast"__    
```
{"toast":"string"}
```
检查toast内容是否包含元素

* __"status"__    
{"status":{}} 检查某个元素的状态，"s_text", "s_id", "s_content", "nex", "index" 通过这几个元素定位要判断的元素，然后判断要检查的属性及其预期的值    
例如：   
```
    {"s_text":"开关", "s_id":"id/button", "nex":0, "index":1, "checked":"false“} 
    表示定位id为id/button, text属性为”开关“ 的第二个元素，"check"属性是否为”false“
```

* __"delta"__
{"delta":{"path": "you/folder/", "file\_re": "文件匹配的正则表达式", "cbt":0, "diff": 1}} 检测某路径下的文件增减情况 "cbt" 为 "count before test"    
例如：
```
    "delta":{"path": "/sdcard/DCIM/Camera", "file_re": "IMG_\\d{8}_\\d{6}\\.jpg", "cbt":0, "diff": 1} 
    表示测试后相机目录下新增1个jpg文件
```

* __"img"__  
```
    {"img":{"text":"string", "language":"chi_sim"}} 
```
通过ocr识别当前界面是否存在目标文本，"language" 可缺省，默认为中文简体"chi_sim", 可支持英文"eng".

* __"logcat"__
```
    {"logcat":"string"}
```
检测logcat中是否存在目标log。

* __"or"__
```
    {"or":"true"} 
```
"true" 
表示对以上判断结果取 _或_

* __"reverse"__
```
    {"reverse":"true"} 
```
"true" 
表示对以上判断结果取 _反_

**check_point 中的执行操作字段如下：**

* __"teardown"__
执行消除测试影响的步骤，使用方法与 case 中的 "step" 字段一致

* __"true"__
检测结果为true时，执行相关操作，使用方法与 case 中的 "step" 字段一致
* __"false"__
检测结果为false时，执行相关操作，使用方法与 case 中的 "step" 字段一致

### 4. skip_condition
skip\_condition 字段的用法继承了check\_point 的用法，check\_point的字段都是支持的。   

* __"scope"__ 
```
    "scope":"all/single"
```
表示跳过条件的影响范围，"all" 表示跳过条件成立时，json文件内当前case后面的所有case都会跳过，"single" 表示只跳过当前case

* __"app"__
```
    "app": {"pkg": "com.android.camera", "version_name": "3.0", "version_code": [100, 300]}
```
判断 app的version name 或 version code 是否符合条件
* __"sim_card"__  
```
    "sim_card":"true/false" 
```
判断设备是否有sim卡安装

* __"nfc"__   
```
     "nfc":"true/false" 
```
判断是否支持nfc功能

* __"dev_white_lst"__   
设备白名单，参数为设备代号，例如：
```
     "dev_white_lst": ["mido"]
```
* __"dev_black_lst"__   
设备黑名单，用法同上   

四. 使用方法
---
* 将写好的testDemo.json文件存储到手机跟目录下/autotest/testcases/路径下  
* 在工具左侧导航栏里选择“重新导入”，用例会显示在主界面，点击每个item会显示 用例详情。   
* 选中要执行的用例集合 点击 “开始” 测试开始执行，选择相应的测试环境，用例配置，目标应用，case_tag，等参数，点击确定即开始测试   
* 测试完成后 点击导航栏 “日志报告” 可以查看测试报告    

  <img src="https://raw.githubusercontent.com/yanglikai0806/testool/master/resource/0.gif" width="300" height="570" alt="展示"/>
  
 五. 加入交流群(群号：439269565)
 ---
  <img src="https://github.com/yanglikai0806/testool/blob/master/resource/testool%E7%BE%A4%E4%BA%8C%E7%BB%B4%E7%A0%81.png" width="230" height="300" alt="展示"/>


Thank to
---
* [https://github.com/openatx/android-uiautomator-server](https://github.com/openatx/android-uiautomator-server) 
* [https://github.com/cgutman/AdbLib](https://github.com/cgutman/AdbLib)
