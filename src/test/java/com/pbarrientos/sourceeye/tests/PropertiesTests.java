package com.pbarrientos.sourceeye.tests;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import com.pbarrientos.sourceeye.config.properties.SourceEyeProperties;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("Test")
public class PropertiesTests {

    @Autowired
    private SourceEyeProperties properties;

    @Test
    @Ignore
    public void testSecrets() {
        Assert.assertNotNull(this.properties);

        Assert.assertNotEquals("******", this.properties.getGithub().getPassword());
        Assert.assertNotEquals("******", this.properties.getGitlab().getPassword());
    }

}
