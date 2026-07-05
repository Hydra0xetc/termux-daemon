-keep public class org.termux.daemon.Main {
    public static void main(java.lang.String[]);
}

-dontwarn android.os.ServiceManager

############################################
## Delete debug logging
############################################
-assumenosideeffects class org.termux.daemon.Logger {
    public void d(...);
}
