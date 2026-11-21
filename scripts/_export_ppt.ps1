$root = "C:\Users\Administrator\Desktop\基于SpringBoot的校园活动管理系统设计与实现"
$ppt = New-Object -ComObject PowerPoint.Application
$p = Join-Path $root "doc\答辩PPT.pptx"
$out = Join-Path $root "doc\slidepng"
if (Test-Path $out) { Remove-Item $out -Recurse -Force }
$pres = $ppt.Presentations.Open($p, $true, $false, $false)
$pres.Export($out, 'PNG', 1600, 900)
$pres.SaveAs((Join-Path $root "doc\答辩PPT.pdf"), 32)
"slides=" + $pres.Slides.Count
$pres.Close(); $ppt.Quit()
