package com.example.lucenedemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(value = "ItemResult")
public class ItemResult {

    @Id
    private String id;
    private String rulesetResultId;
    private String rule;
    private String status;
    private String message;
    private Map<String, String> tags;
    private List<Map<String, String>> filterTags;

}