# Base image
FROM eclipse-temurin:21-jdk

# Working directory
WORKDIR /app

# Copy jar
COPY target/userservice-0.0.1-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 9810

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
