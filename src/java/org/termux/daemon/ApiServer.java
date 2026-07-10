package org.termux.daemon;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.File;

import java.util.List;

import java.net.ServerSocket;
import java.net.Socket;

import java.nio.charset.StandardCharsets;

import org.termux.daemon.Service;
import org.termux.daemon.service.ClipboardManager;
import org.termux.daemon.service.ContentResolver;
import org.termux.daemon.service.MusicPlayer;
import org.termux.daemon.service.ApkManager;

import static org.termux.daemon.Logger.LogLevel.INFO;

public class ApiServer {
  private static final String TAG = "SERVER";
  private static Logger logger = Logger.getInstance();
  private static List<Service> services;

  public static void start(int port)
      throws Exception, java.net.BindException {

      File classpath = new File(System.getProperty("java.class.path"));
      File servicePath = new File(classpath.getParentFile(), "SERVICE");

      try {
        services = Service.parseService(servicePath);
      } catch (IOException e) {
        logger.e(TAG, "failed to parse services file" + e.getMessage());
        System.exit(1); // NOTE: maybe abort would be better
      }

      // scan once in startup
      new Thread(() -> ApkManager.scanApk()).start();

      Service.registerHandler(services,
        "clipboard", "get", (in, outRaw, client) -> {

          long t0 = System.nanoTime();
          String content = ClipboardManager.get();
          long t1 = System.nanoTime();

          if (Config.LOG_LEVEL == INFO) {
            logger.i(TAG, String.format("get=%.3f ms%n", (t1 - t0) / 1e6));
            logger.i(TAG, String.format("get: '%s' from: %s%n", content, client));
          }

          outRaw.write(content.getBytes(StandardCharsets.UTF_8));
          outRaw.flush();
      });

      Service.registerHandler(services,
        "clipboard", "set", (in, outRaw, client) -> {

          long t0 = System.nanoTime();

          ByteArrayOutputStream buf = new ByteArrayOutputStream();

          byte[] tmp = new byte[4096];
          int n;

          while ((n = in.read(tmp)) != -1) {
            buf.write(tmp, 0, n);
          }

          String content =
            new String(buf.toByteArray(), StandardCharsets.UTF_8);

          long t1 = System.nanoTime();
          ClipboardManager.set(content);
          long t2 = System.nanoTime();

          if (Config.LOG_LEVEL == INFO) {
            logger.i(TAG, String.format("set: '%s' from: %s%n", content, client));
            logger.i(TAG, String.format("read=%.3f ms, set=%.3f ms%n",
                (t1 - t0) / 1e6, (t2 - t1) / 1e6
            ));
          }
    });

      Service.registerHandler(services,
        "open", "file", (in, outRaw, client) -> {
          PrintWriter out = new PrintWriter(outRaw, true);

          String path = readLine(in);

          if (path == null || path.isBlank()) {
            out.println("ERROR: missing path");
            return;
          }

          try {
            ContentResolver.file(path, null);

            if (Config.LOG_LEVEL == INFO) {
              logger.i(TAG,
                  String.format("open: '%s' from: %s%n", path, client));
            }

          } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR: " + e.getMessage());
          }
      });

      Service.registerHandler(services,
        "open", "url", (in, outRaw, client) -> {
          PrintWriter out = new PrintWriter(outRaw, true);
          String url = readLine(in);

          if (url == null || url.isBlank()) {
            out.println("ERROR: missing url");
            return;
          }

          ContentResolver.url(url);
      });

      Service.registerHandler(services,
        "music", "play", (in, outRaw, client) -> {
          PrintWriter out = new PrintWriter(outRaw, true);
          String path = readLine(in);

          if (path == null || path.isBlank()) {
            out.println("ERROR: missing path");
            return;
          }

          try {
            MusicPlayer.play(path);

            if (Config.LOG_LEVEL == INFO) {
              logger.i(TAG,
                  String.format("music: '%s' from: %s%n", path, client));
            }

          } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR: " + e.getMessage());
          }
      });

      Service.registerHandler(services,
        "music", "stop", (in, outRaw, client) -> {
          MusicPlayer.stop();

          if (Config.LOG_LEVEL == INFO) {
            logger.i(TAG, "music stop");
          }
      });

      Service.registerHandler(services,
        "music", "pause", (in, outRaw, client) -> {
          PrintWriter out = new PrintWriter(outRaw, true);
          try {
            MusicPlayer.pause();
          } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR: " + e.getMessage());
          }
      });

      Service.registerHandler(services,
        "music", "resume", (in, outRaw, client) -> {
          PrintWriter out = new PrintWriter(outRaw, true);
          try {
            MusicPlayer.resume();
          } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR: " + e.getMessage());
          }
      });

      Service.registerHandler(services,
        "apk", "scan", (in, outRaw, client) -> {
          PrintWriter out = new PrintWriter(outRaw, true);
          try {
            String msg = ApkManager.scanApk();
            if (msg != null) {
              out.println(msg);
            }
          } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR: " + e.getMessage());
          }
      });

      Service.registerHandler(services,
        "apk", "open", (in, outRaw, client) -> {
          PrintWriter out = new PrintWriter(outRaw, true);
          try {
            String apkName = readLine(in);
            String msg = ApkManager.openApk(apkName);
            if (msg != null) {
              out.println(msg);
            }
          } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR: " + e.getMessage());
          }
      });

      Service.registerHandler(services,
        "apk", "list", (in, outRaw, client) -> {
          PrintWriter out = new PrintWriter(outRaw, true);
          try {
            for (String apk : ApkManager.listApk()) {
              out.println(apk);
            }

          } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR: " + e.getMessage());
          }
      });

      Service.registerHandler(services,
        "apk", "uninstall", (in, outRaw, client) -> {
          PrintWriter out = new PrintWriter(outRaw, true);
          try {
            String apkName = readLine(in);
            ApkManager.uninstallApk(apkName);

          } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR: " + e.getMessage());
          }
      });

      ServerSocket server = new ServerSocket(port);
      System.out.println("Server listening on 127.0.0.1:" + port);

      while (true) {
        Socket client = server.accept();
        new Thread(() -> handle(client)).start();
      }
  }

  private static String readLine(InputStream in) throws Exception {

    ByteArrayOutputStream buf = new ByteArrayOutputStream();

    int b;
    while ((b = in.read()) != -1) {
      if (b == '\n') {
        break;
      }

      buf.write(b);
    }

    if (b == -1 && buf.size() == 0) {
      return null;
    }

    return new String(buf.toByteArray(), StandardCharsets.UTF_8);
  }

  private static void handle(Socket socket) {
    try (
        socket;
        InputStream in = socket.getInputStream();
        OutputStream outRaw = socket.getOutputStream();
        PrintWriter out = new PrintWriter(outRaw, true);
        ) {

      String client =
        socket.getInetAddress()
        .getHostAddress()
        + ":" + socket.getPort();

      String line = readLine(in);

      if (line == null) {
        return;
      }

      String[] parts = line.trim().split("\\s+", 2);

      String cmd = parts[0];
      String action = parts.length > 1 ? parts[1] : "";

      Service matched = null;
      for (Service s : services) {
        if (s.name.equals(cmd)) {
          matched = s;
          break;
        }
      }

      if (matched == null) {
        out.println("ERROR: unknown service " + cmd);
        return;
      }

      Service.ServiceCmd matchedCmd = matched.findCmd(action);

      if (matchedCmd == null || matchedCmd.handler == null) {
        out.println("ERROR: unknown command " + action + " for service " + cmd);
        return;
      }

      matchedCmd.handler.handle(in, outRaw, client);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
