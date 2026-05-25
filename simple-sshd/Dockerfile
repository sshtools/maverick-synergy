FROM eclipse-temurin:17-jre

WORKDIR /app

# Build the jar first with:
# mvn -DskipTests package -pl simple-sshd
COPY target/simple-sshd-3.2.0-SNAPSHOT.jar /app/simple-sshd.jar

EXPOSE 2222

ENTRYPOINT ["java", "-jar", "/app/simple-sshd.jar"]
CMD ["--port", "2222"]
