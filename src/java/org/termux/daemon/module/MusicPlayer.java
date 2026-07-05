package org.termux.daemon.module;

import android.media.MediaPlayer;
import android.os.Looper;
import android.os.Handler;

import java.util.concurrent.CountDownLatch;
import java.net.Socket;

import java.io.IOException;

import org.termux.daemon.Logger;

import static org.termux.daemon.Logger.LogLevel.*;

public class MusicPlayer {
  private static final String TAG = "MusicPlayer";
  private static MediaPlayer mp;
  private static Looper looper;
  private static Logger logger = Logger.getInstance();

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

  private static String mediaStrError(int what) {
    switch (what) {
      case MediaPlayer.MEDIA_ERROR_IO: return "MEDIA_ERROR_IO";
      case MediaPlayer.MEDIA_ERROR_MALFORMED: return "MEDIA_ERROR_MALFORMED";
      case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK: return "MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK";
      case MediaPlayer.MEDIA_ERROR_SERVER_DIED: return "MEDIA_ERROR_SERVER_DIED";
      case MediaPlayer.MEDIA_ERROR_TIMED_OUT: return "MEDIA_ERROR_TIMED_OUT";
      case MediaPlayer.MEDIA_ERROR_UNKNOWN: return "MEDIA_ERROR_UNKNOWN";
      case MediaPlayer.MEDIA_ERROR_UNSUPPORTED: return "MEDIA_ERROR_UNSUPPORTED";
      case MediaPlayer.MEDIA_INFO_AUDIO_NOT_PLAYING: return "MEDIA_INFO_AUDIO_NOT_PLAYING";
      case MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING: return "MEDIA_INFO_BAD_INTERLEAVING";
      case MediaPlayer.MEDIA_INFO_BUFFERING_END: return "MEDIA_INFO_BUFFERING_END";
      case MediaPlayer.MEDIA_INFO_BUFFERING_START: return "MEDIA_INFO_BUFFERING_START";
      case MediaPlayer.MEDIA_INFO_METADATA_UPDATE: return "MEDIA_INFO_METADATA_UPDATE";
      case MediaPlayer.MEDIA_INFO_NOT_SEEKABLE: return "MEDIA_INFO_NOT_SEEKABLE";
      case MediaPlayer.MEDIA_INFO_STARTED_AS_NEXT: return "MEDIA_INFO_STARTED_AS_NEXT";
      case MediaPlayer.MEDIA_INFO_SUBTITLE_TIMED_OUT: return "MEDIA_INFO_SUBTITLE_TIMED_OUT";
      case MediaPlayer.MEDIA_INFO_UNSUPPORTED_SUBTITLE: return "MEDIA_INFO_UNSUPPORTED_SUBTITLE";
      case MediaPlayer.MEDIA_INFO_VIDEO_NOT_PLAYING: return "MEDIA_INFO_VIDEO_NOT_PLAYING";
      case MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START: return "MEDIA_INFO_VIDEO_RENDERING_START";
      case MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING: return "MEDIA_INFO_VIDEO_TRACK_LAGGING";
      default:
        return null;
    }
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

        logger.d(TAG, "Setting up: " + fullpath);

        mp.setOnPreparedListener((player) -> {
          mp.start();
        });

        mp.setOnErrorListener((player, what, extra) -> {
          myLooper.quitSafely();

          logger.d(TAG, "what = " + mediaStrError(what));
          return true;
        });

        mp.setOnCompletionListener(player -> myLooper.quitSafely());

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

        mp.prepareAsync();

        myLooper.loop();
        watcher.interrupt();
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        logger.d(TAG, "releasing the player");
        mp.release();
        done.countDown();
      }
    });

    playerThread.start();
    done.await();
  }
}
