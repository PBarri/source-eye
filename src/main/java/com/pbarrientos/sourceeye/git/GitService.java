package com.pbarrientos.sourceeye.git;

import java.nio.file.Path;
import java.util.List;

import com.pbarrientos.sourceeye.data.model.Project;
import com.pbarrientos.sourceeye.exceptions.SourceEyeServiceException;

/**
 * Interface for Git Services. It provides methods that will make the scanner able to process repositories and files
 *
 * @author Pablo Barrientos
 */
public interface GitService {

    /**
     * Method that will retrieve all available projects from the repository. In the process, this will update projects
     * on the database.
     *
     * @return a list of discovered projects
     * @throws SourceEyeServiceException in case there is an error
     * @since 0.1.0
     */
    List<Project> getProjects() throws SourceEyeServiceException;

    /**
     * Method that will return a path pointing to the root folder of a given project
     *
     * @param project the project
     * @return a path pointing out to the project root
     * @throws SourceEyeServiceException in case there is an error
     * @since 0.1.0
     */
    Path getProjectRoot(Project project) throws SourceEyeServiceException;

    /**
     * Fetches all the projects present in the repository. Then, it will compare with already existing projects in the
     * database and update the database, removing deleted projects, updating existing ones and adding news.
     *
     * @throws SourceEyeServiceException in case there is an error
     * @since 0.1.0
     */
    void updateProjects() throws SourceEyeServiceException;

    /**
     * Method that will indicate if files analyzed should be deleted after the scan. This is useful to implementations
     * that downloads data from online repositories to the temporary folder, such as {@link GithubService} and
     * {@link GitlabService}
     *
     * @return if files were deleted
     * @since 0.1.0
     */
    boolean deleteFilesAfterScan();

}
