package com.example.lucenedemo.controller;

import com.example.lucenedemo.model.ItemResult;
import com.example.lucenedemo.service.LuceneIndexService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class LuceneController {

    private final LuceneIndexService luceneIndexService;

    @Autowired
    public LuceneController(LuceneIndexService luceneIndexService) {
        this.luceneIndexService = luceneIndexService;
    }

//    @GetMapping("/indexData")
//    public void indexData() {
//        luceneIndexService.indexData();
//    }

    @GetMapping("/search")
    public List<ItemResult> search(@RequestParam String queryString) {
        return luceneIndexService.search(queryString);
    }

    @GetMapping("/searchField")
    public Long searchField(@RequestParam String queryString, @RequestParam String secondQueryString, @RequestParam String dateRange, @RequestBody List<Double> scoreRange) {
        return luceneIndexService.searchField(queryString,secondQueryString,dateRange,scoreRange);
    }

}