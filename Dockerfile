# ---------- Etapa 1: compilar el WAR con Maven ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Primero solo el pom: las dependencias quedan en una capa cacheada mientras no cambie.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B package -DskipTests

# ---------- Etapa 2: ejecutar en Tomcat ----------
FROM tomcat:10.1-jre21-temurin

# Sin las aplicaciones de ejemplo y con un usuario sin privilegios que pueda escribir
# en los directorios que Tomcat necesita.
RUN rm -rf "$CATALINA_HOME"/webapps/* \
    && groupadd --system tomcat \
    && useradd --system --gid tomcat --home-dir "$CATALINA_HOME" --shell /usr/sbin/nologin tomcat \
    && mkdir -p "$CATALINA_HOME"/conf/Catalina/localhost \
    && chown -R tomcat:tomcat "$CATALINA_HOME"/logs "$CATALINA_HOME"/temp "$CATALINA_HOME"/work "$CATALINA_HOME"/webapps \
        "$CATALINA_HOME"/conf/Catalina

COPY --from=build --chown=tomcat:tomcat /build/target/slime-tracker.war "$CATALINA_HOME"/webapps/ROOT.war

USER tomcat
EXPOSE 8080
CMD ["catalina.sh", "run"]
