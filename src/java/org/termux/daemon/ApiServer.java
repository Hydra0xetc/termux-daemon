package org.termux.daemon;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

import static org.termux.daemon.Logger.LogLevel;
import static org.termux.daemon.Logger.LogLevel.*;

public class ApiServer {
  private static Logger logger = Logger.getInstance();
  private static LogLevel level;

  public static void start(int port) throws Exception {
    ServerSocket server = new ServerSocket(port);
    System.out.println("Server listening on 127.0.0.1:" + port);

    while (true) {
      Socket client = server.accept();

      handle(client);
    }
  }

  private static void handle(Socket socket) {
    try (
      BufferedReader in = new BufferedReader(
        new InputStreamReader(socket.getInputStream())
      );

      PrintWriter out = new PrintWriter(
        socket.getOutputStream(), true
      );
    ) {
      String client =
        socket.getInetAddress().getHostAddress()
        + ":" + socket.getPort();

      String cmd = in.readLine();

      if (cmd == null) {
        return;
      }

      switch (cmd) {
        case "get" -> {
          long t0 = System.nanoTime();

          long t1 = System.nanoTime();
          String content = ClipboardModule.get();
          long t2 = System.nanoTime();

          if (Config.LOG_LEVEL == INFO) {
            System.out.printf(
              "read=%.3f ms, set=%.3f ms%n",
              (t1 - t0) / 1e6,
              (t2 - t1) / 1e6
            );

            System.out.printf(
              "[SERVER] get: '%s' from: %s%n",
              content, client
            );

          }
          out.print(content);
        }

        case "set" -> {
          long t0 = System.nanoTime();

          String content =
            in.lines().collect(
              java.util.stream.Collectors.joining("\n")
            );

          long t1 = System.nanoTime();
          ClipboardModule.set(content);
          long t2 = System.nanoTime();

          if (Config.LOG_LEVEL == INFO) {
            System.out.printf(
              "[SERVER] set: '%s' from: %s%n",
              content, client
            );

            System.out.printf(
              "read=%.3f ms, set=%.3f ms%n",
              (t1 - t0) / 1e6,
              (t2 - t1) / 1e6
            );

          }
        }

        case "open" -> {
          String path = in.readLine();

          if (path == null || path.isBlank()) {
            out.println("ERROR: missing path");
            break;
          }

          try {
            ContentResolverModule.open(path, null);

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

        default ->
          out.println("ERROR: unknown command: " + cmd);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
