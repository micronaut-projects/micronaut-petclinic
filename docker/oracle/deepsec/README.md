# Oracle Deep Data Security setup

This directory contains the DBA-run setup for the optional `oracle-deepsec` showcase. The application profile mirrors the reference implementation in [micronaut-deep-data-security-demo](https://github.com/sdelamo/micronaut-deep-data-security-demo): Micronaut OAuth2 browser login, the Micronaut Security OJDBC end-user context provider, Azure service-principal JDBC authentication, and TCPS wallet configuration.

The SQL files are not mounted into the Oracle container's automatic startup directory because they need real IAM identifiers and must run after the PetClinic schema has been created. `01-configure-deepsec.sql` contains the generic database policy. `02-seed-demo-identities.sql` is an optional local fixture that associates two sample owner rows with the two test emails.

The default Docker image is the full Oracle AI Database 26ai Free image, `container-registry.oracle.com/database/free:latest`. The full image is used because the Free Lite image does not include the `configTcps.sh` helper required to configure the TCPS listener. Override it with `ORACLE_IMAGE` when using another compatible 26ai image. Deep Data Security also requires a matching 23.26.x JDBC driver, which this repository already provides.

The database password is passed to the Oracle container as a Docker Compose secret. The application runs directly from the terminal using the variables in `.env`.

## Setup sequence

1. Copy the environment template, fill in the Entra values and a local database password, then load it into the shell used by Compose.

   ```bash
   cp .env.example .env
   $EDITOR .env
   set -a && source .env && set +a
   ```

   `ORACLE_DB_PASSWORD` is used only to create the Oracle `oracle_pwd` secret. The Entra values are also used by the terminal-launched application.
   `DEEPSEC_TEST_USER_EMAIL` and `DEEPSEC_ELEVATED_USER_EMAIL` are optional
   local-demo fixture values. If either is unset, Compose applies the generic
   DeepSec policy and skips the sample-row updates.

2. Register the Entra applications and configure their roles/claims as described by the reference demo. Set the variables in `.env`. The PetClinic application registration must have this Web redirect URI registered:

   ```text
   http://localhost:8080/oauth/callback/entraid
   ```

   Also register this post-logout redirect URI under the same PetClinic app
   registration's **Authentication → Web** platform:

   ```text
   http://localhost:8080/logout
   ```

   The database application must expose the delegated scope
   `${DB_APP_ID_URI}/sessions:scope:connect`. PetClinic must be authorized to
   request that scope through **API permissions**, and the database app must
   list PetClinic as an authorized client application. This scope is used by the
   JDBC provider to obtain the separate database-access token.

   PetClinic must also expose a delegated user scope, normally
   `api://<PETCLINIC_CLIENT_ID>/access_as_user`. Set `PETCLINIC_APP_ID_URI` and
   `PETCLINIC_USER_SCOPE` in `.env` to match the values shown under PetClinic's
   **Expose an API** page. The browser login requests this PetClinic scope, so
   the end-user access token is scoped to PetClinic and contains its application roles.

   In the **PetClinic app registration**, create app roles with values `EMPLOYEE`
   and `STAFF`. Assign `EMPLOYEE` to
   `emma@mohamedachbani3gmail.onmicrosoft.com` under **Enterprise applications
   → the PetClinic service principal → Users and groups**. Assign `STAFF` to
   `mohamedachbani3@gmail.com` in the same place. The next browser login for
   Emma must produce an end-user token containing:

   ```json
   {
     "upn": "emma@mohamedachbani3gmail.onmicrosoft.com",
     "roles": ["EMPLOYEE"],
     "scp": "access_as_user"
   }
   ```

   Oracle maps the Entra `EMPLOYEE` role to the database data role
   `PETCLINIC_EMPLOYEE`, which returns only the matching owner row with the
   telephone column excluded. It maps `STAFF` to `PETCLINIC_STAFF`, which
   returns the wider owner set with telephone masking. Telephone is available
   only through the local `PETCLINIC_SUPPORT` role used by the `@RunAs` demo.
   The optional demo fixture maps the first seeded owner to
   Emma's email and the second seeded owner to Mohamed's email. Set
   `DEEPSEC_ELEVATED_USER_EMAIL` in `.env` if the second address differs. The
   database-access token is obtained separately with
   `PETCLINIC_SCOPE=${DB_APP_ID_URI}/.default`.

   `PETCLINIC_SUPPORT` is different: it is a local Oracle data role, not an
   Entra app role. It is granted to the database application identity but
   disabled by default. The `@RunAs("ORACLE_DATA_ROLE_PETCLINIC_SUPPORT")`
   repository method requests it only for the support-contact operation.

3. Start the complete DeepSec database profile. Compose starts Oracle, initializes the schema/data if needed with the local `petclinic` user, and applies the DeepSec grants. No application start/stop cycle is required:

   ```bash
   docker-compose --profile oracle-deepsec up -d --build
   ```

   The bootstrap skips an already populated schema. The terminal-launched application still uses Entra/OJDBC for DeepSec queries.

4. TCPS is configured automatically by the Oracle container. It creates a self-signed server/client wallet under `oracle_data`. Export it to the host for the local application:

   ```bash
   sh docker/oracle/export-wallet.sh
   ```

   The generated certificate uses `CN=localhost`, matching the host-side JDBC URL. The wallet export directory is ignored by Git.

5. From the repository root, load the complete `.env` and start the application:

   ```bash
   set -a
   source .env
   set +a
   MICRONAUT_ENVIRONMENTS=oracle-deepsec ./gradlew clean run --no-daemon
   ```

   For an ordinary Oracle run, set `MICRONAUT_ENVIRONMENTS=oracle`; it connects to `localhost:1521` using the existing `petclinic` profile.

6. Open the application in a browser. Following Sergio Del Amo's Micronaut demo, the anonymous home page provides an `Enter` link to Micronaut's built-in OAuth2 flow:

   ```bash
   open http://localhost:8080/
   ```

   Click the Entra login link, sign in, approve consent if requested, and Entra redirects back to `/oauth/callback/entraid`. Micronaut stores a signed application cookie containing the original delegated access token and redirects to `/deepsec/owners`; the JDBC provider forwards the end-user context to Oracle.

   The DeepSec page's **Log out** button uses `/oauth/logout`. Micronaut
   redirects the browser to Entra's OpenID Connect logout endpoint, and Entra
   returns to `/logout` to clear the local Micronaut cookie before returning to
   the home page.

7. Visit the Deep Sec endpoint in the same browser session:

   ```bash
   open http://localhost:8080/deepsec/owners
   ```

The request uses the ordinary `findAllOwners` Micronaut Data query. Emma should
see one owner row, while Mohamed should see the expanded owner set. Oracle
performs the row filtering and column masking; they are not implemented in the
controller. The email values are owner data used by the generic employee
predicate, not role definitions.

If the application starts without IAM variables, the exported wallet, or TCPS configuration, it will fail fast. This is a proof-of-work integration and has deliberately not been validated against a live Oracle/Entra environment. A locally fabricated JWT can test Micronaut's HTTP authentication only; it does not prove Oracle Deep Data Security because Oracle must validate the end-user and database-access tokens itself.
