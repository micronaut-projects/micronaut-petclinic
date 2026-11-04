package io.micronaut.samples.petclinic.service;

import io.micronaut.context.annotation.Requires;
import io.micronaut.data.exceptions.DataAccessException;
import io.micronaut.data.exceptions.OptimisticLockException;
import io.micronaut.samples.petclinic.exception.NotFoundEntityException;
import io.micronaut.samples.petclinic.oracle.model.OwnerCareView;
import io.micronaut.samples.petclinic.repository.OwnerCareViewRepository;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

/**
 * Loads and updates owner-care documents through Micronaut Data's JSON view
 * repository. The view's operation annotations protect nested membership.
 */
@Singleton
@Requires(env = "oracle")
public class OwnerCareViewService {

    /** Oracle error for attempting to insert into a duality-view table without an INSERT annotation. */
    private static final int ORA_CANNOT_INSERT_INTO_DUALITY_VIEW = 40937;

    /** Oracle error for attempting to delete from a duality-view table without a DELETE annotation. */
    private static final int ORA_CANNOT_DELETE_FROM_DUALITY_VIEW = 40938;

    /** Oracle error for attempting to update a duality-view table without an UPDATE annotation. */
    private static final int ORA_CANNOT_UPDATE_DUALITY_VIEW_TABLE = 40939;

    /** Oracle error for attempting to update a duality-view column without an UPDATE annotation. */
    private static final int ORA_CANNOT_UPDATE_DUALITY_VIEW_COLUMN = 40940;

    /** Oracle error for updating a duality-view column with no UPDATE annotation or a NOUPDATE annotation. */
    private static final int ORA_DUALITY_VIEW_COLUMN_NOT_UPDATABLE = 42689;

    private static final Logger log = LoggerFactory.getLogger(OwnerCareViewService.class);
    private final OwnerCareViewRepository repository;

    public OwnerCareViewService(OwnerCareViewRepository repository) {
        this.repository = repository;
    }

    /**
     * Finds OwnerCareView by id.
     * @param ownerId owner id.
     * @return OwnerCareView matching the provided owner id.
     * @throws NotFoundEntityException if the owner does not exist
     */
    public OwnerCareView findById(Integer ownerId) throws NotFoundEntityException {
        return repository.findById(ownerId).orElseThrow(NotFoundEntityException::new);
    }

    /**
     * Updates an existing document while preserving nested membership and IDs.
     *
     * @param updated edited document
     * @return the persisted document with a fresh ETAG
     * @throws NotFoundEntityException if the owner does not exist after the update
     */
    public OwnerCareView update(OwnerCareView updated) throws NotFoundEntityException {
        try {
            repository.update(updated);
        } catch (OptimisticLockException e) {
            throw e;
        } catch (DataAccessException e) {
            SQLException sqlException = findDisallowedOperation(e);
            if (sqlException != null) {
                log.warn("Oracle JSON duality view rejected the operation", sqlException);
                throw new IllegalArgumentException("The requested operation is not allowed.");
            }
            throw e;
        }
        return findById(updated.id());
    }

    private static SQLException findDisallowedOperation(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sqlException) {
                for (SQLException sqlCause = sqlException; sqlCause != null;
                     sqlCause = sqlCause.getNextException()) {
                    if (isDisallowedDualityOperation(sqlCause.getErrorCode())) {
                        return sqlCause;
                    }
                }
            }
        }
        return null;
    }

    private static boolean isDisallowedDualityOperation(int errorCode) {
        // Oracle reports missing duality-view INSERT/DELETE/UPDATE annotations with these codes.
        return switch (errorCode) {
            case ORA_CANNOT_INSERT_INTO_DUALITY_VIEW,
                 ORA_CANNOT_DELETE_FROM_DUALITY_VIEW,
                 ORA_CANNOT_UPDATE_DUALITY_VIEW_TABLE,
                 ORA_CANNOT_UPDATE_DUALITY_VIEW_COLUMN,
                 ORA_DUALITY_VIEW_COLUMN_NOT_UPDATABLE -> true;
            default -> false;
        };
    }

}
