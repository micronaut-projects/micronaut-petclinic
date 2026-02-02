-- Create petclinic user in the pluggable database
ALTER SESSION SET CONTAINER = FREEPDB1;

-- Create the petclinic user
DECLARE
  v_user_exists NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_user_exists FROM all_users WHERE username = 'PETCLINIC';
  IF v_user_exists = 0 THEN
    EXECUTE IMMEDIATE 'CREATE USER petclinic IDENTIFIED BY petclinic';
    EXECUTE IMMEDIATE 'GRANT CONNECT, RESOURCE TO petclinic';
    EXECUTE IMMEDIATE 'GRANT CREATE SESSION TO petclinic';
    EXECUTE IMMEDIATE 'GRANT CREATE TABLE TO petclinic';
    EXECUTE IMMEDIATE 'GRANT CREATE SEQUENCE TO petclinic';
    EXECUTE IMMEDIATE 'GRANT UNLIMITED TABLESPACE TO petclinic';
  END IF;
END;
/

EXIT;
