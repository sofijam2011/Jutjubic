FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml i build dependencies (cache layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source i build aplikaciju
COPY src ./src
RUN mvn clean package -DskipTests

# Production image
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Install curl za health checks
RUN apk add --no-cache curl

# Copy JAR
COPY --from=build /app/target/*.jar app.jar

# Create uploads directory
RUN mkdir -p /app/uploads

# Expose port
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
