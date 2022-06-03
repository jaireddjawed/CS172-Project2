package com.example.demo;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
//import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
//import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.json.simple.parser.JSONParser;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;


@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class TweetController {
    //
    IndexWriter tweetIndexer;
    // todo: change this to the number of tweet files you have in the tweets folder
    static int numTweetFiles = 5;

    @EventListener(ApplicationReadyEvent.class)
    public void loadTweets() throws FileNotFoundException, IOException, org.json.simple.parser.ParseException {
        Directory tweetIndexDirectory = FSDirectory.open(FileSystems.getDefault().getPath("./", "index"));

        Analyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);

        tweetIndexer = new IndexWriter(tweetIndexDirectory, config);

        JSONParser parser = new JSONParser();
        System.out.println("Loading tweets to Lucene...");

        // iterate through all of the tweet files, file starts at index 1
        for (int i = 1; i <= numTweetFiles; i++) {
            Object tweetFile = parser.parse(
                new FileReader("./tweets/tweets_"+i+"_with_titles.json")
            );

            JSONArray tweets = (JSONArray) tweetFile;
            var tweetIterator = tweets.iterator();

            // iterate throught all of the tweets in the file
            while(tweetIterator.hasNext()) {
                JSONObject tweet = (JSONObject) tweetIterator.next();
                JSONObject tweetData = (JSONObject) tweet.get("data");

                // create and add fields to tweet doc
                // adding tweet text for now, we can add other fields like username, etc.
                Document tweetDoc = new Document();
                tweetDoc.add(new Field("text", tweetData.get("text").toString(), TextField.TYPE_STORED));
                tweetIndexer.addDocument(tweetDoc);
            }
        }

        System.out.println("Done adding tweets to Lucene.");
    }

    @RequestMapping("/tweets")
    public String index(@RequestParam(required = false, defaultValue = "") String query) {
        Analyzer analyzer = new StandardAnalyzer();
        String[] fields = {"text"};

        //MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, analyzer);
        //Query myQuery = parser.parse(query);

        //System.out.println(myQuery);

        if (query.isEmpty()) {
            return "No query provided.";
        }

        return "Hello Spring Boot!";
    }
}
