package com.pbarrientos.sourceeye.data.services;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import com.pbarrientos.sourceeye.data.model.BaseEntity;
import com.pbarrientos.sourceeye.exceptions.SourceEyeServiceException;

public abstract class BaseService<T extends BaseEntity, I extends Serializable> {

    /**
     * Method that will return a {@link JpaRepository} that allows to inject the corresponding repository
     *
     * @return the repository used in the class
     * @since 1.0
     */
    public abstract JpaRepository<T, I> getRepository();

    /**
     * Returns all instances of the type.
     *
     * @return all entities
     * @throws SourceEyeServiceException in case there is an error
     * @since 0.1.0
     */
    public List<T> findAll() throws SourceEyeServiceException {
        return this.getRepository().findAll();
    }

    /**
     * Returns all entities sorted by the given options.
     *
     * @param sort sorting of the query
     * @return all entities sorted by the given options
     * @throws SourceEyeServiceException in case there is an error
     * @since 0.1.0
     */
    public List<T> findAll(final Sort sort) throws SourceEyeServiceException {
        return this.getRepository().findAll(sort);
    }

    /**
     * Returns a {@link Page} of entities meeting the paging restriction provided in the {@code Pageable} object.
     *
     * @param pageable page of the query
     * @return a page of entities
     * @throws SourceEyeServiceException in case there is an error
     * @since 0.1.0
     */
    public Page<T> findAll(final Pageable pageable) throws SourceEyeServiceException {
        return this.getRepository().findAll(pageable);
    }

    /**
     * Returns all instances of the type with the given IDs.
     *
     * @param ids of the objects to search
     * @return list with all found entities
     * @throws SourceEyeServiceException in case there is an error
     * @since 0.1.0
     */
    public List<T> findAll(final Iterable<I> ids) throws SourceEyeServiceException {
        return this.getRepository().findAllById(ids);
    }

    /**
     * Returns the number of entities available.
     *
     * @return the number of entities
     * @throws SourceEyeServiceException in case there is an error
     * @since 0.1.0
     */
    public long count() throws SourceEyeServiceException {
        return this.getRepository().count();
    }

    /**
     * Deletes the entity with the given id.
     *
     * @param id must not be {@literal null}.
     * @throws SourceEyeServiceException in case the given {@code id} is {@literal null}
     * @since 0.1.0
     */
    public void delete(final I id) throws SourceEyeServiceException {
        this.getRepository().deleteById(id);
    }

    /**
     * Deletes a given entity.
     *
     * @param entity entity to remove
     * @throws SourceEyeServiceException in case the given entity is {@literal null}.
     * @since 0.1.0
     */
    public void delete(final T entity) throws SourceEyeServiceException {
        this.getRepository().delete(entity);
    }

    /**
     * Deletes the given entities.
     *
     * @param entities entities to remove
     * @throws SourceEyeServiceException in case the given {@link Iterable} is {@literal null}.
     * @since 0.1.0
     */
    public void deleteAll(final Iterable<? extends T> entities) throws SourceEyeServiceException {
        this.getRepository().deleteAll(entities);
    }

    /**
     * Deletes all entities managed by the repository.
     *
     * @throws SourceEyeServiceException in case there is an error
     * @since 0.1.0
     */
    public void deleteAll() throws SourceEyeServiceException {
        this.getRepository().deleteAll();
    }

    /**
     * Retrieves an entity by its id.
     *
     * @param id must not be {@literal null}.
     * @return the entity with the given id or {@literal null} if none found
     * @throws SourceEyeServiceException if {@code id} is {@literal null}
     * @since 0.1.0
     */
    public T findById(final I id) throws SourceEyeServiceException {
        return this.getRepository().findById(id).orElse(null);
    }

    /**
     * Returns whether an entity with the given id exists.
     *
     * @param id must not be {@literal null}.
     * @return true if an entity with the given id exists, {@literal false} otherwise
     * @throws SourceEyeServiceException if {@code id} is {@literal null}
     */
    public boolean exists(final I id) throws SourceEyeServiceException {
        return this.getRepository().existsById(id);
    }

    /**
     * Saves a given entity. Use the returned instance for further operations as the save operation might have changed
     * the entity instance completely.
     *
     * @param entity entity to save
     * @return the saved entity
     * @throws SourceEyeServiceException in case there is an error
     * @since 0.1.0
     */
    public <S extends T> S save(final S entity) throws SourceEyeServiceException {
        entity.setLastModified(LocalDateTime.now());
        return this.getRepository().save(entity);
    }

    /**
     * Saves all given entities.
     *
     * @param entities list of entities to save
     * @return the saved entities
     * @throws SourceEyeServiceException in case the given entity is {@literal null}.
     * @since 0.1.0
     */
    public <S extends T> List<S> saveAll(final Iterable<S> entities) throws SourceEyeServiceException {
        entities.forEach(entity -> entity.setLastModified(LocalDateTime.now()));
        return this.getRepository().saveAll(entities);

    }

    /**
     * Flushes all pending changes to the database.
     *
     * @throws SourceEyeServiceException in case there is an error
     * @since 0.1.0
     */
    public void flush() throws SourceEyeServiceException {
        this.getRepository().flush();
    }

    /**
     * Saves an entity and flushes changes instantly.
     *
     * @param entity entity to save
     * @return the saved entity
     * @throws SourceEyeServiceException in case there is an error
     * @since 0.1.0
     */
    public <S extends T> S saveAndFlush(final S entity) throws SourceEyeServiceException {
        entity.setLastModified(LocalDateTime.now());
        return this.getRepository().saveAndFlush(entity);
    }

    /**
     * Deletes the given entities in a batch which means it will create a single query. Assume that we will clear the
     * {@link javax.persistence.EntityManager} after the call.
     *
     * @param entities entities to remove
     * @throws SourceEyeServiceException in case there is an error
     * @since 0.1.0
     */
    public void deleteInBatch(final Iterable<T> entities) throws SourceEyeServiceException {
        this.getRepository().deleteInBatch(entities);
    }

    /**
     * Deletes all entites in a batch call.
     *
     * @throws SourceEyeServiceException in case there is an error
     * @since 0.1.0
     */
    public void deleteAllInBatch() throws SourceEyeServiceException {
        this.getRepository().deleteAllInBatch();
    }

    /**
     * Returns a reference to the entity with the given identifier.
     *
     * @param id must not be {@literal null}.
     * @return a reference to the entity with the given identifier.
     * @see EntityManager#getReference(Class, Object)
     * @throws SourceEyeServiceException in case there is an error
     * @since 0.1.0
     */
    public T getOne(final I id) throws SourceEyeServiceException {
        return this.getRepository().getOne(id);
    }

}
