package com.example.demo;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;



@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class TweetController {

    @RequestMapping("/tweets")
    public String index(@RequestParam(required = false, defaultValue = "") String query) {
        System.out.println("Query: " + query);

        Analyzer analyzer = new StandardAnalyzer();

        if (query.isEmpty()) {
            return "No query provided.";
        }

        return "Hello Spring Boot!";
    }

}
