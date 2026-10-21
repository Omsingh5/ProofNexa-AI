FROM node:20-alpine AS web
WORKDIR /web
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM eclipse-temurin:17-jdk-jammy AS api
WORKDIR /api
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*
COPY backend/pom.xml .
COPY backend/src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre-jammy
RUN apt-get update && apt-get install -y nginx curl && rm -rf /var/lib/apt/lists/*
COPY --from=web /web/dist /usr/share/nginx/html
COPY --from=api /api/target/proofora-api-0.1.0.jar /app/app.jar
COPY deploy/nginx.conf.template /etc/nginx/templates/default.conf.template
COPY deploy/start.sh /start.sh
RUN sed -i 's/\r$//' /start.sh && chmod +x /start.sh
ENV PORT=10000
ENV JAVA_TOOL_OPTIONS="-Xmx300m"
EXPOSE 10000
CMD ["/start.sh"]
