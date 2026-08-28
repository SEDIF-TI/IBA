# deploy/

Archivos de despliegue del servidor.

Aquí van las configuraciones del reverse proxy (Nginx Proxy Manager), plantillas
de systemd, scripts de respaldo de la base de datos y del volumen de documentos,
y cualquier artefacto que dependa del entorno de producción y no del código.

> El volumen `sea_storage` contiene documentos personales (INE, CURP,
> comprobantes de domicilio). Cualquier script de respaldo que se agregue aquí
> debe cifrar el destino.
