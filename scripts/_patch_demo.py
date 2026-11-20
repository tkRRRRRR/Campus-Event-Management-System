import io

p = r"scripts\prepare-demo.ps1"
s = io.open(p, encoding="utf-8-sig").read()

old = '''function PostJson($url, $token, $obj) {
    $json = $obj | ConvertTo-Json -Compress -Depth 5
    $headers = @{}
    if ($token) { $headers["Authorization"] = "Bearer $token" }
    $resp = Invoke-WebRequest -Uri $url -Method Post -Headers $headers -ContentType "application/json" -Body $utf8.GetBytes($json) -UseBasicParsing
    return $resp.Content | ConvertFrom-Json
}'''

new = '''function PostJson($url, $token, $obj) {
    $headers = @{}
    if ($token) { $headers["Authorization"] = "Bearer $token" }
    if ($null -eq $obj) {
        $resp = Invoke-WebRequest -Uri $url -Method Post -Headers $headers -ContentType "application/json" -Body ([byte[]]@()) -UseBasicParsing
    } else {
        $json = $obj | ConvertTo-Json -Compress -Depth 5
        $resp = Invoke-WebRequest -Uri $url -Method Post -Headers $headers -ContentType "application/json" -Body $utf8.GetBytes([string]$json) -UseBasicParsing
    }
    return $resp.Content | ConvertFrom-Json
}'''

assert old in s, "pattern not found"
s = s.replace(old, new)
io.open(p, "w", encoding="utf-8-sig", newline="\r\n").write(s)
print("patched")
