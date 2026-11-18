# ============================================================================
#  校园活动管理系统 后端镜像
#
#  只构建后端，前端单独部署在 Vercel（两者都走 https，不会触发混合内容拦截）
#  Render / Koyeb / 阿里云函数计算 等支持 Docker 的平台都可以直接用
# ============================================================================

# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-8 AS build

WORKDIR /build
# 先只拷 pom，利用镜像层缓存，改代码时不用重新下依赖
COPY pom.xml .
COPY scripts/settings.xml scripts/settings.xml
RUN mvn -s scripts/settings.xml -B -q dependency:go-offline || true

COPY src ./src
COPY db ./db
RUN mvn -s scripts/settings.xml -B -q -DskipTests package

# ---------- 运行阶段 ----------
FROM eclipse-temurin:8-jre

WORKDIR /app

ENV TZ=Asia/Shanghai
# 免费实例普遍 512MB 内存，堆上限压到 256m，避免被 OOM Kill
ENV JAVA_OPTS="-Xms128m -Xmx256m -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"

COPY --from=build /build/target/campus-activity.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
