FROM tomcat:8.5.81-jdk8-openjdk-bullseye as build

WORKDIR /build

RUN wget https://dlcdn.apache.org/maven/maven-3/3.8.6/binaries/apache-maven-3.8.6-bin.tar.gz
RUN tar xzvf apache-maven-3.8.6-bin.tar.gz
ENV PATH="${PATH}:/build/apache-maven-3.8.6/bin"

COPY moesif-servlet-debug moesif-servlet-debug
COPY moesif-servlet-debug-example moesif-servlet-debug-example

RUN cd moesif-servlet-debug && mvn install
RUN cd moesif-servlet-debug-example && mvn install


FROM tomcat:8.5.81-jdk8-openjdk-bullseye as app

COPY --from=build /build/moesif-servlet-debug-example/target/moesif-servlet-debug-example-1.3.6.war webapps/ROOT.war
