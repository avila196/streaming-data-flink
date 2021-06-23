package trending;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

import org.apache.flink.util.Collector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer011;
import org.apache.flink.streaming.api.windowing.assigners.SlidingProcessingTimeWindows;

//Class that handles all input text from Kafka and outputs the trending words and phrases cleaned
public class TrendingTopics {

	//Declare Descriptors for values to broadcast (stop words and offensive words)
	public static final MapStateDescriptor<String, String> stopWordsDescriptor =
			new MapStateDescriptor<String, String>("stop_words", BasicTypeInfo.STRING_TYPE_INFO, BasicTypeInfo.STRING_TYPE_INFO);

	public static final MapStateDescriptor<String, String> offensiveWordsDescriptor =
			new MapStateDescriptor<String, String>("offensive_words", BasicTypeInfo.STRING_TYPE_INFO, BasicTypeInfo.STRING_TYPE_INFO);

	/**
	 * Main method that runs the Flink app enabling ingestion from Kafka
	 * @param args Command line args (not needed)
	 * @throws Exception
	 */
	@SuppressWarnings("serial")
	public static void main(String[] args) throws Exception {
		//Get execution environment object
		final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		env.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime);

		//Create DataStream for stop words and get its respective BroadcastStream object
		DataStream<String> stopWords = env.readTextFile("./data/stop_words.txt");
		BroadcastStream<String> stopWordsBroadcast = stopWords.broadcast(stopWordsDescriptor);

		//Create DataStream for offensive words and get its respective BroadcastStream object
		DataStream<String> offensiveWords = env.readTextFile("./data/offensive_words.txt");
		BroadcastStream<String> offensiveWordsBroadcast = offensiveWords.broadcast(offensiveWordsDescriptor);

		//Start listening to Kafka server for all input data. The code parses the data as it comes,
		//removing all punctuation and splitting it
		Properties prop = new Properties();
		prop.setProperty("bootstrap.servers", "127.0.0.1:9092");
		DataStream<ArrayList<String>> kafkaData = env.addSource(new FlinkKafkaConsumer011<String>("data", new SimpleStringSchema(), prop))
				.map(new MapFunction<String, ArrayList<String>>() {
					public ArrayList<String> map(String text) {
						//We filter out the words by removing all non-alphabetic chars, making all lower-case
						//and splitting by any whitespace to get all words
						return (ArrayList<String>)Arrays.asList(text.replaceAll("[^a-zA-Z ]", "").toLowerCase().split("\\s+"));
					}
				});

		//(1) Check against stop words and remove them from list of words
		DataStream<ArrayList<String>> reducedWords = kafkaData
				.connect(stopWordsBroadcast)
				.process(new StopWordsCheck());

		//(2) Check against offensive words and get just the offensive ones
		DataStream<ArrayList<String>> offensiveWordsFounds = reducedWords
				.connect(offensiveWordsBroadcast)
				.process(new OffensiveWordsCheck());

		//(3) Map all original words to categories and aggregate on the categories counts (adding them) to find the 
		//trending topics. The categories are found using a pre-trained ML classification model. A sliding window
		//is used with 60 sec size and 2 sec slide time.
		//TODO!

		//(4) Map all reduced words to counts and aggregate on the counts (adding them) to find the most common ones.
		//This is to find the meaningful trending words. A sliding window is used with 60 sec size and 2 sec slide time
		DataStream<Tuple2<String, Integer>> trendingWords = reducedWords
				.flatMap(new FlatMapFunction<ArrayList<String>, Tuple2<String, Integer>>() {
					public void flatMap(ArrayList<String> words, Collector<Tuple2<String, Integer>> out) {
						//Loop through all reduced words and map them to counts of 1
						for(String word : words) {
							out.collect(new Tuple2<String, Integer>(word, 1));
						}
					}
				}).keyBy(0)
				.window(SlidingProcessingTimeWindows.of(Time.seconds(60), Time.seconds(2)))
				.sum(1);

		//(5) Map all offensive words to counts and aggregate on the counts (adding them) to find the most common ones.
		//This is to find the most common offensive words. A sliding window is used with 60 sec size and 2 sec slide time
		DataStream<Tuple2<String, Integer>> mostCommonOffensive = offensiveWordsFounds
				.flatMap(new FlatMapFunction<ArrayList<String>, Tuple2<String, Integer>>() {
					public void flatMap(ArrayList<String> words, Collector<Tuple2<String, Integer>> out) {
						//Loop through all offensive words and map them to counts of 1
						for(String word : words) {
							out.collect(new Tuple2<String, Integer>(word, 1));
						}
					}
				}).keyBy(0)
				.window(SlidingProcessingTimeWindows.of(Time.seconds(60), Time.seconds(2)))
				.sum(1);

		//(5) Output all results found:
		//   - Trending topics (categories)
		//   - Trending words
		//   - Trending offensive words
		//TODO!

		// execute program
		env.execute("Streaming Trending Topics!");
	}

	@SuppressWarnings("serial")
	//Class that broadcasts all input words against stop words
	public static class StopWordsCheck extends BroadcastProcessFunction<ArrayList<String>, String, ArrayList<String>> {
		/**
		 * Processes the given non-broadcast element (list of words)
		 */
		public void processElement(ArrayList<String> words, ReadOnlyContext ctx, Collector<ArrayList<String>> out) throws Exception {
			//We start looping through all input words and we create a new ArrayList to store all non-stop words found
			ArrayList<String> nonStopWords = new ArrayList<String>();
			for (String inputWord : words) {
				//Check if current word is equal to any of the stop words
				boolean isStop = false;
				for(Map.Entry<String, String> stopWord : ctx.getBroadcastState(stopWordsDescriptor).immutableEntries()) {
					if(stopWord.getKey().equals(inputWord)) {
						//The current word is a stop word
						isStop = true;
						break;
					}
				}
				//Now, if the current word isn't a stop word, we add it to new list
				if(!isStop) {
					nonStopWords.add(inputWord);
				}
			}
			//Once the loop ends, the list "nonStopWords" contains all non stop words
			out.collect(nonStopWords);
		}

		/**
		 * Processes the given broadcast element
		 */
		public void processBroadcastElement(String word, Context ctx, Collector<ArrayList<String>> out) throws Exception {
			//We just add the put inside the broadcast state
			ctx.getBroadcastState(stopWordsDescriptor).put(word, word);
		} 
	}

	@SuppressWarnings("serial")
	//Class that broadcasts all input words against offensive words
	public static class OffensiveWordsCheck extends BroadcastProcessFunction<ArrayList<String>, String, ArrayList<String>> {
		/**
		 * Processes the given non-broadcast element (list of words)
		 */
		public void processElement(ArrayList<String> words, ReadOnlyContext ctx, Collector<ArrayList<String>> out) throws Exception {
			//We start looping through all offensive words and we create a new ArrayList to store all words found
			ArrayList<String> offensive = new ArrayList<String>();
			for (Map.Entry<String, String> stopWord: ctx.getBroadcastState(offensiveWordsDescriptor).immutableEntries()) {
				//Get next offensive word and check if any of the input words is equal
				String word = stopWord.getKey();
				for(String inputWord : words) {
					if(word.equals(inputWord)) {
						//The word is added to list of offensive words found
						offensive.add(word);
						break;
					}
				}
			}
			//Once the loop ends, the list "words" contains all non offensive words and the list "offensive" contains
			//all offensive words found, this is the one we need to return
			out.collect(offensive);
		}

		/**
		 * Processes the given broadcast element
		 */
		public void processBroadcastElement(String word, Context ctx, Collector<ArrayList<String>> out) throws Exception {
			//We just put the word inside the broadcast state
			ctx.getBroadcastState(stopWordsDescriptor).put(word, word);
		} 
	}
}