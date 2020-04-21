package com.pbarrientos.sourceeye.data.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.pbarrientos.sourceeye.data.model.Project;
import com.pbarrientos.sourceeye.data.repositories.ProjectRepository;
import com.pbarrientos.sourceeye.exceptions.SourceEyeServiceException;
import com.pbarrientos.sourceeye.utils.GitSource;

/**
 * Service to handle transactions with project entities
 *
 * @author Pablo Barrientos
 */
@Service
@Transactional
public class ProjectService extends BaseService<Project, Long> {

    /**
     * The project spring data repository
     */
    @Autowired
    private ProjectRepository repository;

    @Override
    public JpaRepository<Project, Long> getRepository() {
        return this.repository;
    }

    /**
     * Override method to initialize the vulnerabilities collection of the project.
     *
     * @see com.pbarrientos.sourceeye.data.services.BaseService#findAll()
     */
    @Override
    public List<Project> findAll() throws SourceEyeServiceException {
        List<Project> projects = super.findAll();
        if (!CollectionUtils.isEmpty(projects)) {
            projects.forEach(p -> Hibernate.initialize(p.getVulnerabilities()));
        }
        return projects;
    }

    /**
     * Override the method to initialize the data about vulnerabilities
     *
     * @see com.pbarrientos.sourceeye.data.services.BaseService#findById(java.io.Serializable)
     */
    @Override
    public Project findById(final Long id) throws SourceEyeServiceException {
        Project project = super.findById(id);
        if (project != null) {
            Hibernate.initialize(project.getVulnerabilities());
        }
        return project;
    }

    /**
     * Will return the project with the provided qualified name
     *
     * @param name the qualified name of the project
     * @return the project
     * @throws SourceEyeServiceException in case there is any error
     * @since 0.1.0
     */
    public Project findByQualifiedName(final String name) throws SourceEyeServiceException {
        Project res = this.repository.findByQualifiedName(name);

        if (res != null) {
            Hibernate.initialize(res.getVulnerabilities());
        }

        return res;
    }

    /**
     * Will return a list of projects that were scanned in the given source
     *
     * @param source the source
     * @return a list of projects
     * @throws SourceEyeServiceException in case there is any error
     * @since 0.1.0
     */
    public List<Project> findBySource(final GitSource source) throws SourceEyeServiceException {
        List<Project> res = this.repository.findBySource(source);

        if (!CollectionUtils.isEmpty(res)) {
            res.forEach(p -> Hibernate.initialize(p.getVulnerabilities()));
        }

        return res;
    }

    /**
     * Will delete all the projects that were scanned with the given source
     *
     * @param source the source
     * @throws SourceEyeServiceException in case there is any error
     * @since 0.1.0
     */
    public void deleteBySource(final GitSource source) throws SourceEyeServiceException {
        this.repository.deleteBySource(source);
    }

    /**
     * Method that returns a Map with the project name as key, and the project itself as value
     *
     * @param source the {@link GitSource} to search for
     * @return a map with its name as key, and the project as a value
     * @throws SourceEyeServiceException in case there is any error
     */
    public Map<String, Project> getProjectsBySource(final GitSource source) throws SourceEyeServiceException {
        List<Project> projects = this.findBySource(source);

        return projects.stream().collect(Collectors.toMap(Project::getName, Function.identity()));
    }

    /**
     * Method that calculates the projects that were deleted from the repository. Then, it saves the updated projects
     * and removes the deleted ones
     *
     * @param existingProjects Projects already existing
     * @param projects Projects to save
     * @throws SourceEyeServiceException in case there is any error
     */
    public void updateProjects(final Map<String, Project> existingProjects, final Collection<Project> projects)
            throws SourceEyeServiceException {

        List<Project> projectsToSave = new ArrayList<>();

        for (Project p : projects) {
            if (existingProjects.containsKey(p.getName())) {
                // Already existing, update it
                Project project = existingProjects.get(p.getName());

                Project updated = this.updateProject(project, p);
                projectsToSave.add(updated);
            } else {
                // Add new
                projectsToSave.add(p);
            }
        }

        List<Project> projectsToRemove = new ArrayList<>(existingProjects.values());
        projectsToRemove.removeAll(projectsToSave);

        if (!CollectionUtils.isEmpty(projectsToSave)) {
            this.saveAll(projectsToSave);
        }

        if (!CollectionUtils.isEmpty(projectsToRemove)) {
            this.deleteAll(projectsToRemove);
        }
    }

    /**
     * Method that will update the first project with the information of the new project
     *
     * @param old the old project
     * @param actual the new project
     * @return the updated project
     * @since 0.1.0
     */
    private Project updateProject(final Project old, final Project actual) {
        Project res = old;

        if (!old.getLastUpdate().isEqual(actual.getLastUpdate())) {
            res.setLastUpdate(actual.getLastUpdate());
            res.setCreatedAt(actual.getCreatedAt());
            res.setDescription(actual.getDescription());
            res.setHttpsUrl(actual.getHttpsUrl());
        }

        return res;
    }
}
