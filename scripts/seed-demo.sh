#!/usr/bin/env bash
# Seeds the Part 2.3 walkthrough: one company, one job with 6 skills, ten
# candidates with varied resumes and applications waiting to be screened.
#
#   ./scripts/seed-demo.sh
#
# Login: demo.hr@fairfold.dev / Password123!
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
if [ -f "$ROOT/.env" ]; then
  set -a
  # shellcheck disable=SC1091
  . "$ROOT/.env"
  set +a
fi

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-fairfold}"
DB_USER="${DB_USER:-fairfold}"
DB_PASSWORD="${DB_PASSWORD:-}"

python3 "$ROOT/scripts/seed_demo.py" | MYSQL_PWD="$DB_PASSWORD" mariadb \
  -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" "$DB_NAME"

# Reuse the generated CVs for resume previews when dummy-data exists.
STORAGE_DIR="${STORAGE_DIR:-../data/resumes}"
case "$STORAGE_DIR" in
  /*) ;;
  *) STORAGE_DIR="$ROOT/backend/$STORAGE_DIR" ;;
esac
mkdir -p "$STORAGE_DIR"
if compgen -G "$ROOT/dummy-data/cvs/*.pdf" > /dev/null; then
  mapfile -t CVS < <(find "$ROOT/dummy-data/cvs" -maxdepth 1 -name '*.pdf' | sort)
  for i in $(seq 1 10); do
    source_file="${CVS[$(((i - 1) % ${#CVS[@]}))]}"
    cp "$source_file" "$STORAGE_DIR/demo-resume-$i.pdf"
  done
  echo "Copied demo resume files to $STORAGE_DIR"
else
  echo "No dummy-data CVs found; demo resumes are ready for screening but have no preview file."
fi

echo "Demo seeded: demo.hr@fairfold.dev / Password123!"
echo "Open /employer/jobs/33000000-0000-0000-0000-000000000001 and click Screen All."
