package com.pbarrientos.sourceeye.data.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;

import io.swagger.annotations.ApiModelProperty;

@MappedSuperclass
public abstract class BaseEntity implements Serializable {

    /**
     * Serial Version UID -----------------------------------------------------
     */
    private static final long serialVersionUID = 1L;

    // Constructors -----------------------------------------------------------
    public BaseEntity() {
    }

    // Attributes -------------------------------------------------------------

    /**
     * The id of the entity
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "The id", required = true)
    private Long id;

    /**
     * The version of the entity
     */
    @Version
    @Column(nullable = false)
    @ApiModelProperty(value = "The version of the model", required = true)
    private Long version;

    /**
     * The timestamp of the last modification of the entity
     */
    @Column(nullable = false)
    @ApiModelProperty(value = "Last date in which the model was modified", required = true)
    private LocalDateTime lastModified;

    // Getters and Setters ----------------------------------------------------

    /**
     * @return the id
     */
    public Long getId() {
        return this.id;
    }

    /**
     * @param id the id to set
     */
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * @return the version
     */
    public Long getVersion() {
        return this.version;
    }

    /**
     * @param version the version to set
     */
    public void setVersion(final Long version) {
        this.version = version;
    }

    /**
     * @return the lastModified
     */
    public LocalDateTime getLastModified() {
        return this.lastModified;
    }

    /**
     * @param lastModified the lastModified to set
     */
    public void setLastModified(final LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public int hashCode() {
        return (this.id != null) ? this.id.intValue() : super.hashCode();
    }

    @Override
    public boolean equals(final Object other) {
        boolean result;

        if (this == other) {
            result = true;
        } else if (other == null) {
            result = false;
        } else if (other instanceof Long) {
            result = this.getId().equals(other);
        } else if (!this.getClass().isInstance(other)) {
            result = false;
        } else if (this.getId() != null) {
            result = this.getId().equals(((BaseEntity) other).getId());
        } else {
            result = false;
        }

        return result;
    }

}
