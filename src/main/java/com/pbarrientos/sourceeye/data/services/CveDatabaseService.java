package com.pbarrientos.sourceeye.data.services;

import org.owasp.dependencycheck.data.nvdcve.ConnectionFactory;
import org.owasp.dependencycheck.data.update.NvdCveUpdater;
import org.owasp.dependencycheck.data.update.exception.UpdateException;
import org.owasp.dependencycheck.exception.H2DBLockException;
import org.owasp.dependencycheck.utils.H2DBLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pbarrientos.sourceeye.engine.SourceEyeEngine;

/**
 * Service that will handle the update of the database
 *
 * @author Pablo Barrientos
 */
@Service
public class CveDatabaseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CveDatabaseService.class);

    /**
     * The engine
     */
    @Autowired
    private SourceEyeEngine engine;

    /**
     * Method that will update the database. Is equivalent to the update job, but can be triggered manually.
     *
     * @throws UpdateException in case there is any error
     * @since 0.1.0
     */
    public void updateDatabase() throws UpdateException {
        H2DBLock dblock = null;
        try {
            if (ConnectionFactory.isH2Connection(this.engine.getSettings())) {
                dblock = new H2DBLock(this.engine.getSettings());
                CveDatabaseService.LOGGER.debug("locking for update");
                dblock.lock();
            }
            CveDatabaseService.LOGGER.info("Checking for updates");
            final long updateStart = System.currentTimeMillis();
            NvdCveUpdater updater = new NvdCveUpdater();
            updater.update(this.engine);
            CveDatabaseService.LOGGER.info("Check for updates complete ({} ms)",
                    System.currentTimeMillis() - updateStart);
        } catch (H2DBLockException ex) {
            throw new UpdateException("Unable to obtain an exclusive lock on the H2 database to perform updates", ex);
        } finally {
            if (dblock != null) {
                dblock.release();
            }
        }
    }

}
