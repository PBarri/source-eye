package com.pbarrientos.sourceeye.tests;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.owasp.dependencycheck.data.nvdcve.CveDB;
import org.owasp.dependencycheck.dependency.Vulnerability;
import org.owasp.dependencycheck.dependency.Vulnerability.Source;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.pbarrientos.sourceeye.data.services.CveDatabaseService;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DataTests {

    @Autowired
    private CveDB cveDatabase;

    @Autowired
    private CveDatabaseService dbService;

    @Test
    @Ignore
    public void testAdd() {
        Vulnerability vuln = new Vulnerability();

        vuln.setName("CVE-TEST");
        vuln.setSource(Source.NVD);

        this.cveDatabase.updateVulnerability(vuln);

        Vulnerability dbVuln = this.cveDatabase.getVulnerability("CVE-TEST");
        Assert.assertNotNull(dbVuln);
    }

}
