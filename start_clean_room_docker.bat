docker build -f CleanDockerfile -t clean_ubuntu_vm .
rem pwd seems to break on background updates, hopefully this enables a stable commit  :(
set "currentDir=%cd%"
echo %currentDir%
docker run --name clean_ubuntu_vm_compile --mount src="%currentDir%",target=/root/project_mount,type=bind -t -d clean_ubuntu_vm  || exit /b
docker exec -it clean_ubuntu_vm_compile bash
