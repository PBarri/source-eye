package com.pbarrientos.sourceeye.data.services;

import org.jboss.logging.Logger;
import org.owasp.dependencycheck.data.cpe.CpeMemoryIndex;
import org.owasp.dependencycheck.data.cpe.IndexException;
import org.owasp.dependencycheck.data.nvdcve.CveDB;

/**
 * Service that will create and maintain the {@link CpeMemoryIndex} Lucene Index with all the CPEs present in the NVD
 * database.
 *
 * @author Pablo Barrientos
 */
public class CPEMemoryService {

    private static final Logger LOGGER = Logger.getLogger(CPEMemoryService.class);

    private CpeMemoryIndex cpe = CpeMemoryIndex.getInstance();

    /**
     * Constructor that initializes the index from the database
     *
     * @param database the cve database
     * @throws IndexException in case there is any error
     */
    public CPEMemoryService(final CveDB database) throws IndexException {
        try {
            this.createIndex(database);
        } catch (IndexException e) {
            CPEMemoryService.LOGGER.error("There was an error initializing the CPE Lucene index", e);
            throw e;
        }
    }

    /**
     * @return the index
     * @since 0.1.0
     */
    public CpeMemoryIndex getIndex() {
        return this.cpe;
    }

    /**
     * Delegates to {@link CpeMemoryIndex#open(CveDB)}, which only creates the index if it does not exist
     *
     * @param database the {@link CveDB} database
     * @throws IndexException in case of errors
     */
    public void createIndex(final CveDB database) throws IndexException {
        this.cpe.open(database);
    }

    /**
     * Deletes the current index and creates it again. Intended to recreate the index after database changes
     *
     * @param database the {@link CveDB} database
     * @throws IndexException in case of errors
     */
    public void recreateIndex(final CveDB database) throws IndexException {
        if (this.cpe.isOpen()) {
            this.cpe.close();
        }
        this.createIndex(database);
    }

}
