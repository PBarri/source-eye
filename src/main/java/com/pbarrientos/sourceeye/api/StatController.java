package com.pbarrientos.sourceeye.api;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pbarrientos.sourceeye.api.views.ProjectVulnerability;
import com.pbarrientos.sourceeye.data.model.Project;
import com.pbarrientos.sourceeye.data.services.ProjectService;
import com.pbarrientos.sourceeye.exceptions.SourceEyeServiceException;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * Controller class responsible of returning processed data to show in graphs
 *
 * @author Pablo Barrientos
 */
@RestController
@RequestMapping("/stats")
@Api(value = "API for getting statistical information")
public class StatController {

    /**
     * The project service
     */
    @Autowired
    private ProjectService projectService;

    /**
     * This method will process all the analysed projects
     *
     * @return a list of {@link ProjectVulnerability} DTOs with data of the projects
     * @throws SourceEyeServiceException in case there is an error
     * @since 0.1.0
     */
    @GetMapping(path = "/projects/", produces = "application/json")
    @ApiOperation(value = "Return project stats for all projects")
    public List<ProjectVulnerability> getProjectsVulnerabilities() throws SourceEyeServiceException {
        List<Project> projects = this.projectService.findAll();

        return projects.stream().map(ProjectVulnerability::createFromProject).collect(Collectors.toList());
    }

    /**
     * This method will process a given project. In case the given name is a qualified name, the method will return a
     * single project. Otherwise, the method will return all the projects that matches that name
     *
     * @param projectName the project name
     * @return a {@link ProjectVulnerability} DTO with the data of the project
     * @throws SourceEyeServiceException in case there is an error
     * @since 0.1.0
     */
    @GetMapping(path = "/projects/{name}", produces = "application/json")
    @ApiOperation(value = "Return project stats by project name")
    public ProjectVulnerability getProjectsVulnerability(@PathVariable("name") final String projectName)
            throws SourceEyeServiceException {
        Project project = this.projectService.findByQualifiedName(projectName);
        return ProjectVulnerability.createFromProject(project);
    }

}
