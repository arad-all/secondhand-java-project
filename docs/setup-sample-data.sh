#!/usr/bin/env bash
# ============================================================================
# SecondHand – Sample Data Setup Script (Linux/macOS/Git Bash)
# ============================================================================
# Run this from the project root AFTER creating the database with Hibernate's
# ddl-auto=update (i.e., after starting the backend at least once).
#
# Usage:
#   bash docs/setup-sample-data.sh
# ============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

if ! command -v sqlite3 &> /dev/null; then
    echo "Error: sqlite3 is not installed."
    echo "Install it from https://sqlite.org/download.html and try again."
    exit 1
fi

if [ ! -f "$PROJECT_ROOT/secondhand.db" ]; then
    echo "Error: secondhand.db not found."
    echo "Start the backend at least once so Hibernate creates the database,"
    echo "then run this script."
    exit 1
fi

echo "==> 1. Populating database with sample data..."
sqlite3 "$PROJECT_ROOT/secondhand.db" < "$SCRIPT_DIR/sample-data.sql"
echo "    Done."

echo "==> 2. Copying sample photos to uploads/advertisements/..."
mkdir -p "$PROJECT_ROOT/uploads/advertisements"
for ad_dir in "$SCRIPT_DIR/sample-photos"/*/; do
    ad_id=$(basename "$ad_dir")
    mkdir -p "$PROJECT_ROOT/uploads/advertisements/$ad_id"
    cp "$ad_dir"* "$PROJECT_ROOT/uploads/advertisements/$ad_id/"
done
echo "    Done."

echo ""
echo "Sample data is ready! Start the backend and frontend to use the app."
echo "Test accounts:  admin / admin_pass  |  arad / 1234  |  amirmohammad / 1234"
