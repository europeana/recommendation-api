# Europeana Recommendation API

Spring-Boot3 web application for recommending similar or relevant Cultural Heritage Objects (CHOs) for
another CHO, a set of CHOs or an entity.
The application uses Search API to return answers in the same format as used by Search API.

## Prerequisites
To build:
 * Java 17
 * Maven<sup>*</sup> 
 * [Europeana parent pom](https://github.com/europeana/europeana-parent-pom)
 * [Europeana recommendations-updater](https://github.com/europeana/recommendations-updater) (for common files)

<sup>* A Maven installation is recommended, but you could use the accompanying `mvnw` (Linux) or `mvnw.cmd` (Windows)
files instead.

## Properties to configure
  <li>Milvus database hostname and port</li>
  <li>Embeddings API endpoint</li> 
  <li>Search API endpoint</li> 
  <li>Set API endpoint</li>
  <li>Entity API endpoint</li> 

## Build
``mvn clean install`` (add ``-DskipTests``) to skip the unit tests during build

## Run locally
The application has a Tomcat web server that is embedded in Spring-Boot.
Either select the `RecommendApplication` class in your IDE and 'run' it
 
 or 
 
go to the application root where the pom.xml is located and excute  
 `./mvnw spring-boot:run` (Linux) or `mvnw.cmd spring-boot:run` (Windows)
 
## Deployment
1. Generate a Docker image using the project's [Dockerfile](Dockerfile)

2. Configure the application by generating a `iiif.user.properties` file and placing this in the 
[k8s](k8s) folder. After deployment this file will override the settings specified in the `iiif.properties` file
located in the [src/main/resources](src/main/resources) folder. The .gitignore file makes sure the .user.properties file
is never committed.

3. Configure the deployment by setting the proper environment variables specified in the configuration template files
in the [k8s](k8s) folder

4. Deploy to Kubernetes infrastructure
   
## License
Licensed under the EUPL 1.2. For full details, see [LICENSE.md](LICENSE.md).
