package com.pbarrientos.sourceeye.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pbarrientos.sourceeye.data.model.Project;
import com.pbarrientos.sourceeye.data.services.ProjectService;
import com.pbarrientos.sourceeye.exceptions.SourceEyeServiceException;
import com.pbarrientos.sourceeye.utils.GitSource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * Controller responsible of returning information about the projects analysed.
 *
 * @author Pablo Barrientos
 */
@RestController
@RequestMapping("/project")
@Api(value = "API for getting information about projects")
public class ProjectController {

    /**
     * The project service
     */
    @Autowired
    private ProjectService projectService;

    /**
     * This method will retrieve a {@link Project} by its id and will return a JSON representation of it
     *
     * @param id the id of the project
     * @return a project
     * @throws SourceEyeServiceException in case there is any error
     * @since 0.1.0
     */
    @GetMapping(path = "/{id}")
    @ApiOperation(value = "Find a project by its id")
    public Project getProject(@PathVariable("id") final Long id) throws SourceEyeServiceException {
        return this.projectService.findById(id);
    }

    /**
     * This method will retrieve all projects analyzed and return a JSON representation of it
     *
     * @return the projects
     * @throws SourceEyeServiceException in case there is any error
     * @since 0.1.0
     */
    @GetMapping("/")
    @ApiOperation(value = "Find all projects")
    public List<Project> getAllProjects() throws SourceEyeServiceException {
        return this.projectService.findAll();
    }

    /**
     * Return the information about all the projects of the given source
     *
     * @param source the source to search. Can be either 'LOCAL', 'GITLAB', or 'GITHUB'.
     * @return the projects
     * @throws SourceEyeServiceException in case there is any error
     * @since 0.1.0
     */
    @GetMapping(path = "/source/{source}")
    @ApiOperation(value = "Find all the projects found in the requested source",
            notes = "Source can be either 'LOCAL', 'GITLAB' or 'GITHUB'")
    public List<Project> getAllProjectsBySource(@PathVariable("source") final String source)
            throws SourceEyeServiceException {
        GitSource gitSource = null;

        for (GitSource gs : GitSource.values()) {
            if (gs.getSource().equalsIgnoreCase(source)) {
                gitSource = gs;
                break;
            }
        }

        if (gitSource == null) {
            throw new SourceEyeServiceException("The source does not exist");
        }

        return this.projectService.findBySource(gitSource);
    }

}
