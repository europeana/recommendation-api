# Builds a docker image from a locally built Maven war. Requires 'mvn package' to have been run beforehand
FROM tomcat:9.0-jre17
LABEL Author="Europeana Foundation <development@europeana.eu>"
WORKDIR /usr/local/tomcat/webapps

ENV ELASTIC_APM_VERSION 1.34.1
ADD https://repo1.maven.org/maven2/co/elastic/apm/elastic-apm-agent/$ELASTIC_APM_VERSION/elastic-apm-agent-$ELASTIC_APM_VERSION.jar /usr/local/elastic-apm-agent.jar

# Copy unzipped directory so we can mount config files in Kubernetes pod
COPY target/recommend/ ./ROOT/
