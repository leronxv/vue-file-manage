# vue-file-manage

## 一、项目介绍

适用于 Vue2 + Springboot 的文件管理/文件共享组件

## 二、功能概览

| 操作         | 文件 | 文件夹 | 备注                                              |
| :----------- | :--: | :----: | :------------------------------------------------ |
| 创建         |  ❌   |   ✅    |                                                   |
| 删除         |  ✅   |   ✅    |                                                   |
| 上传         |  ✅   |   -    |                                                   |
| 拖拽上传     |  ✅   |   ❌    | 📅                                                 |
| 拖拽批量上传 |  ✅   |   ❌    | 📅                                                 |
| 重命名       |  ✅   |   ✅    |                                                   |
| 拖拽移动     |  ✅   |   ✅    |                                                   |
| 复制         |  ❌   |   ❌    | 📅                                                 |
| 解压缩       |  ❌   |   ❌    | 📅                                                 |
| 预览         |  ✅   |   -    | 目前支持文本、图片、视频、office文件的预览        |
| 搜索         |  ✅   |   ✅    | 支持 本地化索引 和 ElasticSearch索引 两种方式搜索 |

## 三、UI框架支持

| 框架           | 支持 | 备注                     |
| -------------- | :--: | ------------------------ |
| ElementUI      |  ❌   | 📅                        |
| Ant Design Vue |  ✅   | 使用树形组件渲染菜单列表 |

## 四、项目部署

### 1、作为第三方组件引入

#### 1、下载源码

```shell
git clone https://gitee.com/leronx/vue-file-manage.git
```

#### 2、复制对应的 ui 到项目

```
.
├── App.vue
├── assets
│   └── logo.png
├── components
│   └── fileManage // vue-file-manage 组件
├── main.js
└── utils
    └── request.js
```

将 components 下的 fileManage 整个目录复制到项目

#### 3、修改接口地址

本项采用 axios 作为网络请求工具，您需要将 fileManage/index.vue 文件夹下的请求工具更换为您项目中使用的网络请求工具

```js
getFolderTree() {
  // 以获取文件列表为例， request.getAction 需要更换为您项目中封装的网络请求工具
  request.getAction('/file-manager/folder-tree').then(res => {
    if (res.success) {
      this.fileTreeData = res.data
      this.rootPath = res.data.filePath
    }
  })
},
```

#### 4、集成业务后端

```
.
├── file-mange-example  // 演示项目
│   ├── pom.xml
│   └── src
├── file-mange-springboot-starter  // springboot starter
│   ├── pom.xml
│   └── src
└── pom.xml
```



项目后端使用 maven 构建，将 `file-manage-springboot-starter` 安装至本地项目仓库

```shell
cd file-manage-boot/file-mange-springboot-starter
mvn clean install
```

在您的项目中引入 `file-manage-springboot-starter` 依赖

```xml
<dependency>
    <groupId>com.ler.fm</groupId>
    <artifactId>file-mange-springboot-starter</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

在 application.yml 新增以下配置

```yml
spring:
  servlet:
  	# (可选)
    multipart:
      max-file-size: 1GB
      max-request-size: 1GB

fm:
	# 文件索引类型，可选择本地化文件索引和 elasticsearch 索引
	# local | elasticsearch
  file-index: local
  storage-path: file-storage/
```

文件/目录变更监听

例如在文件/目录被重命名、移动目录等操作时记录数据库

// 待补充

### 2、本地开发

如果本项目不能满足您的业务需求，例如给文件夹/文件添加权限功能等场景，需要对本项目进行二次开发

将 file-mange-springboot-starter 模块下除  autoconfigure 包下的所有包拷贝至您的项目即可

```java
/**
	* 以文件夹列表为例，进行权限二次开发
	* @param fileSimpleDigest 文件树
	* @param permissionsMap 用户具有的资源列表
	* @param isAdmin 是否为管理员
	*/
public static void traverseFolder(FileSimpleDigest fileSimpleDigest, Map<String, Integer> permissionsMap, boolean isAdmin) {
        File folder = new File(fileSimpleDigest.getFilePath());
        File[] files = folder.listFiles();
        if (files == null) return;
        fileSimpleDigest.setChild(new ArrayList<>());
        for (File file : files) {
            if (file.isDirectory()) {
                boolean hasPer = false;
                for (String path : permissionsMap.keySet()) {
                    if (path.startsWith(file.getPath())) {
                        hasPer = true;
                        break;
                    }
                }
                if (hasPer || isAdmin) {
                    Integer level = permissionsMap.get(file.getPath());
                    FileSimpleDigest child = new FileSimpleDigest();
                    child.setFilePath(file.getPath());
                    child.setFileName(file.getName());
                    child.setAccessLevel(level);
                    fileSimpleDigest.getChild().add(child);
                    traverseFolder(child, permissionsMap, isAdmin);
                }
            }
        }
    }
```

### 3、单独部署

待补充...



