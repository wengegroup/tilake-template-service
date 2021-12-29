FROM  openjdk:8-jdk-alpine
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
# 拷贝分解到/app目录
COPY target/tilake-template-service-1.0-SNAPSHOT.jar /atlas/atlas.jar
# 分配权限
RUN chmod -R 777 /atlas
# 设置工作目录
WORKDIR /atlas
# 挂载配置文件卷
EXPOSE 30999
ENTRYPOINT ["java","-jar","/atlas/atlas.jar"]




