package com.pbarrientos.sourceeye.utils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.springframework.util.CollectionUtils;

/**
 * Utility class to handle projects.
 *
 * @author Pablo Barrientos
 */
public class ProjectUtils {

    private ProjectUtils() {
        // Static class
    }

    /**
     * Returns the project type of a given project root path
     *
     * @param path the project root path
     * @return the project type
     * @since 0.1.0
     */
    public static ProjectType getProjectType(final Path path) {
        if (ProjectUtils.isMavenProject(path)) {
            return ProjectType.MAVEN;
        } else if (ProjectUtils.isGradleProject(path)) {
            return ProjectType.GRADLE;
        } else {
            return ProjectType.UNKNOWN;
        }
    }

    /**
     * Method that checks if a project is a Maven project, checking if there is any 'pom.xml' file in the root of the
     * project.
     *
     * @param path the project root path
     * @return if it's a maven project
     * @since 0.1.0
     */
    public static boolean isMavenProject(final Path path) {
        boolean res = false;

        if (Files.isDirectory(path)) {
            Collection<File> pomFiles = FileUtils.listFiles(path.toFile(), new NameFileFilter("pom.xml"), null);
            if (!CollectionUtils.isEmpty(pomFiles)) {
                res = true;
            }
        }

        return res;
    }

    /**
     * Method that checks if a project is a Gradle project, checking if there is any '*.gradle' file in the root of the
     * project.
     *
     * @param path the project root path
     * @return if it's a gradle project
     * @since 0.1.0
     */
    public static boolean isGradleProject(final Path path) {
        boolean res = false;

        if (Files.isDirectory(path)) {
            Collection<File> buildFiles = FileUtils.listFiles(path.toFile(), new String[] { "gradle" }, false);
            if (!CollectionUtils.isEmpty(buildFiles)) {
                res = true;
            }
        }

        return res;
    }

    /**
     * Method that returns the parent pom file of a given project
     *
     * @param project the project root path
     * @return a File representing the pom file.
     * @throws Exception in case there is an error
     * @since 0.1.0
     */
    public static File getParentPomFile(final Path project) throws Exception {
        if (!Files.isDirectory(project)) {
            // TODO: Raise a custom exception
            throw new Exception("The file is not a directory");
        }

        Collection<File> pomFiles = FileUtils.listFiles(project.toFile(), new NameFileFilter("pom.xml"), null);
        if (CollectionUtils.isEmpty(pomFiles) || (pomFiles.size() > 1)) {
            throw new Exception("There are more than one pom files within the parent folder");
        }

        // return the first pom files
        return pomFiles.iterator().next();
    }

    /**
     * Method that finds the parent 'build.gradle' file within a project.
     * 
     * @param project the project root path
     * @return a file representing the build file
     * @throws Exception in case there is an error
     * @since 0.1.0
     */
    public static File getParentBuildFile(final Path project) throws Exception {
        if (!Files.isDirectory(project)) {
            // TODO: Raise a custom exception
            throw new Exception("The file is not a directory");
        }

        Collection<File> buildFiles = FileUtils.listFiles(project.toFile(), new NameFileFilter("build.gradle"), null);
        if (CollectionUtils.isEmpty(buildFiles) || (buildFiles.size() > 1)) {
            throw new Exception("There are more than one build files within the parent folder");
        }

        // return the first pom files
        return buildFiles.iterator().next();
    }

}
