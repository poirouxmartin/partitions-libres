# Fetches the pinned Verovio sources into render/third_party/verovio (gitignored, LGPLv3, ~100 MB).
# Same tag as the composition app, so both apps share one rendering engine version.
$ErrorActionPreference = 'Stop'
$tag = 'version-6.4.1'
$dest = Join-Path $PSScriptRoot 'third_party/verovio'
if (Test-Path $dest) { Write-Output "already present: $dest"; exit 0 }
git clone --depth 1 --branch $tag -c core.autocrlf=false https://github.com/rism-digital/verovio.git $dest
