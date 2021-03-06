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

import java.util.Locale;

import java.util.HashMap;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.Instant;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.json.simple.parser.JSONParser;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.ScoreDoc;


@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class TweetController {
    Directory tweetIndexDirectory;
    //
    IndexWriter tweetIndexer;
    // todo: change this to the number of tweet files you have in the tweets folder
    static int numTweetFiles = 65;

    @EventListener(ApplicationReadyEvent.class)
    public void loadTweets() throws FileNotFoundException, IOException, org.json.simple.parser.ParseException {
        tweetIndexDirectory = FSDirectory.open(FileSystems.getDefault().getPath("./", "index"));

        Analyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);

        tweetIndexer = new IndexWriter(tweetIndexDirectory, config);

        tweetIndexer.deleteAll();

        // HashMap<String, Float> boosts = new HashMap<String, Float>();
        // boosts.put(fields[0], 2.0f);
        // boosts.put(fields[1], 1.0f);
        // MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, analyzer, boosts);

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

                // Formats the date
                // Format: DD/MM/YYYY, HH:MM
                // 26/05/2022, 13:24
                String tweetDate = tweetData.get("created_at").toString();
                Instant instantDate = Instant.parse(tweetDate);
                DateTimeFormatter format = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(Locale.UK).withZone(ZoneId.systemDefault());
                String dateTime = format.format(instantDate);
                
                // Splits into date and time
                String[] dateTimeSplit = dateTime.split(", ", 2);
                String date = dateTimeSplit[0];
                String time = dateTimeSplit[1];
                tweetDoc.add(new StringField("date", date, Field.Store.YES));
                tweetDoc.add(new StringField("time", time, Field.Store.YES));
                
                // Adds username
                JSONObject tweetIncludes = (JSONObject) tweet.get("includes");
                JSONArray users = (JSONArray) tweetIncludes.get("users");
                JSONObject user = (JSONObject) users.get(0);
                String username = user.get("username").toString();
                tweetDoc.add(new StringField("username", username, Field.Store.YES));

                // Adds link titles
                if (user.containsKey("entities")) {
                    JSONObject entities = (JSONObject) user.get("entities");
                    if (entities.containsKey("url")) {
                        JSONObject url = (JSONObject) entities.get("url");
                        JSONArray urls = (JSONArray) url.get("urls");
                        JSONObject firstUrl = (JSONObject) urls.get(0);
                        if (firstUrl.containsKey("title")) {
                            String title = firstUrl.get("title").toString();
                            tweetDoc.add(new StringField("link", title, Field.Store.YES));
                        }
                    }
                }

                tweetIndexer.addDocument(tweetDoc);

            }
        }

        System.out.println("Done adding tweets to Lucene.");
        tweetIndexer.close();
    }

    @RequestMapping("/tweets")
    public JSONArray index(@RequestParam(required = false, defaultValue = "") String query) throws ParseException, IOException {
        String[] fields = {"text","username", "date", "time", "link"};
        HashMap<String, Float> boosts = new HashMap<String, Float>();
        boosts.put(fields[0], 2.5f); //text; can change the boost number
        boosts.put(fields[1], 2.0f); //username; can change the boost number
        boosts.put(fields[2], 1.0f); //date; shouldn't be boosted
        boosts.put(fields[3], 1.0f); //time; shouldn't be boosted
        boosts.put(fields[4], 1.5f); //title;

        tweetIndexDirectory = FSDirectory.open(FileSystems.getDefault().getPath("./", "index"));

        IndexReader reader = DirectoryReader.open(tweetIndexDirectory);

        IndexSearcher indexSearcher = new IndexSearcher(reader);
        indexSearcher.setSimilarity(new BM25Similarity());

        StandardAnalyzer analyzer = new StandardAnalyzer();
        MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, analyzer, boosts);

        JSONArray docList = new JSONArray();

        if (query.isEmpty()) {
            return docList;
        }

        Query myQuery = parser.parse(query);

        System.out.println(myQuery);

        //var hits = indexSearcher.search(myQuery, 10);
        TopDocs hits = indexSearcher.search(myQuery, 10);

        for(ScoreDoc scoreDoc : hits.scoreDocs){
            Document doc = indexSearcher.doc(scoreDoc.doc);
            System.out.println(doc.get("text"));
            System.out.println(doc.get("username"));
            System.out.println(doc.get("link"));
            System.out.println(doc.get("date"));
            System.out.println(doc.get("time") + "\n");
        }

        ScoreDoc[] docs = hits.scoreDocs;
        
        for (int i = 0; i < docs.length; i++) {
            JSONObject docJSON = new JSONObject();
            docJSON.put("text", indexSearcher.doc(docs[i].doc).getField("text").stringValue());
            docJSON.put("username", indexSearcher.doc(docs[i].doc).getField("username").stringValue());
            if(indexSearcher.doc(docs[i].doc).getField("title") != null){
                docJSON.put("title", indexSearcher.doc(docs[i].doc).getField("title").stringValue());
            }
            docJSON.put("date", indexSearcher.doc(docs[i].doc).getField("date").stringValue());
            docJSON.put("time", indexSearcher.doc(docs[i].doc).getField("time").stringValue());
            docList.add(docJSON);
        }
        
        System.out.println("Found " + hits.totalHits + " tweets.");

        return docList;
    }
}
