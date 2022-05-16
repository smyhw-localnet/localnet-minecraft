package online.smyhw.mc.localnet;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.java.JavaPlugin;

import online.smyhw.localnet.LN;
import online.smyhw.localnet.data.DataPack;
import online.smyhw.localnet.lib.*;
import online.smyhw.localnet.lib.Exception.Json_Parse_Exception;

import java.io.File;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import org.bukkit.*;


public class smyhw extends JavaPlugin implements Listener 
{
	public static JavaPlugin smyhw_;
	public static localnetTCP2 connection;
	public static ShowMsg SendToMcThread;
	public static Logger loger;
	public static FileConfiguration configer;
	public static String ID;
	@Override
    public void onEnable() 
	{
		getLogger().info("localnet_MC开始加载");
		getLogger().info("正在加载环境...");
	    Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Lag(), 100L, 1L);
	    smyhw_ = this;
		loger=getLogger();
		configer = getConfig();
		if(!(new File("./plugins/localnet_MC/config.yml")).exists() && !configer.getBoolean("ForceExist")) 
		{
			this.saveDefaultConfig();
			getLogger().warning("localnet_MC没有检测到配置文件，将创建默认配置文件，请修改默认配置文件后重载本插件或重启服务器....");
			return;
		}
		SendToMcThread = new ShowMsg();
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
    
	/**
	 * 连接到localnet
	 */
	public void conn()
	{
		Socket s = new Socket();
		String IP = configer.getString("IP");
		int port = configer.getInt("port");
		ID = configer.getString("ID");
		LN.ID = ID;
		loger.info("正在尝试连接到localnet..."+"(IP="+IP+";端口="+port+";ID="+ID+"}");
		try 
		{
			loger.info("建立TCP...");
			 s = new Socket(IP,port);
			loger.info("初始化协议...");
			connection = new online.smyhw.localnet.lib.localnetTCP2(s,this,this.getClass().getMethod("on_recv", String.class),this,this.getClass().getMethod("on_error",  String.class,Exception.class));
			connection.send_msg("{\"ID\":\""+ID+"\",\"type\":\"auth\"}");
		} 
		catch (Exception e) 
		{
			loger.warning("警告!连接到localnet出错!详情如下:");
			e.printStackTrace();
			loger.warning("稍后将再次尝试连接!");
		}
	}
	
	@EventHandler
	public void chat(AsyncPlayerChatEvent e)
    {
		new SendMsg("<"+e.getPlayer().getName()+"> "+e.getMessage());
    }
	
	
	
	//localnet相关

	/**
	 * 从localnet接收到消息时
	 * 注册到localnetTCP2
	 * @param re
	 */
	public void on_recv(String re)
	{
		smyhw.loger.info("接收到信息 -> "+re);
		HashMap<String, String> tmp1;
		try {
			tmp1 = Json.Parse(re);
		} catch (Json_Parse_Exception e) {
			smyhw.loger.warning("消息解码失败 -> "+re);
			e.printStackTrace();
			return;
		}
		String message = tmp1.get("message");
		if(message==null)
		{
			smyhw.loger.info("接收到不支持消息 -> "+re);
			return;
		}
		//指令
		if(message.endsWith("!!st") || message.endsWith("!!status"))
		{
			new SendMsg("\n节点ID: "+smyhw.ID+"\n状态: 在线\nTPS: "+Lag.getTPS());
		}
		if(message.endsWith("!!pl") || message.endsWith("!!player_list"))
		{				
			String reSend="["+smyhw.ID+"]\n=在线客户端=\n";
			Collection<? extends Player> Players = Bukkit.getOnlinePlayers();
			for(Player p :Players)
			{
				reSend=reSend+p.getName()+"\n";
			}
			reSend = reSend.substring(0, reSend.length()-1);
			new SendMsg(reSend);
		}
		//染色
		String[] tmp2 = message.split(" ");
		if(tmp2.length<3) {
			smyhw.loger.warning("染色处理失败 -> "+message);
			ShowMsg.msgList.add(message);
			return;
		}
		String tmp3=message.substring(tmp2[0].length()+tmp2[1].length()+2);
		tmp2[0] = tmp2[0].replaceAll("<", "§o§7<§6");
		tmp2[0] = tmp2[0].replaceAll(">", "§7>§r");
		tmp3 = tmp2[0]+tmp2[1]+" §o"+tmp3;
		ShowMsg.msgList.add(tmp3);
	}
	
	/**
	 * 当连接错误时...
	 * 注册到localnetTCP2
	 * @param msg
	 * @param e
	 */
	public void on_error(String msg,Exception e)
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
		this.conn();
	}
	//=============
}

//线程发送消息至localnet
class SendMsg extends Thread
{
	String msg;
	SendMsg(String msg)
	{
		this.msg=msg;
		this.start();
	}
	public void run()
	{
		smyhw.connection.send_msg("{"
				+ "\"type\":\"message\","
				+ "\"message\":\""+Json.Encoded( this.msg)+"\""
				+ "}");
	}
}

//线程发送消息至MC
//这是为了同步
class ShowMsg extends BukkitRunnable
{
	
	public static CopyOnWriteArrayList<String> msgList = new CopyOnWriteArrayList<String>();
	ShowMsg()
	{
		this.runTaskTimer(smyhw.smyhw_, 0, 5);
	}
	public void run()
	{
		if(msgList.isEmpty()) {return;}
		String msg = msgList.get(0);
		msgList.remove(0);
		Bukkit.broadcastMessage(msg);
	}
}

class Lag implements Runnable {
	public static int TICK_COUNT = 0;
	public static long[] TICKS = new long[600];
	public static long LAST_TICK = 0L;

	public static double getTPS() {
		return getTPS(100);
	}

	public static double getTPS(int ticks) {
		if (TICK_COUNT < ticks) {
			return 20.0D;
		}
		int target = (TICK_COUNT - 1 - ticks) % TICKS.length;
		long elapsed = System.currentTimeMillis() - TICKS[target];

		return ticks / (elapsed / 1000.0D);
	}

	public static long getElapsed(int tickID) {
		if (TICK_COUNT - tickID >= TICKS.length) {
		}

		long time = TICKS[(tickID % TICKS.length)];
		return System.currentTimeMillis() - time;
	}

	public void run() {
		TICKS[(TICK_COUNT % TICKS.length)] = System.currentTimeMillis();

		TICK_COUNT += 1;
	}
}