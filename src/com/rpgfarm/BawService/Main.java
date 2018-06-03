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

    System.out.println("[Baw Service] 보안을 위하여 다음 아이피의 요청만 받습니다: " + this.ip);
    if (!this.ver.equals(this.getDescription().getVersion()))
    {
      System.out.println("[Baw Service Updater] Baw Service API 업데이트 버전 발견! 업데이트전 반드시 서버를 백업하고 업데이트하세요.");
      System.out.println("[Baw Service Updater] Baw Service API는 되도록 최신 버전을 유지할 수 있도록 해주세요.");
      System.out.println("[Baw Service Updater] 현재 버전: " + this.getDescription().getVersion());
      System.out.println("[Baw Service Updater] 새로운 버전: " + this.ver);
    }
    System.out.println("[Baw Service] Baw Service API 플러그인 콘피그 초기화중");
    config = getConfig();
    config.addDefault("lastcommand", "BawServiceCommand");
    config.addDefault("setting.id", "BawServiceID");
    config.addDefault("setting.api-key", "BawServiceAPI_KEY");
    config.addDefault("setting.port", Integer.valueOf(3203));
    config.options().copyDefaults(true);
    saveConfig();
    saveDefaultConfig();
    System.out.println("[Baw Service] Baw Service API v" + this.getDescription().getVersion() + "가 활성화중입니다. 환영합니다. " + config.getString("setting.id") + "님");
    System.out.println("[Baw Service] 현재 활성화중인 Baw Service API는 Socket 사용 버전입니다.");
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
              System.out.println("[Baw Service] 잘못된 IP의 Socket 연결 시도이므로 무시합니다.");
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
          System.out.println("[Baw Service] 잘못된 IP의 Socket 연결 시도이므로 무시합니다.");
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
        writer.println(stringBuffer.toString() + "[" + frmtdDate + "] " + "Baw Service API "+this.getDescription().getVersion()+" 원격 명령어 실행: " + fla);
        writer.close();
      }
      catch (FileNotFoundException localFileNotFoundException) {}catch (UnsupportedEncodingException localUnsupportedEncodingException) {}
      return;
    }
    catch (IOException localIOException1) {}
  }
}
