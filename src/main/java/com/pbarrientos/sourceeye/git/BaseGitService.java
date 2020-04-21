package com.pbarrientos.sourceeye.git;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Abstract class for Git services. It provides a pattern to find analyzable
 * files.
 *
 * @author Pablo Barrientos
 */
public abstract class BaseGitService implements GitService {

    /**
     * Branch to be analyzed. At the moment is fixed to master
     */
    protected static final String BRANCH_NAME = "master";

    /**
     * Pattern of files that can be analyzed by the services
     */
    protected final Pattern pattern = Pattern
        .compile("(pom.xml)|(.*.gradle)|(.*.properties)|(gradlew)|(gradlew.bat)|(.*gradle-wrapper.jar)");

    protected void setExecutablePermissions(final Path path) throws IOException {
        try {
            Set<PosixFilePermission> perms = new HashSet<>();
            perms.add(PosixFilePermission.OWNER_EXECUTE);
            perms.add(PosixFilePermission.OWNER_READ);
            perms.add(PosixFilePermission.OWNER_WRITE);
            perms.add(PosixFilePermission.GROUP_EXECUTE);
            perms.add(PosixFilePermission.GROUP_READ);
            perms.add(PosixFilePermission.OTHERS_EXECUTE);
            perms.add(PosixFilePermission.OTHERS_READ);

            Files.setPosixFilePermissions(path, perms);
        } catch (UnsupportedOperationException e) {
            // Should be a windows os then:
            path.toFile().setExecutable(true);
        }
    }

}
