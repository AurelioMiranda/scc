FROM tomcat:10.0-jdk17-openjdk
WORKDIR /usr/local/tomcat
ADD target/*.war webapps/tukano.war
COPY config/app.properties /path/in/container/app.properties
EXPOSE 8080