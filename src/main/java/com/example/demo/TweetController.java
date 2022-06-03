package com.example.demo;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class TweetController {
    static ArrayList<Tweet> tweets;
    static int numTweetFiles = 0;

    @EventListener(ApplicationReadyEvent.class)
    public void doSomethingAfterStartup() throws IOException {
        tweets = new ArrayList<Tweet>();


        try {
            File myObj = new File("tweets.txt");
      Scanner reader = new Scanner(myObj);
            while (reader.hasNextLine()) {
                String data = reader.nextLine();
                System.out.println(data);
              }
            reader.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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
