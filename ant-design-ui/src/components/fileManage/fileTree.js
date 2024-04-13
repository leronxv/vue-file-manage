export default {
  name: "fileTree",
  props: {
    treeData: {
      type: Array,
      default() {
        return []
      }
    }
  },
  data() {
    return {

    }
  },
  methods: {
    renderTreeNode(data) {
      return data.map(item => (
        <a-tree-node key={item.filePath} title={item.fileName}>
          {item.child && item.child.length > 0 ? this.renderTreeNode(item.child) : null}
        </a-tree-node>
      ))
    },
  },
  render() {
    return (
      <a-directory-tree
        on-select={(keys, event) => this.$emit('select', keys, event)}>
        { this.renderTreeNode(this.treeData) }
      </a-directory-tree>
    )
  }
}
