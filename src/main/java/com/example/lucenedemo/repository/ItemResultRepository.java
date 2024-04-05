package com.example.lucenedemo.repository;

import com.example.lucenedemo.model.ItemResult;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ItemResultRepository extends MongoRepository<ItemResult, String>{
    @Override
    public @NonNull Page<ItemResult> findAll(@NonNull Pageable pageable);
}
