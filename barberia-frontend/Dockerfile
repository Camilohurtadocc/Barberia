FROM nginx:alpine
# Reemplaza la config por defecto: la de serie no manda Cache-Control y el
# navegador acaba sirviendo un index.html viejo que apunta a un bundle que ya
# no existe. Ver los comentarios de nginx.conf.
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY dist/spa /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
