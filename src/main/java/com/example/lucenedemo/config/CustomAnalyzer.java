package com.example.lucenedemo.config;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ngram.EdgeNGramTokenFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

public class CustomAnalyzer extends Analyzer {
    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        StandardTokenizer tokenizer = new StandardTokenizer();
        TokenStream tokenStream = tokenizer;
        tokenStream = new EdgeNGramTokenFilter(tokenStream, 3, 8, true);
        tokenStream = new LowerCaseFilter(tokenStream);
//        tokenStream = new PhoneticFilter(tokenStream, new DoubleMetaphone(), false);
        return new TokenStreamComponents(tokenizer, tokenStream);
    }
}

