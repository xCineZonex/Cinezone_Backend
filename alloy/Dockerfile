FROM grafana/alloy:latest
COPY config.alloy /etc/alloy/config.alloy
COPY entrypoint.sh /entrypoint.sh
# Eliminar saltos de línea Windows (CRLF→LF) para que el script funcione en Linux
RUN sed -i 's/\r//' /entrypoint.sh && chmod +x /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]
