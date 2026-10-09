#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# Cloud Vault end-to-end acceptance test (spec §56).
# Runs REAL operations against a live server: register -> login -> folders ->
# upload -> physical-storage verification -> download + checksum -> search ->
# public share -> user share -> authorization denials -> trash -> restore ->
# permanent delete -> quota enforcement -> admin workflows.
#
# Usage:  BASE_URL=http://localhost:8080 STORAGE_ROOT=./data/storage ./e2e.sh
# Optional fixture reset (defaults to the app's own datasource):
#         DB_URL=jdbc:postgresql://localhost:5432/cloudvault DB_USER=... DB_PASSWORD=... ./e2e.sh
# ---------------------------------------------------------------------------
set -u

BASE="${BASE_URL:-http://localhost:8080}"
STORAGE_ROOT="${STORAGE_ROOT:-$(cd "$(dirname "$0")/.." && pwd)/data/storage}"
WORK="$(mktemp -d)"
ALICE_JAR="$WORK/alice.jar"
BOB_JAR="$WORK/bob.jar"
ADMIN_JAR="$WORK/admin.jar"

PASS=0; FAIL=0; FAILED_NAMES=()

check() { # check <name> <expected> <actual>
  if [ "$2" = "$3" ]; then
    PASS=$((PASS+1)); echo "PASS  $1"
  else
    FAIL=$((FAIL+1)); FAILED_NAMES+=("$1 (expected [$2] got [$3])")
    echo "FAIL  $1  (expected [$2] got [$3])"
  fi
}
check_gt() { # check_gt <name> <actual> > <min>
  if [ "$2" -gt "$3" ] 2>/dev/null; then
    PASS=$((PASS+1)); echo "PASS  $1"
  else
    FAIL=$((FAIL+1)); FAILED_NAMES+=("$1 (got [$2] want > $3)")
    echo "FAIL  $1  (got [$2] want > $3)"
  fi
}
jsonval() { # jsonval <json> <python-expr, obj is `o`>
  python3 -c "import json,sys; o=json.loads(sys.argv[1]); print($2)" "$1" 2>/dev/null
}
sha() { shasum -a 256 "$1" | awk '{print $1}'; }

# --- CSRF helper: fetch XSRF-TOKEN cookie, print its value ------------------
csrf() { # csrf <jar>
  curl -s -c "$1" -b "$1" -o /dev/null "$BASE/login"
  awk '$6=="XSRF-TOKEN" {print $7}' "$1" | tail -1
}

# --- Fixture reset: makes this suite rerunnable against a live server ---------
# The assertions below assume documented preconditions: no alice/bob users, no
# leftover folders/files under those names, and fresh rate-limit windows. A
# previous run leaves all three behind, which would surface as 409 duplicates
# and 429 rate limits — fixture leakage, not application defects. This restores
# the preconditions only; every assertion stays exactly as written.
PG=()
fixture_reset() {
  local url="${DB_URL:-jdbc:postgresql://localhost:5432/cloudvault}"
  url="${url#jdbc:}"; url="${url#postgresql://}"
  local rest="${url%%\?*}"
  local hostport="${rest%%/*}"
  local dbname="${rest#*/}"
  local host="${hostport%%:*}"
  local port="${hostport##*:}"
  [ "$port" = "$hostport" ] && port=5432

  if ! command -v psql >/dev/null 2>&1; then
    echo "NOTE: psql not found — fixture reset skipped (a dirty database will fail this suite)"
    return 0
  fi

  if psql -d "$dbname" -tAc 'SELECT 1' >/dev/null 2>&1; then
    PG=(psql -d "$dbname" -v ON_ERROR_STOP=1 -q)
  else
    local -a try=(psql -h "$host" -p "$port" -d "$dbname" -v ON_ERROR_STOP=1 -q)
    [ -n "${DB_USER:-}" ] && try+=(-U "$DB_USER")
    if PGPASSWORD="${DB_PASSWORD:-}" "${try[@]}" -tAc 'SELECT 1' >/dev/null 2>&1; then
      PG=("${try[@]}")
      export PGPASSWORD="${DB_PASSWORD:-}"
    else
      echo "NOTE: database unreachable — fixture reset skipped (a dirty database will fail this suite)"
      return 0
    fi
  fi

  # 1) remove physical objects owned by the test users (only paths resolving
  #    under STORAGE_ROOT are ever deleted)
  local root refs p resolved
  root=$(cd "$STORAGE_ROOT" 2>/dev/null && pwd -P)
  refs=$("${PG[@]}" -tAc "SELECT physical_reference FROM storage_objects
          WHERE id IN (SELECT fv.storage_object_id FROM file_versions fv
                         JOIN files f ON f.id = fv.file_id
                         JOIN users u ON u.id = f.owner_id
                        WHERE u.username IN ('alice','bob'))")
  if [ -n "$root" ] && [ -n "$refs" ]; then
    while IFS= read -r p; do
      [ -n "$p" ] || continue
      resolved="$(cd "$(dirname "$p")" 2>/dev/null && pwd -P)/$(basename "$p")"
      case "$resolved" in
        "$root"/*) rm -f -- "$p" ;;
      esac
    done <<< "$refs"
  fi

  # 2) drop prior test-user state (cascades folders/files/shares/sessions/usage),
  #    then now-unreferenced storage-object rows, then rate-limit windows
  if ! "${PG[@]}" <<'SQL'
DELETE FROM users WHERE username IN ('alice','bob');
DELETE FROM storage_objects WHERE id NOT IN (SELECT storage_object_id FROM file_versions);
TRUNCATE TABLE rate_limit_counters;
SQL
  then
    echo "FAIL: fixture reset SQL errored"
    exit 2
  fi
  echo "FIXTURE RESET: prior alice/bob state and rate-limit windows cleared"
}

fixture_reset

echo "== 0. Health =="
code=$(curl -s -o "$WORK/health.json" -w '%{http_code}' "$BASE/actuator/health")
check "health endpoint 200" "200" "$code"
check "health status UP" "UP" "$(jsonval "$(cat "$WORK/health.json")" "o['status']")"

echo "== 1. Registration =="
T=$(csrf "$ALICE_JAR")
code=$(curl -s -o "$WORK/reg.json" -w '%{http_code}' -X POST "$BASE/api/v1/auth/register" \
  -H "Content-Type: application/json" -H "X-XSRF-TOKEN: $T" -b "$ALICE_JAR" -c "$ALICE_JAR" \
  -d '{"username":"alice","email":"alice@example.com","password":"AliceSecret!42","displayName":"Alice"}')
check "register alice 201" "201" "$code"
code=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/v1/auth/register" \
  -H "Content-Type: application/json" -H "X-XSRF-TOKEN: $T" -b "$ALICE_JAR" -c "$ALICE_JAR" \
  -d '{"username":"alice","email":"alice2@example.com","password":"AliceSecret!42"}')
check "duplicate username 409" "409" "$code"
code=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/v1/auth/register" \
  -H "Content-Type: application/json" -H "X-XSRF-TOKEN: $T" -b "$ALICE_JAR" -c "$ALICE_JAR" \
  -d '{"username":"bob","email":"bob@example.com","password":"short"}')
check "weak password 422" "422" "$code"

echo "== 2. Login =="
T=$(csrf "$ALICE_JAR")
code=$(curl -s -o "$WORK/login.json" -w '%{http_code}' -X POST "$BASE/api/v1/auth/login" \
  -H "Content-Type: application/json" -H "X-XSRF-TOKEN: $T" -b "$ALICE_JAR" -c "$ALICE_JAR" \
  -d '{"username":"alice","password":"AliceSecret!42"}')
check "login alice 200" "200" "$code"
check "login returns plan" "FREE" "$(jsonval "$(cat "$WORK/login.json")" "o['plan']")"
T=$(csrf "$ALICE_JAR")
code=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/v1/auth/login" \
  -H "Content-Type: application/json" -H "X-XSRF-TOKEN: $T" -b "$ALICE_JAR" -c "$ALICE_JAR" \
  -d '{"username":"alice","password":"WrongPassword!99"}')
check "bad password 401" "401" "$code"
code=$(curl -s -o "$WORK/me.json" -w '%{http_code}' "$BASE/api/v1/auth/me" -b "$ALICE_JAR")
check "session me 200" "200" "$code"
T=$(csrf "$ALICE_JAR")
code=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/v1/auth/logout" \
  -H "X-XSRF-TOKEN: $T" -b "$ALICE_JAR" -c "$ALICE_JAR")
check "logout 204" "204" "$code"
code=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/api/v1/auth/me" -b "$ALICE_JAR")
check "me after logout 401" "401" "$code"
# re-login for the rest of the test
T=$(csrf "$ALICE_JAR")
curl -s -o /dev/null -X POST "$BASE/api/v1/auth/login" \
  -H "Content-Type: application/json" -H "X-XSRF-TOKEN: $T" -b "$ALICE_JAR" -c "$ALICE_JAR" \
  -d '{"username":"alice","password":"AliceSecret!42"}'

echo "== 3. Folders =="
T=$(csrf "$ALICE_JAR")
code=$(curl -s -o "$WORK/folder1.json" -w '%{http_code}' -X POST "$BASE/api/v1/folders" \
  -H "Content-Type: application/json" -H "X-XSRF-TOKEN: $T" -b "$ALICE_JAR" -c "$ALICE_JAR" \
  -d '{"name":"Documents","parentId":null}')
check "create folder 201" "201" "$code"
FOLDER1=$(jsonval "$(cat "$WORK/folder1.json")" "o['id']")
code=$(curl -s -o "$WORK/folder2.json" -w '%{http_code}' -X POST "$BASE/api/v1/folders" \
  -H "Content-Type: application/json" -H "X-XSRF-TOKEN: $T" -b "$ALICE_JAR" -c "$ALICE_JAR" \
  -d "{\"name\":\"Nested\",\"parentId\":$FOLDER1}")
check "create nested folder 201" "201" "$code"
FOLDER2=$(jsonval "$(cat "$WORK/folder2.json")" "o['id']")
code=$(curl -s -o /dev/null -w '%{http_code}' -X PATCH "$BASE/api/v1/folders/$FOLDER1" \
  -H "Content-Type: application/json" -H "X-XSRF-TOKEN: $T" -b "$ALICE_JAR" -c "$ALICE_JAR" \
  -d '{"name":"Papers"}')
check "rename folder 200" "200" "$code"
code=$(curl -s -o "$WORK/browse.json" -w '%{http_code}' "$BASE/api/v1/files?folderId=$FOLDER2" -b "$ALICE_JAR")
check "browse nested 200" "200" "$code"

echo "== 4. Upload (real bytes) =="
head -c 5242880 /dev/urandom > "$WORK/upload.bin"
UP_SHA=$(sha "$WORK/upload.bin")
T=$(csrf "$ALICE_JAR")
code=$(curl -s -o "$WORK/upload.json" -w '%{http_code}' -X POST "$BASE/api/v1/files?folderId=$FOLDER1&name=secret.bin" \
  -H "Content-Type: application/octet-stream" -H "X-XSRF-TOKEN: $T" \
  -b "$ALICE_JAR" -c "$ALICE_JAR" --data-binary "@$WORK/upload.bin")
check "upload 201" "201" "$code"
FILE_ID=$(jsonval "$(cat "$WORK/upload.json")" "o['id']")
check "db checksum == local sha256" "$UP_SHA" "$(jsonval "$(cat "$WORK/upload.json")" "o['checksum']")"
check "db size == local size" "5242880" "$(jsonval "$(cat "$WORK/upload.json")" "o['size']")"

echo "== 5. Physical storage verification =="
sleep 0.3
PHYS=$(find "$STORAGE_ROOT" -type f -newer "$WORK/upload.bin" 2>/dev/null | head -50 | xargs -I{} shasum -a 256 {} 2>/dev/null | grep -c "$UP_SHA")
check "physical file with matching sha256 exists in STORAGE_ROOT" "1" "$PHYS"
OUTSIDE=$(find "$STORAGE_ROOT/.." -maxdepth 1 -type f -name 'secret.bin' | wc -l | tr -d ' ')
check "no file escaped the storage root" "0" "$OUTSIDE"

echo "== 6. Download + checksum + range =="
curl -s -o "$WORK/download.bin" "$BASE/api/v1/files/$FILE_ID/download" -b "$ALICE_JAR"
check "downloaded sha256 matches upload" "$UP_SHA" "$(sha "$WORK/download.bin")"
code=$(curl -s -o "$WORK/range.bin" -w '%{http_code}' -H 'Range: bytes=0-15' \
  "$BASE/api/v1/files/$FILE_ID/download" -b "$ALICE_JAR")
check "range request 206" "206" "$code"
check "range returns 16 bytes" "16" "$(wc -c < "$WORK/range.bin" | tr -d ' ')"

echo "== 7. Search (real DB query) =="
code=$(curl -s -o "$WORK/search.json" -w '%{http_code}' "$BASE/api/v1/files/search?q=secret" -b "$ALICE_JAR")
check "search 200" "200" "$code"
check "search finds uploaded file" "$FILE_ID" "$(jsonval "$(cat "$WORK/search.json")" "[str(f['id']) for f in o['content'] if f['id']==$FILE_ID][0]")"

echo "== 8. Public share =="
T=$(csrf "$ALICE_JAR")
code=$(curl -s -o "$WORK/share.json" -w '%{http_code}' -X POST "$BASE/api/v1/shares" \
  -H "Content-Type: application/json" -H "X-XSRF-TOKEN: $T" -b "$ALICE_JAR" -c "$ALICE_JAR" \
  -d "{\"fileId\":$FILE_ID,\"type\":\"PUBLIC_LINK\",\"permission\":\"DOWNLOAD\"}")
check "create public share 201" "201" "$code"
SHARE_TOKEN=$(jsonval "$(cat "$WORK/share.json")" "o['token']")
SHARE_ID=$(jsonval "$(cat "$WORK/share.json")" "o['id']")
check "share token is long + random" "1" "$(python3 -c "import sys; print(1 if len('$SHARE_TOKEN') >= 20 else 0)")"
code=$(curl -s -o "$WORK/pubshare.json" -w '%{http_code}' "$BASE/api/v1/public/shares/$SHARE_TOKEN")
check "anonymous share view 200" "200" "$code"
curl -s -o "$WORK/shared.bin" "$BASE/api/v1/public/shares/$SHARE_TOKEN/download"
check "anonymous shared download sha256" "$UP_SHA" "$(sha "$WORK/shared.bin")"
code=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/api/v1/public/shares/$(python3 -c "print('a'*32)")")
check "guessed token 404" "404" "$code"
T=$(csrf "$ALICE_JAR")
code=$(curl -s -o /dev/null -w '%{http_code}' -X DELETE "$BASE/api/v1/shares/$SHARE_ID" \
  -H "X-XSRF-TOKEN: $T" -b "$ALICE_JAR" -c "$ALICE_JAR")
check "revoke share 204" "204" "$code"
code=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/api/v1/public/shares/$SHARE_TOKEN")
check "revoked share now 403" "403" "$code"

echo "== 9. Share expiry enforced server-side =="
T=$(csrf "$ALICE_JAR")
EXPIRES=$(python3 -c "import datetime;print((datetime.datetime.now(datetime.timezone.utc)+datetime.timedelta(seconds=3)).isoformat().replace('+00:00','Z'))")
code=$(curl -s -o "$WORK/share2.json" -w '%{http_code}' -X POST "$BASE/api/v1/shares" \
  -H "Content-Type: application/json" -H "X-XSRF-TOKEN: $T" -b "$ALICE_JAR" -c "$ALICE_JAR" \
  -d "{\"fileId\":$FILE_ID,\"type\":\"PUBLIC_LINK\",\"permission\":\"VIEW\",\"expiresAt\":\"$EXPIRES\"}")
check "create expiring share 201" "201" "$code"
TOKEN2=$(jsonval "$(cat "$WORK/share2.json")" "o['token']")
code=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/api/v1/public/shares/$TOKEN2")
check "share works before expiry" "200" "$code"
sleep 4
code=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/api/v1/public/shares/$TOKEN2")
check "expired share 403 (server-enforced)" "403" "$code"

echo "== 10. Second user + cross-user authorization =="
T=$(csrf "$BOB_JAR")
code=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/v1/auth/register" \
  -H "Content-Type: application/json" -H "X-XSRF-TOKEN: $T" -b "$BOB_JAR" -c "$BOB_JAR" \
  -d '{"username":"bob","email":"bob@example.com","password":"BobSecret!42"}')
check "register bob 201" "201" "$code"
T=$(csrf "$BOB_JAR")
code=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/v1/auth/login" \
  -H "Content-Type: application/json" -H "X-XSRF-TOKEN: $T" -b "$BOB_JAR" -c "$BOB_JAR" \
  -d '{"username":"bob","password":"BobSecret!42"}')
check "login bob 200" "200" "$code"
code=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/api/v1/files/$FILE_ID" -b "$BOB_JAR")
check "IDOR: bob reads alice file -> 404" "404" "$code"
code=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/api/v1/files/$FILE_ID/download" -b "$BOB_JAR")
check "IDOR: bob downloads alice file -> 404" "404" "$code"
T=$(csrf "$BOB_JAR")
code=$(curl -s -o /dev/null -w '%{http_code}' -X DELETE "$BASE/api/v1/files/$FILE_ID" \
  -H "X-XSRF-TOKEN: $T" -b "$BOB_JAR" -c "$BOB_JAR")
check "IDOR: bob deletes alice file -> 404" "404" "$code"
code=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/api/v1/admin/users" -b "$BOB_JAR")
check "bob cannot reach admin API -> 403" "403" "$code"
code=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/admin/users" -b "$BOB_JAR")
check "bob cannot reach admin UI -> 403" "403" "$code"
code=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/api/v1/admin/users")
check "anonymous admin API -> 401" "401" "$code"

echo "== 11. USER_SHARE (alice shares to bob) =="
T=$(csrf "$ALICE_JAR")
code=$(curl -s -o "$WORK/usershare.json" -w '%{http_code}' -X POST "$BASE/api/v1/shares" \
  -H "Content-Type: application/json" -H "X-XSRF-TOKEN: $T" -b "$ALICE_JAR" -c "$ALICE_JAR" \
  -d "{\"fileId\":$FILE_ID,\"type\":\"USER_SHARE\",\"permission\":\"DOWNLOAD\",\"recipient\":\"bob\"}")
check "user share created 201" "201" "$code"
code=$(curl -s -o "$WORK/incoming.json" -w '%{http_code}' "$BASE/api/v1/shares/incoming" -b "$BOB_JAR")
check "bob sees incoming share 200" "200" "$code"
check "incoming share contains file" "1" "$(jsonval "$(cat "$WORK/incoming.json")" "sum(1 for s in o if s['fileId']==$FILE_ID)")"
USER_SHARE_ID=$(jsonval "$(cat "$WORK/usershare.json")" "o['id']")
# find which file id bob can now download (share grants access to the file)
curl -s -o "$WORK/bob_shared.bin" "$BASE/api/v1/files/$FILE_ID/download" -b "$BOB_JAR"
check "bob downloads shared file sha256" "$UP_SHA" "$(sha "$WORK/bob_shared.bin")"
T=$(csrf "$ALICE_JAR")
curl -s -o /dev/null -X DELETE "$BASE/api/v1/shares/$USER_SHARE_ID" -H "X-XSRF-TOKEN: $T" -b "$ALICE_JAR" -c "$ALICE_JAR"
code=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/api/v1/files/$FILE_ID/download" -b "$BOB_JAR")
# Design: a revoked share the recipient once had returns 403; unknown files 404.
case "$code" in
  403|404) PASS=$((PASS+1)); echo "PASS  bob access revoked -> $code" ;;
  *) FAIL=$((FAIL+1)); FAILED_NAMES+=("bob access revoked got $code"); echo "FAIL  bob access revoked got $code" ;;
esac

echo "== 12. Trash + restore + permanent delete =="
T=$(csrf "$ALICE_JAR")
code=$(curl -s -o /dev/null -w '%{http_code}' -X DELETE "$BASE/api/v1/files/$FILE_ID" \
  -H "X-XSRF-TOKEN: $T" -b "$ALICE_JAR" -c "$ALICE_JAR")
check "trash file 204" "204" "$code"
code=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/api/v1/files/$FILE_ID/download" -b "$ALICE_JAR")
check "trashed file download 404" "404" "$code"
PHYS_STILL=$(find "$STORAGE_ROOT" -type f -exec shasum -a 256 {} \; 2>/dev/null | grep -c "$UP_SHA")
check "physical object retained while in trash" "1" "$PHYS_STILL"
T=$(csrf "$ALICE_JAR")
code=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/v1/files/$FILE_ID/restore" \
  -H "X-XSRF-TOKEN: $T" -b "$ALICE_JAR" -c "$ALICE_JAR")
check "restore from trash 204" "204" "$code"
curl -s -o "$WORK/restored.bin" "$BASE/api/v1/files/$FILE_ID/download" -b "$ALICE_JAR"
check "restored file downloadable + intact" "$UP_SHA" "$(sha "$WORK/restored.bin")"
T=$(csrf "$ALICE_JAR")
curl -s -o /dev/null -X DELETE "$BASE/api/v1/files/$FILE_ID" -H "X-XSRF-TOKEN: $T" -b "$ALICE_JAR" -c "$ALICE_JAR"
T=$(csrf "$ALICE_JAR")
code=$(curl -s -o /dev/null -w '%{http_code}' -X DELETE "$BASE/api/v1/files/$FILE_ID/permanent" \
  -H "X-XSRF-TOKEN: $T" -b "$ALICE_JAR" -c "$ALICE_JAR")
check "permanent delete 204" "204" "$code"
GONE=$(find "$STORAGE_ROOT" -type f -exec shasum -a 256 {} \; 2>/dev/null | grep -c "$UP_SHA")
check "physical object removed after purge" "0" "$GONE"
code=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/api/v1/files/$FILE_ID" -b "$ALICE_JAR")
check "purged file metadata gone -> 404" "404" "$code"

echo "== 13. Path traversal / malicious filenames =="
T=$(csrf "$ALICE_JAR")
for evil in '../evil.txt' '..\\evil.txt' '/etc/passwd' 'a/b.txt' '..'; do
  code=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/v1/files?name=$(python3 -c "import urllib.parse,sys;print(urllib.parse.quote(sys.argv[1]))" "$evil")" \
    -H "Content-Type: application/octet-stream" -H "X-XSRF-TOKEN: $T" \
    -b "$ALICE_JAR" -c "$ALICE_JAR" --data-binary "pwn")
  case "$code" in
    422) PASS=$((PASS+1)); echo "PASS  malicious name rejected: $(printf '%q' "$evil") -> 422" ;;
    *)   FAIL=$((FAIL+1)); FAILED_NAMES+=("malicious name [$evil] got $code"); echo "FAIL  malicious name: $(printf '%q' "$evil") -> $code" ;;
  esac
done
# NUL-byte injection is sent raw in the URL (bash cannot hold NUL in a var)
code=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/v1/files?name=bad%00name" \
  -H "Content-Type: application/octet-stream" -H "X-XSRF-TOKEN: $T" \
  -b "$ALICE_JAR" -c "$ALICE_JAR" --data-binary "pwn")
case "$code" in
  422|400) PASS=$((PASS+1)); echo "PASS  NUL-byte filename rejected -> $code" ;;
  *) FAIL=$((FAIL+1)); FAILED_NAMES+=("NUL-byte filename got $code"); echo "FAIL  NUL-byte filename -> $code" ;;
esac
ESCAPED=$(find "$STORAGE_ROOT/.." -maxdepth 2 \( -name 'evil.txt' -o -name 'passwd' \) 2>/dev/null | wc -l | tr -d ' ')
check "nothing escaped the storage root" "0" "$ESCAPED"

echo "== 14. Quota enforcement (admin-managed subscription) =="
# login admin
T=$(csrf "$ADMIN_JAR")
code=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/v1/auth/login" \
  -H "Content-Type: application/json" -H "X-XSRF-TOKEN: $T" -b "$ADMIN_JAR" -c "$ADMIN_JAR" \
  -d '{"username":"admin","password":"Admin!Pass123"}')
check "admin login 200" "200" "$code"
T=$(csrf "$ADMIN_JAR")
ALICE_ID=$(curl -s "$BASE/api/v1/admin/users" -b "$ADMIN_JAR" | python3 -c "import json,sys;print([u['id'] for u in json.load(sys.stdin) if u['username']=='alice'][0])")
check "admin sees alice" "1" "$([ -n "$ALICE_ID" ] && echo 1 || echo 0)"
# upload an anchor file (1 MiB) under normal quota
head -c 1048576 /dev/urandom > "$WORK/quota.bin"
QUOTA_SHA=$(sha "$WORK/quota.bin")
T=$(csrf "$ALICE_JAR")
code=$(curl -s -o "$WORK/quota_up.json" -w '%{http_code}' -X POST "$BASE/api/v1/files?name=quota.bin" \
  -H "Content-Type: application/octet-stream" -H "X-XSRF-TOKEN: $T" \
  -b "$ALICE_JAR" -c "$ALICE_JAR" --data-binary "@$WORK/quota.bin")
check "upload under quota 201" "201" "$code"
QFILE_ID=$(jsonval "$(cat "$WORK/quota_up.json")" "o['id']")
# shrink alice's quota below her current usage (downgrade: must NOT delete files)
T=$(csrf "$ADMIN_JAR")
code=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/v1/admin/users/$ALICE_ID/plan" \
  -H "Content-Type: application/json" -H "X-XSRF-TOKEN: $T" -b "$ADMIN_JAR" -c "$ADMIN_JAR" \
  -d '{"planCode":"FREE","status":"ACTIVE","storageOverride":1048576}')
check "admin sets 1 MiB quota override == current usage" "204" "$code"
# she should now be at/over quota: new upload must be rejected with 507 or 413
head -c 2097152 /dev/urandom > "$WORK/toobig.bin"
T=$(csrf "$ALICE_JAR")
code=$(curl -s -o "$WORK/toobig.json" -w '%{http_code}' -X POST "$BASE/api/v1/files?name=toobig.bin" \
  -H "Content-Type: application/octet-stream" -H "X-XSRF-TOKEN: $T" \
  -b "$ALICE_JAR" -c "$ALICE_JAR" --data-binary "@$WORK/toobig.bin")
case "$code" in
  507|413) PASS=$((PASS+1)); echo "PASS  over-quota upload rejected -> $code" ;;
  *) FAIL=$((FAIL+1)); FAILED_NAMES+=("over-quota upload got $code"); echo "FAIL  over-quota upload got $code" ;;
esac
# files still exist + downloadable while over quota (downgrade must not delete)
code=$(curl -s -o "$WORK/still.bin" -w '%{http_code}' "$BASE/api/v1/files/$QFILE_ID/download" -b "$ALICE_JAR")
check "existing file still downloadable while over quota" "200" "$code"
check "existing file intact while over quota" "$QUOTA_SHA" "$(sha "$WORK/still.bin")"
# concurrent uploads: give alice exactly 1.5 MiB of headroom, then race two
# 1 MiB uploads in parallel -> reservation semantics allow exactly one to win.
T=$(csrf "$ADMIN_JAR")
curl -s -o /dev/null -X POST "$BASE/api/v1/admin/users/$ALICE_ID/plan" \
  -H "Content-Type: application/json" -H "X-XSRF-TOKEN: $T" -b "$ADMIN_JAR" -c "$ADMIN_JAR" \
  -d '{"planCode":"FREE","status":"ACTIVE","storageOverride":2621440}'
T=$(csrf "$ALICE_JAR")
curl -s -o "$WORK/par1.json" -w '%{http_code}' -X POST "$BASE/api/v1/files?name=par1.bin" \
  -H "Content-Type: application/octet-stream" -H "X-XSRF-TOKEN: $T" -b "$ALICE_JAR" \
  --data-binary "@$WORK/quota.bin" > "$WORK/par1.code" 2>/dev/null &
P1=$!
curl -s -o "$WORK/par2.json" -w '%{http_code}' -X POST "$BASE/api/v1/files?name=par2.bin" \
  -H "Content-Type: application/octet-stream" -H "X-XSRF-TOKEN: $T" -b "$ALICE_JAR" \
  --data-binary "@$WORK/quota.bin" > "$WORK/par2.code" 2>/dev/null &
P2=$!
wait $P1; wait $P2
C1=$(cat "$WORK/par1.code" 2>/dev/null); C2=$(cat "$WORK/par2.code" 2>/dev/null)
WINS=$(python3 -c "print(sum(1 for c in ['$C1','$C2'] if c=='201'))")
check "concurrent over-quota: exactly one wins" "1" "$WINS"
# restore alice's quota
T=$(csrf "$ADMIN_JAR")
curl -s -o /dev/null -X POST "$BASE/api/v1/admin/users/$ALICE_ID/plan" \
  -H "Content-Type: application/json" -H "X-XSRF-TOKEN: $T" -b "$ADMIN_JAR" -c "$ADMIN_JAR" \
  -d '{"planCode":"FREE","status":"ACTIVE","storageOverride":null}'
T=$(csrf "$ALICE_JAR")
code=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/v1/files?name=after-restore.bin" \
  -H "Content-Type: application/octet-stream" -H "X-XSRF-TOKEN: $T" \
  -b "$ALICE_JAR" -c "$ALICE_JAR" --data-binary "@$WORK/quota.bin")
check "upload allowed again after quota restore" "201" "$code"

echo "== 15. Admin dashboard data is real =="
T=$(csrf "$ADMIN_JAR")
code=$(curl -s -o "$WORK/admin_overview.json" -w '%{http_code}' "$BASE/api/v1/admin/overview" -b "$ADMIN_JAR")
check "admin overview 200" "200" "$code"
code=$(curl -s -o "$WORK/admin_locs.json" -w '%{http_code}' "$BASE/api/v1/admin/storage/locations" -b "$ADMIN_JAR")
check "storage locations 200" "200" "$code"
check "default location registered" "1" "$(jsonval "$(cat "$WORK/admin_locs.json")" "len(o)")"
LOC_STATUS=$(jsonval "$(cat "$WORK/admin_locs.json")" "o[0]['status']")
check "default location ONLINE" "ONLINE" "$LOC_STATUS"
check "location has real capacity > 0" "1" "$(jsonval "$(cat "$WORK/admin_locs.json")" "1 if o[0]['totalCapacityBytes'] > 0 else 0")"
code=$(curl -s -o "$WORK/admin_jobs.json" -w '%{http_code}' "$BASE/api/v1/admin/jobs" -b "$ADMIN_JAR")
check "jobs list 200" "200" "$code"
code=$(curl -s -o "$WORK/admin_audit.json" -w '%{http_code}' "$BASE/api/v1/admin/audit" -b "$ADMIN_JAR")
check "audit log 200" "200" "$code"
check "audit log has real entries" "1" "$(jsonval "$(cat "$WORK/admin_audit.json")" "1 if (o['content'] if isinstance(o,dict) else o) else 0")"
code=$(curl -s -o "$WORK/admin_users.json" -w '%{http_code}' "$BASE/api/v1/admin/users" -b "$ADMIN_JAR")
USERS_OK=$(jsonval "$(cat "$WORK/admin_users.json")" "1 if {u['username'] for u in o} >= {'alice','bob','admin'} else 0")
if [ "$USERS_OK" != "1" ]; then
  echo "  [debug] users list HTTP=$code body: $(head -c 500 "$WORK/admin_users.json")"
fi
check "admin user list contains alice+bob" "1" "$USERS_OK"
# storage audit (real reconciliation scan)
T=$(csrf "$ADMIN_JAR")
code=$(curl -s -o "$WORK/audit_scan.json" -w '%{http_code}' -X POST "$BASE/api/v1/admin/storage/audit" \
  -H "Content-Type: application/json" -H "X-XSRF-TOKEN: $T" -b "$ADMIN_JAR" -c "$ADMIN_JAR" -d '{}')
case "$code" in
  200|201) PASS=$((PASS+1)); echo "PASS  storage reconciliation scan -> $code" ;;
  *) FAIL=$((FAIL+1)); FAILED_NAMES+=("storage reconciliation scan got $code"); echo "FAIL  storage reconciliation scan got $code" ;;
esac

echo "== 16. Rate limiting (login) =="
T=$(csrf "$WORK/rl.jar")
LAST=""
for i in $(seq 1 12); do
  LAST=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/v1/auth/login" \
    -H "Content-Type: application/json" -H "X-XSRF-TOKEN: $T" -b "$WORK/rl.jar" -c "$WORK/rl.jar" \
    -d '{"username":"rl_probe","password":"Wrong!Pass123"}')
  [ "$LAST" = "429" ] && break
done
check "login rate limit triggers 429" "429" "$LAST"

echo "== 17. UI pages (no dummy markers) =="
code=$(curl -s -o "$WORK/ui_login.html" -w '%{http_code}' "$BASE/login")
check "login page 200" "200" "$code"
code=$(curl -s -o "$WORK/ui_app.html" -w '%{http_code}' "$BASE/app" -b "$ALICE_JAR")
check "app dashboard 200" "200" "$code"
code=$(curl -s -o "$WORK/ui_files.html" -w '%{http_code}' "$BASE/app/files" -b "$ALICE_JAR")
check "files page 200" "200" "$code"
code=$(curl -s -o "$WORK/ui_admin.html" -w '%{http_code}' "$BASE/admin" -b "$ADMIN_JAR")
check "admin dashboard 200" "200" "$code"
# Full page sweep: every visible page must render without server errors.
for page in /app /app/files /app/trash /app/shares /app/subscription /app/notifications /app/settings "/app/search?q=test"; do
  code=$(curl -s -o "$WORK/sweep.html" -w '%{http_code}' "$BASE$page" -b "$ALICE_JAR")
  if [ "$code" = "200" ]; then PASS=$((PASS+1)); echo "PASS  page 200: $page";
  else FAIL=$((FAIL+1)); FAILED_NAMES+=("page $page got $code"); echo "FAIL  page $page -> $code"; fi
done
for page in /admin /admin/users /admin/storage /admin/jobs /admin/audit; do
  code=$(curl -s -o "$WORK/sweep.html" -w '%{http_code}' "$BASE$page" -b "$ADMIN_JAR")
  if [ "$code" = "200" ]; then PASS=$((PASS+1)); echo "PASS  page 200: $page";
  else FAIL=$((FAIL+1)); FAILED_NAMES+=("page $page got $code"); echo "FAIL  page $page -> $code"; fi
done
DUMMY=$(grep -riE "coming soon|not implemented|TODO|lorem ipsum|25 GB used" "$WORK"/ui_*.html | wc -l | tr -d ' ')
check "no dummy markers in UI" "0" "$DUMMY"
code=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/app" )
check "anonymous /app redirects to login" "302" "$code"

echo
echo "=================================================="
echo "E2E RESULT: $PASS passed, $FAIL failed"
if [ "$FAIL" -gt 0 ]; then
  printf '  - %s\n' "${FAILED_NAMES[@]}"
fi
echo "=================================================="
rm -rf "$WORK"
[ "$FAIL" -eq 0 ]
