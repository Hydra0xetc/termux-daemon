package org.termux.daemon;

import android.os.Looper;

public class Main {
    private static int port = 6969;
    public static void main(String[] args) throws Exception {
        if (Looper.getMainLooper() == null) {
            Looper.prepare();
        }

        for (int i = 0; i < args.length; i++) {
          if (args[i].equals("--port")) {
            port = Integer.valueOf(args[i + 1]);
          }
        }

        /* Simple test
         * String content = "Hello From org.clipboard";
         * ClipboardModule.set(content);
         * logger.log(INFO, "Clipboard", "set: " + content);
         * logger.log(INFO, "Clipboard", "get: " + ClipboardModule.get());
         * */


        ApiServer.start(port);
    }
}
