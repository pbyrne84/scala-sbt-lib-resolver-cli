call sbt clean;test;assembly
call build_windows.bat
call scala-sbt-lib-resolver-cli.exe --hotlist zio-app
