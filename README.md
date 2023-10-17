# Europeana Recommendation API

Spring-Boot2 web application for recommending similar or relevant Cultural Heritage Objects (CHOs) for
another CHO, a set of CHOs or an entity.
The application uses Search API to return answers in the same format as used by Search API.

## Prerequisites
To build:
 * Java 17
 * Maven<sup>*</sup> 
 * [Europeana parent pom](https://github.com/europeana/europeana-parent-pom)
 * [Europeana recommendations-updater](https://github.com/europeana/recommendations-updater)

<sup>* A Maven installation is recommended, but you could use the accompanying `mvnw` (Linux) or `mvnw.cmd` (Windows)
files instead.


 ## To configure
  <li>Milvus database hostname and port</li>
  <li>Embeddings API endpoint</li> 
  <li>Search API endpoint</li> 
  <li>Set API endpoint</li>
  <li>Entity API endpoint</li> 

  
 ## Run
 
 The application has a Tomcat web server that is embedded in Spring-Boot.
 
 Either select the `RecommendApplication` class in your IDE and 'run' it
 
 or 
 
 go to the application root where the pom.xml is located and excute  
 `./mvnw spring-boot:run` (Linux) or `mvnw.cmd spring-boot:run` (Windows)
 
 
 ## License
 
 Licensed under the EUPL 1.2. For full details, see [LICENSE.md](LICENSE.md).