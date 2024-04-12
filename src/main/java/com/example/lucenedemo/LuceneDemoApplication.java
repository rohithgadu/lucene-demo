package com.example.lucenedemo;

import com.example.lucenedemo.config.CustomAnalyzer;
import com.example.lucenedemo.service.LuceneIndexService;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Paths;

@SpringBootApplication
public class LuceneDemoApplication {

    public static void main(String[] args) throws IOException {
        SpringApplication.run(LuceneDemoApplication.class, args);

//        String indexPath = "indexes"; // replace with your index path
//        String field = "tag_primaryVulnerabilityDescription"; // replace with your field
//        String termText = "79"; // replace with the term you want to check
//
//        Directory directory = FSDirectory.open(Paths.get(indexPath));
//        IndexReader reader = DirectoryReader.open(directory);
//        IndexSearcher searcher = new IndexSearcher(reader);
//
//        Term term = new Term(field, termText);
//        Query query = new TermQuery(term);
//        TopDocs topDocs = searcher.search(query, Integer.MAX_VALUE); // search for all documents containing the term
//
//        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
//            Document doc = searcher.doc(scoreDoc.doc);
//            String id = doc.get("id"); // assuming "id" is the field name of the document ID
//            System.out.println("The term '" + termText + "' is present in the document with ID: " + id);
//        }
//        System.out.println("Total documents containing the term '" + termText + "': " + topDocs.totalHits.value);
////
////        reader.close();
////        directory.close();
//
//
//        Analyzer analyzer = new CustomAnalyzer();
//// Iterate over all documents in the index
//        for (int i = 0; i < reader.maxDoc(); i++) {
//            Document doc = reader.document(i);
//            String fieldValue = doc.get(field);
//
//            // Tokenize the field value
//            TokenStream tokenStream = analyzer.tokenStream(field, new StringReader(fieldValue));
//            CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
//            tokenStream.reset();
//
//            // Print the tokens
//            while (tokenStream.incrementToken()) {
//                String token = charTermAttribute.toString();
//                System.out.println("Token: " + token);
//            }
//
//            tokenStream.end();
//            tokenStream.close();
//        }
//
//        reader.close();
//        directory.close();
    }

}