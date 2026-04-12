# Base image
FROM eclipse-temurin:21-jdk

# Working directory
WORKDIR /app

# Copy jar
COPY target/ExpenseTracker-0.0.1-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 9898

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
