# 部署到 Railway 指南

本指南介绍如何将 Campus Health Platform 部署到 Railway。

## 前提条件

1. Railway 账户（免费注册：https://railway.app）
2. GitHub 账户
3. Docker Desktop（可选，本地测试）

## 部署步骤

### 方式一：使用 Railway 网页界面（推荐，最快）

1. **登录 Railway**
   - 访问 https://railway.app
   - 点击 "Start a New Project"

2. **选择部署来源**
   - 选择 "Deploy from GitHub"
   - 授权 GitHub 并选择 `YanMo5/shuzishaobing` 仓库

3. **Railway 自动检测配置**
   - Railway 将识别 `docker-compose.yml`
   - 自动创建后端和前端服务，以及 PostgreSQL 数据库

4. **配置环境变量**
   
   在 Railway 仪表板中为以下服务设置环境变量：
   
   **PostgreSQL 服务（自动配置）：**
   - 已预设（无需手动修改）
   
   **Backend 服务：**
   ```
   SPRING_DATASOURCE_URL=postgresql://postgres:5432/campus_health
   SPRING_DATASOURCE_USERNAME=campus_health
   SPRING_DATASOURCE_PASSWORD=[从 PostgreSQL 获取]
   CAMPUS_PLATFORM_OUTBOX_DISPATCH_INTERVAL_MS=1000
   SERVER_PORT=8080
   ```
   
   **Frontend 服务：**
   ```
   VITE_API_BASE_URL=https://[backend-railway-url]
   NODE_ENV=production
   ```

5. **部署**
   - 点击 "Deploy"
   - 等待 3-5 分钟部署完成
   - 获得生成的公开 URL

6. **访问应用**
   - Frontend URL: `https://[railway-frontend-url]`
   - Backend API: `https://[railway-backend-url]:8080`

### 方式二：使用 Railway CLI（适合开发者）

1. **安装 Railway CLI**
   ```powershell
   npm install -g @railway/cli
   ```

2. **登录**
   ```powershell
   railway login
   ```

3. **初始化项目**
   ```powershell
   cd "F:\数字哨兵\campus-health-platform"
   railway init
   ```

4. **连接 GitHub 仓库**
   - 按照 CLI 提示进行

5. **设置环境变量**
   ```powershell
   railway env SPRING_DATASOURCE_PASSWORD your_password
   railway env POSTGRES_PASSWORD your_password
   ```

6. **部署**
   ```powershell
   railway up
   ```

## 常见问题与解决方案

### Q1: 部署失败，提示"Docker 镜像过大"

**解决方案：**
- 删除本地 `backend/target/` 目录
- 创建 `.dockerignore` 文件，忽略大文件：
  ```
  target/
  node_modules/
  .git/
  .gitignore
  ```

### Q2: 前端无法连接后端

**原因：** CORS 或 API 基础 URL 错误

**解决方案：**
1. 检查前端 `.env` 中的 `VITE_API_BASE_URL` 是否指向正确的后端 URL
2. 在后端添加 CORS 配置（`backend/src/main/java/.../config/CorsConfig.java`）：
   ```java
   @Configuration
   public class CorsConfig {
       @Bean
       public WebMvcConfigurer corsConfigurer() {
           return new WebMvcConfigurer() {
               @Override
               public void addCorsMappings(CorsRegistry registry) {
                   registry.addMapping("/api/**")
                       .allowedOrigins("*")
                       .allowedMethods("GET", "POST", "PUT", "DELETE");
               }
           };
       }
   }
   ```

### Q3: 数据库连接失败

**原因：** PostgreSQL 密码或 URL 不匹配

**解决方案：**
1. 在 Railway 仪表板中，找到 PostgreSQL 服务
2. 复制连接字符串
3. 更新所有后端服务的 `SPRING_DATASOURCE_URL` 和密码

### Q4: 如何查看部署日志？

**方法：**
- 在 Railway 仪表板点击相应服务
- 点击 "Deployments" 标签
- 查看实时日志

## 成本估计

Railway 免费层额度：
- $5/月额度（各项服务共用）
- 超出后按使用量计费

本项目估计月成本：
- PostgreSQL: ~$1/月
- Backend (Java): ~$2-3/月
- Frontend (Static): ~$0.5/月
- **总计：约 $3-5/月**

## 下一步

1. ✅ 部署完成后，访问前端 URL
2. 📊 测试 API 端点（健康检查、数据查询）
3. 🔧 监控日志和性能指标
4. 📈 根据需要扩展资源

## 回滚和更新

### 更新代码
```powershell
git push origin main
# Railway 自动检测并重新部署
```

### 回滚到前一个版本
- 在 Railway 仪表板选择 "Deployments"
- 点击要回滚的版本并选择 "Redeploy"

---

**需要帮助？** 查看 Railway 官方文档：https://docs.railway.app
