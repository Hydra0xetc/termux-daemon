package org.termux.daemon;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import java.net.ServerSocket;
import java.net.Socket;

import java.nio.charset.StandardCharsets;

import org.termux.daemon.service.ClipboardManager;
import org.termux.daemon.service.ContentResolver;
import org.termux.daemon.service.MusicPlayer;

import static org.termux.daemon.Logger.LogLevel.INFO;

enum Service {
  CLIPBOARD("clipboard"),
  OPEN("open"),
  MUSIC("music");

  private final String key;

  Service(String key) {
    this.key = key;
  }

  public String getKey() {
    return key;
  }

  public static Service fromKey(String key) {
    for (Service service : values()) {
      if (service.getKey().equals(key)) {
        return service;
      }
    }

    throw new IllegalArgumentException(
      "Unknown service: " + key
    );
  }

}

public class ApiServer {
  private static final String TAG = "SERVER";
  private static Logger logger = Logger.getInstance();

  public static void start(int port)
      throws Exception, java.net.BindException {

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

      String line = readLine(in);

      if (line == null) {
        return;
      }

      String[] parts =
        line.trim().split("\\s+", 2);

      String cmd = parts[0];
      String action =
        parts.length > 1 ? parts[1] : "";

      Service service =
        Service.fromKey(cmd);

      switch (service) {

        case CLIPBOARD -> {

          switch (action) {

            case "get" -> {

              long t0 = System.nanoTime();
              String content = ClipboardManager.get();
              long t1 = System.nanoTime();

              if (Config.LOG_LEVEL == INFO) {
                logger.i(TAG, String.format("get=%.3f ms%n", (t1 - t0) / 1e6));
                logger.i(TAG, String.format("get: '%s' from: %s%n", content, client));
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

              byte[] tmp = new byte[4096];
              int n;

              while ((n = in.read(tmp)) != -1) {
                buf.write(tmp, 0, n);
              }

              String content =
                new String(
                  buf.toByteArray(),
                  StandardCharsets.UTF_8
                );

              long t1 = System.nanoTime();
              ClipboardManager.set(content);
              long t2 = System.nanoTime();

              if (Config.LOG_LEVEL == INFO) {
                logger.i(TAG, String.format("set: '%s' from: %s%n", content, client));
                logger.i(TAG, String.format("read=%.3f ms, set=%.3f ms%n",
                    (t1 - t0) / 1e6, (t2 - t1) / 1e6
                ));
              }
            }

            default ->
              out.println("ERROR: clipboard [set|get]");
          }
        }

        case OPEN -> {

          switch (action) {
            case "file" -> {

              String path = readLine(in);

              if (path == null || path.isBlank()) {
                out.println("ERROR: missing path");
                break;
              }

              try {
                ContentResolver.file(path, null);

                if (Config.LOG_LEVEL == INFO) {
                  logger.i(TAG,
                      String.format("open: '%s' from: %s%n", path, client));
                }

              } catch (Exception e) {
                e.printStackTrace();
              }
            }

            case "url" -> {
              String url = readLine(in);

              if (url == null || url.isBlank()) {
                out.println("ERROR: missing url");
                break;
              }

              ContentResolver.url(url);
            }
          }
        }

        case MUSIC -> {

          switch (action) {
            case "stop" -> {
              MusicPlayer.stop();

              if (Config.LOG_LEVEL == INFO) {
                logger.i(TAG, "music stop");
              }

              break;
            }

            case "play" -> {

              String path = readLine(in);

              if (path == null || path.isBlank()) {
                out.println("ERROR: missing path");
                break;
              }

              try {
                MusicPlayer.play(path);

                if (Config.LOG_LEVEL == INFO) {
                  logger.i(TAG,
                      String.format("music: '%s' from: %s%n", path, client));
                }

              } catch (Exception e) {
                e.printStackTrace();
              }
            }

            case "pause" -> {
              try {
                MusicPlayer.pause();
              } catch (Exception e) {
                e.printStackTrace();
              }
            }

            case "resume" -> {
              try {
                MusicPlayer.resume();
              } catch (Exception e) {
                e.printStackTrace();
              }
            }

          }
        }
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
