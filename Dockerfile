FROM tomcat:8.5.81-jdk8-openjdk-bullseye as build

WORKDIR /build

RUN wget https://dlcdn.apache.org/maven/maven-3/3.8.6/binaries/apache-maven-3.8.6-bin.tar.gz
RUN tar xzvf apache-maven-3.8.6-bin.tar.gz
ENV PATH="${PATH}:/build/apache-maven-3.8.6/bin"

COPY pom.xml pom.xml
COPY moesif-servlet-debug/pom.xml moesif-servlet-debug/pom.xml
COPY moesif-servlet-debug-example/pom.xml moesif-servlet-debug-example/pom.xml

# Resolve dependencies for `common` module, e.g., shared libraries
# Also build all the required projects needed by the common module.
# In this case, it will also resolve dependencies for the `root` module.
RUN mvn -q -ntp -B -pl :moesif-servlet-debug -am dependency:go-offline

# Resolve dependencies for the main application
RUN mvn -q -ntp -B -pl :moesif-servlet-debug-example -am dependency:go-offline

# Copy full sources for `common` module
COPY moesif-servlet-debug moesif-servlet-debug

# Install the common module in the local Maven repo (`.m2`)
# This will also install the `root` module.
# See: `la -lat ~/.m2/repository/org/example/*/*`
RUN mvn -q -B -pl moesif-servlet-debug install

# Resolve dependencies for the main application
RUN mvn -q -ntp -B -pl moesif-servlet-debug-example -am dependency:go-offline
# Copy sources for main application
COPY moesif-servlet-debug-example moesif-servlet-debug-example
# Package the common and application modules together
RUN mvn -q -ntp -B -pl moesif-servlet-debug-example install


FROM tomcat:8.5.81-jdk8-openjdk-bullseye as app

COPY --from=build /build/target/servlet-example-1.3.6.war webapps/ROOT.war
