package io.micronaut.samples.petclinic.service;

import io.micronaut.context.annotation.Requires;
import io.micronaut.data.exceptions.DataAccessException;
import io.micronaut.data.exceptions.OptimisticLockException;
import io.micronaut.samples.petclinic.exception.NotFoundEntityException;
import io.micronaut.samples.petclinic.oracle.OracleErrorCode;
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
        OracleErrorCode code = OracleErrorCode.fromCode(errorCode);
        if (code == null) return false;
        return switch (code) {
            case ORA_40937_DUALITY_VIEW_INSERT_NOT_ALLOWED,
                 ORA_40938_DUALITY_VIEW_DELETE_NOT_ALLOWED,
                 ORA_40939_DUALITY_VIEW_UPDATE_NOT_ALLOWED,
                 ORA_40940_DUALITY_VIEW_COLUMN_UPDATE_NOT_ALLOWED,
                 ORA_42689_DUALITY_VIEW_COLUMN_NOT_UPDATABLE -> true;
            default -> false;
        };
    }

}
