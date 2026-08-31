-- Create petclinic user in the pluggable database
ALTER SESSION SET CONTAINER = FREEPDB1;

-- VECTOR columns require automatic segment space management. The lite image
-- does not provide a USERS tablespace, so create a small dedicated one.
DECLARE
  v_tablespace_exists NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_tablespace_exists
  FROM dba_tablespaces
  WHERE tablespace_name = 'PETCLINIC_DATA';
  IF v_tablespace_exists = 0 THEN
    EXECUTE IMMEDIATE 'CREATE TABLESPACE petclinic_data DATAFILE ''/opt/oracle/oradata/FREE/FREEPDB1/petclinic_data01.dbf'' SIZE 100M AUTOEXTEND ON NEXT 10M MAXSIZE 2G EXTENT MANAGEMENT LOCAL SEGMENT SPACE MANAGEMENT AUTO';
  END IF;
END;
/

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

ALTER USER petclinic DEFAULT TABLESPACE PETCLINIC_DATA;
ALTER USER petclinic QUOTA UNLIMITED ON PETCLINIC_DATA;

EXIT;
