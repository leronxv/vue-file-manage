FROM swr.cn-east-2.myhuaweicloud.com/yxgw-prod/basic-template-image:8

WORKDIR /app

ADD target/app.jar ./

ENTRYPOINT ["java","-Dfile.encoding=utf-8","-Djava.security.egd=file:/dev/./urandom","-jar","app.jar"]

EXPOSE 9999
