package com.pbarrientos.sourceeye.tests;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import com.pbarrientos.sourceeye.engine.SourceEyeEngine;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("Test")
public class MavenTest {

    @Autowired
    private SourceEyeEngine engine;

    @Test
    @Ignore
    public void mavenTest() throws Exception {

        this.engine.run();
    }

}
