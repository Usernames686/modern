#!/bin/sh
set -e

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
CHECK_DIRS="
$ROOT_DIR/crmeb-modern-admin-api/src/main
$ROOT_DIR/crmeb-modern-front-api/src/main
$ROOT_DIR/crmeb-modern-service/src/main
$ROOT_DIR/crmeb-modern-common/src/main
"

PATTERN='本地迁移|迁移环境|迁移模式|迁移阶段|迁移版|迁移中|正在迁移|未迁移|本地交付环境|本地安全模式|暂不可用|（dry-run）|假数据|mock 数据|mock'

if grep -RInE "$PATTERN" $CHECK_DIRS >/tmp/crmeb-modern-release-copy-hits.txt 2>/dev/null; then
  cat /tmp/crmeb-modern-release-copy-hits.txt >&2
  echo "backend release copy check failed: internal migration copy found" >&2
  exit 1
fi

echo "backend release copy check passed"
