package online.smyhw.mc.localnet;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import online.smyhw.localnet.lib.CommandFJ;
import online.smyhw.localnet.lib.Exception.TCP_LK_Exception;

import java.io.File;
import java.net.Socket;
import java.util.Collection;
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
		new sss("*"+e.getPlayer().getName()+":"+e.getMessage());
    }
}

class localnet_TCP extends online.smyhw.localnet.lib.TCP_LK
{
	public localnet_TCP(Socket s) 
	{
		super(s, 2);
		this.Smsg("&"+smyhw.ID);
	}	
	public void CLmsg(String msg)
	{
		smyhw.loger.info("接受到信息:"+msg);
		if(msg.startsWith("&")) {smyhw.loger.info("接受到认证信息:"+msg);return;}
		if(msg.startsWith("*")) {msg=msg.substring(1);}
		String[] temp1 = msg.split(":");
		if( temp1.length>2 && temp1[2].startsWith("!!"))//判断是否为指令消息
		{
			msg=temp1[2].substring(2);
			switch(CommandFJ.fj(msg, 0))
			{
			case"st":
			case"status":
			{
				
//				String TPS = new String("%server_tps_1%");
//				TPS = PlaceholderAPI.setPlaceholders(null,TPS);
//				this.sendto("\nOurWorld["+smyhw.ID+"]服务器状态\n状态:在线\nTPS:"+TPS);
				this.sendto("\nOurWorld["+smyhw.ID+"]服务器状态\n状态:在线");
				break;
			}
			case"pl":
			case"PlayerList":
			{
				String re="\nOurWorld["+smyhw.ID+"]在线列表:";
				Collection<? extends Player> Players = Bukkit.getOnlinePlayers();
				for(Player p :Players)
				{
				      re=re+"\n"+p.getName();
				}
				this.sendto(re);
				break;
			}
			case"help":
			{
				this.sendto("localnet&MC 指令列表\n"
						+ "!!st 查看服务器状态\n"
						+ "!!pl 查看玩家列表\n"
						+ "!!help 查看该列表");
			}
			default:
			{
				this.sendto("owr 未知的OW指令,使用!!help列出命令列表");
				break;
			}
			}
		}
		else
		{
			msg="§2[§a"+temp1[0]+"§2]§r"+temp1[1];
			Bukkit.broadcastMessage(msg);
		}
	}
	
	public void sendto(String msg)
	{
		msg="*"+msg;
		this.Smsg(msg);
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
		smyhw.ltcp.sendto(this.msg);
	}
}
