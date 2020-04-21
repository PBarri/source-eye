package com.pbarrientos.sourceeye.api;

import org.apache.http.HttpStatus;
import org.owasp.dependencycheck.exception.ExceptionCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pbarrientos.sourceeye.engine.SourceEyeEngine;
import com.pbarrientos.sourceeye.exceptions.SourceEyeServiceException;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;

/**
 * Controller class responsible of managing the executions of the scanning and
 * analysis.
 *
 * @author Pablo Barrientos
 */
@RestController
@RequestMapping("/engine")
@Api(value = "API for launching analysis")
public class EngineController {

    /**
     * The engine
     */
    @Autowired
    private SourceEyeEngine engine;

    /**
     * Flag that will indicate if there is already any existing analysis running
     */
    boolean canExecute = true;

    /**
     * Method that will check if there is an analysis running and, if not, will
     * launch a new analysis.
     *
     * @return
     *         <ul>
     *         <li><b>200:</b> If the analysis task have been launched</li>
     *         <li><b>409:</b> If there is already one analysis task running</li>
     *         </ul>
     * @since 0.1.0
     */
    @PostMapping(path = "/launch")
    @ApiOperation(value = "Return project stats for all projects")
    @ApiResponse(code = 409, message = "The engine is already processing a request")
    public ResponseEntity<String> launchAnalysis() {
        if (!this.canExecute) {
            return ResponseEntity.status(HttpStatus.SC_CONFLICT).build();
        }

        this.changeBoolean(false);
        Runnable runnable = () -> {
            try {
                this.engine.run();
            } catch (SourceEyeServiceException | ExceptionCollection e) {
            } finally {
                this.changeBoolean(true);
            }
        };
        new Thread(runnable).start();
        return ResponseEntity.ok().build();
    }

    /**
     * Synchronized method that will handle the flag change
     *
     * @param flag the flag to set
     * @since 0.1.0
     */
    private synchronized void changeBoolean(final boolean flag) {
        this.canExecute = flag;
    }

}
