#!/bin/sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
MODE="${1:-full}"

if [ "$MODE" = "--help" ] || [ "$MODE" = "-h" ]; then
  cat <<'EOF'
Usage:
  sh modern/scripts/run-delivery-audit.sh [full|fast|browser|writeback]

Modes:
  full       Run build gates, runtime smokes, writeback smokes, and browser smokes.
  fast       Run build gates and read-only runtime smokes.
  browser    Run browser page and interaction smokes only.
  writeback  Run writeback smokes only.

All modes use modern local entries:
  admin web: http://127.0.0.1:19527
  app web:   http://127.0.0.1:19528
  admin api: http://127.0.0.1:18080
  front api: http://127.0.0.1:18081
EOF
  exit 0
fi

run_step() {
  name="$1"
  shift
  printf '\n==> %s\n' "$name"
  "$@"
}

run_backend_gate() {
  (cd "$ROOT_DIR/backend" && sh scripts/check-delivery.sh)
}

run_admin_gate() {
  (cd "$ROOT_DIR/admin-web" && npm run check:delivery && npm run build)
}

run_app_gate() {
  (cd "$ROOT_DIR/app-web" && npm run check:delivery && npm run build)
}

run_runtime_smokes() {
  (cd "$ROOT_DIR" && \
    node scripts/runtime-guard-smoke.mjs && \
    node scripts/runtime-smoke.mjs && \
    node scripts/runtime-deep-smoke.mjs)
}

run_writeback_smokes() {
  (cd "$ROOT_DIR" && \
    node scripts/runtime-order-safe-smoke.mjs && \
    node scripts/runtime-writeback-smoke.mjs && \
    node scripts/runtime-coupon-writeback-smoke.mjs && \
    node scripts/runtime-seckill-manager-writeback-smoke.mjs && \
    node scripts/runtime-seckill-product-writeback-smoke.mjs && \
    node scripts/runtime-bargain-product-writeback-smoke.mjs && \
    node scripts/runtime-combination-product-writeback-smoke.mjs && \
    node scripts/runtime-product-writeback-smoke.mjs && \
    node scripts/runtime-shipping-template-writeback-smoke.mjs && \
    node scripts/runtime-article-writeback-smoke.mjs && \
    node scripts/runtime-product-reply-writeback-smoke.mjs && \
    node scripts/runtime-user-options-writeback-smoke.mjs && \
    node scripts/runtime-store-pickup-writeback-smoke.mjs && \
    node scripts/runtime-form-temp-writeback-smoke.mjs && \
    node scripts/runtime-system-group-writeback-smoke.mjs && \
    node scripts/runtime-page-layout-writeback-smoke.mjs && \
    node scripts/runtime-wechat-reply-writeback-smoke.mjs && \
    node scripts/runtime-wechat-menu-writeback-smoke.mjs)
}

run_browser_smokes() {
  (cd "$ROOT_DIR" && \
    node scripts/browser-smoke.mjs && \
    node scripts/browser-interaction-smoke.mjs)
}

case "$MODE" in
  fast)
    run_step "backend delivery gate" run_backend_gate
    run_step "admin-web delivery gate" run_admin_gate
    run_step "app-web delivery gate" run_app_gate
    run_step "runtime read-only smokes" run_runtime_smokes
    ;;
  writeback)
    run_step "runtime writeback smokes" run_writeback_smokes
    ;;
  browser)
    run_step "browser smokes" run_browser_smokes
    ;;
  full)
    run_step "backend delivery gate" run_backend_gate
    run_step "admin-web delivery gate" run_admin_gate
    run_step "app-web delivery gate" run_app_gate
    run_step "runtime read-only smokes" run_runtime_smokes
    run_step "runtime writeback smokes" run_writeback_smokes
    run_step "browser smokes" run_browser_smokes
    ;;
  *)
    echo "Unknown mode: $MODE" >&2
    echo "Run with --help for usage." >&2
    exit 2
    ;;
esac

printf '\nCRMEB Modern delivery audit passed (%s).\n' "$MODE"
