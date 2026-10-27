FROM maven:3.9-eclipse-temurin-21 AS construction
WORKDIR /construction

COPY pom.xml ./
COPY src ./src
RUN mvn -B -ntp verify

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=construction /construction/target/railpulse-0.0.1-SNAPSHOT.jar ./railpulse.jar

ENV SPRING_PROFILES_ACTIVE=supabase,render
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60.0 -XX:+ExitOnOutOfMemoryError"

USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/railpulse.jar"]
