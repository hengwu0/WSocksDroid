WSocksDroid
---
WSocksDroid是基于SocksDroid和WSocks(Java)开发的安卓客户端。现已经实现WSocks桌面版的所有功能。如果出现任何问题，欢迎提交issue。

测试账户
----
为了测试App适配情况，现开放一台服务器作为测试主机。

* Server IP: 154.85.14.132
* Server Port: 1888
* 勾选 Username & Password Authentication
* Username: test
* Password: thisistest

> Apk地址：https://github.com/Wooyme/WSocksDroid/releases/tag/1.0.0

DNS
---
WSocksDroid依赖pdnsd和内置的TCP版DNS服务端(伪)。由于内置DNS的查询流程与正常DNS不同(包装成WSocks指令)，所以查询速度比较慢。往往会导致初次访问网站速度变慢，但因为pdnsd会做本地缓存，所以之后的访问速度会恢复正常。

**请确保DNS一栏中，DNS Server对应127.0.0.1，DNS PORT(TCP)对应5553。**

Routing
---
SocksDroid内置一个非大陆IP列表，用户可以使用这个列表选择性通过VPN，由于内置DNS，该功能可能无效。

IPv6 & UDP Forwarding
----
由于WSocks本身并未支持这两项功能，所以请不要勾选IPv6和UDP Forwarding，以免出现未知的错误


License
---
This project is licensed under GNU General Public License Version 3 or later.
