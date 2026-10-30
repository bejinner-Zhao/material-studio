# Material Studio · 素材制作网站

小影科技全栈笔试题实现。用户浏览模板、上传图片、提交制作任务，任务在后台分三步异步执行，前端跟踪状态并查看历史结果。

> 当前状态：**主流程完成**。前后端与容器编排均已实现，模板浏览、上传、提交、后台三段制作、轮询与历史记录可以串成闭环。
>
> 仓库：https://github.com/bejinner-Zhao/material-studio ｜ 验收 commit：`730eedf`（GitHub Actions 已通过）

---

## 技术栈

| 层 | 选型 | 版本 |
|---|---|---|
| 前端 | Next.js App Router + React + TypeScript | Next 15.5.28 / React 19 / TS 5.7 |
| 前端数据层 | TanStack Query | 5.x |
| 样式 | Tailwind CSS | 3.4 |
| 后端 | Java + Spring Boot + MyBatis | Java 17 / Spring Boot 3.3.4 / MyBatis Starter 3.0.3 |
| 数据库 | MySQL | 8.4（镜像 `mysql:8.4`） |
| 构建 | Maven | 3.9 |

## 目录结构

```
material-studio/
├─ docker-compose.yml        MySQL + 后端 + 前端
├─ .env.example              环境变量样例（无真实凭据）
├─ db/init/
│  ├─ 01-schema.sql          建表
│  └─ 02-seed.sql            模板与演示用户种子数据
├─ backend/
│  ├─ Dockerfile             多阶段构建（maven → jre 17）
│  ├─ pom.xml
│  └─ src/main/
│     ├─ java/com/xiaoying/materialstudio/
│     │  ├─ common/          统一错误响应
│     │  ├─ config/          存储目录与静态资源映射
│     │  ├─ mapper/          MyBatis Mapper 接口
│     │  └─ template/        模板查询（model / service / controller）
│     └─ resources/
│        ├─ application.yml
│        ├─ mapper/*.xml
│        └─ static/demo/     模板封面与预置结果图
└─ frontend/
   ├─ Dockerfile            多阶段构建（standalone 输出）
   ├─ app/
   │  ├─ page.tsx           模板列表 + 分类筛选
   │  ├─ templates/[id]/    详情、上传、History
   │  └─ history/           全部个人记录
   ├─ components/           卡片、上传位、任务卡等
   └─ lib/                  api 客户端、查询 hooks、类型
```

## 启动

### Docker（推荐）

```bash
copy .env.example .env
docker compose up --build
```

- 前端：http://localhost:3000
- 后端：http://localhost:8080
- 健康检查：http://localhost:8080/actuator/health
- MySQL：localhost:3306，库名 `material_studio`

首次启动时 MySQL 会自动执行 `db/init` 下的建表与种子脚本。要重新初始化：`docker compose down -v` 后再启动。

### 本地运行（需要 JDK 17 与 MySQL 8）

```bash
mvn -f backend/pom.xml spring-boot:run
```

数据库连接通过环境变量覆盖，默认值见 `application.yml`：`DB_HOST`、`DB_PORT`、`DB_NAME`、`DB_USER`、`DB_PASSWORD`。建表脚本需手动在 MySQL 中执行 `db/init/01-schema.sql` 与 `02-seed.sql`。

上传素材与生成结果写入 `app.storage.root`（默认 `./data`，Docker 中为 `/data`），通过 `/files/**` 对外提供。

### 前端本地运行（需要后端已启动）

```bash
cd frontend
npm install
npm run dev
```

国内网络访问 npm 官方源较慢时，可加镜像：`npm install --registry=https://registry.npmmirror.com`。首次安装后请把生成的 `package-lock.json` 一并提交，CI 与 Docker 构建会优先使用它。

默认打开 http://localhost:3000。前端不直接请求后端域名，而是由 `next.config.ts` 的 rewrites 把 `/api/**`、`/files/**`、`/demo/**` 转发到 `BACKEND_ORIGIN`（默认 `http://localhost:8080`），因此没有跨域问题，Cookie 也按同源处理。

## 前端实现要点

| 关注点 | 做法 |
|---|---|
| 服务端状态 | TanStack Query 统一管理模板与任务，组件不自己维护请求状态 |
| 轮询生命周期 | 任务列表的 `refetchInterval` 是函数：缓存里还有 `PENDING` / `RUNNING` 才返回 2 秒，全部结束返回 `false`；页面不可见时不跑 |
| 请求异常 vs 任务失败 | 任务卡片只依据 `task.status` 渲染；查询失败只在面板上方提示「暂时无法获取任务状态」并保留上一次数据，不会把任务标成失败 |
| 多任务隔离 | 每个任务按 id 单独渲染，服务端只更新对应行；缓存键带上 `userId`，切换身份后不会读到别人的缓存 |
| 提交后立即反馈 | `POST /api/tasks` 返回后直接 `setQueryData` 把新任务插到列表头部，按钮从「提交中」恢复，History 立刻出现处理中卡片 |
| 上传状态 | 每个上传位独立状态机 `empty → uploading → ready / error`，支持替换与移除；替换时会删除服务端旧素材，避免孤儿文件 |
| 预览地址 | 本地预览用 `createObjectURL`，替换、移除、提交成功、组件卸载时统一 `revokeObjectURL` |
| 刷新恢复 | 任务状态全部在后端，刷新后重新拉取列表，未结束的任务自动恢复轮询 |

## 接口

### 模板列表

`GET /api/templates?category={分类}`

`category` 省略、为空、`all` 或 `全部` 时返回全部模板。响应：

```json
{
  "categories": ["创意图片", "节日贺卡", "婚纱写真", "萌宠写真"],
  "templates": [
    {
      "id": "demo-postcard",
      "title": "旅行明信片",
      "category": "创意图片",
      "coverUrl": "/demo/covers/postcard.svg",
      "description": "上传两张照片，模拟合成一张带边框与印章的旅行明信片。",
      "requiredImageCount": 2,
      "acceptedMimeTypes": ["image/jpeg", "image/png"],
      "maxFileSizeBytes": 5242880
    }
  ]
}
```

### 模板详情

`GET /api/templates/{id}` → 与列表中的单个模板对象相同；不存在返回 `404` 与 `{"code":"NOT_FOUND","message":"模板不存在：xxx"}`。

### 素材上传

`POST /api/uploads`，`multipart/form-data`，字段 `templateId` + `file`。成功返回 `201`：

```json
{ "id": 12, "url": "/files/uploads/1/6f3a....jpg", "mimeType": "image/jpeg", "sizeBytes": 183244, "originalName": "pet.jpg" }
```

服务端依次校验：模板存在 → 文件非空 → 大小不超过模板 `maxFileSizeBytes` → **按文件头识别真实图片类型**（不信任客户端声明的 Content-Type）→ 类型在模板 `acceptedMimeTypes` 白名单内。任一条不满足返回 `400` 与可读原因。

图片落盘到 `{STORAGE_ROOT}/uploads/{userId}/{uuid}.{ext}`，通过 `/files/**` 访问。

### 素材删除

`DELETE /api/uploads/{id}` → `204`。只能删自己的素材，越权或不存在返回 `404`。

### 提交制作任务

`POST /api/tasks`，JSON：

```json
{ "templateId": "demo-postcard", "assetIds": [12, 13] }
```

校验通过后**立即返回** `202` 与任务快照，不等待制作完成：

```json
{
  "id": 7,
  "templateId": "demo-postcard",
  "templateTitle": "旅行明信片",
  "templateCoverUrl": "/demo/covers/postcard.svg",
  "status": "PENDING",
  "stage": null,
  "errorStage": null,
  "errorMessage": null,
  "resultUrl": null,
  "createdAt": "2026-10-29T15:20:11",
  "startedAt": null,
  "finishedAt": null
}
```

受理前校验：模板存在 → 素材数量等于模板要求 → 素材不重复 → 素材属于当前用户且与所选模板匹配。任一不满足返回 `400`。

### 查询任务

| 接口 | 说明 |
|---|---|
| `GET /api/tasks/{id}` | 单个任务快照，只读，不会推进任务 |
| `GET /api/tasks?page=1&size=20` | 个人历史，按创建时间倒序 |
| `GET /api/tasks/active` | 未结束任务（`PENDING` / `RUNNING`），供刷新后恢复跟踪 |

## 任务状态与后台执行

整体状态与执行阶段是两个独立字段：

- `status`：`PENDING`（等待处理）→ `RUNNING`（处理中）→ `SUCCEEDED` / `FAILED`
- `stage`：`MATERIAL_CHECK`（素材检查）→ `GENERATING`（模拟生成）→ `FINALIZING`（结果整理）

执行链路：

1. 提交接口只写 `creation_task` 与 `task_asset`，状态 `PENDING`。
2. `TaskDispatcher` 每 300ms 扫描 `PENDING` 任务；先确认线程池有空闲线程，再用
   `UPDATE ... WHERE id=? AND status='PENDING'` 原子抢占，抢到的才交给线程池。
3. `TaskWorker` 顺序执行三个阶段，每阶段开始与结束各写一条 `task_stage_log`；阶段之间按
   `app.simulator.stage-delay-ms` 等待。任一步失败即停止，写入 `FAILED` 与失败阶段、原因。
4. 所有状态更新都带 `WHERE status IN ('PENDING','RUNNING')`，进入终态后不会被后续操作改写。

任务执行只有一个实例内的线程池（`max-concurrent` 默认 2），超出部分留在库里排队。

### 演示失败与耗时

配置项（`application.yml` / 环境变量）：

| 配置 | 默认 | 说明 |
|---|---|---|
| `app.simulator.fail-stage` | `NONE` | 指定阶段必定失败：`MATERIAL_CHECK` / `GENERATING` / `FINALIZING` |
| `app.simulator.stage-delay-ms` | `3000` | 每阶段模拟耗时 |
| `app.simulator.max-concurrent` | `2` | 同时执行的任务数 |

运行期也可以直接改，不必重启（`app.simulator.dev-api-enabled` 为 `true` 时开放）：

```bash
curl -X POST http://localhost:8080/api/dev/simulator -H "Content-Type: application/json" -d '{"failStage":"GENERATING","stageDelayMs":1500}'
```

之后提交的任务都会在「模拟生成」阶段失败；把 `failStage` 传 `NONE` 即恢复正常。

## 用户身份

不做账号体系。首次访问 `/api/**` 时后端生成 UUID 写入 HttpOnly Cookie `ms_uid`，并在 `app_user` 建一条记录；此后所有接口按该用户隔离数据。

| 接口 | 说明 |
|---|---|
| `GET /api/me` | 返回当前身份的 id 与昵称；前端用它给查询缓存编排键 |
| `POST /api/me/reset` | 签发新的匿名身份并覆盖 Cookie，返回新身份 |

页面右上角有「切换身份」按钮：调用 `/api/me/reset` 后清空前端缓存并重载页面，所有在途请求与轮询随页面一起结束，不会出现旧身份的数据覆盖新身份。原身份的历史记录仍保留在数据库中，用开发者工具把 `ms_uid` Cookie 改回旧值并刷新即可看到。

## 演示路径

1. **成功**：首页选「旅行明信片」→ 上传两张 jpg/png → `Create Now` → 右侧出现 `Processing…` → 约 9 秒后变为结果图 → 刷新页面，记录仍在。
2. **指定阶段失败**：`POST /api/dev/simulator` 传 `{"failStage":"GENERATING"}` → 再提交一次 → 卡片显示失败原因与「失败于『模拟生成』」→ 传 `NONE` 恢复正常。
3. **调整耗时**：`{"stageDelayMs":8000}` 让处理中状态持续更久，便于观察阶段切换。
4. **多任务**：连续提交三次；`max-concurrent=2` 时第三个先显示「等待处理」，有空闲线程后才进入处理中。
5. **刷新恢复**：提交后立刻刷新页面，任务继续展示并自动恢复轮询。
6. **记录隔离**：点右上角「切换身份」，历史随即清空并换成一个新用户；把 `ms_uid` Cookie 改回旧值再刷新，原记录仍在。

## 测试

后端（`mvn test`，12 个用例，不需要数据库）：

- `TaskWorkerTest`：三阶段严格按顺序执行、指定阶段失败后不再执行后续阶段、素材文件缺失在检查阶段终止、终态任务不重复执行。
- `TaskServiceTest`：模板不存在、素材数量不符、素材重复、素材属于他人、素材来自其他模板、成功受理并保存模板快照。
- `MeControllerTest`：切换身份下发新的 HttpOnly Cookie、两次切换得到不同身份。

前端（`npm run test`，Vitest + Testing Library）：

- `polling.test.ts`：有未完成任务时按间隔轮询、全部结束即停止、无数据不轮询。
- `TaskBoard.test.tsx`：三个不同状态的任务各自渲染互不串扰；查询异常时提示「暂时无法获取任务状态」而不会把进行中的任务标成失败。

## 部署与回滚

部署：

1. 复制 `.env.example` 为 `.env`，按需修改数据库密码等（示例值仅供本地演示）。
2. `docker compose up -d --build`。
3. 首次启动自动执行 `db/init` 建表与种子数据；之后重启不会重复执行。
4. 检查 `http://localhost:8080/actuator/health` 与前端首页。

回滚：

1. 代码回退到上一个可用 commit/tag，执行 `docker compose up -d --build` 重建即可。
2. 本项目只做增量建表，旧版本代码可兼容新增的列；若要回退表结构，需手工执行对应的逆向 SQL。
3. 数据卷 `mysql-data`、`backend-data` 不会因重建容器丢失；`docker compose down -v` 会清空数据库与上传文件，仅在确认可丢弃时使用。

## 关于 AI 工具

本项目开发过程使用了 AI 编程助手（Claude Code），工作方式如下：

- **拆解**：先按评分项把需求拆成「后端异步执行 / 前端异步状态 / 上传交互 / 身份与记录 / 工程交付」五块，再逐块实现，避免一次性生成大段难以核对的代码。
- **给上下文**：把接口约定、状态机定义、数据表结构先写成文档（本 README），再让 AI 按约定写代码，减少前后端字段对不上的情况。
- **验证**：每完成一块就编译（`mvn package`）、跑单元测试（`mvn test`），前端则跑 `tsc --noEmit`、`next lint`、`next build`。
- **修正的一个具体例子**：最初的全局异常处理器直接捕获 `Exception` 并返回 500，导致访问不存在的静态资源（本应是 404）也变成 500。发现后改为先判断异常是否实现 Spring 的 `ErrorResponse`，是则透出它自带的状态码与描述，只对真正的未知异常返回 500。

未使用 AI 的地方：需求取舍、状态机设计、演示方案与已知限制的判断由本人决定，AI 负责按这些决定产出与修改代码。

## 数据模型

| 表 | 用途 |
|---|---|
| `template` | 模板定义（分类、封面、所需图片数、MIME 白名单、大小上限） |
| `app_user` | 轻量匿名用户（Cookie 携带 `public_id`） |
| `upload_asset` | 用户上传的素材，落盘 + 入库 |
| `creation_task` | 制作任务，含状态、阶段、失败信息、结果地址与模板快照 |
| `task_asset` | 任务与素材的关联 |
| `task_stage_log` | 各阶段执行日志，用于证明真实的后台执行边界 |

## 进度

| 项 | 状态 |
|---|---|
| docker-compose（MySQL + 后端 + 前端） | 已完成 |
| 建表脚本与种子数据 | 已完成 |
| 模板列表 / 详情接口 | 已完成 |
| 用户身份（Cookie 匿名用户）与记录隔离 | 已完成 |
| 素材上传、类型与大小校验、删除 | 已完成 |
| 任务提交、三段异步执行、状态 / 历史查询 | 已完成 |
| 失败注入与耗时调整 | 已完成 |
| 前端：模板列表、分类筛选、详情与上传交互 | 已完成 |
| 前端：轮询、异常态、多任务、刷新恢复、历史记录 | 已完成 |
| 后端单元测试（步骤顺序 / 失败中断 / 提交校验 / 身份切换） | 已完成，12 个用例通过 |
| 前端单元测试（轮询策略 / 多任务 / 异常区分） | 已完成，6 个用例通过 |
| GitHub Actions CI | 已完成，远端实跑通过（前端 2m22s / 后端 24s） |
| README（AI 使用说明、部署与回滚） | 已完成 |

## 素材与依赖来源

- 模板封面（`backend/src/main/resources/static/demo/covers/*.svg`）与预置结果图（`.../results/*.svg`）均为本项目自绘的 SVG 占位图，不含第三方素材，可自由替换。
- 未使用前端脚手架（未执行 `create-next-app`），Next.js 工程与配置按官方文档手写。
- 后端依赖：Spring Boot 3.3.4、MyBatis Spring Boot Starter 3.0.3、MySQL Connector/J（版本由 Spring Boot BOM 管理），见 `backend/pom.xml`。
- 前端依赖：Next 15.5.28、React 19、TanStack Query 5、Tailwind CSS 3.4，见 `frontend/package.json`。
- 没有引入任何开源项目代码或第三方素材，因此不涉及额外的许可声明。

## 设计取舍

| 决策 | 选择 | 理由与代价 |
|---|---|---|
| 状态刷新 | 轮询（2 秒）而非 SSE / WebSocket | 实现与清理都简单，刷新页面天然恢复；代价是空转请求，但只在有未完成任务时发生 |
| 异步执行 | 数据库当队列 + 本地线程池 | 状态与任务同一份事务数据，不会出现「消息队列里没了但库里还是处理中」；代价是吞吐上限受单机限制，跨实例要靠原子抢占 |
| 状态与阶段 | 两个独立字段 | 状态是结论、阶段是过程；拆开后「失败在第几步」不会丢。代价是前端要多读一个字段 |
| 上传时机 | 先落盘入库，再凭 assetId 建任务 | 服务端能校验、能重放，不依赖浏览器临时地址；代价是失败任务会留下孤儿素材文件 |
| 失败模拟 | 后端配置 + dev 接口 | 不进正式页面，符合题目「不必出现在用户界面」，又能现场切换 |
| 用户身份 | HttpOnly Cookie 匿名 UUID | 不考身份服务，但又需要记录隔离；代价是没有账号体系 |

## 已知限制

- **重启不续跑**：进程重启时，处于 `RUNNING` 的任务会在启动自检中标记为 `FAILED`（原因写明「服务重启，任务执行中断」），不会自动续跑；`PENDING` 任务无需特殊处理，新进程的调度器会照常取走。
- **单实例假设**：任务的原子抢占本身支持多实例，但启动自检会把其他实例正在执行的任务误判为中断，多实例部署需要额外的心跳机制。
- **结果是预置图**：制作结果是 4 张预置图轮转拷贝，不做真实图像处理。
- **未做账号体系**：身份是 Cookie 中的匿名 UUID，没有登录、找回、注销等能力。

## 实际投入

待填写：建议按「需求拆解 / 后端 / 前端 / 测试与文档」四段分别记录实际耗时。

## 验证状态

- 构建与单测：后端 `mvn test` 12 个用例、前端 `vitest run` 6 个用例全部通过；`tsc --noEmit`、`next lint`、`next build` 均无报错。
- 后端运行时：已在 MySQL 8.0.31 + JDK 17 上实跑通过——模板查询、上传与文件头校验、三阶段异步执行、失败注入、多任务排队、身份隔离均验证；`task_stage_log` 的实际顺序与单元测试一致。
- 前端运行时：已在浏览器实际操作——分类筛选、上传、提交、轮询、失败卡片、刷新恢复、切换身份均正常，桌面与移动两种宽度都检查过。
- 容器：**未验证**。开发机未安装 Docker，`docker compose up --build` 未执行过。
- CI：**已通过**。push 触发 GitHub Actions，后端 `mvn verify`（24s）与前端 `npm ci` → `lint` → `typecheck` → `test` → `build`（2m22s）两个 job 均为 success。
