package com.example.lucenedemo.config;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;
import java.util.Deque;
import java.util.LinkedList;

//public class NGramAnalyzer extends Analyzer {
//    private final int minGram;
//    private final int maxGram;
//    private final boolean preserveOriginal;
//
//    public NGramAnalyzer(int minGram, int maxGram, boolean preserveOriginal) {
//        this.minGram = minGram;
//        this.maxGram = maxGram;
//        this.preserveOriginal = preserveOriginal;
//    }
//
//    @Override
//    protected TokenStreamComponents createComponents(String fieldName) {
//        StandardTokenizer tokenizer = new StandardTokenizer();
//        TokenStream tokenStream = tokenizer;
//        tokenStream = new LowerCaseFilter(tokenStream);
//        tokenStream = new EdgeNGramTokenFilter(tokenStream, minGram, maxGram, preserveOriginal);
//        return new TokenStreamComponents(tokenizer, tokenStream);
//    }
//}

public class NGramAnalyzer extends Analyzer {
    private final int minGram;
    private final int maxGram;
    private final boolean preserveOriginal;

    public NGramAnalyzer(int minGram, int maxGram, boolean preserveOriginal) {
        this.minGram = minGram;
        this.maxGram = maxGram;
        this.preserveOriginal = preserveOriginal;
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer source = new StandardTokenizer();
        TokenStream result = new ReverseEdgeNGramTokenFilter(source, minGram, maxGram);
        return new TokenStreamComponents(source, result);
    }

    private static class ReverseEdgeNGramTokenFilter extends TokenFilter {
        private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
        private String currentToken = null;
        private Deque<String> nGrams = new LinkedList<>();
        private final int minGram;
        private final int maxGram;

        public ReverseEdgeNGramTokenFilter(TokenStream input, int minGram, int maxGram) {
            super(input);
            this.minGram = minGram;
            this.maxGram = maxGram;
        }

        @Override
        public boolean incrementToken() throws IOException {
            while (nGrams.isEmpty()) {
                if (!input.incrementToken()) {
                    currentToken = null;
                    return false;
                }
                currentToken = termAtt.toString();
                int len = currentToken.length();
                if (len < minGram) {
                    continue;
                }
                for (int i = len; i >= minGram; i--) {
                    nGrams.addLast(currentToken.substring(0, i));
                }
            }

            String nGram = nGrams.removeFirst();
            termAtt.setEmpty().append(nGram);
            return true;
        }

        @Override
        public void reset() throws IOException {
            super.reset();
            currentToken = null;
            nGrams.clear();
        }
    }
}
