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
| 移动         |  ✅   |   ✅    |                                                   |
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
│   └── fileManage
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

