package com.rpgfarm.BawService;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

public class ThreadSocket
  extends Thread
{
  protected Socket socket;
  private final Main plugin;
  
  public ThreadSocket(Socket socket, Main main)
  {
    this.socket = socket;
    this.plugin = main;
  }
  
  public void run()
  {
    try
    {
      InputStream is = this.socket.getInputStream();
      InputStreamReader isr = new InputStreamReader(is, "UTF-8");
      OutputStream os = this.socket.getOutputStream();
      OutputStreamWriter osr = new OutputStreamWriter(os);
      
      BufferedReader br = new BufferedReader(isr);
      String command = br.readLine();
      if (command.indexOf(";") != -1)
      {
        if (command.split(";").length >= 2)
        {
          String api_key = command.split(";")[0];
          String id = command.split(";")[1];
          String[] commands = command.replace(api_key + ";" + id + ";", "").split(";");
          if ((Main.config.getString("setting.id").equals(id)) && (Main.config.getString("setting.api-key").equals(api_key)))
          {
            String[] arrayOfString1;
            int j = (arrayOfString1 = commands).length;
            for (int i = 0; i < j; i++)
            {
              String str = arrayOfString1[i];
	          this.plugin.getConfig().set("lastcommand", str);
	          this.plugin.saveConfig();
	          Bukkit.dispatchCommand(Bukkit.getConsoleSender(), str);
	          this.plugin.saver(str);
	          System.out.println("[Baw Service] Baw Service API 명령어 실행: "+str);
            }
            sendPacket(osr, "Error OK");
            this.socket.close();
          }
          else if (Main.config.getString("setting.id").equals(id))
          {
            System.out.println("[Baw Service] 보안을 위하여 전송받은 데이터를 검증하였으나, 잘못된 Baw Service API KEY 입니다.");
            sendPacket(osr, "Error API_KEY");
            this.socket.close();
          }
          else
          {
            System.out.println("[Baw Service] 보안을 위하여 전송받은 데이터를 검증하였으나, 잘못된 Baw Service ID 입니다.");
            sendPacket(osr, "Error ID");
            this.socket.close();
          }
        }
        else
        {
          sendPacket(osr, "Error Server_Error");
          this.socket.close();
        }
      }
      else
      {
        System.out.println("[Baw Service] Baw Service로부터 이 버전의 API 플러그인으로 해석할 수 없는 데이터를 수신했습니다. 무시합니다.");
        sendPacket(osr, "Error Server_Error");
        this.socket.close();
      }
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
  }
  
  private void sendPacket(OutputStreamWriter osr, String msg)
  {
    BufferedWriter bw = new BufferedWriter(osr);
    PrintWriter pw = new PrintWriter(bw);
    pw.println(msg);
    pw.flush();
  }
}
