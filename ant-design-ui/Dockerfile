FROM nginx:1.22.0

RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \

    && mkdir -p /app

ADD dist /usr/share/nginx/html

ENTRYPOINT [ "nginx", "-g", "daemon off;"]

EXPOSE 80
