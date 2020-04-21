/**
 *
 */
package com.pbarrientos.sourceeye.engine;

import org.jboss.logging.Logger;
import org.owasp.dependencycheck.exception.ExceptionCollection;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import com.pbarrientos.sourceeye.exceptions.SourceEyeServiceException;

/**
 * Quartz job that will handle the analysis tasks.
 *
 * @author Pablo Barrientos
 */
@Component
@DisallowConcurrentExecution
public class SourceEyeAnalysisJob extends QuartzJobBean {

    private static final Logger LOGGER = Logger.getLogger(SourceEyeAnalysisJob.class);

    /**
     * The engine
     */
    @Autowired
    private SourceEyeEngine engine;

    @Override
    protected void executeInternal(final JobExecutionContext context) throws JobExecutionException {
        try {
            this.engine.run();
        } catch (SourceEyeServiceException | ExceptionCollection e) {
            SourceEyeAnalysisJob.LOGGER.error("Ha habido un error al actualizar la base de datos", e);
            throw new JobExecutionException(e);
        }
    }

}
