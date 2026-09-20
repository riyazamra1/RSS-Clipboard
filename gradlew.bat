@echo off
setlocal
set APP_HOME=%~dp0
set JAR=%APP_HOME%gradle\wrapper\gradle-wrapper.jar
if not exist "%JAR%" (
  if not exist "%APP_HOME%gradle\wrapper" mkdir "%APP_HOME%gradle\wrapper"
  powershell -NoProfile -Command "Invoke-WebRequest -UseBasicParsing -Uri 'https://raw.githubusercontent.com/gradle/gradle/v8.13.0/gradle/wrapper/gradle-wrapper.jar' -OutFile '%JAR%'"
)
java -classpath "%JAR%" org.gradle.wrapper.GradleWrapperMain %*
