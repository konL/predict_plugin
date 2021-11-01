# 使用文档
【推荐使用版本】**
IDEA官方仓库：IDEA版本2020.3，JDK11
【可预测语言】
Java

## 简易安装教程

下载提供的zip文件，打开 IDEA，选择 File -> Settings -> Plugins -> Install Plugin from Disk... 
选择下载的zip文件即安装完成
![GIF 2021-10-21 10-20-05](https://user-images.githubusercontent.com/24618393/138200640-bba6f9a0-21a3-4d41-8b17-0676d2e2115b.gif)

## 简易使用教程
#### （1）Git Project准备
首先确保待预测文件处于一个git项目之中，即包含提交历史。
为了使得插件中使用的git命令生效，对git需要进行配置。
在.gitattributes文件中增加以下内容
`*.java diff=java`
也可以复制以下完整.gitattributes文件内容：
```
# The default behavior, which overrides 'core.autocrlf', is to use Git's
# built-in heuristics to determine whether a particular file is text or binary.
# Text files are automatically normalized to the user's platforms.
* text=auto

# Explicitly declare text files that should always be normalized and converted
# to native line endings.
.gitattributes text
.gitignore text
LICENSE text
Dockerfile text
*.avsc text
*.go text
*.html text
*.java text
*.md text
*.properties text
*.proto text
*.py text
*.sh text
*.xml text
*.yml text
*.java diff=java

# Declare files that will always have CRLF line endings on checkout.
# *.sln text eol=crlf

# Explicitly denote all files that are truly binary and should not be modified.
# *.jpg binary

# Declare files that should be ignored when creating an archive of the
# git repository
.gitignore export-ignore
.gitattributes export-ignore
/gradlew* export-ignore
/gradle export-ignore

```

接着在shell中使得该配置生效 `./.gitattributes`或`bash .gitattributes`
#### （2）插件使用
点击待预测文件任意一处，点击EditMenu下的插件"重命名方法检测"，预测为可校正的标识符高亮显示。用户可以双击选中高亮的标识符，获得插件推荐的方法名，决定是否重构。
![GIF 2021-10-21 10-48-50](https://user-images.githubusercontent.com/24618393/138202862-05a15c9e-5da1-4208-a6ff-10416a9a7123.gif)
