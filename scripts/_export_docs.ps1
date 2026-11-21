$ErrorActionPreference = "Stop"
$root = "C:\Users\Administrator\Desktop\基于SpringBoot的校园活动管理系统设计与实现"
$word = New-Object -ComObject Word.Application
$word.Visible = $false
$d = $word.Documents.Open((Join-Path $root "doc\毕业论文初稿.docx"))
$d.Fields.Update() | Out-Null
$d.Save()
$d.ExportAsFixedFormat((Join-Path $root "doc\毕业论文初稿.pdf"), 17)
"thesis pages=" + $d.ComputeStatistics(2) + " words=" + $d.ComputeStatistics(0)
$d.Close()

foreach ($n in @("开题报告", "任务书", "文献综述", "答辩材料")) {
    $p = Join-Path $root "doc\$n.docx"
    if (Test-Path $p) {
        $x = $word.Documents.Open($p)
        $x.ExportAsFixedFormat((Join-Path $root "doc\$n.pdf"), 17)
        "$n pages=" + $x.ComputeStatistics(2) + " words=" + $x.ComputeStatistics(0)
        $x.Close()
    }
}
$word.Quit()
