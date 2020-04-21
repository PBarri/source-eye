package com.pbarrientos.sourceeye.scanners;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.owasp.dependencycheck.dependency.Confidence;
import org.owasp.dependencycheck.dependency.Dependency;
import org.owasp.dependencycheck.dependency.EvidenceType;

/**
 * Interface that defines the methods that the scanners will need to implement
 *
 * @author Pablo Barrientos
 */
public interface DependencyScanner {

    /**
     * This analyzes one project from the build file. This will create a new temporary file that contains the
     * dependencies of the project in a following format:
     * <p>
     * <code>
     * groupId:artifactId:x:version
     * </code>
     * </p>
     *
     * @param buildFile the build file
     * @return the path to the created temporary file
     * @throws Exception in case there is an error
     * @since 0.1.0
     */
    Path getDependencyTree(File buildFile) throws Exception;

    /**
     * Get dependencies from a project, resolving all the project dependencies
     *
     * @param pomFile the parent pom file of the project
     * @return a list of all the dependencies of the maven project
     * @throws Exception in case something goes wrong
     * @since 0.1.0
     */
    default List<Dependency> getDependencies(final File pomFile) throws Exception {
        // TODO: Check inputs

        List<Dependency> dependencies = new ArrayList<>();

        Path compileDependencies = this.getDependencyTree(pomFile);

        List<String> lines = Files.readAllLines(compileDependencies);

        lines.forEach(line -> {
            String[] splittedLine = line.split(":");

            // TODO: Check array

            String originalGroupId = splittedLine[0];
            String groupId = originalGroupId;
            if (groupId.startsWith("org.") || groupId.startsWith("com.")) {
                groupId = groupId.substring(4);
            }

            String originalArtifactId = splittedLine[1];
            String artifactId = originalArtifactId;
            if (artifactId.startsWith("org.") || artifactId.startsWith("com.")) {
                artifactId = artifactId.substring(4);
            }

            String version = splittedLine[splittedLine.length - 1];

            Dependency dependency = new Dependency(true);
            dependency.setName(line);

            // Add dependency evidences
            dependency.addEvidence(EvidenceType.VENDOR, "pom", "groupid", groupId, Confidence.HIGHEST);
            dependency.addEvidence(EvidenceType.PRODUCT, "pom", "groupid", groupId, Confidence.LOW);
            dependency.addEvidence(EvidenceType.PRODUCT, "pom", "artifactid", artifactId, Confidence.HIGHEST);
            dependency.addEvidence(EvidenceType.VENDOR, "pom", "artifactid", artifactId, Confidence.LOW);
            dependency.addEvidence(EvidenceType.VERSION, "pom", "version", version, Confidence.HIGHEST);

            // Add dependency identifiers
            dependency.addIdentifier("maven", String.format("%s:%s:%s", originalGroupId, originalArtifactId, version),
                    null, Confidence.HIGH);

            dependencies.add(dependency);
        });

        FileUtils.forceDelete(compileDependencies.toFile());

        return dependencies;
    }

}
