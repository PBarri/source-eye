package com.pbarrientos.sourceeye.config;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.pbarrientos.sourceeye.config.properties.SourceEyeProperties;
import com.pbarrientos.sourceeye.engine.SourceEyeAnalysisJob;
import com.pbarrientos.sourceeye.nvd.NvdUpdateJob;

/**
 * Configuration class that initializes the Quartz jobs that will update the
 * database and analyse the projects.
 *
 * @author Pablo Barrientos
 */
@Configuration
@Profile("!Test")
public class JobsConfiguration {

    /**
     * The project configuration properties
     */
    @Autowired
    private SourceEyeProperties properties;

    /**
     * Bean that creates the job for updating the NVD.
     *
     * @return the quartz job that will handle the updates
     * @since 0.1.0
     */
    @Bean
    public JobDetail updateNvdJob() {
        return JobBuilder.newJob(NvdUpdateJob.class).withIdentity("updateNvdJob").storeDurably().build();
    }

    /**
     * Bean that creates the job for run the analysis.
     *
     * @return the quart job that will run the analysis.
     * @since 0.1.0
     */
    @Bean
    public JobDetail analysisJob() {
        return JobBuilder.newJob(SourceEyeAnalysisJob.class).withIdentity("sourceEyeAnalysisJob").storeDurably()
            .build();
    }

    /**
     * <p>
     * Bean that creates the trigger that will launch the update job. This trigger
     * will only be created if the property 'sourceeye.jobsEnabled' is set to true.
     * </p>
     * <p>
     * This trigger will launch the update job every 4 hours by default. This
     * behavior can be modified by the user in the property
     * 'sourceeye.nvd.validHours'
     * </p>
     *
     * @return the trigger
     * @since 0.1.0
     */
    @Bean
    @ConditionalOnProperty(name = "sourceeye.nvd.autoUpdate")
    public Trigger updateNvdJobTrigger() {
        Integer hours = this.properties.getNvd().getValidHours();

        SimpleScheduleBuilder schedulerBuilder = SimpleScheduleBuilder.simpleSchedule().withIntervalInHours(hours)
            .repeatForever();

        return TriggerBuilder.newTrigger().forJob(this.updateNvdJob()).withIdentity("updateNvdTrigger")
            .withSchedule(schedulerBuilder).build();
    }

    /**
     * <p>
     * Bean that creates the trigger that will launch the analysis job. This trigger
     * will only be created if the property 'sourceeye.jobsEnabled' is set to true.
     * </p>
     * <p>
     * This trigger will launch the update job every business day at 5:00 in the
     * morning. This behavior can be changed in the property
     * 'sourceeye.analysisJobCron'
     * </p>
     *
     * @return the trigger
     * @since 0.1.0
     */
    @Bean
    @ConditionalOnProperty(name = "sourceeye.analysis.enabled")
    public Trigger analysisTrigger() {
        String cronExpression = this.properties.getAnalysis().getPeriodicity();

        CronScheduleBuilder schedulerBuilder = CronScheduleBuilder.cronSchedule(cronExpression);

        return TriggerBuilder.newTrigger().forJob(this.updateNvdJob()).withIdentity("sourceEyeAnalysisJob")
            .withSchedule(schedulerBuilder).build();
    }

}
