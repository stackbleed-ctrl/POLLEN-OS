\
    @ECHO OFF
    SET DIRNAME=%~dp0
    SET APP_HOME=%DIRNAME%
    SET CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

    IF NOT EXIST "%CLASSPATH%" (
      ECHO Missing gradle-wrapper.jar.
      ECHO Run: gradle wrapper
      EXIT /B 1
    )

    java -Dorg.gradle.appname=gradlew -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
