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

CREATE OR REPLACE DATA ROLE PETCLINIC_PET_OWNER
  MAPPED TO 'AZURE_ROLE=PET_OWNER';
CREATE OR REPLACE DATA ROLE PETCLINIC_CLINIC_STAFF
  MAPPED TO 'AZURE_ROLE=CLINIC_STAFF';

-- This local role is authorized for the application but disabled by default. It
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

-- The pet-owner/clinic-staff roles come from the end-user token. Do not grant them
-- to the application identity, otherwise they would become application-wide.
DECLARE
  role_names SYS.ODCIVARCHAR2LIST := SYS.ODCIVARCHAR2LIST(
    'PETCLINIC_PET_OWNER',
    'PETCLINIC_CLINIC_STAFF'
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

-- The pet-owner policy uses the EMAIL business attribute to match the current
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

-- Owners: pet owners see their own row and clinic staff see all rows, but both roles
-- have telephone excluded. Support reveals all columns only for @RunAs.
CREATE OR REPLACE DATA GRANT petclinic.pet_owner_owners_read
  AS SELECT (ALL COLUMNS EXCEPT TELEPHONE)
  ON petclinic.OWNERS
  WHERE LOWER(EMAIL) = LOWER(ORA_END_USER_CONTEXT.username)
  TO PETCLINIC_PET_OWNER;

CREATE OR REPLACE DATA GRANT petclinic.clinic_staff_owners_read
  AS SELECT (ALL COLUMNS EXCEPT TELEPHONE)
  ON petclinic.OWNERS
  TO PETCLINIC_CLINIC_STAFF;

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
  TO PETCLINIC_PET_OWNER;

CREATE OR REPLACE DATA GRANT petclinic.clinic_staff_pets_read
  AS SELECT
  ON petclinic.PETS
  TO PETCLINIC_CLINIC_STAFF;

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
  TO PETCLINIC_PET_OWNER;

CREATE OR REPLACE DATA GRANT petclinic.clinic_staff_visits_read
  AS SELECT
  ON petclinic.VISITS
  TO PETCLINIC_CLINIC_STAFF;

CREATE OR REPLACE DATA GRANT petclinic.owner_pet_types_read
  AS SELECT
  ON petclinic.TYPES
  TO PETCLINIC_PET_OWNER;

CREATE OR REPLACE DATA GRANT petclinic.clinic_staff_pet_types_read
  AS SELECT
  ON petclinic.TYPES
  TO PETCLINIC_CLINIC_STAFF;

CREATE OR REPLACE DATA GRANT petclinic.clinic_staff_vets_read
  AS SELECT ON petclinic.VETS
  TO PETCLINIC_CLINIC_STAFF;
CREATE OR REPLACE DATA GRANT petclinic.clinic_staff_specialties_read
  AS SELECT ON petclinic.SPECIALTIES
  TO PETCLINIC_CLINIC_STAFF;
CREATE OR REPLACE DATA GRANT petclinic.clinic_staff_vet_specialties_read
  AS SELECT ON petclinic.VET_SPECIALTIES
  TO PETCLINIC_CLINIC_STAFF;
CREATE OR REPLACE DATA GRANT petclinic.clinic_staff_clinics_read
  AS SELECT ON petclinic.CLINICS
  TO PETCLINIC_CLINIC_STAFF;

-- Remove data grants left by older versions of this showcase.
DROP DATA GRANT IF EXISTS petclinic.employee_owners_read;
DROP DATA GRANT IF EXISTS petclinic.receptionist_owners_read;
DROP DATA GRANT IF EXISTS petclinic.vet_owners_read;
DROP DATA GRANT IF EXISTS petclinic.admin_owners_crud;
DROP DATA GRANT IF EXISTS petclinic.staff_pets_read;
DROP DATA GRANT IF EXISTS petclinic.staff_visits_read;
DROP DATA GRANT IF EXISTS petclinic.staff_pet_types_read;
DROP DATA GRANT IF EXISTS petclinic.staff_vets_read;
DROP DATA GRANT IF EXISTS petclinic.staff_specialties_read;
DROP DATA GRANT IF EXISTS petclinic.staff_vet_specialties_read;
DROP DATA GRANT IF EXISTS petclinic.staff_clinics_read;

-- Remove roles left by older versions of this showcase.
DROP DATA ROLE IF EXISTS PETCLINIC_EMPLOYEE;
DROP DATA ROLE IF EXISTS PETCLINIC_STAFF;
DROP DATA ROLE IF EXISTS PETCLINIC_RECEPTIONIST;
DROP DATA ROLE IF EXISTS PETCLINIC_VET;
DROP DATA ROLE IF EXISTS PETCLINIC_ADMIN;

SET USE DATA GRANTS ONLY ON petclinic.OWNERS ENABLED;
SET USE DATA GRANTS ONLY ON petclinic.PETS ENABLED;
SET USE DATA GRANTS ONLY ON petclinic.VISITS ENABLED;
SET USE DATA GRANTS ONLY ON petclinic.TYPES ENABLED;

PROMPT Oracle Deep Data Security setup completed for PetClinic.
PROMPT Start the application with MICRONAUT_ENVIRONMENTS=oracle-deepsec.
EXIT;
