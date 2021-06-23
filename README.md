# Trendify!-> find the trending topics among several texts live!
The current repository uses streaming texts (from a source served on a web app) to find the trending topics on them! It summarizes the data with time windows of 60 seconds, using slides of 2 seconds between each window, to find:
* Trending topics within the texts (using ML classification model)
* Trending words (not accounting for stop nor offensive words)
* Trending offensive words (watch your mouth folk!)

## Tech Stack
The model uses the next technologies:
* Apache Flink: Does the heavy weight for processing the input streaming data enqueued using Apache Kafka.
* Apache Kafka: Ingests the data from the web source and serves it to the Flink main application.
* TensorFlow: Makes the predictions of the categories of the input texts. Uses a pre-trained model to classify between __ known topics.
* Docker: Both the backend (Flink processing app) and frontend are composed and deployed using Docker containers orchestrated with Docker Compose. 

The following languages are used:
* Java: All Kafka connections (ingestion) and Flink processes are written in Java 8.
* Python: The web app that allows any user to input data and the web app that shows all results are written in Python using a Streamlit library.

## How to run it locally?
TBD!

## How to run it in the cloud?
TBD!
