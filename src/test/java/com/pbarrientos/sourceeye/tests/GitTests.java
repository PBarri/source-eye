package com.pbarrientos.sourceeye.tests;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import com.pbarrientos.sourceeye.data.model.Project;
import com.pbarrientos.sourceeye.git.GithubService;
import com.pbarrientos.sourceeye.git.GitlabService;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("Test")
public class GitTests {

    @Autowired
    private GitlabService gitlab;

    @Autowired
    private GithubService github;

    @Test
    @Ignore
    public void testUpdateGitlabProjects() throws Exception {
        List<Project> projects = this.gitlab.getProjects();

        for (Project p : projects) {
            this.gitlab.getProjectRoot(p);
        }

    }

    @Test
    @Ignore
    public void testUpdateGithubProjects() throws Exception {
        List<Project> projects = this.github.getProjects();

        for (Project p : projects) {
            this.github.getProjectRoot(p);
        }
    }

}
