# Use the official Tomcat 10 image
FROM tomcat:10-jdk17

# Remove default web apps (optional, to keep the image clean)
RUN rm -rf /usr/local/tomcat/webapps/*

# Copy your WAR file into the webapps directory
COPY target/tukano-1.war /usr/local/tomcat/webapps/

# Expose port 8080
EXPOSE 8080