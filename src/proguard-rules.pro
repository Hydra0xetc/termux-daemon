

-keep public class org.termux.daemon.Main {
    public static void main(java.lang.String[]);
}

# obfuscating stubClass cause RuntimeException
# see: https://issuetracker.google.com/issues/131619590
-keep class android.os.** { *; }
-dontwarn android.os.ServiceManager

############################################
## Delete debug logging
############################################
-assumenosideeffects class org.termux.daemon.Logger {
    public void d(...);
}
