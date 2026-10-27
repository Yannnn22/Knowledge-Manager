// 通用组件导出文件
import Loading from './Loading.vue'
import ErrorBoundary from './ErrorBoundary.vue'
import Empty from './Empty.vue'
import ConfirmDialog from './ConfirmDialog.vue'
import type { App } from 'vue'

// 导出所有通用组件
export {
  Loading,
  ErrorBoundary,
  Empty,
  ConfirmDialog
}

// 默认导出
export default {
  Loading,
  ErrorBoundary,
  Empty,
  ConfirmDialog
}

// 组件安装函数（可选，用于全局注册）
export const installCommonComponents = (app: App) => {
  app.component('Loading', Loading)
  app.component('ErrorBoundary', ErrorBoundary)
  app.component('Empty', Empty)
  app.component('ConfirmDialog', ConfirmDialog)
}

// 类型定义（Vue组件的Props接口无法直接导出，需要在使用时重新定义）