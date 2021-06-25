FROM maven:3.8.1-jdk-8

VOLUME /usr/share/maven/ref/repository
VOLUME /root/.m2

RUN mkdir /docker/proj /docker/flink /docker/kafka -p
RUN cd /docker/flink && wget https://downloads.apache.org/flink/flink-1.13.1/flink-1.13.1-bin-scala_2.11.tgz
RUN cd /docker/kafka && wget https://downloads.apache.org/kafka/2.8.0/kafka_2.13-2.8.0.tgz

COPY /trending/pom.xml /docker/proj/pom.xml
COPY /trending/src /docker/proj/src

EXPOSE 8081

RUN cd /docker/proj && mvn clean package
RUN cd /docker/flink && tar -xf flink-1.13.1-bin-scala_2.11.tgz
RUN cd /docker/kafka && tar -xf kafka_2.13-2.8.0.tgz