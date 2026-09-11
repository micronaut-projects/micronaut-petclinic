#!/bin/bash

# The application connects from the host, so the certificate name must match
# the host-side JDBC hostname.
/opt/oracle/configTcps.sh "${TCPS_HOSTNAME:-localhost}"
