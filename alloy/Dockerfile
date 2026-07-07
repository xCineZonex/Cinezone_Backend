FROM grafana/alloy:latest
COPY config.alloy /etc/alloy/config.alloy
# Cloud Run requiere que el contenedor escuche en $PORT.
# Alloy tiene un servidor HTTP integrado, lo activamos en ese puerto.
ENTRYPOINT ["sh", "-c", "alloy run /etc/alloy/config.alloy --server.http.listen-addr=:${PORT:-8080}"]
