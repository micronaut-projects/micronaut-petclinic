-- Optional local-demo fixture.
--
-- This file contains demo data, not the DeepSec policy. The policy in
-- 01-configure-deepsec.sql is generic and matches any owner's EMAIL to the
-- current Entra identity for the EMPLOYEE role.

WHENEVER SQLERROR EXIT SQL.SQLCODE
SET DEFINE ON
SET VERIFY OFF

DEFINE deepsec_employee_email = '&1'
DEFINE deepsec_staff_email = '&2'

ALTER SESSION SET CONTAINER = FREEPDB1;

-- Associate the first two sample records with the two local demo accounts.
-- Real applications should write EMAIL when an owner is created or linked to
-- an identity, rather than relying on this fixture.
UPDATE petclinic.OWNERS
SET EMAIL = LOWER('&deepsec_employee_email')
WHERE ID = (SELECT MIN(ID) FROM petclinic.OWNERS);

UPDATE petclinic.OWNERS
SET EMAIL = LOWER('&deepsec_staff_email')
WHERE ID = (
  SELECT MIN(ID)
  FROM petclinic.OWNERS
  WHERE ID > (SELECT MIN(ID) FROM petclinic.OWNERS)
);

COMMIT;

PROMPT DeepSec demo identities seeded.
EXIT;
