-keep public class org.termux.entry.Cli {
  public static void main(java.lang.String[]);
}

-keep public class org.termux.daemon.MainActivity {
}

# obfuscating stubClass cause RuntimeException
# see: https://issuetracker.google.com/issues/131619590
-keep class android.os.** { *; }
-dontwarn android.os.ServiceManager

############################################
## Delete debug logging
############################################
-assumenosideeffects class org.termux.util.Logger {
  public void d(...);
}
