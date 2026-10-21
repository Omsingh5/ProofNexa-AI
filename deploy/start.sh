#!/bin/sh
set -eu
PORT="${PORT:-10000}"
sed "s/__PORT__/${PORT}/" /etc/nginx/templates/default.conf.template > /etc/nginx/conf.d/default.conf
rm -f /etc/nginx/sites-enabled/default
java -jar /app/app.jar &
i=0
until curl -sf http://127.0.0.1:8080/actuator/health >/dev/null; do
  i=$((i + 1))
  if [ "$i" -gt 60 ]; then
    echo "API did not become healthy" >&2
    exit 1
  fi
  sleep 2
done
exec nginx -g "daemon off;"
