package com.pbarrientos.sourceeye.data.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pbarrientos.sourceeye.data.model.Project;
import com.pbarrientos.sourceeye.utils.GitSource;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    Project findByQualifiedName(String qualifiedName);

    void deleteBySource(GitSource source);

    List<Project> findBySource(GitSource source);

    List<Project> findByName(String name);

}
