import { existsSync } from 'node:fs'
import { execSync } from 'node:child_process'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const projectRoot = resolve(__dirname, '..')
const nodeModulesDir = resolve(projectRoot, 'node_modules')
const requiredPackages = [
  'vue',
  'vue-router',
  'pinia',
  'element-plus',
  'axios',
  'vite',
  '@vitejs/plugin-vue'
]

const missing = requiredPackages.filter((pkg) => !existsSync(resolve(nodeModulesDir, pkg)))

if (missing.length > 0) {
  console.log(`\n[ensure-deps] 检测到缺失依赖: ${missing.join(', ')}`)
  console.log('[ensure-deps] 正在执行 `npm install` 以补全依赖...')
  try {
    execSync('npm install', { stdio: 'inherit', cwd: projectRoot })
    console.log('\n[ensure-deps] 依赖安装完成。')
  } catch (error) {
    console.error('\n[ensure-deps] 自动安装依赖失败，请手动运行 `npm install`。')
    throw error
  }
}
