# localnet-minecraft
localnet的minecraft服务器对接插件

# 这是一个mc服务端的插件
支持bukkit系服务端  
往plugins文件夹里塞就成  
第一次启动会生成默认配置文件  
没有任何权限节点，没有任何指令
#
第一次启动会提示检测不到配置文件，并生成默认配置文件，请编辑默认配置文件后重载插件或重启服务器  
如果配置文件已经存在，插件在加载时却仍提示检测不到配置文件，请将配置文件中的条目"ForceExist: false"的值更改为true
#
如果发生失灵，尝试在游戏聊天频道内发送任意字符，如果连接的确出错，那么这会触发错误重连，这可以解决部分问题
