旺旺金融信贷系统 - 服务器部署包说明
=====================================

一、目录结构（上传到服务器后按此摆放）
--------------------------------------------------
/www/wwwroot/ww_bank/
    ww_bank-1.0.0.jar          # 模拟银行（9090端口）
    bank.log                   # 启动后自动生成

/www/wwwroot/finance/
    finance-api-1.0-exec.jar   # 主后端（8990端口）
    application.yml            # ★ 服务器配置（连RDS+关Nacos），必须和jar放同目录
    api.log                    # 启动后自动生成

二、启动命令（宝塔终端执行）
--------------------------------------------------
# 1. 启动模拟银行
cd /www/wwwroot/ww_bank
nohup java -Xmx256m -jar ww_bank-1.0.0.jar > bank.log 2>&1 &

# 2. 启动主后端
cd /www/wwwroot/finance
nohup java -Xmx256m -jar finance-api-1.0-exec.jar > api.log 2>&1 &

# 3. 查看是否启动成功（看到 Started 字样即成功）
tail -f /www/wwwroot/ww_bank/bank.log
tail -f /www/wwwroot/finance/api.log
# Ctrl+C 退出查看

三、验证
--------------------------------------------------
curl http://localhost:9090    # 银行有响应
curl http://localhost:8990    # 后端有响应

四、数据库（已用阿里云RDS，服务器无需装MySQL）
--------------------------------------------------
ww_bank      -> RDS ww_bank 库（jar内已配好，无需改动）
finance-api  -> RDS www_finance 库（由 application.yml 覆盖）

五、防火墙放行端口
--------------------------------------------------
80    Nginx（后面托管前端）
8888  宝塔面板
9090  模拟银行
8990  后端（可选，配了Nginx反向代理后可不放）
