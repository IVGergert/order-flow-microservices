FROM nginx:1.28-alpine

COPY order-service-frontend/nginx.conf /etc/nginx/conf.d/default.conf

COPY order-service-frontend/index.html /usr/share/nginx/html/
COPY order-service-frontend/css/ /usr/share/nginx/html/css/
COPY order-service-frontend/js/ /usr/share/nginx/html/js/

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]