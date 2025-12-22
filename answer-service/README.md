# Quiz System

基于 Spring Boot 3 + MyBatis + JWT + BCrypt 的答题系统后端。

## 项目结构

```
quiz/
├─ pom.xml
├─ api-doc.json                 # OpenAPI 3.0 接口文档（可导入Apifox）
└─ src/main/
   ├─ java/com/djy/quiz/
   │   ├─ QuizApplication.java
   │   ├─ config/               # WebConfig, PasswordConfig
   │   ├─ constant/             # RoleConstant
   │   ├─ controller/           # UserController, QuizController, AdminController
   │   ├─ exception/            # GlobalExceptionHandler
   │   ├─ filter/               # JwtFilter
   │   ├─ mapper/               # MyBatis Mapper接口
   │   ├─ pojo/dto/             # DTO
   │   ├─ pojo/model/           # Model
   │   ├─ pojo/vo/              # VO
   │   ├─ response/             # Result 统一响应体
   │   ├─ service/              # 业务接口
   │   ├─ service/impl/         # 业务实现
   │   └─ util/                 # JwtUtil
   └─ resources/
       ├─ application.yml
       └─ mapper/               # MyBatis XML
```

## 技术栈

- JDK 17
- Spring Boot 3.3.x
- MyBatis 3.x
- MySQL 8.x
- JWT (jjwt 0.11.5)
- BCrypt（spring-security-crypto）
- Lombok

## 快速开始

### 1. 创建数据库

执行 `数据库.sql` 脚本创建 `quiz` 库及表结构。

### 2. 修改数据库配置

编辑 `src/main/resources/application.yml`，确认数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/quiz?useSSL=false&useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: 123456
```

### 3. 构建并运行

```bash
cd quiz
mvn clean package -DskipTests
java -jar target/quiz-0.0.1-SNAPSHOT.jar
```

或使用 IDEA 直接运行 `QuizApplication.java`。

### 4. 接口文档

项目根目录下 `api-doc.json` 可直接导入 Apifox / Postman / Swagger Editor。

## 接口概览

### 用户模块（无需token）

| 方法 | 路径                | 说明     |
|------|---------------------|----------|
| POST | /api/user/register  | 用户注册 |
| POST | /api/user/login     | 用户登录 |

### 用户模块（需token）

| 方法 | 路径               | 说明           |
|------|--------------------|----------------|
| GET  | /api/user/info     | 获取当前用户信息 |
| POST | /api/user/logout   | 退出登录       |

### 答题模块（需token）

| 方法 | 路径                     | 说明             |
|------|--------------------------|------------------|
| GET  | /api/quiz/questions      | 获取题目列表     |
| GET  | /api/quiz/questions/{id} | 获取单个题目     |
| POST | /api/quiz/submit         | 提交答案         |
| GET  | /api/quiz/history        | 获取用户答题记录 |

### 管理员模块（需token，角色=1）

| 方法   | 路径                        | 说明               |
|--------|-----------------------------|--------------------|
| GET    | /api/admin/users            | 获取用户列表       |
| GET    | /api/admin/users/{id}       | 获取用户信息       |
| PUT    | /api/admin/users/{id}       | 更新用户           |
| DELETE | /api/admin/users/{id}       | 删除用户（软删除） |
| GET    | /api/admin/questions        | 获取题目列表       |
| POST   | /api/admin/questions        | 新增题目           |
| GET    | /api/admin/questions/{id}   | 获取单个题目       |
| PUT    | /api/admin/questions/{id}   | 更新题目           |
| DELETE | /api/admin/questions/{id}   | 删除题目（软删除） |
| GET    | /api/admin/history          | 获取答题记录列表   |
| GET    | /api/admin/history/{id}     | 获取单条答题记录   |
| DELETE | /api/admin/history/{id}     | 删除答题记录       |

