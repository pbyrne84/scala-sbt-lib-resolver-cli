docker build -t lib_resolver_vm .
rem pwd seems to break on background updates, hopefully this enables a stable commit  :(
set "currentDir=%cd%"
echo %currentDir%
docker run --name lib_resolver_compile --mount src="%currentDir%",target=/root/project_mount,type=bind -t -d lib_resolver_vm  || exit /b
docker exec -it lib_resolver_compile bash
