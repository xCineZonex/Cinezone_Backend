#!/bin/sh
# Cloud Run inyecta el puerto en $PORT. Alloy debe escuchar ahí.
exec /bin/alloy run /etc/alloy/config.alloy \
  --server.http.listen-addr=0.0.0.0:${PORT:-8080} \
  --storage.path=/tmp/alloy-data \
  --stability.level=generally-available
