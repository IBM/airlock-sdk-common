-libraryjars  /Library/Java/JavaVirtualMachines/jdk1.8.0_111.jdk/Contents/Home/jre/lib/rt.jar
-printseeds
-dontwarn
-dontwarn com.github.**
-dontwarn com.ibm.airlock.common.util.Decryptor
-keepattributes Signature

-keep public class com.ibm.airlock.common.AirlockManager{
  public *;
}

-keep public class com.ibm.airlock.common.engine.AirlockContextManager{
  public *;
}

-keep public class com.ibm.airlock.common.cache.PersistenceHandler{
  public *;
}


-keep public class com.ibm.airlock.common.cache.PercentageManager{
  public *;
}

-keep public enum  com.ibm.airlock.common.cache.PercentageManager$** {
    **[] $VALUES;
    public *;
}

-keep public class com.ibm.airlock.common.streams.StreamsManager{
  public *;
}


-keep public class com.ibm.airlock.common.streams.AirlockStream{
  public *;
}


-keep public class com.ibm.airlock.common.AirlockProductManager{
  public *;
}
-keep public class com.ibm.airlock.common.data.Feature{
  public *;
}
-keep public class com.ibm.airlock.common.util.Constants{
  public *;
}

-keep public class com.ibm.airlock.common.BaseAirlockProductManager {
  public *;
  protected *;
}

-keep public class com.ibm.airlock.common.util.AirlockMessages {*;}

-keep public class  com.ibm.airlock.common.data.FeaturesList {*;}

-keep public class com.ibm.airlock.common.AirlockInvalidFileException {*;}

-keep public interface com.ibm.airlock.common.cache.SharedPreferences {*;}

-keep public interface com.ibm.airlock.common.cache.SharedPreferences$* {*;}


-keep public class com.ibm.airlock.common.engine.StateFullContext{
  public *;
}
-keep public class com.ibm.airlock.common.cache.Context{
  public *;
}
-keep public class com.ibm.airlock.common.AirlockCallback{
  public *;
}
-keep public class com.ibm.airlock.common.AirlockNotInitializedException{
  public *;
}

-keep public class com.ibm.airlock.common.data.StreamTrace{
  public *;
}

-keep public enum com.ibm.airlock.common.data.Feature$** {
    **[] $VALUES;
    public *;
}



-keep public enum com.ibm.airlock.common.net.AirlockDAO$** {
    **[] $VALUES;
    public *;
}


-keep public class com.ibm.airlock.common.AirlockInvalidFileException
-keepattributes InnerClasses
-keepattributes Exceptions

-keepclassmembers class * extends java.lang.Enum {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}