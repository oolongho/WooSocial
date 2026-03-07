# WooSocial

🍵一款简洁、多功能的 Minecraft 社交插件

## 特色

### 👥 好友系统
- **好友管理**：添加、删除、列表查看
- **好友请求**：发送、接受、拒绝好友请求
- **上线提醒**：好友上线时自动通知
- **屏蔽功能**：屏蔽指定玩家

### 🚀 传送系统
- **好友传送**：传送到好友位置
- **传送倒计时**：可配置倒计时时间
- **传送冷却**：防止频繁传送
- **权限控制**：允许/禁止好友传送

### 🖥️ GUI界面
- **社交菜单**：`/social` 打开总菜单
- **好友列表**：分页显示，在线状态
- **好友详情**：传送、设置、删除等操作
- **传送设置**：可视化权限管理

### 🌐 跨服同步
- **BungeeCord**：Plugin Message Channel
- **Velocity**：Plugin Message Channel
- **Redis**：Pub/Sub 发布订阅
- **MySQL**：轮询同步（兜底方案）

### 💾 数据持久化
- **SQLite**：单机模式默认存储
- **MySQL**：支持跨服数据共享
- **HikariCP**：高性能连接池

## 环境

- Minecraft 1.21+
- Java 21+
- Paper 核心（推荐）

## 命令

### 主命令

| 命令 | 描述 | 权限 |
|------|------|------|
| `/social` | 打开社交菜单 | `woosocial.use` |
| `/social help` | 查看帮助 | `woosocial.use` |
| `/social reload` | 重载配置 | `woosocial.admin` |

### 好友命令

| 命令 | 描述 | 权限 |
|------|------|------|
| `/friend` | 打开好友列表GUI | `woosocial.friend.list` |
| `/friend add <玩家>` | 添加好友 | `woosocial.friend.add` |
| `/friend accept [玩家]` | 接受好友请求 | `woosocial.friend.accept` |
| `/friend deny [玩家]` | 拒绝好友请求 | `woosocial.friend.deny` |
| `/friend remove <玩家>` | 删除好友 | `woosocial.friend.remove` |
| `/friend list [页码]` | 查看好友列表 | `woosocial.friend.list` |
| `/friend notify <玩家> [on/off]` | 设置上线提醒 | `woosocial.friend.notify` |
| `/friend requests` | 查看好友请求 | `woosocial.friend.requests` |
| `/friend block <玩家>` | 屏蔽玩家 | `woosocial.block.add` |
| `/friend unblock <玩家>` | 取消屏蔽 | `woosocial.block.remove` |
| `/friend blocked [页码]` | 查看屏蔽列表 | `woosocial.block.list` |

### 传送命令

| 命令 | 描述 | 权限 |
|------|------|------|
| `/tpf <玩家>` | 传送到好友 | `woosocial.teleport.to` |
| `/tpftoggle` | 切换好友传送权限 | `woosocial.teleport.toggle` |
| `/tpfallow [玩家]` | 允许好友传送 | `woosocial.teleport.toggle` |
| `/tpfdeny [玩家]` | 禁止好友传送 | `woosocial.teleport.toggle` |

## 权限

### 基础权限

| 权限 | 描述 | 默认 |
|------|------|------|
| `woosocial.use` | 基础使用权限 | true |
| `woosocial.admin` | 管理员权限 | op |
| `woosocial.help` | 查看帮助 | true |
| `woosocial.reload` | 重载配置 | op |
| `woosocial.gui.social` | 打开社交菜单 | true |

### 好友权限

| 权限 | 描述 | 默认 |
|------|------|------|
| `woosocial.friend` | 好友基础权限 | true |
| `woosocial.friend.list` | 查看好友列表 | true |
| `woosocial.friend.add` | 添加好友 | true |
| `woosocial.friend.accept` | 接受好友请求 | true |
| `woosocial.friend.deny` | 拒绝好友请求 | true |
| `woosocial.friend.remove` | 删除好友 | true |
| `woosocial.friend.requests` | 查看好友请求 | true |
| `woosocial.friend.notify` | 设置上线提醒 | true |
| `woosocial.friend.limit.bypass` | 绕过好友数量限制 | op |

### 屏蔽权限

| 权限 | 描述 | 默认 |
|------|------|------|
| `woosocial.block` | 屏蔽基础权限 | true |
| `woosocial.block.add` | 屏蔽玩家 | true |
| `woosocial.block.remove` | 取消屏蔽 | true |
| `woosocial.block.list` | 查看屏蔽列表 | true |

### 传送权限

| 权限 | 描述 | 默认 |
|------|------|------|
| `woosocial.teleport` | 传送基础权限 | true |
| `woosocial.teleport.to` | 传送到好友 | true |
| `woosocial.teleport.toggle` | 切换传送权限 | true |
| `woosocial.teleport.cooldown.bypass` | 绕过传送冷却 | op |
| `woosocial.teleport.countdown.bypass` | 绕过传送倒计时 | op |
