#!/usr/bin/env bash
# End-to-end test of the login flow against the running Docker Compose stack, with curl
# and a cookie jar instead of a browser:
#   login with Keycloak -> session cookie -> GET /token -> Bearer token on /api ->
#   logout (token revoked, session and Keycloak session ended).
#
# Usage (from the repository root, after `docker compose up -d --build --wait`):
#   bash scripts/smoke-auth.sh
#
# The demo password is read from .env (KEYCLOAK_DEMO_PASSWORD); nothing secret is stored
# here. Optional environment variables: BASE_URL (default http://localhost:8080),
# SMOKE_USER (default student1), SMOKE_EMAIL (default student1@example.com), ENV_FILE.
set -uo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8081}"
SMOKE_USER="${SMOKE_USER:-student1}"
SMOKE_EMAIL="${SMOKE_EMAIL:-student1@example.com}"
ENV_FILE="${ENV_FILE:-$(dirname "$0")/../.env}"

failures=0
ok()   { printf 'OK    %s\n' "$1"; }
fail() { printf 'FEIL  %s\n' "$1"; [ -n "${2:-}" ] && printf '      %s\n' "$2"; failures=$((failures + 1)); }
check() { if [ "$1" = "$2" ]; then ok "$3"; else fail "$3" "forventet $2, fikk $1"; fi; }

# Read one value from .env without executing the file.
env_value() {
    grep -E "^$1=" "$ENV_FILE" 2>/dev/null | tail -n 1 | cut -d= -f2- | tr -d '\r'
}

if [ ! -f "$ENV_FILE" ]; then
    echo "Fant ikke $ENV_FILE" >&2
    exit 2
fi
PASSWORD="$(env_value KEYCLOAK_DEMO_PASSWORD)"
if [ -z "$PASSWORD" ]; then
    echo "KEYCLOAK_DEMO_PASSWORD mangler i $ENV_FILE" >&2
    exit 2
fi

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
jar="$work/cookies.txt"          # the "browser": cookies for both localhost:8080 and :8081
old_jar="$work/cookies-before-logout.txt"
: > "$jar"

# curl helpers. Every call uses the cookie jar like a browser would.
status_of() { curl -s -o /dev/null -w '%{http_code}' -b "$jar" -c "$jar" "$@"; }

echo "Smoke test mot $BASE_URL (bruker $SMOKE_USER)"

# a. The frontend is served.
body="$(curl -s -w '\n%{http_code}' "$BASE_URL/")"
code="${body##*$'\n'}"
if [ "$code" = 200 ] && grep -q '<div id="root">' <<<"$body"; then
    ok "a. GET / gir frontend (200)"
else
    fail "a. GET / gir frontend (200)" "status $code"
fi

# b. No session -> no token; no token -> 401 Bearer challenge, no redirect.
check "$(curl -s -o /dev/null -w '%{http_code}' "$BASE_URL/token")" 401 "b1. GET /token uten sesjon gir 401"
headers="$(curl -s -o /dev/null -D - "$BASE_URL/api/users/me" | tr -d '\r')"
code="$(head -n 1 <<<"$headers" | awk '{print $2}')"
if [ "$code" = 401 ] && grep -qi '^WWW-Authenticate: Bearer' <<<"$headers" \
        && ! grep -qi '^Location:' <<<"$headers"; then
    ok "b2. GET /api/users/me uten token gir 401 med WWW-Authenticate: Bearer og uten Location"
else
    fail "b2. GET /api/users/me uten token gir 401 med WWW-Authenticate: Bearer og uten Location" \
         "status $code, headers: $(tr '\n' ' ' <<<"$headers")"
fi

# c. Login: backend -> Keycloak login form -> POST credentials -> back to backend -> SPA.
login_page="$(curl -s -L -b "$jar" -c "$jar" "$BASE_URL/oauth2/authorization/keycloak")"
action="$(grep -o 'action="[^"]*login-actions/authenticate[^"]*"' <<<"$login_page" | head -n 1 \
          | sed -e 's/^action="//' -e 's/"$//' -e 's/&amp;/\&/g')"
if [ -z "$action" ]; then
    fail "c. Innlogging via Keycloak" "fant ikke innloggingsskjemaet til Keycloak"
else
    final_url="$(curl -s -L -o "$work/after-login.html" -w '%{url_effective} %{http_code}' \
        -b "$jar" -c "$jar" \
        --data-urlencode "username=$SMOKE_USER" --data-urlencode "password=$PASSWORD" \
        --data-urlencode "credentialId=" "$action")"
    if [ "$final_url" = "$BASE_URL/ 200" ]; then
        ok "c. Innlogging via Keycloak (skjema -> POST -> tilbake til $BASE_URL/)"
    else
        fail "c. Innlogging via Keycloak" "endte på: $final_url"
    fi
fi

# d. The session cookie buys an API token.
token_json="$(curl -s -b "$jar" -c "$jar" -w '\n%{http_code}' "$BASE_URL/token")"
code="${token_json##*$'\n'}"
token="$(sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p' <<<"$token_json")"
if [ "$code" = 200 ] && [ -n "$token" ]; then
    ok "d. GET /token med sesjons-cookie gir JSON med accessToken"
else
    fail "d. GET /token med sesjons-cookie gir JSON med accessToken" "status $code"
fi

# e. The token works on the API and identifies the right user (no cookies sent).
me="$(curl -s -w '\n%{http_code}' -H "Authorization: Bearer $token" "$BASE_URL/api/users/me")"
code="${me##*$'\n'}"
if [ "$code" = 200 ] && grep -q "\"email\":\"$SMOKE_EMAIL\"" <<<"$me"; then
    ok "e. GET /api/users/me med Bearer-token gir 200 og $SMOKE_EMAIL"
else
    fail "e. GET /api/users/me med Bearer-token gir 200 og $SMOKE_EMAIL" "status $code: ${me%$'\n'*}"
fi

# f. A made-up token is rejected.
check "$(curl -s -o /dev/null -w '%{http_code}' -H 'Authorization: Bearer tull' "$BASE_URL/api/users/me")" \
      401 "f. GET /api/users/me med 'Bearer tull' gir 401"

# g-j only mean something after a real login: "401 after logout" proves nothing for a
# token that never worked.
if [ -z "$token" ]; then
    for point in "g. Utlogging" "h. Tokenet gir 401 etter utlogging" \
                 "i. Gammel cookie gir 401" "j. Innloggingsskjemaet vises igjen"; do
        fail "$point" "ikke testet: innloggingen (c/d) ga ikke noe token"
    done
    echo
    echo "$failures punkt(er) feilet."
    exit 1
fi

# g. Logout: POST /logout -> Keycloak's end-session endpoint -> back to the SPA.
# The POST is sent once without -L; a browser then follows the 302 with GET requests.
cp "$jar" "$old_jar"
logout_headers="$(curl -s -o /dev/null -D - -b "$jar" -c "$jar" --data '' "$BASE_URL/logout" | tr -d '\r')"
location="$(grep -i '^Location:' <<<"$logout_headers" | head -n 1 | cut -d' ' -f2-)"
if [ -z "$location" ]; then
    fail "g. POST /logout gir redirect" "ingen Location"
else
    final_url="$(curl -s -L -o /dev/null -w '%{url_effective} %{http_code}' -b "$jar" -c "$jar" "$location")"
    if [[ "$location" == "$KEYCLOAK_URL/realms/lms/protocol/openid-connect/logout?"* ]] \
            && [ "$final_url" = "$BASE_URL/ 200" ]; then
        ok "g. POST /logout -> Keycloak logout -> tilbake til $BASE_URL/"
    else
        fail "g. POST /logout -> Keycloak logout -> tilbake til $BASE_URL/" \
             "Location: ${location%%\?*}, endte på: $final_url"
    fi
fi

# h. The token from (e) is revoked, although it has not expired.
check "$(curl -s -o /dev/null -w '%{http_code}' -H "Authorization: Bearer $token" "$BASE_URL/api/users/me")" \
      401 "h. Samme token som i (e) gir 401 etter utlogging"

# i. The old session cookie no longer gives a token.
check "$(curl -s -o /dev/null -w '%{http_code}' -b "$old_jar" "$BASE_URL/token")" \
      401 "i. GET /token med den gamle cookien gir 401"

# j. Keycloak's session is gone too: a new login shows the form instead of logging in.
page="$(curl -s -L -w '\n%{url_effective}' -b "$jar" -c "$jar" "$BASE_URL/oauth2/authorization/keycloak")"
if grep -q 'login-actions/authenticate' <<<"$page" && grep -q 'name="password"' <<<"$page"; then
    ok "j. Ny innlogging viser Keycloak sitt innloggingsskjema igjen"
else
    fail "j. Ny innlogging viser Keycloak sitt innloggingsskjema igjen" "endte på: ${page##*$'\n'}"
fi

echo
if [ "$failures" -eq 0 ]; then
    echo "Alle punktene er OK."
else
    echo "$failures punkt(er) feilet."
fi
exit $((failures > 0 ? 1 : 0))
