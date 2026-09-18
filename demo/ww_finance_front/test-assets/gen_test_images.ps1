# 生成借款人认证联调用测试占位图（明确标注"测试用图 · 非真实证件"）
Add-Type -AssemblyName System.Drawing

$outDir = "D:\financial-warehouse\demo\ww_finance_front\test-assets"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

function New-TestImage {
    param(
        [string]$FileName,
        [string]$Title,
        [string]$Sub,
        [int]$W = 800,
        [int]$H = 500,
        [string]$Bg = "#FFFFFF",
        [string]$Accent = "#E8421F"
    )
    $bmp = New-Object System.Drawing.Bitmap($W, $H)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAlias

    $bgBrush = New-Object System.Drawing.SolidBrush([System.Drawing.ColorTranslator]::FromHtml($Bg))
    $g.FillRectangle($bgBrush, 0, 0, $W, $H)

    $fontTitle = New-Object System.Drawing.Font("Microsoft YaHei", 40, [System.Drawing.FontStyle]::Bold)
    $fontSub   = New-Object System.Drawing.Font("Microsoft YaHei", 18)
    $fontMark  = New-Object System.Drawing.Font("Microsoft YaHei", 14, [System.Drawing.FontStyle]::Bold)
    $accentBrush = New-Object System.Drawing.SolidBrush([System.Drawing.ColorTranslator]::FromHtml($Accent))
    $darkBrush   = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(64, 64, 64))
    $grayBrush   = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(150, 150, 150))
    $markBrush   = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(200, 60, 60))

    # 顶部安全标记条
    $markBrush2 = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(30, 232, 66, 31))
    $g.FillRectangle($markBrush2, 0, 0, $W, 34)
    $g.DrawString("TEST DEMO MATERIAL - 测试素材，非真实证件", $fontMark, $markBrush, 20, 6)

    # 主标题居中
    $fmt = New-Object System.Drawing.StringFormat
    $fmt.Alignment = [System.Drawing.StringAlignment]::Center
    $fmt.LineAlignment = [System.Drawing.StringAlignment]::Center
    $y1 = [double]($H / 2) - 90
    $y2 = [double]($H / 2) + 10
    $y3 = [double]$H - 70
    $rect1 = New-Object System.Drawing.RectangleF -ArgumentList @(0, $y1, $W, 80)
    $rect2 = New-Object System.Drawing.RectangleF -ArgumentList @(0, $y2, $W, 50)
    $g.DrawString($Title, $fontTitle, $accentBrush, $rect1, $fmt)
    $g.DrawString($Sub, $fontSub, $grayBrush, $rect2, $fmt)

    # 底部信息
    $rect3 = New-Object System.Drawing.RectangleF -ArgumentList @(0, $y3, $W, 40)
    $g.DrawString("旺旺金融信贷系统 · 借款人认证上传测试", $fontSub, $darkBrush, $rect3, $fmt)

    $g.Dispose()
    $path = Join-Path $outDir $FileName
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    "生成: $path"
}

New-TestImage "idCard1.png" "身份证正面（测试）" "姓名：测试用户 · 证件示例" 800 500 "#F7FBFF" "#1F6FEB"
New-TestImage "idCard2.png" "身份证反面（测试）" "签发机关：测试示例" 800 500 "#F7FBFF" "#1F6FEB"
New-TestImage "car.png"     "车辆行驶证（测试）" "车牌：豫A·TEST00 · 示例图片" 800 500 "#FFFDF5" "#AD6800"
New-TestImage "house.png"   "房产证明（测试）"   "不动产权证 · 示例图片" 800 500 "#F5FFF7" "#389E0D"
New-TestImage "work.png"    "工作/收入证明（测试）" "在职证明 · 示例图片" 800 500 "#FDF5FF" "#722ED1"
