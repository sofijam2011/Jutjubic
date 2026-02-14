package com.example.jutjubic.scheduler;

import com.example.jutjubic.service.ETLService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ETLScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ETLScheduler.class);

    @Autowired
    private ETLService etlService;


    @Scheduled(cron = "0 * * * * *")
    public void scheduleETLPipelineForTesting() {
        logger.info("ETL Scheduler triggered - starting ETL pipeline for testing");
        try {
            etlService.runETLPipeline();
            logger.info("ETL Scheduler completed successfully");
        } catch (Exception e) {
            logger.error("ETL Scheduler failed: {}", e.getMessage(), e);
        }
    }
}
