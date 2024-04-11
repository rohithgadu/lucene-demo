package com.example.lucenedemo.service;

import com.example.lucenedemo.config.CustomAnalyzer;
import com.example.lucenedemo.model.ItemResult;
import com.example.lucenedemo.repository.ItemResultRepository;
import jakarta.annotation.PostConstruct;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class LuceneIndexService {

    private Directory memoryIndex;
    private Analyzer analyzer;
    private static final String INDEXED_IDS_FILE = "indexed_ids.txt";
    private static final String RESULT_IDS = "result_ids.txt";

    @Autowired
    private ItemResultRepository itemResultRepository;

    @PostConstruct
    public void init() throws IOException {
        Path path = Paths.get("indexes");
        memoryIndex = FSDirectory.open(path);
        analyzer = new CustomAnalyzer();
//        indexData();
    }

    public void indexData() {
        Set<String> indexedIds = loadIndexedIds(); // Load previously indexed document IDs
        int pageNumber = 0;
        int pageSize = 1000;
        boolean newDocumentsIndexed = true; // Flag to track if new documents are indexed
        while (newDocumentsIndexed) {
            Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "id"));
            Page<ItemResult> latestItems = itemResultRepository.findAll(pageable);
            if (latestItems.isEmpty()) {
                // No more items to index, exit loop
                break;
            }

            try (IndexWriter indexWriter = new IndexWriter(memoryIndex, new IndexWriterConfig(analyzer))) {
                newDocumentsIndexed = false; // Reset the flag before iterating through items
                for (ItemResult data : latestItems) {
                    if (!indexedIds.contains(data.getId())) { // Check if the document ID is already indexed
                        Document document = new Document();
                        // Add fields to the document
                        document.add(new TextField("id", data.getId(), Field.Store.YES));
                        document.add(new TextField("message", data.getMessage(), Field.Store.YES));
                        // Add tags to the document
                        Map<String, String> tags = data.getTags();
                        if (tags != null) {
                            for (Map.Entry<String, String> tag : tags.entrySet()) {
                                if (tag.getKey().equals("scanFullDate")) {
                                    // Handle date tags
                                    String dateValue = tag.getValue();
                                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
                                    ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateValue, formatter);
                                    long timestamp = zonedDateTime.toInstant().toEpochMilli();
                                    document.add(new LongPoint("tag_" + tag.getKey(), timestamp));
                                    document.add(new StoredField("tag_" + tag.getKey() + "_string", dateValue));
                                } else if (tag.getKey().equals("primaryVulnerabilitycvssScore") || tag.getKey().equals("epssScore")) {
                                    // Handle numeric tags
                                    double score = tag.getValue().isEmpty() ? 0 : Double.parseDouble(tag.getValue());
                                    document.add(new DoublePoint("tag_" + tag.getKey(), score));
                                } else {
                                    // Handle other tags
                                    document.add(new TextField("tag_" + tag.getKey(), tag.getValue().toLowerCase(), Field.Store.YES));
                                }
                            }
                        }
                        // Add filter tags to the document
                        List<Map<String, String>> filterTags = data.getFilterTags();
                        if (filterTags != null) {
                            for (Map<String, String> filterTag : filterTags) {
                                for (Map.Entry<String, String> entry : filterTag.entrySet()) {
                                    document.add(new TextField("filterTag_" + entry.getKey(), entry.getValue().toLowerCase(), Field.Store.YES));
                                }
                            }
                        }
                        try {
                            // Add the document to the index
                            indexWriter.addDocument(document);
                            indexedIds.add(data.getId()); // Update indexed IDs set
                            System.out.println("Indexed document: " + data.getId());
                            newDocumentsIndexed = true; // Set the flag to true if new documents are indexed
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            pageNumber++; // Move to the next page for the next iteration
        }

        saveIndexedIds(indexedIds); // Save updated indexed IDs to file
    }



    private Set<String> loadIndexedIds() {
        Set<String> indexedIds = new HashSet<>();
        try {
            List<String> lines = Files.readAllLines(Paths.get(INDEXED_IDS_FILE));
            indexedIds.addAll(lines);
        } catch (IOException e) {
            System.out.println("File not found: " + INDEXED_IDS_FILE);
        }
        return indexedIds;
    }

    private void saveIndexedIds(Set<String> indexedIds) {
        try {
            Files.write(Paths.get(INDEXED_IDS_FILE), indexedIds);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public List<ItemResult> search(String queryString) {
        List<ItemResult> results = new ArrayList<>();
        IndexReader reader = null;
        try {
            reader = DirectoryReader.open(memoryIndex);
            IndexSearcher searcher = new IndexSearcher(reader);

            // Retrieve all indexed fields from the index reader
            Set<String> indexedFields = new HashSet<>();
            for (LeafReaderContext leaf : reader.leaves()) {
                LeafReader leafReader = leaf.reader();
                for (FieldInfo fieldInfo : leafReader.getFieldInfos()) {
                    indexedFields.add(fieldInfo.name);
                }
            }

            // Convert the set of fields to an array
            String[] fields = indexedFields.toArray(new String[0]);

            // Create a BooleanQuery to combine the exact match and fuzzy match queries
            BooleanQuery.Builder combinedQuery = new BooleanQuery.Builder();

            // Split the query string into words
            String[] words = queryString.split("\\s+");

            for (String field : fields) {
                // Create a PhraseQuery for exact match
                PhraseQuery.Builder phraseQueryBuilder = new PhraseQuery.Builder();
                for (String word : words) {
                    phraseQueryBuilder.add(new Term(field, word));
                }
                PhraseQuery phraseQuery = phraseQueryBuilder.build();
//                PhraseQuery phraseQuery = new PhraseQuery(field, queryString);

                // Boost the PhraseQuery
                float boostValue = 2.0f; // Adjust this value as needed
                BoostQuery boostedPhraseQuery = new BoostQuery(phraseQuery, boostValue);

                // Create a BooleanQuery for fuzzy match
                BooleanQuery.Builder fuzzyQueryBuilder = new BooleanQuery.Builder();
//                for (String word : words) {
//                    FuzzyQuery fuzzyQuery = new FuzzyQuery(new Term(field, word), 2); // Adjust the edit distance as needed
//                    fuzzyQueryBuilder.add(fuzzyQuery, BooleanClause.Occur.SHOULD);
//                }
                FuzzyQuery fuzzyQuery = new FuzzyQuery(new Term(field, queryString), 1); // Adjust the edit distance as needed
//                BooleanQuery fuzzyQuery = fuzzyQueryBuilder.build();

                // Add the exact match and fuzzy match queries to the combined query
                combinedQuery.add(boostedPhraseQuery, BooleanClause.Occur.SHOULD);
                combinedQuery.add(fuzzyQuery, BooleanClause.Occur.SHOULD);
            }
            System.out.println(combinedQuery.build());
            // Search for documents matching the combined query
            TopDocs topDocs = searcher.search(combinedQuery.build(), 10);

            // Iterate over the retrieved documents
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                System.out.println(scoreDoc);
                Document doc = searcher.doc(scoreDoc.doc);

                ItemResult itemResult = new ItemResult();
                itemResult.setId(doc.get("id"));
                itemResult.setMessage(doc.get("message"));

                // Retrieve tags
                Map<String, String> tags = new HashMap<>();
                for (IndexableField field : doc.getFields()) {
                    String fieldName = field.name();
                    if (fieldName.startsWith("tag_")) {
                        String tagName = fieldName.substring(4); // remove "tag_" prefix
                        tags.put(tagName, field.stringValue());
                    }
                }
                itemResult.setTags(tags);

                // Retrieve filterTags
                List<Map<String, String>> filterTags = new ArrayList<>();
                for (IndexableField field : doc.getFields()) {
                    String fieldName = field.name();
                    if (fieldName.startsWith("filterTag_")) {
                        String filterTagName = fieldName.substring(10); // remove "filterTag_" prefix
                        Map<String, String> filterTag = new HashMap<>();
                        filterTag.put(filterTagName, field.stringValue());
                        filterTags.add(filterTag);
                    }
                }
                itemResult.setFilterTags(filterTags);

                results.add(itemResult);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Close the IndexReader instance
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return results;
    }

    public Long searchField(String queryString,String secondQueryString,String dateRange, List<Double> scoreRange) {
        List<ItemResult> results = new ArrayList<>();
        long totalHits = 0L;
        IndexReader reader = null;
        try {
            reader = DirectoryReader.open(memoryIndex);
            IndexSearcher searcher = new IndexSearcher(reader);

            StandardQueryParser queryParser = new StandardQueryParser(analyzer);
            Query queryStringQuery = queryParser.parse(queryString,"");

            // Combine the queries using a BooleanQuery
            BooleanQuery.Builder combinedQuery = new BooleanQuery.Builder();
            combinedQuery.add(queryStringQuery, BooleanClause.Occur.MUST);

            if(!secondQueryString.isEmpty()){
                String[] words = secondQueryString.toLowerCase().split("\\s+");
                String field = "tag_primaryVulnerabilityDescription";
                for (String word : words) {
                    FuzzyQuery fuzzyQuery = new FuzzyQuery(new Term(field, word), 2); // 2 is the maximum edit distance
                    combinedQuery.add(fuzzyQuery, BooleanClause.Occur.MUST);
                }
            }

            if(!dateRange.isEmpty()){
                String[] dateRangeArray = dateRange.split(" ");
                String startDate = dateRangeArray[0];
                String endDate = dateRangeArray[1];

                // Parse the dates to the format used in the index
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
                LocalDate startLocalDate = LocalDate.parse(startDate, formatter);
                LocalDate endLocalDate = LocalDate.parse(endDate, formatter);
                long startTimestamp = startLocalDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
                long endTimestamp = endLocalDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();

                System.out.println(startLocalDate);
                System.out.println(endLocalDate);

                System.out.println("Start timestamp: " + startTimestamp);
                System.out.println("End timestamp: " + endTimestamp);

                // Create a range query for the "tag_scanFullDate" field
                Query dateRangeQuery = LongPoint.newRangeQuery("tag_scanFullDate", startTimestamp, endTimestamp);
                combinedQuery.add(dateRangeQuery, BooleanClause.Occur.MUST);
            }

            if (!scoreRange.isEmpty()){
                double min = scoreRange.get(0);
                double max = scoreRange.get(1);

                Query scoreRangeQuery = DoublePoint.newRangeQuery("tag_primaryVulnerabilitycvssScore", min, max);
                combinedQuery.add(scoreRangeQuery, BooleanClause.Occur.MUST);
            }


            System.out.println(combinedQuery.build());

            // Search for documents matching the combined query
            TopDocs topDocs = searcher.search(combinedQuery.build(), Integer.MAX_VALUE);

            totalHits = topDocs.totalHits.value;

            List<String> resultMap = new ArrayList<>();

            // Iterate over the retrieved documents
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);

                String id = doc.get("id");
                String description = doc.get("tag_primaryVulnerabilityDescription");

                resultMap.add(id + "\t" + description);
            }

            if (!resultMap.isEmpty()) {
                try {
                    Files.write(Paths.get(RESULT_IDS), resultMap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Close the IndexReader instance
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return totalHits;
    }

}