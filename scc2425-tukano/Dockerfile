# Use an official JDK 17 runtime as the base image
FROM eclipse-temurin:17-jdk

# Set environment variables for Tomcat
ENV CATALINA_HOME /usr/local/tomcat
ENV PATH $CATALINA_HOME/bin:$PATH
ENV TOMCAT_VERSION 9.0.76

# Install Tomcat
RUN mkdir -p "$CATALINA_HOME" && \
    curl -fSL https://archive.apache.org/dist/tomcat/tomcat-9/v$TOMCAT_VERSION/bin/apache-tomcat-$TOMCAT_VERSION.tar.gz -o /tmp/tomcat.tar.gz && \
    tar -xvf /tmp/tomcat.tar.gz -C $CATALINA_HOME --strip-components=1 && \
    rm /tmp/tomcat.tar.gz

# Set the working directory in the container
WORKDIR /usr/local/tomcat

# Copy the application files (e.g., WAR file) into Tomcat's webapps directory
# Assuming you have a WAR file created by your build process in a `target` folder
COPY target/*.war $CATALINA_HOME/webapps/

# Copy the properties/secrets file into the Docker
COPY config/app.properties /path/in/container/app.properties

# Expose the Tomcat default port
EXPOSE 8080

# Run Tomcat when the container starts
CMD ["catalina.sh", "run"]