# 基础配置
-ignorewarnings
-dontnote **
-dontwarn **

# ---------------------------------------------------------
# 资源与 SPI (服务发现) 适配 - 解决 Logback 找不到提供者的核心
# ---------------------------------------------------------
# JVM 环境下保留 META-INF/services 配置文件的正确指令
-adaptresourcefilenames META-INF/services/**
-adaptresourcefilecontents META-INF/services/**

# ---------------------------------------------------------
# 日志库保留 (SLF4J + Logback + Kotlin-Logging)
# ---------------------------------------------------------
-keep class org.slf4j.** { *; }
-keep class ch.qos.logback.** { *; }
-keep interface org.slf4j.** { *; }

# ---------------------------------------------------------
# Apache SSHD & MINA (Scrcpy 核心依赖)
# ---------------------------------------------------------
-keep class org.apache.sshd.** { *; }
-keep class org.apache.mina.** { *; }
-keep class org.bouncycastle.** { *; }
-keep class org.apache.putty.** { *; }

# ---------------------------------------------------------
# Kodein DI (依赖注入)
# https://kosi-libs.org/kodein/7.30/core/install.html#_with_gradle
# ---------------------------------------------------------
#-keep class org.kodein.di.** { *; }
#-keepnames class * extends org.kodein.di.DI
-keepattributes Signature

# ---------------------------------------------------------
# Kotlinx Serialization (序列化)
# ---------------------------------------------------------
-keepclassmembers class * {
    *** Companion;
    *** $serializer;
}