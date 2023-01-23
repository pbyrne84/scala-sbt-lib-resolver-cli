docker build -t amazon_vm .
rem pwd seems to break on background updates, hopefully this enables a stable commit  :(
set "currentDir=%cd%"
echo %currentDir%
docker run --privileged  --name amazon_compile --mount src="%currentDir%",target=/root/project_mount,type=bind -t -d amazon_vm
docker exec -it amazon_compile bash
