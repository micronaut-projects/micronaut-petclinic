package io.micronaut.samples.petclinic.service;

import io.micronaut.context.MessageSource;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.samples.petclinic.dto.LoyaltyRedemptionResponse;
import io.micronaut.samples.petclinic.exception.IllegalRequestArgumentException;
import io.micronaut.samples.petclinic.service.oracle.LoyaltyRedemptionTransaction;
import io.micronaut.samples.petclinic.dto.OwnerLoyaltySummary;
import io.micronaut.samples.petclinic.model.Owner;
import io.micronaut.samples.petclinic.model.Pet;
import io.micronaut.samples.petclinic.model.PetType;
import io.micronaut.samples.petclinic.model.Visit;
import io.micronaut.samples.petclinic.oracle.model.OwnerLoyaltyAccount;
import io.micronaut.samples.petclinic.oracle.model.OwnerRewardVoucher;
import io.micronaut.samples.petclinic.oracle.model.OwnerCareView;
import io.micronaut.samples.petclinic.repository.OwnerRepository;
import io.micronaut.samples.petclinic.repository.OwnerCareViewRepository;
import io.micronaut.samples.petclinic.repository.VisitRepository;
import io.micronaut.samples.petclinic.repository.oracle.OwnerLoyaltyAccountRepository;
import io.micronaut.samples.petclinic.repository.oracle.OwnerRewardVoucherRepository;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.sql.Connection;
import io.micronaut.transaction.TransactionOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Oracle integration coverage for reservable loyalty balances and reward vouchers. */
@MicronautTest(transactional = false)
@Requires(env = "oracle", notEnv = "oracle-deepsec")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OracleOwnerLoyaltyIntegrationTest {

    @Inject
    MessageSource messageSource;

    @Inject
    ClinicService clinicService;

    @Inject
    TransactionOperations<Connection> transactions;

    @Inject
    OwnerRepository ownerRepository;

    @Inject
    OwnerLoyaltyAccountRepository accountRepository;

    @Inject
    OwnerRewardVoucherRepository voucherRepository;

    @Inject
    VisitRepository visitRepository;

    @Inject
    OwnerLoyaltyService ownerLoyaltyService;

    @Inject
    LoyaltyRedemptionTransaction redemptionTransaction;

    @Inject
    OwnerCareViewRepository ownerCareViewRepository;

    @Inject
    OwnerCareViewService ownerCareViewService;

    @Inject
    OracleLoyaltyTestDatabase testDatabase;

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    @Order(1)
    void ownerCreationVisitCreationAndVisitUpdatesMaintainTheExpectedPoints() {
        Owner owner = createOwner();
        assertThat(accountRepository.findByOwner(owner).orElseThrow().points()).isZero();

        Pet pet = createPet(owner);
        assertThatThrownBy(() -> clinicService.saveVisit(new Visit(null, null, "Invalid visit", pet,
                Duration.ofMinutes(30), Period.ofMonths(6)))).isInstanceOf(RuntimeException.class);
        assertThat(visitRepository.findByPetId(pet.id())).isEmpty();
        assertThat(accountRepository.findByOwner(owner).orElseThrow().points()).isZero();

        Visit visit = createVisit(pet, 1);
        assertThat(accountRepository.findByOwner(owner).orElseThrow().points()).isEqualTo(25L);

        clinicService.saveVisit(new Visit(visit.id(), visit.date(), "Updated description", visit.pet(),
                visit.duration(), visit.period()));
        assertThat(accountRepository.findByOwner(owner).orElseThrow().points()).isEqualTo(25L);

        Owner persisted = ownerRepository.findById(owner.id()).orElseThrow();
        clinicService.saveOwner(persisted.withCity("Updated City"));
        assertThat(accountRepository.findByOwner(owner).orElseThrow().points()).isEqualTo(25L);
    }

    @Test
    @Order(5)
    void jsonDualityOwnerUpdatesCannotReplaceTheSeparateLoyaltyBalance() throws Exception {
        Owner owner = createOwner();
        createPet(owner);
        OwnerLoyaltyAccount ownerLoyaltyAccount = createOwnerLoyaltyAccount(owner);

        incrementPoints(ownerLoyaltyAccount, 75L);
        OwnerCareView original = ownerCareViewRepository.findById(owner.id()).orElseThrow();
        OwnerCareView updated = new OwnerCareView(original.id(), original.firstName(), original.lastName(),
                original.address(), original.city() + " JSON", original.telephone(), original.pets(),
                original.metadata());

        ownerCareViewService.update(updated);

        assertThat(accountRepository.findByOwner(owner).orElseThrow().points()).isEqualTo(75L);
    }

    @Test
    @Order(2)
    void fourVisitsRedeemOneVoucherAndSeventyFivePointsAreRejected() {
        Owner owner = createOwner();
        Pet pet = createPet(owner);
        createVisits(pet, 4);
        OwnerLoyaltyAccount ownerLoyaltyAccount = createOwnerLoyaltyAccount(owner);

        HttpRequest<?> request = HttpRequest.POST("/owners/" + owner.id() + "/loyalty/redemptions", "")
                .contentType(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE);
        var created = client.toBlocking().exchange(request, LoyaltyRedemptionResponse.class);

        assertThat((CharSequence) created.status()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.body().points()).isZero();
        assertThat(created.body().voucher().rewardCode()).isEqualTo("PET_CARE_REWARD");
        assertThat(accountRepository.findByOwner(owner).orElseThrow().points()).isZero();
        assertThat(vouchers(owner)).hasSize(1);

        Owner shortOwner = createOwner();
        Pet shortPet = createPet(shortOwner);
        createVisits(shortPet, 3);
        HttpClientResponseException conflict = org.junit.jupiter.api.Assertions.assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(HttpRequest.POST(
                        "/owners/" + shortOwner.id() + "/loyalty/redemptions", "")
                        .contentType(MediaType.APPLICATION_JSON_TYPE)
                        .accept(MediaType.APPLICATION_JSON_TYPE), String.class));

        assertThat((CharSequence) conflict.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(conflict.getResponse().getBody(String.class).orElseThrow())
                .contains("INSUFFICIENT_LOYALTY_POINTS");
        assertThat(accountRepository.findByOwner(shortOwner).orElseThrow().points()).isEqualTo(75L);
        assertThat(vouchers(shortOwner)).isEmpty();
    }

    @Test
    @Order(3)
    void ownerAndPetPathMustMatchBeforeAVisitCanEarnPoints() {
        Owner owner = createOwner();
        Pet pet = createPet(owner);
        String body = """
                {"date":"2025-01-01","description":"Routine checkup","durationMinutes":30,"followUpPeriodMonths":6}
                """;

        HttpClientResponseException exception = org.junit.jupiter.api.Assertions.assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(HttpRequest.POST(
                                "/owners/2147483647/pets/" + pet.id() + "/visits/new", body)
                        .contentType(MediaType.APPLICATION_JSON_TYPE)
                        .accept(MediaType.APPLICATION_JSON_TYPE), String.class));

        assertThat((CharSequence) exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(accountRepository.findByOwner(owner).orElseThrow().points()).isZero();
    }

    @Test
    @Order(4)
    void unknownOwnerReturnsNotFoundFromBothLoyaltyEndpoints() {
        HttpClientResponseException getFailure = org.junit.jupiter.api.Assertions.assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(HttpRequest.GET("/owners/2147483647/loyalty"), String.class));
        HttpClientResponseException postFailure = org.junit.jupiter.api.Assertions.assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(HttpRequest.POST(
                        "/owners/2147483647/loyalty/redemptions", ""), String.class));

        assertThat((CharSequence) getFailure.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat((CharSequence) postFailure.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @Order(12)
    void generatedSchemaUsesReservablePointsWithANonnegativeCheckAndRejectsDirectAssignments() throws Exception {
        Owner owner = createOwner();
        assertThat(accountRepository.findByOwner(owner).orElseThrow().points()).isZero();

        assertThat(testDatabase.reservablePointsColumn()).isEqualToIgnoringCase("YES");
        assertThat(testDatabase.pointsColumnIsNotNullable()).isTrue();
        assertThat(testDatabase.nonnegativeCheckConstraintCount()).isGreaterThan(0);
        assertThat(testDatabase.uniqueOwnerRelationIndexCount()).isGreaterThan(0);
        assertThatThrownBy(() -> testDatabase.directAssignPoints(owner.id())).isInstanceOf(Exception.class);

        assertThat(accountRepository.findByOwner(owner).orElseThrow().points()).isZero();
    }

    @Test
    @Order(10)
    void voucherInsertFailureRollsBackTheReservation() throws Exception {
        Owner owner = createOwner();
        OwnerLoyaltyAccount ownerLoyaltyAccount = createOwnerLoyaltyAccount(owner);
        incrementPoints(ownerLoyaltyAccount, 200L);
        assertThat(accountRepository.findByOwner(owner).orElseThrow().points()).isEqualTo(200L);

        testDatabase.execute("ALTER TABLE OWNER_REWARD_VOUCHERS ADD CONSTRAINT TEST_LOYALTY_VOUCHER_FAIL "
                + "CHECK (OWNER_LOYALTY_ACCOUNT_ID <> " + ownerLoyaltyAccount.id() + " OR REWARD_CODE <> 'PET_CARE_REWARD')");

        try {
            assertThatThrownBy(() -> ownerLoyaltyService.redeem(ownerLoyaltyAccount))
                    .isInstanceOf(RuntimeException.class)
                    .isNotInstanceOf(InsufficientLoyaltyPointsException.class);
            assertThat(accountRepository.findByOwner(owner).orElseThrow().points()).isEqualTo(200L);
            assertThat(vouchers(owner)).isEmpty();
        } finally {
            testDatabase.execute("ALTER TABLE OWNER_REWARD_VOUCHERS DROP CONSTRAINT TEST_LOYALTY_VOUCHER_FAIL");
        }
    }

    @Test
    @Order(11)
    void failedCreditRollsBackTheInsertedVisit() throws Exception {
        Owner owner = createOwner();
        Pet pet = createPet(owner);
        OwnerLoyaltyAccount ownerLoyaltyAccount = createOwnerLoyaltyAccount(owner);
        incrementPoints(ownerLoyaltyAccount, 100L);
        long visitsBefore = visitRepository.findByPetId(pet.id()).size();

        testDatabase.execute("ALTER TABLE OWNER_LOYALTY_ACCOUNT ADD CONSTRAINT TEST_LOYALTY_MAX "
                + "CHECK (OWNER_ID <> " + owner.id() + " OR POINTS <= 100)");

        try {
            assertThatThrownBy(() -> createVisit(pet, 5)).isInstanceOf(RuntimeException.class);
            assertThat(visitRepository.findByPetId(pet.id())).hasSize((int) visitsBefore);
            assertThat(accountRepository.findByOwner(owner).orElseThrow().points()).isEqualTo(100L);
        } finally {
            testDatabase.execute("ALTER TABLE OWNER_LOYALTY_ACCOUNT DROP CONSTRAINT TEST_LOYALTY_MAX");
        }
    }

    @Test
    @Order(9)
    void ownerDeletionRemovesItsAccountAndVouchers() {
        Owner owner = createOwner();
        OwnerLoyaltyAccount ownerLoyaltyAccount = createOwnerLoyaltyAccount(owner);

        voucherRepository.save(new OwnerRewardVoucher(null, ownerLoyaltyAccount, "PET_CARE_REWARD", 100L,
                LocalDateTime.now()));

        clinicService.deleteOwner(owner);

        assertThat(ownerRepository.findById(owner.id())).isEmpty();
        assertThat(accountRepository.findByOwner(owner)).isEmpty();
        assertThat(voucherRepository.findTop10ByOwnerLoyaltyAccountOrderByCreatedAtDescAndIdDesc(ownerLoyaltyAccount)).isEmpty();
    }

    @Test
    @Order(8)
    void accountReconciliationStartsExistingOwnersAtZeroWithoutHistoricalEarnings() {
        Owner owner = ownerRepository.save(new Owner("Existing", "Owner" + ThreadLocalRandom.current().nextInt(1_000_000),
                "123 Test Street", "Madison", "6085551111"));
        Pet pet = createPet(owner);
        for (int index = 1; index <= 4; index++) {
            visitRepository.save(new Visit(LocalDate.of(2024, 1, index), "Historical visit " + index,
                    pet, Duration.ofMinutes(30), Period.ofMonths(6)));
        }

        ownerLoyaltyService.ensureAccounts(List.of(owner));

        assertThat(accountRepository.findByOwner(owner).orElseThrow().points()).isZero();
    }

    @Test
    @Order(6)
    void summaryReturnsOnlyTheTenNewestVouchersInDescendingDateOrder() {
        Owner owner = createOwner();
        OwnerLoyaltyAccount ownerLoyaltyAccount = createOwnerLoyaltyAccount(owner);
        LocalDateTime firstDate = LocalDateTime.of(2025, 1, 1, 0, 0);
        for (int index = 0; index < 12; index++) {
            voucherRepository.save(new OwnerRewardVoucher(null, ownerLoyaltyAccount, "PET_CARE_REWARD", 100L,
                    firstDate.plusDays(index)));
        }

        OwnerLoyaltyAccount loaded = accountRepository.findByOwner(owner).orElseThrow();
        List<OwnerLoyaltySummary.Voucher> recent = ownerLoyaltyService.summary(loaded).recentVouchers();

        assertThat(recent).hasSize(10);
        assertThat(recent.getFirst().createdAt()).isEqualTo(firstDate.plusDays(11));
        assertThat(recent.getLast().createdAt()).isEqualTo(firstDate.plusDays(2));
    }

    @Test
    @Order(7)
    void ordinaryReservationMethodsApplyDeltasAfterCommitAndOwnerPageIsLocalized() {
        Owner owner = createOwner();
        OwnerLoyaltyAccount ownerLoyaltyAccount = createOwnerLoyaltyAccount(owner);
        incrementPoints(ownerLoyaltyAccount, 75L);
        assertThat(accountRepository.findByOwner(owner).orElseThrow().points()).isEqualTo(75L);
        incrementPoints(ownerLoyaltyAccount, 25L);
        assertThat(accountRepository.findByOwner(owner).orElseThrow().points()).isEqualTo(100L);
        decrementPoints(owner, 25L);
        assertThat(accountRepository.findByOwner(owner).orElseThrow().points()).isEqualTo(75L);

        HttpResponse<OwnerLoyaltySummary> apiResponse = client.toBlocking().exchange(
                HttpRequest.GET("/owners/" + owner.id() + "/loyalty"), OwnerLoyaltySummary.class);
        assertThat((CharSequence) apiResponse.status()).isEqualTo(HttpStatus.OK);
        assertThat(apiResponse.body().points()).isEqualTo(75L);
        assertThat(apiResponse.body().pointsPerVisit()).isEqualTo(25);
        assertThat(apiResponse.body().rewardCost()).isEqualTo(100L);

        String page = client.toBlocking().retrieve(HttpRequest.GET("/owners/" + owner.id())
                .cookie(io.micronaut.http.cookie.Cookie.of("locale", "es")));
        assertThat(page).contains("Puntos de fidelidad", "id=\"loyalty-balance\">75</span> puntos",
                "Canjear 100 puntos");
        String germanPage = client.toBlocking().retrieve(HttpRequest.GET("/owners/" + owner.id())
                .cookie(io.micronaut.http.cookie.Cookie.of("locale", "de")));
        assertThat(germanPage).contains("Treuepunkte", "Aktueller Punktestand");
        assertThat(ownerLoyaltyService.summary(ownerLoyaltyAccount).recentVouchers()).isEmpty();
    }

    @Test
    @Order(13)
    void simultaneousRedemptionsCannotOverspendOneReward() throws Exception {
        assertConcurrentRedemptions(100L, 1);
    }

    @Test
    @Order(14)
    void simultaneousRedemptionsCanBothSpendTwoRewards() throws Exception {
        assertConcurrentRedemptions(200L, 2);
    }

    private void assertConcurrentRedemptions(long points, int expectedVouchers) throws Exception {
        Owner owner = createOwner();
        OwnerLoyaltyAccount account = createOwnerLoyaltyAccount(owner);
        incrementPoints(account, points);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = executor.submit(() -> redeemAfter(start, account));
            var second = executor.submit(() -> redeemAfter(start, account));
            start.countDown();
            int successes = (first.get(15, TimeUnit.SECONDS) ? 1 : 0)
                    + (second.get(15, TimeUnit.SECONDS) ? 1 : 0);
            assertThat(successes).isEqualTo(expectedVouchers);
        }
        assertThat(vouchers(owner)).hasSize(expectedVouchers);
        assertThat(accountRepository.findByOwner(owner).orElseThrow().points()).isZero();
    }

    private boolean redeemAfter(CountDownLatch start, OwnerLoyaltyAccount account) throws Exception {
        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("The redemption test did not start.");
        }
        try {
            ownerLoyaltyService.redeem(account);
            return true;
        } catch (InsufficientLoyaltyPointsException expected) {
            return false;
        }
    }

    @Test
    @Order(15)
    void concurrentVisitsCreditEveryCommittedVisit() throws Exception {
        Owner owner = createOwner();
        Pet pet = createPet(owner);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = executor.submit(() -> { start.await(); return createVisit(pet, 1); });
            var second = executor.submit(() -> { start.await(); return createVisit(pet, 2); });
            start.countDown();
            first.get(15, TimeUnit.SECONDS);
            second.get(15, TimeUnit.SECONDS);
        }
        assertThat(visitRepository.findByPetId(pet.id())).hasSize(2);
        assertThat(accountRepository.findByOwner(owner).orElseThrow().points()).isEqualTo(50L);
    }

    @Test
    @Order(16)
    void missingAccountFallbackCreatesOnceAndPreservesTheExistingBalance() {
        Owner owner = ownerRepository.save(new Owner("Existing", "MissingAccount", "123 Test Street",
                "Madison", "6085551111"));
        assertThat(accountRepository.findByOwner(owner)).isEmpty();

        OwnerLoyaltyAccount created = ownerLoyaltyService.findOrCreateOwnerLoyaltyAccount(owner);
        assertThat(created.points()).isZero();
        incrementPoints(created, 75L);

        OwnerLoyaltyAccount existing = ownerLoyaltyService.findOrCreateOwnerLoyaltyAccount(owner);
        assertThat(existing.id()).isEqualTo(created.id());
        assertThat(existing.points()).isEqualTo(75L);
        assertThat(accountRepository.findAllByOwnerIn(List.of(owner))).hasSize(1);
    }

    @Test
    @Order(17)
    void anotherReservationCommitsBeforeTheFirstTransactionCompletes() throws Exception {
        Owner owner = createOwner();
        OwnerLoyaltyAccount account = createOwnerLoyaltyAccount(owner);
        incrementPoints(account, 200L);
        CountDownLatch reserved = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = executor.submit(() -> {
                testDatabase.holdReservation(account.id(), -100L, reserved, release, true);
                return null;
            });
            try {
                assertThat(reserved.await(10, TimeUnit.SECONDS)).isTrue();
                var second = executor.submit(() -> ownerLoyaltyService.redeem(account));
                assertThat(second.get(5, TimeUnit.SECONDS).points()).isEqualTo(100L);
                assertThat(first.isDone()).isFalse();
            } finally {
                release.countDown();
            }
            first.get(15, TimeUnit.SECONDS);
        }
        assertThat(accountRepository.findByOwner(owner).orElseThrow().points()).isZero();
        assertThat(vouchers(owner)).hasSize(1);
    }

    @Test
    @Order(18)
    void rollbackReleasesPointsReservedByAnotherTransaction() throws Exception {
        Owner owner = createOwner();
        OwnerLoyaltyAccount account = createOwnerLoyaltyAccount(owner);
        incrementPoints(account, 100L);
        CountDownLatch reserved = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var pending = executor.submit(() -> {
                testDatabase.holdReservation(account.id(), -100L, reserved, release, false);
                return null;
            });
            try {
                assertThat(reserved.await(10, TimeUnit.SECONDS)).isTrue();
                var rejected = executor.submit(() -> {
                    try {
                        ownerLoyaltyService.redeem(account);
                        return false;
                    } catch (InsufficientLoyaltyPointsException expected) {
                        return true;
                    }
                });
                assertThat(rejected.get(5, TimeUnit.SECONDS)).isTrue();
                assertThat(vouchers(owner)).isEmpty();
            } finally {
                release.countDown();
            }
            pending.get(15, TimeUnit.SECONDS);
        }
        assertThat(ownerLoyaltyService.redeem(account).points()).isZero();
        assertThat(vouchers(owner)).hasSize(1);
    }

    @Test
    @Order(19)
    void rollingBackTheVisitAlsoRollsBackItsCredit() {
        Owner owner = createOwner();
        Pet pet = createPet(owner);
        assertThatThrownBy(() -> transactions.executeWrite(status -> {
            createVisit(pet, 1);
            throw new IllegalStateException("Force the enclosing visit transaction to roll back.");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(visitRepository.findByPetId(pet.id())).isEmpty();
        assertThat(accountRepository.findByOwner(owner).orElseThrow().points()).isZero();
    }

    @Test
    @Order(20)
    void vouchersWithTheSameTimestampAreOrderedByNewestId() {
        Owner owner = createOwner();
        OwnerLoyaltyAccount account = createOwnerLoyaltyAccount(owner);
        LocalDateTime timestamp = LocalDateTime.of(2025, 1, 1, 0, 0);
        OwnerRewardVoucher older = voucherRepository.save(new OwnerRewardVoucher(null, account,
                "PET_CARE_REWARD", 100L, timestamp));
        OwnerRewardVoucher newer = voucherRepository.save(new OwnerRewardVoucher(null, account,
                "PET_CARE_REWARD", 100L, timestamp));
        OwnerLoyaltyAccount loaded = accountRepository.findByOwner(owner).orElseThrow();
        assertThat(ownerLoyaltyService.summary(loaded).recentVouchers())
                .extracting(OwnerLoyaltySummary.Voucher::reference)
                .containsExactly("PCR-%08d".formatted(newer.id()), "PCR-%08d".formatted(older.id()));
    }

    @Test
    @Order(21)
    void loyaltyLabelsDatesAndInsufficientPointsUseTheSelectedLocale() {
        Owner owner = createOwner();
        OwnerLoyaltyAccount account = createOwnerLoyaltyAccount(owner);
        LocalDateTime timestamp = LocalDateTime.of(2025, 7, 14, 12, 0);
        voucherRepository.save(new OwnerRewardVoucher(null, account, "PET_CARE_REWARD", 100L, timestamp));

        for (String language : List.of("en", "de", "es")) {
            Locale locale = Locale.forLanguageTag(language);
            var cookie = io.micronaut.http.cookie.Cookie.of("locale", language);
            String page = client.toBlocking().retrieve(HttpRequest.GET("/owners/" + owner.id()).cookie(cookie));
            int cardStart = page.indexOf("<section id=\"owner-loyalty-card\"");
            assertThat(cardStart).isGreaterThanOrEqualTo(0);
            String card = page.substring(cardStart, page.indexOf("</section>", cardStart));
            assertThat(card).contains("data-locale=\"" + language + "\"",
                    timestamp.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)));
            for (String key : List.of("title", "balance", "points", "recentVouchers", "noVouchers",
                    "success", "insufficientPoints", "ownerNotFound", "requestFailed")) {
                assertThat(card).contains(messageSource.getMessage("loyalty." + key, locale).orElseThrow());
            }
            assertThat(card).doesNotContain("loyalty.", "2025-07-14</time>");

            HttpClientResponseException failure = org.junit.jupiter.api.Assertions.assertThrows(
                    HttpClientResponseException.class,
                    () -> client.toBlocking().exchange(HttpRequest.POST(
                                    "/owners/" + owner.id() + "/loyalty/redemptions", "")
                            .cookie(cookie).accept(MediaType.APPLICATION_JSON_TYPE), String.class));
            assertThat((CharSequence) failure.getStatus()).isEqualTo(HttpStatus.CONFLICT);
            Map<?, ?> body = failure.getResponse().getBody(Map.class).orElseThrow();
            assertThat(body.get("code")).isEqualTo("INSUFFICIENT_LOYALTY_POINTS");
            assertThat(body.get("message")).isEqualTo(
                    messageSource.getMessage("loyalty.insufficientPoints", locale).orElseThrow());
        }
    }

    @Test
    @Order(22)
    void invalidPointsCostsRejectBothDebitPathsWithoutChangingTheBalance() {
        Owner owner = createOwner();
        OwnerLoyaltyAccount account = createOwnerLoyaltyAccount(owner);
        incrementPoints(account, 200L);

        for (long pointsCost : List.of(0L, -100L)) {
            assertThatThrownBy(() -> ownerLoyaltyService.reserveDecrementPoints(account, pointsCost))
                    .isExactlyInstanceOf(IllegalRequestArgumentException.class);
            assertThatThrownBy(() -> redemptionTransaction.redeem(account, pointsCost))
                    .isExactlyInstanceOf(IllegalRequestArgumentException.class);
            assertThat(accountRepository.findByOwner(owner).orElseThrow().points()).isEqualTo(200L);
            assertThat(vouchers(owner)).isEmpty();
        }
    }

    private List<OwnerRewardVoucher> vouchers(Owner owner) {
        return voucherRepository.findTop10ByOwnerLoyaltyAccountOrderByCreatedAtDescAndIdDesc(
                accountRepository.findByOwner(owner).orElseThrow());
    }

    private Owner createOwner() {
        int suffix = ThreadLocalRandom.current().nextInt(1_000_000, 9_999_999);
        return clinicService.saveOwner(new Owner("Loyalty", "Owner" + suffix,
                "123 Test Street", "Madison", "608" + suffix));
    }

    private Pet createPet(Owner owner) {
        PetType type = clinicService.findPetTypes().stream()
                .filter(petType -> petType.name().equals("cat"))
                .findFirst().orElseThrow();
        return clinicService.savePet(new Pet("Pet" + owner.id(), LocalDate.of(2020, 1, 1), type, owner));
    }

    private void createVisits(Pet pet, int count) {
        for (int index = 1; index <= count; index++) {
            createVisit(pet, index);
        }
    }

    private OwnerLoyaltyAccount createOwnerLoyaltyAccount(@NonNull Owner owner) {
        return ownerLoyaltyService.findOrCreateOwnerLoyaltyAccount(owner);
    }

    private void incrementPoints(OwnerLoyaltyAccount account, long points) {
        accountRepository.reserveIncrementPoints(account.id(), points);
    }

    private void decrementPoints(Owner owner, long points) {
        OwnerLoyaltyAccount account = accountRepository.findByOwner(owner).orElseThrow();
        accountRepository.reserveDecrementPoints(account.id(), points);
    }

    private Visit createVisit(Pet pet, int index) {
        return clinicService.saveVisit(new Visit(LocalDate.of(2025, 1, index), "Visit " + index,
                pet, Duration.ofMinutes(30), Period.ofMonths(6)));
    }
}
