*如果觉得不错，请 watch fork star 三连~~~*
# Testool 
[![GitHub stars](https://img.shields.io/github/stars/yanglikai0806/testool.svg)](https://github.com/yanglikai0806/testool/stargazers) [![GitHub forks](https://img.shields.io/github/forks/yanglikai0806/testool.svg)](https://github.com/yanglikai0806/testool/forkgazers) [![GitHub release](https://img.shields.io/github/release/yanglikai0806/testool.svg)](https://github.com/yanglikai0806/testool/releases)
> Testool是为Android设备打造的更可靠，更易用，更高效的测试执行工具；  
> 可以配合测试平台实现分布式的测试执行能力，能更好的发挥Testool的作用。[测试平台demo](https://github.com/yanglikai0806/testool-web)
* [工具特色](#工具特色)
* [开发环境](#开发环境)
* [首次使用](#首次使用)
* [配置文件](#配置文件)
* [用例格式](#用例格式)
* [关键字说明](#关键字说明)

工具特色
---
>* Testool主要应用于Android端应用的UI自动化测试，目前已支持 手机、电视、音箱、手表等多种Android设备
>* Testool可直接安装在Android设备中，无需通过USB连接adb，所以Testool可以在完全**脱机**的情况下使用
>* Testool的测试用例是通过Json的形式来实现的, 消除自动化测试入门的语言门槛，让不懂编程的人员也可以完成自动化测试的搭建
>* Testool实现了进程**保持机制**来保障测试执行的稳定性，来保障低端设备上测试的稳定执行
>* Testool致力于做极致的测试执行工具，推崇极简化一键式的测试执行模式
>* Testool的最佳使用方式是结合测试平台实现分布式的测试执行体系

开发环境
---
>* Android Studio Bumblebee | 2021.1.1 Patch 3
>* Gradle 6.5
>* CMake 3.10
>* minSdkVersion 18
>* targetSdkVersion 28

首次使用
---
 >- 安装Testool后, 确保网络连接，初次使用会自动下载依赖并提示用户安装，根据提示安装即可。如果下载失败可在resource目录下将testassist.apk和testassist-test.apk文件手动安装
 >- 进入App权限管理页面，存储读写权限：始终允许，开启WLAN：始终允许，自启动：开启，获取手机信息:始终允许（最好将能给的权限全部赋予始终允许）
 >- 进入设备的电池管理功能（如有）->省电优化->锁屏后断开数据：从不；锁屏后清理内存：从不；应用智能省电：无限制
 >- 配置shell执行模式：支持三种shell执行模式，用户可根据自身情况选择其一。
 ```
    1. 设备具有root权限的情况下，为testool赋予root执行权限
    2. 连接PC后开启 tcpip 5555 端口（命令：adb tcpip 5555）
    3. 通过app_process命令执行shell。先通过adb shell进入设备的shell，再输入命令：nohup app_process -Djava.class.path=/sdcard/autotest/shellserver.dex /system/bin --nice-name=shellServer shellService.Main > /dev/null 2>&1 & ，最后执行exit退出shell。
    （确保/sdcard/autotest/shellserver.dex文件存在，若不存在需从resource目录下手动push到设备中）
 ```
>- shellserver.dex 实现原理：[shell-server](https://github.com/yanglikai0806/shell-server)
>- 可以通过 **adb shell logcat | grep KEVIN_DEBUG** 查看工具执行日志

配置文件
---
> * 为方便Testool应对不同的使用场景，很多功能给予配置文件进行设置
> * Testool安装完成后，首次启动会自动生成config.json文件（文件路径：/sdcard/autotest/config.json）

配置文件说明
```
{  
 "RETRY": 2,             # 重试次数，表示case失败后的重试次数
 "CASE_TAG": "",         # 用例标签, 在本地测试执行时可根据标签过滤case
 "LOG": "true",          # log开关，表示失败case是否抓取bugreprot
 "SCREENSHOT": "true",   # 截图开关，表示用例失败用例是否截图
 "SCREEN_RECORD":"false",# 录屏开关，表示用例失败时进行屏幕录制
 "ALARM_MSG": "false",   # 报警开关，表示是否发送报警短信
 "SCREEN_LOCK_PW": "",   # 锁屏密码，设置执行设备的解锁数字密码
 "CHECK_TYPE"：1,        # 支持三种检测级别，0：只检查稳定性问题crash & anr，忽略检查点check_point; 1: 根据check_point内容检查; 2: 综合0&1
 "GET_ELEMENT_BY": -1,   # 界面元素获取方式设置。 -1：自动判断获取方式；0：通过adb命令获取；1：通过UIautomator获取；2：通过Accessibility获取
 "POST_RESULT": "true",  # 数据上传开关，关闭后测试数据保存在本地不进行上传 
 "MUTE":"false",         # 测试执行中是否自动静音
 "MP42GIF":"false",      # 是否将测试视频转为gif
 "POP_WINDOW_LIST":[     # 测试过程中自动点击的弹窗列表
      "同意并继续",
      "允许",
      "始终允许"
  ],
"RECORD_CURRENT":"false",   # 是否记录电流消耗
"RECORD_MEMINFO":"false",   # 是否记录内存消耗
"SERVER_BASE_URL":"",       # 测试平台服务url
"TABLE":"test_cases",       # 测试用例存储表格
"TARGET_APP":"",            # 被测应用包名
"TEST_TAG":"",              # 测试标签，云端测试执行时只执行与之匹配的CASE_TAG的用例
"REMOTE_DEVICE_IP":"",      # 协同操作时，配置协同机ip
"DISABLE_CLEAR_RECENT_APP": "false", # 测试执行中是否禁止清理后台程序
"CLEAR_BUTTON_ID":"com.miui.home:id\/clearAnimView", # 配置清理手机后台时的空间ID，默认为小米手机，其他手机自行配置
}
```    

用例格式
---
> * Testool 的测试用例以Json的形式实现，通过关键字驱动测试。
> * 用例结构由 id, case, check_point, skip_condition 四个关键字组成的JSONObject
> * 通过JSONArray来组建测试集合，一个json文件是一个测试集合。

测试文件内容示例：（测试文件存储于/sdccard/autotest/testcases/路径下）    
```json
[
  {
    "id":123,
    "case":{
      "owner":"用例维护人",
      "case_desc":"此处输入用例描述",
      "case_tag":"此处输入用例标签",
      "step":[
        "执行步骤示例",
        { "launchApp":"com.android.settings" },
        { "text":"我的设备"}
      ],
      "wait_time":[1,2,4]
    },
    "check_point":{
      "text":["设备名称"]
    }
  }
]
```
关键字说明
---
## 1. "id"
一般为数据库存储时生成的id

## 2. "case"
case 是测试用例的主体，执行测试用例的核心部分。

* __"owner"__    
类型 _String_, 值 _用例维护人_，描述性字段（非必须）

* __"case_desc"__    
类型 _String_, 值 _用例说明_，描述性字段（非必须）

* __"case_tag"__    
类型 _String_, 值 _标签名称_，如 "monitor", 执行时会根据所选择的case_tag 过滤测试用例 （非必须）

* __"step"__    
类型 _JSONArray_, 值_用例步骤_, 如 [{"text":"设置"}], 表示执行的操作，目前支持的字段如下 
#### __UI基础操作__
> 通过shell input 命令实现基础UI操作能力
###### 点击坐标
```
{"click": [x, y]} 根据x，y坐标进行点击操作，支持相对值
示例: {"click": [0.5, 0.5]} 点击屏幕中心
```
###### 点击控件
```
{"text/id/content/class":"string", "nex":0, "index":0, "long":"true/10000", "checked":"true", "timeout":5000} 
根据界面元素属性点击界面控件
"nex" 表示查找上下关系控件例如:1表示下一个元素，-1表示上一个元素，
"index"当前界面有多个符合条件元素时，第几个元素，0均代表第一个元素。"nex","index"缺省默认均为0。
"long" 表示控件是否为长按操作，"true"默认长按1.5s，"数字"长按xxxms，
"checked"字段表示控件期望的checked状态，适用于切换开关、复选框等状态类控件, 
"timeout" 表示在超时时间内查找元素，默认 5000 ms。

示例：
{"text":"设置"} 根据控件text属性，点击text属性值为"设置" 的第1个元素
{"text":"设置", "index":2} 根据控件text属性，点击text属性值为"设置" 的第3个元素
{"text":"设置", "nex":1} 根据控件text属性，点击text属性值为"设置" 的元素的下一个元素
{"text":"设置", "long":"true"} 根据控件text属性，长按text属性值为"设置" 的第1个元素
{"text":"设置", "long":"2000"} 根据控件text属性，长按text属性值为"设置" 的第1个元素 2000 ms
{"text":"设置", "timeout":12000} 根据控件text属性，在12秒内循环查找
{"id":"android:id/button"} 根据控件id属性，点击id属性值为"android:id/button" 的第一个元素
{"id":"android:id/button", "checked":"true"} 根据控件id属性，查找id属性值为"android:id/button" 的第一个元素，如果 checked属性为false则点击，为true则不点击
```
###### 界面滑动
```
{"swipe":[xs, ys, xe, ye, duration]|"string"} 
xs, ys 滑动的起始点  xe, ye滑动结束点 ；
duration 为滑动时长ms，控制滑动快慢， 缺省默认500ms。
支持快捷滑动 如 {"swipe":"left|right|up|down"}

示例：
{"swipe":[0.5, 0.3, 0.5, 0.8]} 从一个点滑动到另一个点
{"swipe":[0.5, 0.3, 0.5, 0.8, 2000]} 从一个点滑动到另一个点, 在2秒中内
{"swipe":"up"} 向上滑动
```
###### 通知栏操作
```
{"notification":""} 下滑打开通知栏,支持参数"left/right"。

示例：
{"notification":"right"} 打开右侧通知栏
```
###### 清理后台程序
```
{"clearRecentApp":""} 清理后台应用，无参数，需修改配置文件中CLEAR_BUTTON_ID的值，或修改源码 share-> common -> clearRecentApp() 方法实现
```
#### __UI复杂操作__
> 通过反射UiAutomator实现复杂操作执行

###### 双指缩放
```
{"uiautomator":{"method":"pinchOpen/pinchClose", "args":[{"id":"xxxx"}, 1.0, 500]}} 
通过双指进行缩放操作，
args：[可缩放元素的属性, 比例percent(缺省值1.0), 速度speed(缺省值500)]

示例：
{"uiautomator":{"method":"pinchOpen", "args":[{"id":"android:id/camera"}, 0.8, 500]}} 将控件放大80%, 速度 500像素/秒
```
###### 滚动操作
```
{"uiautomator":{"method":"scrollLeft/scrollRight/scrollUp/scrollDown", "args":[{"id":"xxxx"}, 1.0, 500]}} 
滚动目标控件，
args：[可定位元素的属性, 滚动比例percent, 滚动速度speed]

示例：
{"uiautomator":{"method":"scrollLeft", "args":[{"id":"xxx", "index":0}, 0.8, 500]}} 将控件向左滚动80%, 速度 500像素/秒
```
###### 拖动操作
```
{"uiautomator":{"method":"drag", "args":[{"id":"xxxx"}/[x,y], {"id":"xxx"}/[x,y], 500]}} 
拖动控件，
args：[被拖动元素的属性或坐标,拖动的目标元素或坐标,拖动速度speed]

示例：
{"uiautomator":{"method":"drag", "args":[{"id":"android:id/camera"}, [0.5,0.5], 500]}} 将控件拖动到屏幕中央, 速度 500像素/秒
```
###### fling操作
```
{"uiautomator":{"method":"flingLeft/flingRight/flingUp/flingDown", "args":[{"id":"xxxx"}, 5000]}} 
fling操作，
args：[元素的属性,速度speed]

示例：
{"uiautomator":{"method":"flingLeft", "args":[{"id":"xxx"},  5000]}} 将控件拖动到屏幕中央, 速度 500像素/秒
```
###### 输入文本
```
{"uiautomator":{"method":"setText", "args":[{"id":"xxxx"}, "text文本", "a"]}} 
输入文本，
args：[元素的属性,输入文本内容, 输入方式："a" 为追加]

示例：
{"uiautomator":{"method":"setText", "args":[{"id":"xxx"},  "今天天气"]}} 在控件中输入"今天天气"
```
###### 双指手势
```
{"uiautomator":{"method":"twoPointerGesture", "args":[{"id":"xxxx"}, [500, 500], [800, 500], [500, 800], [800, 800]]}} 
args：[元素的属性,point1起始点, point2起始点, point1结束点, point2结束点]
```
###### 多点手势
```
{"uiautomator":{"method":"multiPointerGesture", "args":[{"id":"xxxxx"}, [530, 1484], [530, 2049], [832, 2049]]}}
```
###### toast获取
```
{"uiautomator":{"method":"lastToast"} 获取20s内最近的toast内容，只支持Android实现的toast(不支持前端技术实现的toast)，并输出到/sdcard/toast.txt中，可以在 check_point/check_add 中通过"toast"关键字进行检测。
```
#### __设备操作__
> 通过Android方法实现/shell 命令实现
```
{"activity":"string", "mode":"restart"} "mode"参数可指定启动方式，restart表示会杀掉已启动的应用进程后,重新启动。实现方式为adb命令。
{"launchApp":"string"} 支持activity启动，支持package name 启动应用，Android方法实现。
{"kill":"string"} 根据应用package name 结束应用进程。
{"clear":"string"} 根据应用package name 清除应用数据。
{"install":"url"} 根据url安装应用，url为安装包下载地址
{"uninstall":"string"} 根据应用 package name 卸载应用。
{"lock":""} 无参数，锁屏。
{"unlock":"string"} 根据数字密码解锁屏幕，参数为空则执行上滑解锁。
{"press":"string"/int} 根据参数执行按键操作，支持：home, recent, back, power, keycode值
{"wait":int} 等待，单位 秒。
{"shell":"string"} 执行shell命令。
{"wifi":"on/off"} on/off 开关wifi。
{"video_record": int} 开启录屏，参数为int类型，表示最长录屏时间
{"audio_record": int} 开启录音，参数为int类型，表示最长录制时间
```
#### __图像识别__
> 基于openCv实现
```
{"image":"image_id/image_tag", "bounds":"[0,0][100,100]", "index":1, "limit":0.98, "similarity":0.98, "method":"sift"} 
image_id 为图像库中的图像id,image_tag 为图像库中图像tag名,bounds限制检查范围，缺省为当前界面范围；"index" 表示在界面内有多个匹配目标时，指定特定目标，缺省则默认最后一个匹配的目标, limit 可缺省，表示匹配精度0~1，similarity 可缺省，表示匹配相似度，判断颜色等 0~1，method 表示特征匹配所用算法，目前支持sift，template, contour算法。
```

#### __网络请求__
```
{"post":{"url":"xxxxxx", "data":{}}} 发送post请求，data字段为参数
{"get":{"url":"xxxxx"}} 发送get请求
```

#### __逻辑实现__
```
{"if": {}} 执行过程判断，参数与check_point 用法一致。通过增加"true":[],"false":[] 执行相应操作用法同"step"字段
{"check_point":{}} 用法同check_point, 用于重写(覆盖)原检测点
{"check_add":{}} 用法同check_point, 根据条件增加检测点。
{"$var":{"txt/img":{}} 获取界面内容设置为参数，用于对比检查，配合check_point中 "$var"配合使用，例如 {"$var":{"txt":{"text":"换个话题","nex":-4}}}
{"loop":10, "break":{}, "do":[] }  loop为最大循环次数， break为循环截止条件与check_point用法一致， "do"为循环中执行的操作与step字段用法一致 
```
## 3. check_point
> 对测试执行后的结果进行断言, 支持的断言方法如下：

#### __界面内容检查__

###### 界面元素检查
> 通过对window_dump.xml文件进行内容匹配来进行判断，所以文件里的属性皆可用text字段进行判断
```
{"text":[]} 检测当前界面**(xml布局文件)**是否存在文本内容，list 元素之间为 _与_ 的关系，元素中 "|" 分割 为 _或_ 的关系
示例：
{"text":["今天天气|空气","度"]}
```
###### activity检查
```
{"activity":"string"} 检查当前activity,元素中 "|" 分割 为 _或_ 的关系
```

###### toast检查
```
{"toast":"string"} 检查toast内容是否包含元素
```

###### 检查某个元素的状态属性
```
{"status":{"s_text":"xxx","nex":0, "index":1, "checked":"false"}} 检查某个元素的状态，"s_text", "s_id", "s_content", "nex", "index" 通过这几个元素定位要判断的元素，然后判断要检查的属性及其预期的值  
示例：
{"status":{"s_text":"开关","s_id":"id/button", "nex":0, "index":1, "checked":"false"}} 表示定位id为id/button, text为"开关" 的第二个元素，"check"属性是否为"false"
```
###### 图像检查
```
{"image":{"src":"图像id/图像tag","resource-id/text/content/class":"xxxxxx", "nex":0, "index":0}} 识别src图像是否存在，支持根据 "resource-id/text/content/class","nex", "index" 对图像进行截取/通过"bounds"限制范围， limit 可缺省，表示匹配精度，similarity 可缺省，表示匹配相似度，判断颜色等
{"image":{"text":"String","resource-id/text/content/class":"xxxxxx", "nex":0, "index":0}} 识别src图像中是否存在文本内容，支持根据 "resource-id/text/content/class","nex", "index" 对图像进行截取/通过"bounds"限制范围
{"video":{"src":"图像id/图像tag","gap": 500, "bounds":"[][]"}} 通过视频识别src图像是否存在，"gap"控制视频截图时间，单位ms，缺省默认500ms
{"video":{"text":"string","gap": 500, "bounds":"[][]","language":"chi_sim"}} 通过视频识别text文本是否存在，"gap"控制视频截图时间，单位ms，缺省默认500ms，"language" 默认为中文
{"bounds":"[0,0][500,500]"} 用于对image，ocr，video 字段所判断的图片范围进行限定，以提高识别准确性，支持相对值例如：{"bounds":"[0.2, 0.5][0.6,0.8]"}
```

#### 其他检查项
###### 检查activity
```
{"activity":"string"} 检查当前activity,元素中 "|" 分割 为 或 的关系
```
###### 检查文件个数变化
```
{"delta":{"path": "your/folder/path/", "file\_re": "文件匹配的正则表达式", "cbt":0, "diff": 1}} 检测某路径下的文件增减情况 "cbt" 为 "count before test"，值为固定数0，执行中会根据正则自动赋值。
示例：
{"delta":{"path": "/sdcard/DCIM/Camera", "file_re": "IMG_\\d{8}_\\d{6}\\.jpg", "cbt":0, "diff": 1}} 表示测试后相机目录下新增1个jpg文件
```
###### 检查shell结果
```
{"shell":"string"|{"cmd":"your shell command", "result":"your expext result", "mode":""}} 检测该指令得到的结果是否符合预期，如果只有第一个string则只校验shell结果是否为空; 严格写法： {"cmd":"", "result":"", "mode":""} cmd指定执行的shell指令，result指定预期的结果，mode指定判断模式支持三种： ==（相等）,!=（不等）, contain(包含，默认缺省值) 
```
###### 检查logcat
```
{"logcat":"string"} 检测logcat中是否存在目标log
```
###### 检查网络请求
```
{"response":{}/string} 对 post/get 请求的结果进行判断，判断规则为包含预期字段或文本
```

#### 断言逻辑
###### 或
```
 {"or":"true"} 多个检查项的结果取或
```
###### 反
```
 {"reverse":"true"} 最终结果取反
```
#### 断言后的操作
```
{"teardown":[]} 执行消除测试影响的步骤，使用方法与 case 中的 "step" 字段一致
{"true":[]} 检测结果为true时，执行相关操作，使用方法与 case 中的 "step" 字段一致
{"false":[]} 检测结果为false时，执行相关操作，使用方法与 case 中的 "step" 字段一致
```

## 4. skip_condition
> 用法继承了check\_point 的用法，check\_point的字段都是支持的。   

```
{"scope":"all/single"} 表示跳过条件的影响范围，"all" 表示跳过条件成立时，json文件内当前case后面的所有case都会跳过，"single" 表示只跳过当前case
{"app": {"pkg": "com.android.camera", "version_name": "3.0", "version_code": [100, 300]}} 判断 app的version name 或 version code 是否符合条件, app不存在时执行跳过
{"sim_card":"true/false"} 判断设备是否有sim卡安装
{"nfc":"true/false" } 判断是否支持nfc功能
{"dev_white_lst": []} 设备白名单，参数为设备代号，例如：{"dev_white_lst": ["xxx"]}
{"dev_black_lst": []} 设备黑名单，参数为设备代号，例如：{"dev_white_lst": ["xxx"]}
```
使用方法
---
* 将写好的testcase.json文件存储到手机跟目录下/autotest/testcases/路径下  
* 在工具左侧导航栏里选择“重新导入”，用例会显示在主界面，点击每个item会显示 用例详情。   
* 选中要执行的用例集合 点击 “测试任务” 从弹窗页面中选择相应的测试配置参数，点击“执行本地测试”后，测试开始执行
* 测试完成后 点击导航栏 “日志报告” 可以查看测试报告
* 新建测试用例:

  <img src="https://raw.githubusercontent.com/yanglikai0806/testool/master/resource/create_case.gif" width="150" height="285" alt="展示"/>
* 执行测试任务:

  <img src="https://raw.githubusercontent.com/yanglikai0806/testool/master/resource/start_test.gif" width="150" height="285" alt="展示"/>
* 录制测试用例:

  <img src="https://raw.githubusercontent.com/yanglikai0806/testool/master/resource/record_case.gif" width="150" height="285" alt="展示"/>


 加入交流群(群号：439269565)
 ---
  <img src="https://github.com/yanglikai0806/testool/blob/master/resource/testool%E7%BE%A4%E4%BA%8C%E7%BB%B4%E7%A0%81.png" width="230" height="300" alt="展示"/>


Thank to
---
* [https://github.com/xiaocong/android-uiautomator-server](https://github.com/xiaocong/android-uiautomator-server) 
* [https://github.com/openatx/uiautomator2](https://github.com/openatx/uiautomator2)
* [https://github.com/gtf35/app_process-shell-use](https://github.com/gtf35/app_process-shell-use)
* [https://github.com/alipay/SoloPi](https://github.com/alipay/SoloPi)
