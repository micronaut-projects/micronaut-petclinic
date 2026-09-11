-- Configure the PetClinic schema for the Oracle Deep Data Security showcase.

WHENEVER SQLERROR EXIT SQL.SQLCODE
SET DEFINE ON
SET VERIFY OFF
SET SERVEROUTPUT ON

DEFINE deepsec_client_id = '&1'
DEFINE deepsec_tenant_id = '&2'
DEFINE deepsec_database_app_id = '&3'
DEFINE deepsec_database_app_id_uri = '&4'

ALTER SESSION SET CONTAINER = FREEPDB1;

-- Tell Oracle which IAM issuer and database resource to trust.
ALTER SYSTEM SET IDENTITY_PROVIDER_TYPE = AZURE_AD SCOPE = BOTH;
ALTER SYSTEM SET IDENTITY_PROVIDER_CONFIG = '{
  "application_id_uri": "&deepsec_database_app_id_uri",
  "tenant_id": "&deepsec_tenant_id",
  "app_id": "&deepsec_database_app_id"
}' SCOPE = BOTH;

CREATE OR REPLACE PUBLIC SYNONYM OWNERS FOR petclinic.OWNERS;
CREATE OR REPLACE PUBLIC SYNONYM PETS FOR petclinic.PETS;
CREATE OR REPLACE PUBLIC SYNONYM TYPES FOR petclinic.TYPES;
CREATE OR REPLACE PUBLIC SYNONYM VISITS FOR petclinic.VISITS;
CREATE OR REPLACE PUBLIC SYNONYM VETS FOR petclinic.VETS;
CREATE OR REPLACE PUBLIC SYNONYM SPECIALTIES FOR petclinic.SPECIALTIES;
CREATE OR REPLACE PUBLIC SYNONYM VET_SPECIALTIES FOR petclinic.VET_SPECIALTIES;
CREATE OR REPLACE PUBLIC SYNONYM CLINICS FOR petclinic.CLINICS;

CREATE OR REPLACE DATA ROLE PETCLINIC_EMPLOYEE
  MAPPED TO 'AZURE_ROLE=EMPLOYEE';
CREATE OR REPLACE DATA ROLE PETCLINIC_STAFF
  MAPPED TO 'AZURE_ROLE=STAFF';

-- Keep the locally managed roles used by the optional @RunAs demonstration.
CREATE DATA ROLE IF NOT EXISTS PETCLINIC_RECEPTIONIST;
CREATE DATA ROLE IF NOT EXISTS PETCLINIC_VET;
CREATE DATA ROLE IF NOT EXISTS PETCLINIC_ADMIN;
-- This role is authorized for the application but disabled by default. It
-- becomes active only when @RunAs explicitly requests it for one method.
-- IF NOT EXISTS keeps reruns safe after the role has been granted to the app.
CREATE DATA ROLE IF NOT EXISTS PETCLINIC_SUPPORT DISABLED;

-- Associate the database-access token's client id with the application. The
-- application identity is also the allow-list for roles requested by the app.
CREATE OR REPLACE APPLICATION IDENTITY petclinic_app_identity
  MAPPED TO 'AZURE_CLIENT_ID=&deepsec_client_id';

-- An application identity that logs on directly needs connection privileges.
-- System privileges cannot be granted directly to an application identity or
-- data role, so inherit them through a database role and a local data role.
BEGIN
  EXECUTE IMMEDIATE 'CREATE ROLE petclinic_app_connect_role';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -1921 THEN
      RAISE;
    END IF;
END;
/

GRANT CREATE SESSION, CREATE END USER SECURITY CONTEXT
  TO petclinic_app_connect_role;

CREATE DATA ROLE IF NOT EXISTS PETCLINIC_APP_LOGON;
GRANT petclinic_app_connect_role TO PETCLINIC_APP_LOGON;
GRANT DATA ROLE PETCLINIC_APP_LOGON TO petclinic_app_identity;

-- Authorize method-scoped privilege escalation. Because the role is disabled,
-- it is not active for ordinary queries; @RunAs enables it only for the
-- support-contact repository method.
GRANT DATA ROLE PETCLINIC_SUPPORT TO petclinic_app_identity;

-- The employee/staff roles come from the end-user token. Do not grant them
-- to the application identity, otherwise they would become application-wide.
DECLARE
  role_names SYS.ODCIVARCHAR2LIST := SYS.ODCIVARCHAR2LIST(
    'PETCLINIC_OWNER',
    'PETCLINIC_RECEPTIONIST',
    'PETCLINIC_VET',
    'PETCLINIC_ADMIN'
  );
BEGIN
  FOR i IN 1..role_names.COUNT LOOP
    BEGIN
      EXECUTE IMMEDIATE 'REVOKE DATA ROLE ' || role_names(i) ||
        ' FROM petclinic_app_identity';
    EXCEPTION
      WHEN OTHERS THEN
        NULL;
    END;
  END LOOP;
END;
/

-- The employee policy uses the EMAIL business attribute to match the current
-- end-user identity. Production data should populate this column as part of
-- the normal owner lifecycle; the optional demo seed script can populate two
-- sample rows for local testing.
BEGIN
  EXECUTE IMMEDIATE 'ALTER TABLE petclinic.OWNERS ADD (EMAIL VARCHAR2(320))';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -1430 THEN
      RAISE;
    END IF;
END;
/

-- Owners: employees see their own row and staff see all rows, but both roles
-- have telephone excluded. Support reveals all columns only for @RunAs.
CREATE OR REPLACE DATA GRANT petclinic.employee_owners_read
  AS SELECT (ALL COLUMNS EXCEPT TELEPHONE)
  ON petclinic.OWNERS
  WHERE LOWER(EMAIL) = LOWER(ORA_END_USER_CONTEXT.username)
  TO PETCLINIC_EMPLOYEE;

CREATE OR REPLACE DATA GRANT petclinic.receptionist_owners_read
  AS SELECT (ALL COLUMNS EXCEPT TELEPHONE)
  ON petclinic.OWNERS
  TO PETCLINIC_RECEPTIONIST, PETCLINIC_STAFF;

CREATE OR REPLACE DATA GRANT petclinic.vet_owners_read
  AS SELECT (ALL COLUMNS EXCEPT TELEPHONE)
  ON petclinic.OWNERS
  TO PETCLINIC_VET, PETCLINIC_STAFF;

-- Keep telephone outside every ordinary role. Data grants are additive, so
-- STAFF must not be included in a full-column CRUD grant intended for ADMIN.
CREATE OR REPLACE DATA GRANT petclinic.admin_owners_crud
  AS SELECT (ALL COLUMNS EXCEPT TELEPHONE),
     INSERT (ALL COLUMNS EXCEPT TELEPHONE),
     UPDATE (ALL COLUMNS EXCEPT TELEPHONE),
     DELETE
  ON petclinic.OWNERS
  TO PETCLINIC_ADMIN;

CREATE OR REPLACE DATA GRANT petclinic.support_owners_read
  AS SELECT
  ON petclinic.OWNERS
  TO PETCLINIC_SUPPORT;

CREATE OR REPLACE DATA GRANT petclinic.owner_pets_read
  AS SELECT
  ON petclinic.PETS
  WHERE OWNER_ID IN (
    SELECT ID
    FROM petclinic.OWNERS
    WHERE LOWER(EMAIL) = LOWER(ORA_END_USER_CONTEXT.username)
  )
  TO PETCLINIC_EMPLOYEE;

CREATE OR REPLACE DATA GRANT petclinic.staff_pets_read
  AS SELECT
  ON petclinic.PETS
  TO PETCLINIC_RECEPTIONIST, PETCLINIC_VET, PETCLINIC_ADMIN, PETCLINIC_STAFF;

CREATE OR REPLACE DATA GRANT petclinic.owner_visits_read
  AS SELECT
  ON petclinic.VISITS
  WHERE PET_ID IN (
    SELECT p.ID
    FROM petclinic.PETS p
    WHERE p.OWNER_ID IN (
      SELECT o.ID
      FROM petclinic.OWNERS o
      WHERE LOWER(o.EMAIL) = LOWER(ORA_END_USER_CONTEXT.username)
    )
  )
  TO PETCLINIC_EMPLOYEE;

CREATE OR REPLACE DATA GRANT petclinic.staff_visits_read
  AS SELECT
  ON petclinic.VISITS
  TO PETCLINIC_RECEPTIONIST, PETCLINIC_VET, PETCLINIC_ADMIN, PETCLINIC_STAFF;

CREATE OR REPLACE DATA GRANT petclinic.owner_pet_types_read
  AS SELECT
  ON petclinic.TYPES
  TO PETCLINIC_EMPLOYEE;

CREATE OR REPLACE DATA GRANT petclinic.staff_pet_types_read
  AS SELECT
  ON petclinic.TYPES
  TO PETCLINIC_RECEPTIONIST, PETCLINIC_VET, PETCLINIC_ADMIN, PETCLINIC_STAFF;

-- Keep the rest of the application usable for staff personas in this demo.
CREATE OR REPLACE DATA GRANT petclinic.staff_vets_read
  AS SELECT ON petclinic.VETS
  TO PETCLINIC_RECEPTIONIST, PETCLINIC_VET, PETCLINIC_ADMIN, PETCLINIC_STAFF;
CREATE OR REPLACE DATA GRANT petclinic.staff_specialties_read
  AS SELECT ON petclinic.SPECIALTIES
  TO PETCLINIC_RECEPTIONIST, PETCLINIC_VET, PETCLINIC_ADMIN, PETCLINIC_STAFF;
CREATE OR REPLACE DATA GRANT petclinic.staff_vet_specialties_read
  AS SELECT ON petclinic.VET_SPECIALTIES
  TO PETCLINIC_RECEPTIONIST, PETCLINIC_VET, PETCLINIC_ADMIN, PETCLINIC_STAFF;
CREATE OR REPLACE DATA GRANT petclinic.staff_clinics_read
  AS SELECT ON petclinic.CLINICS
  TO PETCLINIC_RECEPTIONIST, PETCLINIC_VET, PETCLINIC_ADMIN, PETCLINIC_STAFF;

SET USE DATA GRANTS ONLY ON petclinic.OWNERS ENABLED;
SET USE DATA GRANTS ONLY ON petclinic.PETS ENABLED;
SET USE DATA GRANTS ONLY ON petclinic.VISITS ENABLED;
SET USE DATA GRANTS ONLY ON petclinic.TYPES ENABLED;

PROMPT Oracle Deep Data Security setup completed for PetClinic.
PROMPT Start the application with MICRONAUT_ENVIRONMENTS=oracle-deepsec.
EXIT;
