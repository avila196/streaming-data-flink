FROM maven:3.8.1-jdk-8

VOLUME /usr/share/maven/ref/repository
VOLUME /root/.m2

RUN apt-get update && apt-get install -y vim nano 
RUN mkdir /docker/proj /docker/flink /docker/kafka -p
RUN cd /docker/flink && wget https://downloads.apache.org/flink/flink-1.13.1/flink-1.13.1-bin-scala_2.11.tgz
RUN cd /docker/kafka && wget https://downloads.apache.org/kafka/2.8.0/kafka_2.13-2.8.0.tgz

COPY . /docker/proj

EXPOSE 8081 9092

RUN cd /docker/proj && mvn clean package
RUN cd /docker/flink && tar -xf flink-1.13.1-bin-scala_2.11.tgz
RUN cd /docker/kafka && tar -xf kafka_2.13-2.8.0.tgz

CMD cd /docker/kafka/kafka_2.13-2.8.0  && bin/zookeeper-server-start.sh -daemon config/zookeeper.properties \
	&& bin/kafka-server-start.sh -daemon config/server.properties \
	&& bin/kafka-topics.sh --create --topic news --bootstrap-server localhost:9092 \
	&& cd /docker/flink/flink-1.13.1 && bin/start-cluster.sh \
	&& bin/flink run /docker/proj/target/trending-0.0.1-SNAPSHOT-jar-with-dependencies.jar