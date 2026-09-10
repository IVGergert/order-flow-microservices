FROM nginx:1.28-alpine

COPY delivery-service-frontend/nginx.conf /etc/nginx/conf.d/default.conf

COPY delivery-service-frontend/index.html /usr/share/nginx/html/
COPY delivery-service-frontend/css/ /usr/share/nginx/html/css/
COPY delivery-service-frontend/js/ /usr/share/nginx/html/js/

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]