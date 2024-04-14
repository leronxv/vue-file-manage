<template>
  <div>
    <div class="file-operate-container">
      <div
          @contextmenu.prevent="showMenuForLeftTree($event)"
          @dragover="dragOver"
          :style="{ width: leftWidth + 'px' }"
          @drop="drop"
          class="left-menu">
        <div class="nav-left">
          <span @click="createRootFolder" class="operator-btn">
            <a-tooltip placement="right">
              <template slot="title">
                <span>创建文件夹</span>
              </template>
              <a-icon type="folder-add"/>
            </a-tooltip>
          </span>

          <span @click="refresh" class="operator-btn" style="margin-left: 10px">
            <a-tooltip placement="right">
              <template slot="title">
                <span>刷新</span>
              </template>
              <a-icon type="reload"/>
            </a-tooltip>
          </span>
        </div>
        <file-tree @select="onSelect" :tree-data="fileTreeData.child"></file-tree>
      </div>
      <div id="divider" @mousedown="startDragging"></div>
      <div @contextmenu.prevent="showMenu($event)" class="right-container" @dragover.prevent @drop="handleDrop">
        <div class="nav-right">
          <div class="breadcrumb">
          <span class="pointer" @click="handleBreadcrumb('/',-1)">/
            <span class="separator" v-if="currentPath.split('/').length > 1">></span></span>
            <span
                v-for="(item,index) in currentPath.split('/')"
                :key="index"
                @click="handleBreadcrumb(item,index)">
              <span class="pointer" v-if="item  + '/' !== fileTreeData.filePath">{{ item }}</span>
              <span
                  class="separator"
                  v-if="index !== currentPath.split('/').length - 1 && item + '/' !== fileTreeData.filePath">
                >
              </span>
            </span>
          </div>
          <div class="search">
            <a-input-search placeholder="请输入文件名称" enter-button @search="onSearch"/>
          </div>
        </div>
        <div class="container-title">
          <div class="group-title file-name">文件名</div>
          <div class="group-title upload-date">创建日期</div>
          <div class="group-title file-type">类型</div>
          <div class="group-title file-size">大小</div>
          <div class="group-title file-operate">操作</div>
        </div>
        <div
            class="file-item"
            @contextmenu.prevent.stop="showMenu($event,item.filePath,item.fileType)"
            @dblclick="handleDbClick(item)"
            v-for="(item,index) in fileList"
            :key="index">
          <div
              draggable="true"
              @dragstart="dragStart"
              @dragend="dragEnd(item.filePath,item.fileName)"
              class="file-title file-name">
            <a-icon :type="getTypeIcon(item.fileType)"/>
            {{ item.fileName }}
          </div>
          <div class="file-title upload-date">{{ item.createTime }}</div>
          <div class="file-title file-type">{{ getFileTypeName(item.fileType) }}</div>
          <div class="file-title file-size">{{ item.fileSize }}</div>
          <div class="file-title file-operate">
            <div v-if="item.fileType !== 'path'">
              <span style="color: #2eabff" @click="previewFile(item.filePath)">预览</span>
              <a-divider type="vertical"/>
              <span style="color: #2eabff" @click="downloadFile(item.filePath)">下载</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-show="showContextMenu" :style="{ top: contextMenuStyle.top, left: contextMenuStyle.left }"
         class="custom-menu">
      <div class="custom-menu-item" v-if="rightKeyFilePath && fileType !== 'path'" @click="previewFile()">
        <a-icon type="search"/>
        预览
      </div>
      <div class="custom-menu-item" @click="createFolder">
        <a-icon type="folder-add"/>
        创建目录
      </div>
      <div class="custom-menu-item" v-if="rightKeyPath" @click="removePath">
        <a-icon type="delete"/>
        删除
      </div>
      <div class="custom-menu-item" v-if="rightKeyPath" @click="renamePath">
        <a-icon type="form"/>
        重命名
      </div>
      <div class="custom-menu-item" @click="openFileDialog">
        <a-icon type="upload"/>
        上传
      </div>
      <div class="custom-menu-item" v-if="rightKeyPath && fileType !== 'path'" @click="downloadFile">
        <a-icon type="download"/>
        下载
      </div>
    </div>

    <a-modal
        title="文件预览"
        :visible="previewVisible"
        :destroy-on-close=true
        :width="1000"
        @cancel="closePreview"
        @ok="closePreview"
        cancelText="关闭">
      <office-file-preview v-if="fileType === 'office'" :file-path="rightKeyFilePath"></office-file-preview>
      <img width="600px" v-if="fileType === 'image'" :src="rightKeyFilePath" alt="图片预览">
      <p v-if="fileType === 'txt'">
        {{ fileContent }}
      </p>
    </a-modal>

    <a-modal
        title="创建文件夹"
        :visible="createFolderVisible"
        @ok="confirmCreateFolder"
        @cancel="cancelCreateFolder"
    >
      <a-input v-model="inputFolderName" placeholder="请输入文件夹名称"/>
    </a-modal>

    <a-modal
        title="重命名"
        :visible="renameVisible"
        @ok="confirmRename"
        @cancel="cancelRename"
    >
      <a-input v-model="inputPathName" placeholder="请输入新名称"/>
    </a-modal>

    <a-modal
        title="上传进度"
        :visible="uploadProgressVisible"
        :footer="null">
      <div class="progress-container">
        <div class="progress-bar" :style="'width:' + progressNumber + '%'"></div>
        <div class="progress-number">
          <div style="float: right">
            <span v-if="progressNumber !== 100">{{ progressNumber }}%</span>
            <a-icon v-else type="check-circle" theme="twoTone" two-tone-color="#52c41a"/>
          </div>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script>
import axios from 'axios'
import FileTree from "@/components/fileManage/fileTree";
import request from "@/utils/request";
import OfficeFilePreview from "@/components/fileManage/OfficeFilePreview";

export default {
  name: "fileManage",
  components: {
    OfficeFilePreview,
    FileTree
  },
  data() {
    return {
      fileTreeData: {},
      fileList: [],
      currentPath: '',
      inputPathName: '',
      createFolderVisible: false,
      renameVisible: false,
      showContextMenu: false,
      uploadProgressVisible: false,
      progressNumber: 0,
      isCreateRootFolder: false,
      contextMenuStyle: {
        top: 0,
        left: 0
      },
      inputFolderName: '',
      previewVisible: false,
      rootPath: '',
      // 拖动到的目标元素 path
      draggableTargetPath: '',
      // 右键的元素 http://xxx/xxx.jpg
      rightKeyFilePath: '',
      // 右键的元素 xxx/xxx.jpg
      rightKeyPath: '',
      fileContent: '',
      fileType: '',
      isDragging: false,
      startX: 0,
      startWidth: 0,
      leftWidth: 260
    }
  },
  created() {
    this.getFolderTree()
  },
  methods: {
    previewFile(filePath) {
      if (filePath) {
        filePath = this.removePathPrefix(filePath)
        this.rightKeyFilePath = `${request.BASE_URL}/file-manager/static/` + filePath
      }
      const fileFormat = this.rightKeyFilePath.split('.').pop().toLowerCase()
      let officeType = [
        'xls',
        'xlsx',
        'doc',
        'docx',
        'pdf',
        'xlsx'
      ]
      let imageType = [
        'png',
        'jpg',
        'jpeg',
        'gif'
      ]
      if (officeType.indexOf(fileFormat) !== -1) {
        this.fileType = 'office'
        this.previewVisible = true
      } else if ('txt' === fileFormat) {
        this.fileType = 'txt'
        this.getFileContent(this.rightKeyFilePath)
        this.previewVisible = true
      } else if (imageType.indexOf(fileFormat) !== -1) {
        this.fileType = 'image'
        this.previewVisible = true
      } else {
        this.$message.error('该文件不支持预览，请下载后查看')
      }
    },
    closePreview() {
      this.previewVisible = false
    },
    handleBreadcrumb(path, index) {
      if (index === -1) {
        this.currentPath = this.fileTreeData.filePath
        this.listFilesByPath(this.fileTreeData.filePath)
        return
      }
      let paths = this.currentPath.split('/')
      let realPath = ''
      for (let i = 0; i <= index; i++) {
        realPath += paths[i] + '/'
      }
      this.currentPath = realPath.split('/').slice(0, index + 1).join('/')
      this.listFilesByPath(realPath)
    },
    refresh() {
      this.getFolderTree()
    },
    showMenu(event, filePath, fileType) {
      this.fileType = fileType
      if (filePath) {
        this.rightKeyPath = filePath
        if (fileType !== 'path') {
          filePath = this.removePathPrefix(filePath)
          this.rightKeyFilePath = `${request.BASE_URL}/file-manager/static/` + filePath
        }
      } else {
        this.rightKeyFilePath = ''
        this.rightKeyPath = ''
      }
      event.preventDefault()
      this.showContextMenu = true
      this.contextMenuStyle = {
        top: `${event.clientY}px`,
        left: `${event.clientX}px`
      }
      document.addEventListener('click', this.closeMenu)
    },
    showMenuForLeftTree(event) {
      event.preventDefault()
      const dropZone = event.target
      const targetComponent = this.getComponentFromElement(dropZone)
      const targetVNode = targetComponent._vnode
      this.showMenu(event, targetVNode.context.$options.propsData.eventKey, 'path')
    },
    closeMenu() {
      this.showContextMenu = false
      document.removeEventListener('click', this.closeMenu)
    },
    handleDrop(event) {
      event.preventDefault()
      const files = event.dataTransfer.files
      if (files.length === 0) return
      const formData = new FormData()
      for (let i = 0; i < files.length; i++) {
        const file = files[i]
        formData.append('files', file)
      }
      this.uploadFiles(formData)
    },
    openFileDialog() {
      const input = document.createElement('input')
      input.multiple = true
      input.type = 'file'
      input.addEventListener('change', this.handleFileSelect)
      input.click()
    },
    handleFileSelect(event) {
      const files = event.target.files
      if (files.length === 0) return
      const formData = new FormData()
      for (let i = 0; i < files.length; i++) {
        const file = files[i]
        formData.append('files', file)
      }
      this.uploadFiles(formData)
    },
    uploadFiles(formData) {
      formData.append('path', this.removePathPrefix(this.currentPath))
      this.uploadProgressVisible = true
      axios.post(request.BASE_URL + '/file-manager/multi-upload', formData, {
        onUploadProgress: progressEvent => {
          this.progressNumber = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        },
      }).then(() => {
        this.listFilesByPath(this.currentPath)
        this.uploadProgressVisible = false
        this.$message.success('文件上传成功')
      }).catch(error => {
        this.$message.error('文件上传失败')
        this.uploadProgressVisible = false
        console.error('上传失败:', error);
      });
    },
    getFolderTree() {
      request.getAction('/file-manager/folder-tree').then(res => {
        if (res.success) {
          this.fileTreeData = res.data
          this.rootPath = res.data.filePath
        }
      })
    },
    getFileTypeName(type) {
      if (type) type = type.toLowerCase()
      let map = {
        'xls': '表格',
        'xlsx': '表格',
        'csv': '表格',
        'doc': '文档',
        'docx': '文档',
        'xmind': '思维导图',
        'ppt': '演示文稿',
        'pptx': '演示文稿',
        'pdf': 'PDF',
        'md': 'MD文档',
        'png': '图片',
        'jpg': '图片',
        'jpeg': '图片',
        'gif': '图片',
        'json': 'JSON文件',
        'txt': '文本',
        'zip': '压缩文档',
        'path': '文件夹',
        'xml': '配置文件',
        'log': '日志文件',
        'tmp': '临时文件'
      }
      return map[type]
    },
    getTypeIcon(type) {
      if (type) type = type.toLowerCase()
      let map = {
        'xls': 'file-excel',
        'xlsx': 'file-excel',
        'csv': 'file-excel',
        'doc': 'file-word',
        'docx': 'file-word',
        'md': 'file-markdown',
        'ppt': 'file-ppt',
        'pptx': 'file-ppt',
        'pdf': 'file-pdf',
        'png': 'file-image',
        'jpg': 'file-image',
        'jpeg': 'file-image',
        'gif': 'file-image',
        'txt': 'file-text',
        'zip': 'file-zip',
        'path': 'folder'
      }
      let res = map[type]
      if (!res) {
        return 'file-unknown'
      }
      return res
    },
    handleDbClick(item) {
      if (item.fileType === 'path') {
        this.currentPath = item.filePath
        this.listFilesByPath(item.filePath)
      }
    },
    listFilesByPath(path) {
      request.getAction('/file-manager/list-files', {path}).then(res => {
        if (res.success) {
          this.fileList = res.data
        }
      })
    },
    onSelect(keys, event) {
      let path = event.node._props.eventKey
      this.currentPath = path
      this.listFilesByPath(path)
    },
    dragStart(event) {
      event.dataTransfer.setData('text/plain', event.target.id)
    },
    dragEnd(filePath, fileName) {
      if (this.draggableTargetPath) {
        filePath = filePath.split('/').slice(1, filePath.split('/').length).join('/')
        let targetPath = this.removePathPrefix(this.draggableTargetPath)
        request.postAction('/file-manager/move-path', {path: filePath, targetPath, fileName}).then(res => {
          if (res.success) {
            this.getFolderTree()
            this.listFilesByPath(this.currentPath)
          } else {
            this.$message.error(res.message)
          }
        })
      }
    },
    dragOver(event) {
      event.preventDefault()
    },
    drop(event) {
      event.preventDefault()
      const dropZone = event.target
      const targetComponent = this.getComponentFromElement(dropZone)
      const targetVNode = targetComponent._vnode
      this.draggableTargetPath = targetVNode.context.$options.propsData.eventKey
    },
    getComponentFromElement(element) {
      let vm = element.__vue__
      while (!vm && element.parentNode) {
        element = element.parentNode
        vm = element.__vue__
      }
      return vm
    },
    async getFileContent(filePath) {
      const res = await axios.get(filePath)
      this.fileContent = res.data
    },
    createFolder() {
      this.createFolderVisible = true
      this.isCreateRootFolder = false
      this.inputFolderName = ''
    },
    createRootFolder() {
      this.createFolderVisible = true
      this.inputFolderName = ''
      this.isCreateRootFolder = true
    },
    confirmCreateFolder() {
      if (!this.inputFolderName) {
        this.$message.error('请输入文件夹名称')
      }
      this.createFolderVisible = false
      let folderPath = this.isCreateRootFolder ? this.inputFolderName : this.removePathPrefix(this.currentPath) + '/' + this.inputFolderName
      request.getAction(`/file-manager/create-folder`, {pathName: folderPath}).then(res => {
        if (res.success) {
          this.getFolderTree()
          this.listFilesByPath(this.currentPath)
        } else {
          this.$message.error(res.message)
        }
      })
    },
    cancelCreateFolder() {
      this.createFolderVisible = false
    },
    removePath() {
      request.getAction('/file-manager/remove-path', {pathName: this.removePathPrefix(this.rightKeyPath)}).then(res => {
        if (res.success) {
          this.getFolderTree()
          this.listFilesByPath(this.currentPath)
        } else {
          this.$message.error(res.message)
        }
      })
    },
    renamePath() {
      this.renameVisible = true
      this.inputPathName = this.rightKeyPath.split('/').pop()
    },
    confirmRename() {
      request.getAction('/file-manager/rename-path', {
        pathName: this.removePathPrefix(this.rightKeyPath),
        newName: this.inputPathName
      }).then(res => {
        if (res.success) {
          this.renameVisible = false
          this.getFolderTree()
          this.listFilesByPath(this.currentPath)
        } else {
          this.$message.error(res.message)
        }
      })
    },
    cancelRename() {
      this.renameVisible = false
    },
    downloadFile(filePath) {
      if (filePath) {
        filePath = this.removePathPrefix(filePath)
        const a = document.createElement('a');
        a.href = `${request.BASE_URL}/file-manager/download/` + filePath
        a.download = this.rightKeyFilePath.split('/').pop()
        a.click();
      }
    },
    onSearch(value) {
      if (!value) {
        this.listFilesByPath(this.currentPath)
        return
      }
      request.getAction(`/file-manager/file-search/${value}`).then(res => {
        if (res.success) {
          this.fileList = res.data
        }
      })
    },
    // fileStorage/xx/xx.txt -> /xx/xx.txt
    removePathPrefix(path) {
      return path.replace(this.rootPath, '')
    },
    startDragging(event) {
      this.isDragging = true;
      this.startX = event.clientX;
      this.startWidth = this.leftWidth;

      document.addEventListener('mousemove', this.resizePanels);
      document.addEventListener('mouseup', this.stopDragging);
    },
    resizePanels(event) {
      if (this.isDragging) {
        const deltaX = event.clientX - this.startX;
        this.leftWidth = Math.max(this.startWidth + deltaX, 50); // 设置最小宽度
      }
    },
    stopDragging() {
      this.isDragging = false;
      document.removeEventListener('mousemove', this.resizePanels);
      document.removeEventListener('mouseup', this.stopDragging);
    }
  },
  destroyed() {
    document.removeEventListener('click', this.closeMenu)
  }
}
</script>

<style lang="less" scoped>
.file-operate-container {
  display: flex;
  justify-content: space-between;
  overflow-y: auto;
  padding: 10px;

  .left-menu {
    height: calc(100vh - 30px);
    overflow-y: auto;
    width: 260px;
    background-color: #fff;
    border: 1px solid #e8e8e8;

    .nav-left {
      width: 100%;
      height: 50px;
      border-bottom: 1px solid #e8e8e8;
      line-height: 50px;
      padding: 0 10px;
      background-color: #fff;
      border-right: 1px solid #e8e8e8;

      .operator-btn {
        cursor: pointer;
      }
    }
  }

  .right-container {
    height: calc(100vh - 30px);
    overflow-y: auto;
    flex: 1;
    border: 1px solid #e8e8e8;

    .nav-right {
      padding: 0 10px;
      height: 50px;
      border-bottom: 1px solid #e8e8e8;
      background-color: #fff;
      display: flex;
      justify-content: space-between;

      .breadcrumb {
        display: flex;
        align-items: center;
      }

      .search {
        display: flex;
        align-items: center;
      }
    }

    .container-title {
      width: 100%;
      height: 35px;
      background-color: #f1f9ff;
      display: flex;
      padding: 0 10px;
      border-bottom: 1px solid #5ccce7;

      .group-title {
        height: 35px;
        line-height: 35px;
        display: inline-block;
      }
    }

    .file-item {
      width: 100%;
      height: 35px;
      display: flex;
      padding: 0 10px;
      border-bottom: 1px solid #edeff0;

      .file-title {
        height: 35px;
        line-height: 35px;
        display: inline-block;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }
    }

    .file-item:hover {
      background-color: #edeff5;
      cursor: pointer;
    }

    .file-name {
      width: 30%;
    }

    .upload-date {
      margin-left: 5px;
      width: 25%;
    }

    .file-type {
      width: 15%;
    }

    .file-size {
      width: 15%;
    }

    .file-operate {
      width: 10%;
    }
  }
}

.breadcrumb {
  .pointer {
    cursor: pointer;
    margin-left: 5px;
  }

  .pointer:hover {
    color: #1890ff;
  }

  .separator {
    color: #b2b2b2;
    margin-left: 5px;
  }
}

.custom-menu {
  position: absolute;
  background-color: #FFF;
  padding: 5px 0;
  width: 200px;
  box-shadow: 2px 2px 4px rgba(0, 0, 0, 0.2);
  z-index: 1000;
}

.custom-menu-item {
  padding: 8px 12px;
  cursor: pointer;
}

.custom-menu-item:hover {
  background-color: #edeff5;
}

.progress-container {
  .progress-bar {
    height: 10px;
    background-color: green;
  }

  .progress-number {
    width: 100%;
  }
}

#divider {
  width: 3px;
  cursor: col-resize;
  background: #ccc;
}

/deep/ .ant-card-body {
  padding: 0;
}
</style>
