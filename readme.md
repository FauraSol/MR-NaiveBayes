

# 实现朴素贝叶斯分类器

MapReduce作业，从docker到java都是从开始学的，所以记录一下

## 起Docker

> 所用文件在/docker子文件夹下，该部分资料来源于[L-LYR/hadoop-docker-cluster (github.com)](https://github.com/L-LYR/hadoop-docker-cluster/tree/master)

在宿主机环境中，在docker子文件夹下运行以下命令，可能需要sudo权限

```bash
# 根据dockerfile创建镜像
# dockerfile下载了依赖的包、配了环境变量等等
docker build . 
# 为新创建好的镜像改名，<hash>是build的结果，可以用docker image ls看
# <name>:<tag>与start_cluster.sh内一致，比如zsq/hadoop:2.7.7
docker image tag <hash> <name>:<tag>
# 创建要用到的虚拟网络
docker network create --driver=bridge hadoop
```

随后依次运行`download_package.sh`和`start_cluster.sh`，前者下载了hadoop和spark包，后者创建了master、slave1、slave2两个集群，并暴露了相关端口，master的50070和8088对应到宿主机的对应端口，它们的作用为：

>  Hadoop的端口号 50070 和 8088 是 Hadoop 的两个重要组件的默认端口号：
>
> 1. 端口号 50070：这是 Hadoop 的 NameNode Web UI 的默认端口号。NameNode 是 Hadoop 分布式文件系统（HDFS）的主节点，负责管理文件系统的命名空间和数据块的位置。通过访问 `http://<hadoop-master>:50070`（其中 `<hadoop-master>` 是 Hadoop 集群的主节点主机名或 IP 地址），您可以在浏览器中查看 NameNode Web UI，以了解有关 HDFS 的信息，例如文件系统概览、数据块报告、节点状态等。
> 2. 端口号 8088：这是 Hadoop 的 ResourceManager Web UI 的默认端口号。ResourceManager 是 Hadoop YARN（Yet Another Resource Negotiator）的主要组件，负责集群资源的管理和作业调度。通过访问 `http://<hadoop-master>:8088`，您可以在浏览器中查看 ResourceManager Web UI，以查看有关集群资源的信息，例如节点列表、正在运行的应用程序、应用程序历史记录等。

建议在docker中，配置ssh，方便传文件，操作如下：

``` bash
apt-get update apt-get install -y openssh-server
# 修改/etc/ssh/sshd_config 确保配置项为
# PermitRootLogin yes
# PasswordAuthentication yes

# 选择直接用root了，不创别的用户了
passwd <password>
# 此时可以测试ssh联通，<host_port>为docker映射的22端口
ssh -p <host_port> root@<host_ip_or_localhost>
```

## Java开发

因为不是Java用户，所以懒得搞复杂，就用vscode的java扩展包，比想象中好用很多，傻瓜式教学下载配置，甚至maven一键打jar包，贴一下settings的配置，里面有用到的包依赖

``` json
{
    "java.project.sourcePaths": [
        "src"
    ],
    "java.project.outputPath": "bin",
    "java.project.referencedLibraries": [
        "lib/**/*.jar",
        "/usr/local/hadoop/share/hadoop/common/hadoop-common-2.7.7.jar",
        "/usr/local/hadoop/share/hadoop/common/hadoop-nfs-2.7.7.jar",
        "/usr/local/hadoop/share/hadoop/common/lib/commons-logging-1.1.3.jar",
        "/usr/local/hadoop/share/hadoop/hdfs/hadoop-hdfs-2.7.7.jar",
        "/usr/local/hadoop/share/hadoop/hdfs/hadoop-hdfs-nfs-2.7.7.jar",
        "/usr/local/hadoop/share/hadoop/mapreduce/*.jar",
        "/usr/local/hadoop/",
    ],
    "java.jdt.ls.java.home": "/root/Naive_Bayes/jdk-17.0.9+9",
    "java.configuration.runtimes": [
        {
            "name": "JavaSE-1.8",
            "path": "/usr/lib/jvm/java-8-openjdk-amd64",
            "default": true
        },
        {
            "name": "JavaSE-17",
            "path": "/root/Naive_Bayes/jdk-17.0.9+9",
        },
    ],
    "maven.view": "flat"
}
```

## MapReduce程序

等写完实验报告再说

## 测试

先写程序，vscode插件一键打包传到hadoop里面就好，start.sh里就一句命令即可运行，后面几行是检查各个步骤输出结果的
