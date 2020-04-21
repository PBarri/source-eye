package com.pbarrientos.sourceeye.nvd;

import org.jboss.logging.Logger;
import org.owasp.dependencycheck.data.update.exception.UpdateException;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import com.pbarrientos.sourceeye.data.services.CveDatabaseService;

/**
 * Quartz job that will handle the NVD Database updates.
 *
 * @author Pablo Barrientos
 */
@Component
@DisallowConcurrentExecution
public class NvdUpdateJob extends QuartzJobBean {

    private static final Logger LOGGER = Logger.getLogger(NvdUpdateJob.class);

    /**
     * The database service
     */
    @Autowired
    private CveDatabaseService dbService;

    @Override
    protected void executeInternal(final JobExecutionContext context) throws JobExecutionException {
        try {
            this.dbService.updateDatabase();
        } catch (UpdateException e) {
            NvdUpdateJob.LOGGER.error("Ha habido un error al actualizar la base de datos", e);
            throw new JobExecutionException(e);
        }
    }

}
