package edu.carleton.comp4601final;
import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;

public class SentimentAnalysis {
	private final static String NEGATIVE = "Negative";
	private final static String POSITIVE = "Positive";
	private final static String VERYNEGATIVE = "Very negative";
	private final static String VERYPOSITIVE = "Very positive";
	private final static String NUETRAL = "Nuetral";
	
	private StanfordCoreNLP pipeline;
	
	public SentimentAnalysis(){
		Properties props = new Properties();
	    props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref, sentiment");

	    pipeline = new StanfordCoreNLP(props);
	}
	
	
	//returns 1 if total doc is positive
	//returns -1 if total doc is negative
	public double analyse(String doc){
		Annotation annotation = new Annotation(doc);
		pipeline.annotate(annotation);
		//get the sentences
		List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
		int count = 0;
		for(int i = 0; i < sentences.size(); i++){
			String sentiment = sentences.get(i).get(SentimentCoreAnnotations.SentimentClass.class);
			System.out.println(sentences.get(i));
			System.out.println(sentiment);
			if(sentiment.equals(POSITIVE)){
				count += 1;
			}
			else if(sentiment.equals(NEGATIVE)){
				count -= 1;
			}
			else if(sentiment.equals(VERYPOSITIVE)){
				count += 2;
			}
			else if(sentiment.equals(VERYNEGATIVE)){
				count -= 2;
			}
			else if(sentiment.equals(NUETRAL)){
				count += 0;
			}
		}

		double percent = (double) Math.abs(count) / sentences.size();
		System.out.println(percent);
		return percent;
	}
}
