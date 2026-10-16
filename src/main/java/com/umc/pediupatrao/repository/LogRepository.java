package com.umc.pediupatrao.repository;

import com.umc.pediupatrao.entity.Log;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface LogRepository extends MongoRepository<Log, String> {
}
