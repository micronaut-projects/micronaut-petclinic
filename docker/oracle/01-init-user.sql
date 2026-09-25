-- Create petclinic user in the pluggable database
WHENEVER SQLERROR EXIT SQL.SQLCODE
ALTER SESSION SET CONTAINER = FREEPDB1;

SET SERVEROUTPUT ON

-- Configure Oracle Priority Transactions for the appointment showcase when
-- this database image supports the feature. The guard keeps older or limited
-- Oracle images startable; check this startup output for unsupported images.
DECLARE
  v_supported_parameters NUMBER;
BEGIN
  SELECT COUNT(*)
  INTO v_supported_parameters
  FROM v$parameter
  WHERE name IN (
    'priority_txns_mode',
    'priority_txns_high_wait_target',
    'priority_txns_medium_wait_target'
  );

  IF v_supported_parameters = 3 THEN
    EXECUTE IMMEDIATE 'ALTER SYSTEM SET PRIORITY_TXNS_HIGH_WAIT_TARGET = 3 SCOPE=BOTH';
    EXECUTE IMMEDIATE 'ALTER SYSTEM SET PRIORITY_TXNS_MEDIUM_WAIT_TARGET = 3 SCOPE=BOTH';
    EXECUTE IMMEDIATE 'ALTER SYSTEM SET PRIORITY_TXNS_MODE = ''ROLLBACK'' SCOPE=BOTH';
    DBMS_OUTPUT.PUT_LINE('Oracle Priority Transactions enabled: mode=ROLLBACK, highWaitTarget=3s, mediumWaitTarget=3s');
  ELSE
    DBMS_OUTPUT.PUT_LINE('Oracle Priority Transactions are not available in this database image; the transaction-priority showcase will not demonstrate priority takeover.');
  END IF;
EXCEPTION
  WHEN OTHERS THEN
    DBMS_OUTPUT.PUT_LINE('ERROR: Oracle Priority Transactions could not be configured: ' || SQLERRM);
    RAISE;
END;
/

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
