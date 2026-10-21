#!/bin/sh
set -eu
PORT="${PORT:-10000}"
sed "s/__PORT__/${PORT}/" /etc/nginx/templates/default.conf.template > /etc/nginx/conf.d/default.conf
rm -f /etc/nginx/sites-enabled/default
java -jar /app/app.jar &
API_PID=$!
nginx -g "daemon off;" &
NGINX_PID=$!
while kill -0 "$API_PID" 2>/dev/null && kill -0 "$NGINX_PID" 2>/dev/null; do
  sleep 2
done
exit 1
