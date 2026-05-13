package com.docverify.config;

import com.docverify.repository.DocumentRepository;
import com.docverify.util.CountingBloomFilterUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Optional;


@Component
public class CbfInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CbfInitializer.class);

    private final DocumentRepository      documentRepository;
    private final CountingBloomFilterUtil cbfUtil;

    public CbfInitializer(DocumentRepository documentRepository,
                          CountingBloomFilterUtil cbfUtil) {
        this.documentRepository = documentRepository;
        this.cbfUtil            = cbfUtil;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("CBF Initializer: Checking database for saved CBF state...");

        Optional<String> latestCbfJson = documentRepository.findLatestCbfJson();

        if (latestCbfJson.isPresent()) {
            log.info("CBF Initializer: Found saved CBF — restoring into memory...");
            cbfUtil.loadFromJson(latestCbfJson.get());
            log.info("CBF Initializer: CBF state restored successfully.");
        } else {
            log.info("CBF Initializer: No saved CBF found — starting with empty CBF.");
        }
    }
}