# Europeana Recommendation API

Spring-Boot2 web application for recommending Cultural Heritage Objects (CHOs) or sets of CHOs (see also 
[Europeana's Set API](https://github.com/europeana/set-api)). 
The application is basically a wrapper around an external recommendation engine and uses Search API to
return answers in the same format as used by Search API.

## Prerequisites
 * Java 11
 * Maven<sup>*</sup> 
 * [Europeana parent pom](https://github.com/europeana/europeana-parent-pom)
 
 <sup>* A Maven installation is recommended, but you could use the accompanying `mvnw` (Linux) or `mvnw.cmd` (Windows) 
  files instead.
  
 ## Configure
 Specify a host name for:
 <li>Recommendation engine</li>
 <li>Search API</li> 
  
 ## Run
 
 The application has a Tomcat web server that is embedded in Spring-Boot.
 
 Either select the `RecommendApplication` class in your IDE and 'run' it
 
 or 
 
 go to the application root where the pom.xml is located and excute  
 `./mvnw spring-boot:run` (Linux) or `mvnw.cmd spring-boot:run` (Windows)
 
 
 ## License
 
 Licensed under the EUPL 1.2. For full details, see [LICENSE.md](LICENSE.md).