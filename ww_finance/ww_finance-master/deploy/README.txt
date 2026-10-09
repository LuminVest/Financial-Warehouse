旺旺金融信贷系统 - 微服务全量部署包说明（方案A：标准微服务架构）
=========================================================================

一、架构总览（7 个服务 + Nacos 注册中心）
--------------------------------------------------
Nacos(8848) 注册中心：网关 + 各微服务注册到这里，网关按服务名 lb:// 转发
  |
  |--- finance-gateway   :8000   统一入口网关（路由 /core /sms /oss）
  |      |--- lb://service-core  -> finance-api   :8990  核心业务
  |      |--- lb://service-sms   -> finance-message :8120  短信服务
  |      └--- lb://service-oss   -> finance-oss    :8130  文件服务
  |
  |--- finance-ai        :8089   智能客服（不走网关，Nginx 直连 /ai）
  |--- ww_bank           :9090   模拟银行（老师提供，独立）
  └--- 前端静态（Nginx :80 托管两个 dist，反向代理到网关/服务）

二、目录结构（上传到服务器后按此摆放）
--------------------------------------------------
/www/wwwroot/nacos/                     # Nacos 2.x 注册中心（需 JDK8+）
/www/wwwroot/ww_bank/
    ww_bank-1.0.0.jar          # 模拟银行（9090端口）
    bank.log                   # 启动后自动生成

/www/wwwroot/finance/
    finance-api-1.0-exec.jar   # 核心业务（8990端口）
    finance-message-1.0-exec.jar # 短信服务（8120端口）
    finance-oss-1.0-exec.jar   # 文件服务（8130端口）
    finance-gateway-1.0-exec.jar # 网关（8000端口）
    finance-ai-1.0-exec.jar    # 智能客服（8089端口）
    application.yml            # ★ 服务器配置（连RDS/Nacos地址/上传目录），必须和jar放同目录
    *.log                      # 各服务启动后自动生成

/www/wwwroot/front/
    user/     # 用户端 dist（ww_finance_front 构建产物）
    admin/    # 管理端 dist（ww_finance_admin 构建产物）

三、服务器 application.yml 关键配置（覆盖 jar 内默认值）
--------------------------------------------------
以 finance-api 为例（其余服务同理，见各模块 resources/application.yml）：
  spring.datasource.*         -> 阿里云 RDS 地址
  spring.cloud.nacos.discovery.server-addr -> 服务器IP:8848
  oss.upload-dir              -> /www/uploads/（finance-oss 专用）
  redis.host                  -> 服务器 Redis 地址（api/message/ai 三服务共用同一 Redis，
                                  验证码 key xx:code: 必须同库）

四、启动命令（宝塔终端执行，按依赖顺序）
--------------------------------------------------
# 1. 启动 Nacos（standalone 模式）
cd /www/wwwroot/nacos/bin
sh startup.sh -m standalone

# 2. 启动各服务（等待上一个 Started 再启下一个）
cd /www/wwwroot/finance
nohup java -Xmx512m -jar finance-api-1.0-exec.jar      > api.log 2>&1 &
nohup java -Xmx512m -jar finance-message-1.0-exec.jar  > msg.log 2>&1 &
nohup java -Xmx256m -jar finance-oss-1.0-exec.jar      > oss.log 2>&1 &
nohup java -Xmx256m -jar finance-gateway-1.0-exec.jar  > gw.log 2>&1 &
nohup java -Xmx512m -jar finance-ai-1.0-exec.jar       > ai.log 2>&1 &

# 3. 启动模拟银行
cd /www/wwwroot/ww_bank
nohup java -Xmx256m -jar ww_bank-1.0.0.jar > bank.log 2>&1 &

# 4. 查看是否启动成功（看到 Started 字样即成功）
tail -f /www/wwwroot/finance/api.log

五、Nginx 配置（80 端口：托管前端 + 反向代理）
--------------------------------------------------
server {
    listen 80;
    server_name 你的域名或IP;

    # 用户端静态
    location / { root /www/wwwroot/front/user; try_files $uri $uri/ /index.html; }

    # 管理端静态
    location /admin/ { alias /www/wwwroot/front/admin/; try_files $uri $uri/ /admin/index.html; }

    # 核心业务 → 网关 → service-core
    location /api/ { proxy_pass http://127.0.0.1:8000; proxy_set_header Host $host; }
    location /admin/core/ { proxy_pass http://127.0.0.1:8000; proxy_set_header Host $host; }

    # 智能客服 → finance-ai 直连（网关未注册该服务）
    location /ai/ { proxy_pass http://127.0.0.1:8089; proxy_set_header Host $host; }

    # 文件服务 → 网关 → service-oss（上传接口）
    location /oss/ { proxy_pass http://127.0.0.1:8000; proxy_set_header Host $host; }

    # OSS 静态图片 → finance-oss 8130
    location /uploads/ { proxy_pass http://127.0.0.1:8130; proxy_set_header Host $host; }
}

六、验证
--------------------------------------------------
curl http://localhost:8848/nacos        # Nacos 控制台，所有服务健康
curl http://localhost:8000/api/core/xxx # 网关转发到 api
curl http://localhost:9090              # 银行有响应

七、数据库（已用阿里云RDS）
--------------------------------------------------
ww_bank      -> RDS ww_bank 库
finance-api  -> RDS www_finance 库（由 application.yml 覆盖）
Redis        -> 服务器自建或云 Redis（api/message/ai 共用）

八、防火墙放行端口
--------------------------------------------------
80     Nginx
8000   网关（配了 Nginx 反代后可不放）
8848   Nacos（只允许内网/安全组限制来源）
9090   模拟银行（可选）
8990/8120/8130/8089  各服务（配了 Nginx 反代后可不放）

九、本地开发启动顺序（Windows）
--------------------------------------------------
1. 启动 Nacos（startup.cmd -m standalone）
2. 启动 Redis（本地 6379）
3. 按序启动：finance-api(8990) → finance-message(8120) → finance-oss(8130) → finance-gateway(8000) → finance-ai(8089) → ww_bank(9090)
4. 前端 dev：ww_finance_front(npm run dev,5174) + ww_finance_admin(npm run dev,5173)
   用户端 dev 已配 vite 代理：/api→8990、/ai→8089、/oss→8130、/uploads→8130
