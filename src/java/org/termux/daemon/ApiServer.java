package org.termux.daemon;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import java.net.ServerSocket;
import java.net.Socket;

import java.nio.charset.StandardCharsets;

import org.termux.daemon.module.ClipboardManager;
import org.termux.daemon.module.ContentResolver;
import org.termux.daemon.module.MusicPlayer;

import static org.termux.daemon.Logger.LogLevel.INFO;

public class ApiServer {
  private static final Logger logger = Logger.getInstance();

  public static void start(int port)
      throws Exception {

      ServerSocket server =
        new ServerSocket(port);

      System.out.println("Server listening on 127.0.0.1:" + port);

      while (true) {
        Socket client = server.accept();
        handle(client);
      }
  }

  private static String readLine(InputStream in)
      throws Exception {

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

    return new String(
      buf.toByteArray(),
      StandardCharsets.UTF_8
    );
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

      String cmd = readLine(in);

      if (cmd == null) {
        return;
      }

      switch (cmd) {

        case "get" -> {
          long t0 = System.nanoTime();
          String content = ClipboardManager.get();
          long t1 = System.nanoTime();

          if (Config.LOG_LEVEL == INFO) {
            System.out.printf(
              "get=%.3f ms%n",
              (t1 - t0) / 1e6
            );

            System.out.printf(
              "[SERVER] get: '%s' from: %s%n",
              content,
              client
            );
          }

          outRaw.write(
            content.getBytes(StandardCharsets.UTF_8)
          );

          outRaw.flush();
        }

        case "set" -> {
          long t0 = System.nanoTime();

          ByteArrayOutputStream buf =
            new ByteArrayOutputStream();

          byte[] tmp =
            new byte[4096];

          int n;

          while ((n = in.read(tmp)) != -1) {
            buf.write(tmp, 0, n);
          }

          String content
            = new String(
              buf.toByteArray(), StandardCharsets.UTF_8
            );

          long t1 = System.nanoTime();
          ClipboardManager.set(content);
          long t2 = System.nanoTime();

          if (Config.LOG_LEVEL == INFO) {
            System.out.printf(
              "[SERVER] set: '%s' from: %s%n",
              content,
              client
            );

            System.out.printf(
              "read=%.3f ms, set=%.3f ms%n",
              (t1 - t0) / 1e6,
              (t2 - t1) / 1e6
            );
          }
        }

        case "open" -> {
          String path = readLine(in);

          if (path == null || path.isBlank()) {
            out.println( "ERROR: missing path");
            break;
          }

        try {
            ContentResolver.open(path, null);

            if (Config.LOG_LEVEL == INFO) {
              System.out.printf(
                "[SERVER] open: '%s' from: %s%n",
                path,
                client
              );
            }

          } catch (Exception e) {
            e.printStackTrace();
          }
        }

        case "music" -> {
          String path = readLine(in);

          if (path == null || path.isBlank()) {
            out.println("ERROR: missing path");
            break;
          }

          try {
            MusicPlayer.play(path, socket);

            if (Config.LOG_LEVEL == INFO) {
              System.out.printf(
                "[SERVER] music: '%s' from: %s%n",
                path,
                client
              );
            }

          } catch (Exception e) {
            e.printStackTrace();
          }
        }

        default -> out.println("ERROR: unknown command: " + cmd);
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
