package online.smyhw.mc.localnet;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import online.smyhw.localnet.lib.CommandFJ;
import online.smyhw.localnet.lib.Json;
import online.smyhw.localnet.lib.Exception.TCP_LK_Exception;

import java.io.File;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.*;


public class smyhw extends JavaPlugin implements Listener 
{
	public static localnet_TCP ltcp;
	public static Socket s;
	public static Logger loger;
	public static FileConfiguration configer;
	public static String ID;
	@Override
    public void onEnable() 
	{
		getLogger().info("localnet_MC开始加载");
		getLogger().info("正在加载环境...");
		loger=getLogger();
		configer = getConfig();
		if(!(new File("./plugins/localnet_MC/config.yml")).exists() && !configer.getBoolean("ForceExist")) 
		{
			this.saveDefaultConfig();
			getLogger().warning("localnet_MC没有检测到配置文件，将创建默认配置文件，请修改默认配置文件后重载本插件或重启服务器....");
			getLogger().info("localnet_MC加载完成");
			return;
		}
		getLogger().info("正在注册监听器...");
		Bukkit.getPluginManager().registerEvents(this,this);
		conn();
		getLogger().info("localnet_MC加载完成");
    }

	@Override
    public void onDisable() 
	{
		getLogger().info("localnet_MC卸载");
    }
    
	public static void conn()
	{
		loger.info("正在读取配置文件...");
		String IP = configer.getString("IP");
		int port = configer.getInt("port");
		ID = configer.getString("ID");
		loger.info("IP="+IP+";端口="+port+";ID="+ID);
		loger.info("正在尝试连接到localnet...");
		try 
		{
			 s = new Socket(IP,port);
		} 
		catch (Exception e) 
		{
			loger.warning("警告!连接到localnet出错!详情如下:");
			e.printStackTrace();
			loger.warning("稍后将再次尝试连接!");
		}
		loger.warning("正在创建连接实例...");
		ltcp = new localnet_TCP(s);
	}
	
	@EventHandler
	public void chat(AsyncPlayerChatEvent e)
    {
		new sss("["+e.getPlayer().getName()+"]:"+e.getMessage());
    }
}

class localnet_TCP extends online.smyhw.localnet.network.Client_sl
{
	public localnet_TCP(Socket s) 
	{
		super("localnetTCP",new ArrayList() {{this.add(s);this.add(2);this.add(smyhw.ID);}});
	}	
	public void CLmsg(String msg)
	{
		smyhw.loger.info("接受到信息:"+msg);
		HashMap<String,String> message  = Json.Parse(msg);
		if(message==null) {smyhw.loger.warning("信息解码失败");return;}
		if(message.get("type").equals("auth")) {smyhw.loger.info("连接到localnet服务器<"+message.get("ID")+">");return;}
		if(message.get("message")==null) {smyhw.loger.info("没有找到消息节点");return;}
		String text = message.get("message");
		String[] temp = text.split(":");
		if(temp.length>=2 && temp[1].startsWith("!!"))//判断是否为指令消息
		{
			String temp2 = temp[1].substring(2);
			switch(CommandFJ.fj(temp2, 0))
			{
			case"st":
			case"status":
			{
				this.sendMsg("\n["+smyhw.ID+"]服务器状态\n状态:在线");
				break;
			}
			case"pl":
			case"PlayerList":
			{
				String re="\n["+smyhw.ID+"]在线列表:";
				Collection<? extends Player> Players = Bukkit.getOnlinePlayers();
				for(Player p :Players)
				{
				      re=re+"\n"+p.getName();
				}
				this.sendMsg(re);
				break;
			}
			case"help":
			{
				this.sendMsg("localnet&MC 指令列表\n"
						+ "!!st 查看服务器状态\n"
						+ "!!pl 查看玩家列表\n"
						+ "!!help 查看该列表");
			}
			default:
			{
				this.sendMsg("未知的服务器信息指令,使用!!help列出命令列表");
				break;
			}
			}
		}
		else
		{
			text="§2[§a"+message.get("From")+"§2]§r"+text;
			Bukkit.broadcastMessage(text);
		}
	}
	

	
	public void Serr_u(TCP_LK_Exception e)
	{
		smyhw.loger.warning("与localnet的连接出错,5s后重试!{"+e.getMessage()+"}");
		try 
		{
			Thread.sleep(5000);
		} 
		catch (InterruptedException ee) 
		{
			smyhw.loger.warning("延迟出错!");
			e.printStackTrace();
		}
		smyhw.conn();
	}
	
}

//委托类，负责分线程发生聊天信息
class sss extends Thread
{
	String msg;
	sss(String msg)
	{
		this.msg=msg;
		this.start();
	}
	public void run()
	{
		smyhw.ltcp.sendMsg(this.msg);
	}
}
