package com.docverify.repository;

import com.docverify.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    Optional<Document> findByDocHash(String docHash);
    Optional<Document> findByTxHash(String txHash);
    boolean existsByDocHash(String docHash);
    @Query("SELECT d.cbfJson FROM Document d WHERE d.cbfJson IS NOT NULL ORDER BY d.uploadedAt DESC LIMIT 1")
    Optional<String> findLatestCbfJson();
}
