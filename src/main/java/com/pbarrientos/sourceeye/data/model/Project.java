package com.pbarrientos.sourceeye.data.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.pbarrientos.sourceeye.utils.GitSource;
import com.pbarrientos.sourceeye.utils.ProjectType;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * The entity which maps the discovered projects by the scanner
 *
 * @author Pablo Barrientos
 */
@Entity
@Table(name = "se_projects")
@ApiModel("Project")
public class Project extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * The qualified name of the project. This must be unique and usually takes the following format: '{source}/{name}'
     */
    @ApiModelProperty(value = "The project qualified name", required = true)
    @Column(nullable = false, unique = true)
    private String qualifiedName;

    /**
     * The name of the project.
     */
    @ApiModelProperty(value = "The project name", required = true)
    @Column(nullable = false)
    private String name;

    /**
     * The description of the project
     */
    @ApiModelProperty(value = "The project description", required = false)
    private String description;

    /**
     * The url in which the project can be found. Can be either an http url or a local URI
     */
    @ApiModelProperty(value = "The project url", required = true)
    @Column(nullable = false)
    private String httpsUrl;

    /**
     * Date in which the project was created. Only available for Github and Gitlab projects
     */
    @ApiModelProperty(value = "The project creation date. Not available in local projects", required = false)
    private LocalDateTime createdAt;

    /**
     * Last update time the project was updated
     */
    @ApiModelProperty(value = "The project last update date. Not available in local projects", required = false)
    private LocalDateTime lastUpdate;

    /**
     * Source of the project
     */
    @ApiModelProperty(value = "The project source", required = true, allowableValues = "LOCAL, GITLAB, GITHUB")
    @Column(nullable = false)
    private GitSource source;

    /**
     * Type of the project
     */
    @ApiModelProperty(value = "The project type", required = true, allowableValues = "MAVEN, GRADLE, UNKNOWN")
    private ProjectType projectType;

    /**
     * Vulnerabilities discovered on this project
     */
    @ApiModelProperty(value = "The project vulnerabilities", required = false)
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL)
    @JsonManagedReference
    private Collection<Vulnerability> vulnerabilities;

    /**
     * Id provided internally by Github. Necessary for later identification of the project in the API
     */
    @ApiModelProperty(value = "The project id assigned by Github", required = false)
    private String internalGithubId;

    /**
     * Id provided internally by Gitlab. Necessary for later identification of the project in the API
     */
    @ApiModelProperty(value = "The project id assigned by Gitlab", required = false)
    private Integer internalGitlabId;

    public Project() {
        this.vulnerabilities = new ArrayList<>();
    }

    public Project(final GitSource source) {
        super();
        this.source = source;
        this.vulnerabilities = new ArrayList<>();
    }

    /**
     * @return the qualifiedName
     */
    public String getQualifiedName() {
        return this.qualifiedName;
    }

    /**
     * @param qualifiedName the qualifiedName to set
     */
    public void setQualifiedName(final String qualifiedName) {
        this.qualifiedName = qualifiedName;
    }

    /**
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    /**
     * @param name the name to set
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(final String description) {
        this.description = description;
    }

    /**
     * @return the httpsUrl
     */
    public String getHttpsUrl() {
        return this.httpsUrl;
    }

    /**
     * @param httpsUrl the httpsUrl to set
     */
    public void setHttpsUrl(final String httpsUrl) {
        this.httpsUrl = httpsUrl;
    }

    /**
     * @return the createdAt
     */
    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    /**
     * @param createdAt the createdAt to set
     */
    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * @return the lastUpdate
     */
    public LocalDateTime getLastUpdate() {
        return this.lastUpdate;
    }

    /**
     * @param lastUpdate the lastUpdate to set
     */
    public void setLastUpdate(final LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    /**
     * @return the source
     */
    public GitSource getSource() {
        return this.source;
    }

    /**
     * @param source the source to set
     */
    public void setSource(final GitSource source) {
        this.source = source;
    }

    /**
     * @return the projectType
     */
    public ProjectType getProjectType() {
        return this.projectType;
    }

    /**
     * @param projectType the projectType to set
     */
    public void setProjectType(final ProjectType projectType) {
        this.projectType = projectType;
    }

    /**
     * @return the vulnerabilities
     */
    public Collection<Vulnerability> getVulnerabilities() {
        return this.vulnerabilities;
    }

    /**
     * @param vulnerabilities the vulnerabilities to set
     */
    public void setVulnerabilities(final Collection<Vulnerability> vulnerabilities) {
        this.vulnerabilities = vulnerabilities;
    }

    /**
     * @return the internalGithubId
     */
    public String getInternalGithubId() {
        return this.internalGithubId;
    }

    /**
     * @param internalGithubId the internalGithubId to set
     */
    public void setInternalGithubId(final String internalGithubId) {
        this.internalGithubId = internalGithubId;
    }

    /**
     * @return the internalGitlabId
     */
    public Integer getInternalGitlabId() {
        return this.internalGitlabId;
    }

    /**
     * @param internalGitlabId the internalGitlabId to set
     */
    public void setInternalGitlabId(final Integer internalGitlabId) {
        this.internalGitlabId = internalGitlabId;
    }

    /**
     * Adds a vulnerability to the project
     *
     * @param vuln the vulnerability to add
     * @since 0.1.0
     */
    public void addVulnerability(final Vulnerability vuln) {
        if (!this.vulnerabilities.contains(vuln)) {
            this.vulnerabilities.add(vuln);
            vuln.setProject(this);
            vuln.setLastModified(LocalDateTime.now());
        }
    }

    /**
     * Removes a vulnerability from the project
     *
     * @param vuln the vulnerability to remove
     * @since 0.1.0
     */
    public void removeVulnerability(final Vulnerability vuln) {
        if (this.vulnerabilities.remove(vuln)) {
            vuln.setProject(null);
        }
    }

    // Equals and Hashcode ----------------------------------------------------

    @Override
    public String toString() {
        return String.format("%s[id=%d, name=%s version=%d]", this.getClass().getSimpleName(), this.getId(),
                this.qualifiedName, this.getVersion());
    }

}
