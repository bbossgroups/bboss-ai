轻量级多模态java大模型智能体客户端

使用参考文档：https://esdoc.bbossgroups.com/#/bboss-ai
# 集成bboss ai
maven坐标
```xml
<dependency>
   <groupId>com.bbossgroups</groupId>
   <artifactId>bboss-ai</artifactId>
   <version>6.5.3</version>
</dependency>
```
gradle坐标
```groovy
implementation 'com.bbossgroups:bboss-ai:6.5.3'
```


# 版本构建方法

gradle clean publishToMavenLocal

需要通过gradle构建发布版本,gradle安装配置参考文档：

https://esdoc.bbossgroups.com/#/bboss-build
# bboss ai
基于 httpclient5、httpcore5以及reactor的多模态java大模型智能体客户端，支持同步调用和流式调用两种模式；

模型支持：兼容各种主流最新的LLM模型和多模态模型，包括LLM、计算视觉，音频/视频模型，快速实现智能问答、图片识别/生成、语音识别/生成以及视频识别/生成功能

模型平台：集成和适配Deepseek、Kimi、智谱、阿里百炼通义千问qwen、字节豆包火山引擎、Minimax、腾讯混元以及中国移动九天等国内主流Maas平台，通过简单的适配和扩展即可支持私有化模型平台

工具能力：支持工具和MCP服务发现和调用，提供MCP sse和streamable两种mcp 通讯协议，同时提供mcp server协议实现

多智能体协同：配合bboss graph提供的工作流和有限循环图，实现多智能体协同，快速构建多智能体系统



# 使用案例
## 多模态智能问答web demo

源码工程 https://gitee.com/bboss/bbootdemo

关键代码：

流式问答控制器 https://gitee.com/bboss/bbootdemo/blob/master/src/main/java/org/frameworkset/web/react/ReactorController.java

前端网页 https://gitee.com/bboss/bbootdemo/blob/master/WebRoot/chatBackuppressSession.html

## 后端多模态演示案例
源码工程：https://gitee.com/bboss/bboss-ai

关键代码：https://gitee.com/bboss/bboss-ai/blob/main/bboss-ai/src/test/java/org/frameworkset/spi/ai/StreamTest.java

# 联系我们

**技术交流群：21220580,166471282**

<img src="https://esdoc.bbossgroups.com/images/qrcode.jpg"  height="200" width="200"><img src="https://esdoc.bbossgroups.com/images/douyin.png"  height="200" width="200"><img src="https://esdoc.bbossgroups.com/images/wvidio.png"  height="200" width="200">


# 支持我们

如果您正在使用bboss，或是想支持我们继续开发，您可以通过如下方式支持我们：

1.Star并向您的朋友推荐或分享

[bboss elasticsearch client](https://gitee.com/bboss/bboss-elastic)🚀

[数据采集&流批一体化处理](https://gitee.com/bboss/bboss-elastic-tran)🚀

2.通过[爱发电 ](https://afdian.net/a/bbossgroups)直接捐赠，或者扫描下面二维码进行一次性捐款赞助，请作者喝一杯咖啡☕️

<img src="https://esdoc.bbossgroups.com/images/alipay.png"  height="200" width="200">

<img src="https://esdoc.bbossgroups.com/images/wchat.png"   height="200" width="200" />

非常感谢您对开源精神的支持！❤您的捐赠将用于bboss社区建设、QQ群年费、网站云服务器租赁费用。




# License

The BBoss Framework is released under version 2.0 of the [Apache License][].

[Apache License]: http://www.apache.org/licenses/LICENSE-2.0

# Star History

[![Star History Chart](https://api.star-history.com/svg?repos=bbossgroups/bboss-http&type=Date)](https://star-history.com/#bbossgroups/bboss-http&Date)
