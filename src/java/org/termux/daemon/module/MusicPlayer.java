package org.termux.daemon.module;

import android.media.MediaPlayer;
import android.os.Looper;
import android.os.Handler;

import java.util.concurrent.CountDownLatch;
import java.net.Socket;

import java.io.IOException;

public class MusicPlayer {
  private static MediaPlayer mp;
  private static Looper looper;

  private static synchronized void prepare() {
    if (mp != null) {
      return;
    }

    if (Looper.myLooper() == null) {
      Looper.prepare();
    }

    looper = Looper.myLooper();

    mp = new MediaPlayer();
  }

  public static void play(String fullpath, Socket socket)
      throws IOException, InterruptedException {
    CountDownLatch done = new CountDownLatch(1);

    Thread playerThread = new Thread(() -> {
      Looper.prepare();
      Looper myLooper = Looper.myLooper();
      MediaPlayer mp = new MediaPlayer();
      Handler handler = new Handler(myLooper);

      try {
        mp.setDataSource(fullpath);
        mp.setOnCompletionListener(m -> myLooper.quitSafely());

        Thread watcher = new Thread(() -> {
          try {
            int r = socket.getInputStream().read();
            if (r == -1) handler.post(() -> {
              mp.stop();
              myLooper.quitSafely();
            });
          } catch (IOException e) {
            handler.post(myLooper::quitSafely);
          }
        });
        watcher.setDaemon(true);
        watcher.start();

        mp.prepare();
        mp.start();
        myLooper.loop();
        watcher.interrupt();
        mp.release();
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        done.countDown();
      }
    });

    playerThread.start();
    done.await();
  }
}
