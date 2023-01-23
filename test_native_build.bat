call del scala-sbt-lib-resolver-cli.*  || exit /b
call sbt clean;test;assembly  || exit /b
call vcvars64.bat  || exit /b
call build_windows.bat  || exit /b
call del *.dll
call .\scala-sbt-lib-resolver-cli.exe --hotlist test-single --config internal  || exit /b
rem switch folder so we can test things work relatively from the directory of the executable
call cd .. && scala-sbt-lib-resolver-cli/scala-sbt-lib-resolver-cli.exe --hotlist test-single --config src/main/resources/config.json  || exit /b
