package com.pbarrientos.sourceeye.git;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections.CollectionUtils;
import org.h2.store.fs.FileUtils;
import org.jboss.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.pbarrientos.sourceeye.config.properties.SourceEyeProperties;
import com.pbarrientos.sourceeye.data.model.Project;
import com.pbarrientos.sourceeye.data.services.ProjectService;
import com.pbarrientos.sourceeye.exceptions.SourceEyeServiceException;
import com.pbarrientos.sourceeye.utils.GitSource;
import com.pbarrientos.sourceeye.utils.ProjectType;
import com.pbarrientos.sourceeye.utils.ProjectUtils;

@Service
public class LocalGitService extends BaseGitService {

    private final Logger logger = Logger.getLogger(this.getClass());

    /**
     * The project properties
     */
    @Autowired
    private SourceEyeProperties properties;

    /**
     * The project service
     */
    @Autowired
    private ProjectService projectService;

    /**
     * @see com.pbarrientos.sourceeye.git.GitService#getProjects()
     */
    @Override
    public List<Project> getProjects() throws SourceEyeServiceException {
        this.updateProjects();

        return this.projectService.findBySource(GitSource.LOCAL);
    }

    /**
     * @see com.pbarrientos.sourceeye.git.GitService#updateProjects()
     */
    @Override
    public void updateProjects() throws SourceEyeServiceException {

        Map<String, Project> existingProjects = this.projectService.getProjectsBySource(GitSource.LOCAL);
        List<Project> actualProjects = new ArrayList<>();

        Path localRepository = Paths.get(this.properties.getLocalRepository().getPath());

        // Get all folders within the local directory. We assume that each one is a
        // different project
        try (Stream<Path> projectStream = Files.list(localRepository).filter(Files::isDirectory)
            .filter(p -> !p.toFile().isHidden())) {

            List<Path> projects = projectStream.collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(projects)) {
                for (Path project : projects) {
                    ProjectType type = ProjectUtils.getProjectType(project);
                    if (!ProjectType.UNKNOWN.equals(type)) {
                        Project p = this.localPathToProject(project, type);
                        actualProjects.add(p);
                    }
                }

                // Update projects
                this.projectService.updateProjects(existingProjects, actualProjects);

            } else {
                this.projectService.deleteBySource(GitSource.LOCAL);
            }

        } catch (IOException e) {
            throw new SourceEyeServiceException(e);
        }

    }

    /**
     * @see com.pbarrientos.sourceeye.git.GitService#getProjectRoot(com.pbarrientos.sourceeye.data.model.Project)
     */
    @Override
    public Path getProjectRoot(final Project project) throws SourceEyeServiceException {

        if (!GitSource.LOCAL.equals(project.getSource())) {
            throw new SourceEyeServiceException("The local service cannot handle projects of other sources");
        }

        if (StringUtils.hasText(project.getHttpsUrl()) && FileUtils.exists(project.getHttpsUrl())) {
            return Paths.get(project.getHttpsUrl());
        } else {
            return null;
        }
    }

    /**
     * @see com.pbarrientos.sourceeye.git.GitService#deleteFilesAfterScan()
     */
    @Override
    public boolean deleteFilesAfterScan() {
        return false;
    }

    /**
     * This will map a project present in a local path to a {@link Project} entity
     *
     * @param project the project
     * @param type    the project type
     * @return a project from the local path
     * @since 0.1.0
     */
    private Project localPathToProject(final Path project, final ProjectType type) {
        Project p = new Project(GitSource.LOCAL);
        String projectName = project.getFileName().toString();
        p.setProjectType(type);
        p.setName(projectName);
        p.setQualifiedName("local/" + projectName);
        p.setHttpsUrl(project.toAbsolutePath().toString());

        if (project.toFile().lastModified() > 0) {
            p.setLastUpdate(LocalDateTime.ofInstant(Instant.ofEpochMilli(project.toFile().lastModified()),
                ZoneId.systemDefault()));
        }
        return p;
    }

}
