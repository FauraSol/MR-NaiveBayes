

# 实现朴素贝叶斯分类器

MapReduce作业，从docker到java都是从开始学的，所以记录一下

**内部人员直接去51用已经起好的docker，docker里有Naive_Bayes文件夹，就是本工程，直接跳转到最后的测试阶段**

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

具体等写完实验报告再说
简单提一嘴四个MapReduce任务

* DocCounter：
	* 作用：计算各个类别文档的数目
 
* WordCounter：
	* 作用：计算每个类别文档中，不同单词的数目
 
* TestPreparation：
	* 作用：预处理测试集的文件，因为后面要把每个单词出现在每个类的概率理想化为独立事件，所以要进行文档到单词list的映射
 
 * TestPrediction：
	* 作用：setup里首先计算了先验概率和后验概率，然后完成对测试集文档的预测过程。

## All You Need To Do
这一部分写给像我一样毫无Java、hadoop、Bayes基础的人速通实验报告

1. 通过ssh+vscode在docker上远程开发（先在51上`sudo docker exec -it <tag> bash`启动docker的终端），然后ssh root@192.168.1.51 -p 56780，密码zsq；外部人员可自行在docker内部启动ssh服务，`<tag>`就是`sudo docker container ls -a`看名为`hadoop:master/hadoop:slave1/hadoop:slave2`的对应的那一长串哈希值，想git的commit节点一样，取前几位就可以，不必全抄。
2. 本实验的测试部分参考课程文件夹的数据压缩包内的数据readme.txt，请阅读后食用，已经使用的数据集是AUSTR、CANA、BELG、GREECE，建议换个数据集以防结果完全相同。具体而言，建议测试集和训练集用到的国家相同，每个国家各取70%扔到训练集，30%扔到测试集
3. `hdfs put`将选的到对应路径，并修改Config.java里的路径
4. 安装vscode的Java扩展包，利用下图扩展包的功能一键打包，截歪了，点箭头（在其他人的环境中发生过找不到包依赖的情况，实践得到的解决方案是在settings.json中将注释掉的通配符表示的包依赖取消注释）![1KEYL8)5(UZ06_{JTSSKF98](https://github.com/FauraSol/MR-NaiveBayes/assets/56348816/25a264bf-31b4-4e7c-ad46-05cc13a59c22)
5. 按照`start.sh`中的唯一一条未注释掉的命令，将其复制到第1步启动的终端里运行（ssh目前无法启动hadoop），在启动hadoop服务，前往192.168.1.51:8088（非内部人员使用对应的宿主机ip的8088端口）完成所需的任务截图（其他注释掉的命令是检查任务输出的），保存好执行此命令后的输出，这是需要截图的内容
6. 向报告中复制代码时批量替换所有zsq为你的名字（不建议执行前在代码中修改，因为包名带了zsq，如果修改，需要按照相似的目录格式重新起项目，重新起项目时利用vscode Java扩展包，选择maven构建项目）（第一次写java，创项目是一路确定创建的，没意识到文件目录和包名要对应且好像改了会出问题）

