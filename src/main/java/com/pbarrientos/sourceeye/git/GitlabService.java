package com.pbarrientos.sourceeye.git;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabProject;
import org.gitlab.api.models.GitlabRepositoryTree;
import org.gitlab.api.models.GitlabSession;
import org.jboss.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.pbarrientos.sourceeye.config.properties.SourceEyeProperties;
import com.pbarrientos.sourceeye.data.model.Project;
import com.pbarrientos.sourceeye.data.services.ProjectService;
import com.pbarrientos.sourceeye.exceptions.SourceEyeServiceException;
import com.pbarrientos.sourceeye.utils.GitSource;

/**
 * Service responsible of communicate with Gitlab instances
 *
 * @author Pablo Barrientos
 */
@Service
public class GitlabService extends BaseGitService {

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
     * The Gitlab API client
     */
    private GitlabAPI client;

    /**
     * Method executed right after the creation of the bean that will initialize the
     * API client.
     *
     * @since 0.1.0
     */
    @PostConstruct
    public void initialize() {
        SourceEyeProperties.Gitlab gitlab = this.properties.getGitlab();
        SourceEyeProperties.Proxy proxy = this.properties.getProxy();

        // Set API proxy
        Proxy gitlabProxy = null;

        if (StringUtils.hasText(proxy.getHost()) && (proxy.getPort() != null)) {
            SocketAddress sa = new InetSocketAddress(proxy.getHost(), proxy.getPort());
            gitlabProxy = new Proxy(Type.HTTP, sa);
        }

        if (StringUtils.hasText(gitlab.getApiToken())) {
            this.client = GitlabAPI.connect(gitlab.getUrl(), gitlab.getApiToken());
        } else if (StringUtils.hasText(gitlab.getUsername()) && StringUtils.hasText(gitlab.getPassword())) {
            try {
                if (gitlabProxy != null) {
                    // Create fake API in case we need to connect behind a proxy
                    GitlabAPI api = GitlabAPI.connect(gitlab.getUrl(), "token");
                    api.proxy(gitlabProxy);
                    GitlabSession session = api.dispatch().with("login", gitlab.getUsername())
                        .with("password", gitlab.getPassword()).to(GitlabSession.URL, GitlabSession.class);

                    this.client = GitlabAPI.connect(gitlab.getUrl(), session.getPrivateToken());
                } else {
                    GitlabSession session = GitlabAPI.connect(gitlab.getUrl(), gitlab.getUsername(),
                        gitlab.getPassword());

                    this.client = GitlabAPI.connect(gitlab.getUrl(), session.getPrivateToken());
                }

            } catch (IOException e) {
                // TODO: Throw exception
                this.logger.error("There was an error connecting to Gitlab. Please review your settings", e);
            }

        }

        if (gitlabProxy != null) {
            this.client.proxy(gitlabProxy);
        }
    }

    /**
     * @see com.pbarrientos.sourceeye.git.GitService#getProjects()
     */
    @Override
    public List<Project> getProjects() throws SourceEyeServiceException {
        this.updateProjects();

        return this.projectService.findBySource(GitSource.GITLAB);
    }

    /**
     * @see com.pbarrientos.sourceeye.git.GitService#updateProjects()
     */
    @Override
    public void updateProjects() throws SourceEyeServiceException {

        List<GitlabProject> gitlabProjects = null;

        try {
            gitlabProjects = this.client.getMembershipProjects();
        } catch (IOException e) {
            this.logger.error("There was a problem retrieving user projects");
            throw new SourceEyeServiceException(e);
        }

        Map<String, Project> groupedExistingProjects = this.projectService.getProjectsBySource(GitSource.GITLAB);

        if (!CollectionUtils.isEmpty(gitlabProjects)) {
            List<Project> projects = gitlabProjects.stream().map(this::mapFromGitlabProject)
                .collect(Collectors.toList());

            // Update projects
            this.projectService.updateProjects(groupedExistingProjects, projects);

        } else {
            // Delete all Gitlab projects, removing from database all projects coming from
            // Gitlab:
            this.projectService.deleteBySource(GitSource.GITLAB);
        }
    }

    /**
     * @see com.pbarrientos.sourceeye.git.GitService#getProjectRoot(com.pbarrientos.sourceeye.data.model.Project)
     */
    @Override
    public Path getProjectRoot(final Project project) throws SourceEyeServiceException {

        if (!GitSource.GITLAB.equals(project.getSource())) {
            throw new SourceEyeServiceException("The Gitlab service cannot handle projects of other sources");
        }

        // Creates the gitlab project with its internal ID
        GitlabProject gp = new GitlabProject();
        gp.setId(project.getInternalGitlabId());

        try {
            Path projectTempDirectory = Files.createTempDirectory(project.getName());
            List<GitlabRepositoryTree> repositoryTree = this.client.getRepositoryTree(gp, null, null, true);
            List<GitlabRepositoryTree> buildFiles = repositoryTree.stream().filter(tree -> {
                String name = tree.getName();
                Matcher matcher = this.pattern.matcher(name);
                return matcher.matches();
            }).collect(Collectors.toList());

            if (!CollectionUtils.isEmpty(buildFiles)) {
                // Create temporary directory

                for (GitlabRepositoryTree t : buildFiles) {
                    this.downloadFile(gp, t, projectTempDirectory);
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
     * Method that maps the information provided by the API about the project to a
     * {@link Project} entity
     *
     * @param project the information provided by the API
     * @return a {@link Project}
     * @since 0.1.0
     */
    private Project mapFromGitlabProject(final GitlabProject project) {
        Project p = new Project(GitSource.GITLAB);
        p.setName(project.getName());
        p.setQualifiedName("gitlab/" + project.getName());
        p.setDescription(project.getDescription());
        p.setHttpsUrl(project.getHttpUrl());

        if (project.getCreatedAt() != null) {
            p.setCreatedAt(project.getCreatedAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }

        if (project.getLastActivityAt() != null) {
            p.setLastUpdate(project.getLastActivityAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }

        p.setInternalGitlabId(project.getId());

        return p;
    }

    /**
     * Method that will query the API for a repository tree of a given project. This
     * will list all the files contained in the repository.
     *
     * @param project the project
     * @return a list of files present in the repository
     * @throws SourceEyeServiceException in case there is an error
     * @since 0.1.0
     */
    public List<GitlabRepositoryTree> getProjectRepositoryTree(final Project project) throws SourceEyeServiceException {
        // Creates the gitlab project with its internal ID
        GitlabProject gp = new GitlabProject();
        gp.setId(project.getInternalGitlabId());
        gp.setName(project.getName());

        try {
            return this.client.getRepositoryTree(gp, null, null, true);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new SourceEyeServiceException(e);
        }
    }

    /**
     * Method that will download a file from the repository
     *
     * @param project the project
     * @param tree    the repository tree corresponding with the file
     * @param dir     the folder to download the file
     * @throws IOException in case there is an error
     * @since 0.1.0
     */
    private void downloadFile(final GitlabProject project, final GitlabRepositoryTree tree, final Path dir)
        throws IOException {
        byte[] fileContent = this.client.getRawFileContent(project, BaseGitService.BRANCH_NAME, tree.getPath());
        Path filePath = dir.resolve(tree.getPath());
        // Create intermediary directories
        filePath.getParent().toFile().mkdirs();
        FileUtils.writeByteArrayToFile(filePath.toFile(), fileContent);

        this.setExecutablePermissions(filePath.toAbsolutePath());
    }
}
