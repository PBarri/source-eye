package com.pbarrientos.sourceeye.git;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.eclipse.egit.github.core.Blob;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.Tree;
import org.eclipse.egit.github.core.TreeEntry;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.ContentsService;
import org.eclipse.egit.github.core.service.DataService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.jboss.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.pbarrientos.sourceeye.config.properties.SourceEyeProperties;
import com.pbarrientos.sourceeye.data.model.Project;
import com.pbarrientos.sourceeye.data.services.ProjectService;
import com.pbarrientos.sourceeye.exceptions.SourceEyeServiceException;
import com.pbarrientos.sourceeye.utils.GitSource;

/**
 * Service responsible of communicate with Github instances
 *
 * @author Pablo Barrientos
 */
@Service
public class GithubService extends BaseGitService {

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
     * The github client
     */
    private GitHubClient client;

    /**
     * Github's API content service
     */
    private ContentsService contentsService;

    /**
     * Github's API data service
     */
    private DataService dataService;

    /**
     * Github's API repository service
     */
    private RepositoryService repoService;

    /**
     * Method executed after initialization that initializes the Github API client
     * and services.
     *
     * @since 0.1.0
     */
    @PostConstruct
    private void initialize() {
        SourceEyeProperties.Github github = this.properties.getGithub();

        this.client = new GitHubClient();
        this.client.setCredentials(github.getUsername(), github.getPassword());

        this.contentsService = new ContentsService(this.client);
        this.dataService = new DataService(this.client);
        this.repoService = new RepositoryService(this.client);
    }

    /**
     * @see com.pbarrientos.sourceeye.git.GitService#getProjects()
     */
    @Override
    public List<Project> getProjects() throws SourceEyeServiceException {
        this.updateProjects();

        return this.projectService.findBySource(GitSource.GITHUB);
    }

    /**
     * @see com.pbarrientos.sourceeye.git.GitService#updateProjects()
     */
    @Override
    public void updateProjects() throws SourceEyeServiceException {
        List<Repository> githubProjects = null;

        githubProjects = this.getUserProjects();

        Map<String, Project> groupedExistingProjects = this.projectService.getProjectsBySource(GitSource.GITHUB);

        if (!CollectionUtils.isEmpty(githubProjects)) {
            List<Project> projects = githubProjects.stream().map(this::mapFromGithubProject)
                .collect(Collectors.toList());

            // Update projects
            this.projectService.updateProjects(groupedExistingProjects, projects);

        } else {
            // Delete all Gitlab projects, removing from database all projects coming from
            // Gitlab:
            this.projectService.deleteBySource(GitSource.GITHUB);
        }
    }

    /**
     * @see com.pbarrientos.sourceeye.git.GitService#getProjectRoot(com.pbarrientos.sourceeye.data.model.Project)
     */
    @Override
    public Path getProjectRoot(final Project project) throws SourceEyeServiceException {

        if (!GitSource.GITHUB.equals(project.getSource())) {
            throw new SourceEyeServiceException("The Github service cannot handle projects of other sources");
        }

        RepositoryId repoId = RepositoryId.createFromId(project.getInternalGithubId());

        try {
            Path projectTempDirectory = Files.createTempDirectory(project.getName());
            Tree repoTree = this.dataService.getTree(repoId, BaseGitService.BRANCH_NAME, true);

            List<TreeEntry> buildFiles = repoTree.getTree().stream().filter(content -> {
                String name = content.getPath();
                Matcher matcher = this.pattern.matcher(name);
                return matcher.matches();
            }).collect(Collectors.toList());
//            List<RepositoryContents> contents = this.contentsService.getContents(repoId);
//            List<RepositoryContents> buildFiles = contents.stream().filter(content -> {
//                String name = content.getName();
//                Matcher matcher = this.pattern.matcher(name);
//                return matcher.matches();
//            }).collect(Collectors.toList());

            if (!CollectionUtils.isEmpty(buildFiles)) {
                for (TreeEntry content : buildFiles) {
                    // Download file
                    this.downloadFile(repoId, content, projectTempDirectory);
                }
            }

            return projectTempDirectory;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            throw new SourceEyeServiceException(e);
        }
    }

    /**
     * @see com.pbarrientos.sourceeye.git.GitService#deleteFilesAfterScan()
     */
    @Override
    public boolean deleteFilesAfterScan() {
        return true;
    }

    /**
     * Returns a list of repositories owned by the user
     *
     * @return a list of repositories
     * @throws SourceEyeServiceException in case there is an error
     * @since 0.1.0
     */
    private List<Repository> getUserProjects() throws SourceEyeServiceException {
        try {
            return this.repoService.getRepositories();
        } catch (IOException e) {
            throw new SourceEyeServiceException(e);
        }
    }

    /**
     * Method that maps the information provided by the API about the project to a
     * {@link Project} entity
     *
     * @param project the information provided by the API
     * @return a {@link Project}
     * @since 0.1.0
     */
    private Project mapFromGithubProject(final Repository project) {
        Project p = new Project(GitSource.GITHUB);
        p.setName(project.getName());
        p.setQualifiedName("github/" + project.getName());
        p.setDescription(project.getDescription());
        p.setHttpsUrl(project.getCloneUrl());

        if (project.getCreatedAt() != null) {
            p.setCreatedAt(project.getCreatedAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }

        if (project.getUpdatedAt() != null) {
            p.setLastUpdate(project.getUpdatedAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }

        p.setInternalGithubId(project.generateId());

        return p;
    }

    /**
     * Method that will query the API for a repository tree of a given project. This
     * will list all the files contained in the repository.
     *
     * @param repoId  the id of the repository
     * @param content the content to download
     * @param dir     the folder to download
     * @throws IOException in case there is an error
     * @since 0.1.0
     */
    private void downloadFile(final RepositoryId repoId, final TreeEntry content, final Path dir)
        throws IOException {
        Blob fileBlob = this.dataService.getBlob(repoId, content.getSha());
        byte[] contentBytes = Base64.decodeBase64(fileBlob.getContent());
        Path contentPath = dir.resolve(content.getPath());
        // Create intermediary directories
        contentPath.getParent().toFile().mkdirs();
        FileUtils.writeByteArrayToFile(contentPath.toFile(), contentBytes);

        this.setExecutablePermissions(contentPath.toAbsolutePath());
    }
}
