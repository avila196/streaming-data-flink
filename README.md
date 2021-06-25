# Trendify!-> find the trending topics among several texts live!
The current repository uses streaming texts (from a source served on a web app) to find the trending topics on them! It summarizes the data with time windows of 60 seconds, using slides of 2 seconds between each window, to find:
* Trending topics within the texts (using BERT pre-trained model and fine-tuning to classify among 41 different topics)
* Trending words (not accounting for stop nor offensive words)
* Trending offensive words (watch your mouth folk!)

## Tech Stack
The model uses the next technologies:
* Apache Flink: Does the heavy weight for processing the input streaming data enqueued using Apache Kafka.
* Apache Kafka: Ingests the data from the web source and serves it to the Flink main application.
* TensorFlow: Makes the predictions of the categories of the input texts. Uses a pre-trained model to classify between __ known topics.
* Docker: Both the backend (Flink processing app) and frontend are composed and deployed using Docker containers orchestrated with Docker Compose. 
* Maven: Project and package management tool used for the main Java project.

The following languages are used:
* Java: All Flink processes and Kafka connections (ingestion) are written in Java 8.
* Python: The web app that allows any user to input data and the web app that shows all results are written in Python using a Streamlit library.

## Deep Learning Model
-- small description goes here --

### Input dataset
#### Description
The input dataset was obtained from Kaggle: News Category Dataset (https://www.kaggle.com/rmisra/news-category-dataset)
This dataset contains around 200k news headlines from the year 2012 to 2018 obtained from HuffPost.

#### Categories
Categories (labels) and corresponding article counts are as follows:
* POLITICS: 32739
* WELLNESS: 17827
* ENTERTAINMENT: 16058
* TRAVEL: 9887
* STYLE & BEAUTY: 9649
* PARENTING: 8677
* HEALTHY LIVING: 6694
* QUEER VOICES: 6314
* FOOD & DRINK: 6226
* BUSINESS: 5937
* COMEDY: 5175
* SPORTS: 4884
* BLACK VOICES: 4528
* HOME & LIVING: 4195
* PARENTS: 3955
* THE WORLDPOST: 3664
* WEDDINGS: 3651
* WOMEN: 3490
* IMPACT: 3459
* DIVORCE: 3426
* CRIME: 3405
* MEDIA: 2815
* WEIRD NEWS: 2670
* GREEN: 2622
* WORLDPOST: 2579
* RELIGION: 2556
* STYLE: 2254
* SCIENCE: 2178
* WORLD NEWS: 2177
* TASTE: 2096
* TECH: 2082
* MONEY: 1707
* ARTS: 1509
* FIFTY: 1401
* GOOD NEWS: 1398
* ARTS & CULTURE: 1339
* ENVIRONMENT: 1323
* COLLEGE: 1144
* LATINO VOICES: 1129
* CULTURE & ARTS: 1030
* EDUCATION: 1004

### Model summarize
TBD!

## Text files (list of words)
In order to find the trending words, the following lists are used to remove stop words and offensive words:
* <code>stop_words.txt</code>: List of english stop words used by the NLTK library.
* <code>offensive_words.txt</code>: List of offensive/swear/profane words banned by Google in past years.

## How to run it locally?
TBD!

## How to run it in the cloud?
TBD!
