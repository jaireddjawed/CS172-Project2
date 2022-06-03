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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;

import org.json.simple.parser.JSONParser;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;


@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class TweetController {
    // todo: change this to the number of tweet files you have in the tweets folder
    static int numTweetFiles = 5;

    @EventListener(ApplicationReadyEvent.class)
    public void loadTweets() throws FileNotFoundException, IOException, org.json.simple.parser.ParseException {
        JSONParser parser = new JSONParser();

        // iterate through all of the tweet files
        for (int i = 1; i <= numTweetFiles; i++) {
            Object tweetFile = parser.parse(
                new FileReader("./tweets/tweets_"+i+"_with_titles.json")
            );

            JSONArray tweets = (JSONArray) tweetFile;
            var tweetIterator = tweets.iterator();

            while(tweetIterator.hasNext()) {
                JSONObject tweet = (JSONObject) tweetIterator.next();
                System.out.println(tweet);
            }
        }
    }

    @RequestMapping("/tweets")
    public String index(@RequestParam(required = false, defaultValue = "") String query) throws ParseException {
        Analyzer analyzer = new StandardAnalyzer();
        String[] fields = {"text"};

        MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, analyzer);
        Query myQuery = parser.parse(query);

        System.out.println(myQuery);

        if (query.isEmpty()) {
            return "No query provided.";
        }

        return "Hello Spring Boot!";
    }
}
