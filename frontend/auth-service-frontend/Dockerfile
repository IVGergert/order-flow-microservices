FROM nginx:1.28-alpine

COPY auth-service-frontend/nginx.conf /etc/nginx/conf.d/default.conf

COPY common/ /usr/share/nginx/html/common/
COPY auth-service-frontend/index.html /usr/share/nginx/html/
COPY auth-service-frontend/css/ /usr/share/nginx/html/css/
COPY auth-service-frontend/js/ /usr/share/nginx/html/js/

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]