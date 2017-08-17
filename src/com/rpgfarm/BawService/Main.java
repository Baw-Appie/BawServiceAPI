package com.rpgfarm.BawService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class Main
  extends JavaPlugin
  implements Listener, Runnable
{
  public static FileConfiguration config;
  public Thread serverThread;
  String ip;
  String ver;
  public static String latestCommand = "";
  public static int count = 0;
  
  public static String m(String message)
  {
    return ChatColor.translateAlternateColorCodes('&', message);
  }
  
  public void onEnable()
  {
	this.ip = getOpenStreamHTML("https://baws.kr/api/serveripcheck.php");
    this.ver = getOpenStreamHTML("https://baws.kr/api/versionchecker.php");

    System.out.println("[Baw Service] Baw Service API IP " + this.ip);
    if (!this.ver.equals(this.getDescription().getVersion()))
    {
      System.out.println("[Baw Service] Baw Service API 업데이트 버전 발견! 업데이트전 반드시 서버를 백업하고 업데이트하세요.");
      System.out.println("[Baw Service] 새로운 업데이트는 https://baws.kr/ 에서 진행할 수 있습니다.");
      System.out.println("[Baw Service] 현재 버전: " + this.getDescription().getVersion());
      System.out.println("[Baw Service] 새로운 버전: " + this.ver);
    }
    config = getConfig();
    config.addDefault("lastcommand", "BawServiceCommand");
    config.options().copyDefaults(true);
    saveConfig();
    saveDefaultConfig();
    config.addDefault("setting.id", "BawServiceID");
    config.addDefault("setting.api-key", "BawServiceAPI_KEY");
    config.addDefault("setting.port", Integer.valueOf(3203));
    saveConfig();
    Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA + "[" + ChatColor.GREEN + "Baw Service" + ChatColor.AQUA + "] Baw Service API v" + this.getDescription().getVersion() + "가 활성화중입니다. 환영합니다. " + config.getString("setting.id") + "님");
    final Main mg = this;
    this.serverThread = new Thread(new Runnable()
    {
      public void run()
      {
        ServerSocket server = null;
        try
        {
          server = new ServerSocket(Main.config.getInt("setting.port"));
          for (;;)
          {
            Socket client = server.accept();
            if (client.getInetAddress().getHostName().equals(Main.this.ip))
            {
              ThreadSocket echo = new ThreadSocket(client, mg);
              echo.start();
            }
            else
            {
              System.out.println("[Baw Service] " + client.getInetAddress().getHostName() +" 에서 잘못된 Baw Service API 명령어 패킷이 전송되었습니다.");
              System.out.println("[Baw Service] 서버의 취약점을 이용한 공격으로 추정됩니다. 이는 차단되었습니다.");
              System.out.println(ip);
              client.close();
            }
          }
        }
        catch (IOException e)
        {
          System.out.println("[Baw Service] Baw Service API에 오류가 있습니다.");
        }
      }
    });
    Bukkit.getPluginManager().registerEvents(this, this);
    this.serverThread.start();
  }
  
  public void onDisable()
  {
	System.out.println("[Baw Service] Baw Service API가 비활성화중입니다. 사용해주셔서 감사합니다.");
    this.serverThread.stop();
  }
  
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
  {
    if (!(sender instanceof Player)) {
      return false;
    }
    Player player = (Player)sender;
    if ((command.getName().equalsIgnoreCase("bawservice")) && (sender.isOp())) {
      if (args.length == 0)
      {
        player.sendMessage(m("/bawservice reload: 콘피그 리로드 (포트는 리로드되지 않습니다)"));
      }
      else if ((args[0].equalsIgnoreCase("reload")) && (sender.isOp()))
      {
        File cfile = new File(getDataFolder(), "config.yml");
        try
        {
          getConfig().load(cfile);
        }
        catch (FileNotFoundException e)
        {
          e.printStackTrace();
        }
        catch (IOException e)
        {
          e.printStackTrace();
        }
        catch (InvalidConfigurationException e)
        {
          e.printStackTrace();
        }
        player.sendMessage(m("&aconfig reload"));
    	System.out.println("[Baw Service] config reload");
      }
    }
    return false;
  }
  
  public void run()
  {
    ServerSocket server = null;
    try
    {
      server = new ServerSocket(config.getInt("setting.port"));
      for (;;)
      {
        Socket client = server.accept();
        if (client.getInetAddress().getHostName().equals(this.ip))
        {
          ThreadSocket echo = new ThreadSocket(client, this);
          echo.start();
        }
        else
        {
          client.close();
          System.out.println("[Baw Service] " + client.getInetAddress().getHostName() +" 에서 잘못된 Baw Service API 명령어 패킷이 전송되었습니다.");
          System.out.println("[Baw Service] 서버의 취약점을 이용한 공격으로 추정됩니다. 이는 차단되었습니다.");
          System.out.println(ip);
        }
      }
    }
    catch (IOException e)
    {
      System.out.println("[Baw Service] 인터넷 오류입니다.");
    }
  }
  
  public String getOpenStreamHTML(String urlToRead)
  {
      String result = "";
      try
      {
          URL url = new URL(urlToRead);
          InputStreamReader is = new InputStreamReader(url.openStream(), "UTF-8");
          int ch;
          while((ch = is.read()) != -1) 
              result = (new StringBuilder(String.valueOf(result))).append((char)ch).toString();
      }
      catch(MalformedURLException e)
      {
          e.printStackTrace();
      }
      catch(IOException e)
      {
          e.printStackTrace();
      }
      return result;
  }

  
  public void saver(String fla)
  {
    File f = new File("plugins\\BawService\\log.log");
    if (!f.exists())
    {
      new File("plugins\\BawService\\").mkdirs();
      try
      {
        f.createNewFile();
      }
      catch (IOException e)
      {
        e.printStackTrace();
      }
    }
    SimpleDateFormat dateFormat = new SimpleDateFormat("yy-MM-dd HH:mm");
    Date date = new Date();
    try
    {
      File file = new File("plugins\\BawService\\log.log");
      FileReader fileReader = new FileReader(file);
      BufferedReader in = new BufferedReader(
        new InputStreamReader(
        new FileInputStream("plugins\\BawService\\log.log"), "UTF8"));
      StringBuffer stringBuffer = new StringBuffer();
      String line;
      while ((line = in.readLine()) != null)
      {
        stringBuffer.append(line);
        stringBuffer.append("\n");
      }
      fileReader.close();
      String frmtdDate = dateFormat.format(date);
      try
      {
        PrintWriter writer = new PrintWriter("plugins\\BawService\\log.log", "UTF-8");
        writer.println(stringBuffer.toString() + "[" + frmtdDate + "] " + "Baw Service에서 원격으로 명령어 실행 요청을 전달받았습니다: " + fla);
        writer.close();
      }
      catch (FileNotFoundException localFileNotFoundException) {}catch (UnsupportedEncodingException localUnsupportedEncodingException) {}
      return;
    }
    catch (IOException localIOException1) {}
  }
}
