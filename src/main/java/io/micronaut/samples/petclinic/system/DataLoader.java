package io.micronaut.samples.petclinic.system;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.samples.petclinic.model.Clinic;
import io.micronaut.samples.petclinic.model.ClinicServiceOffering;
import io.micronaut.samples.petclinic.model.Owner;
import io.micronaut.samples.petclinic.model.Pet;
import io.micronaut.samples.petclinic.model.PetType;
import io.micronaut.samples.petclinic.model.Speciality;
import io.micronaut.samples.petclinic.model.Vet;
import io.micronaut.samples.petclinic.model.VetSpeciality;
import io.micronaut.samples.petclinic.model.Visit;
import io.micronaut.samples.petclinic.repository.ClinicRepository;
import io.micronaut.samples.petclinic.repository.ClinicServiceOfferingRepository;
import io.micronaut.samples.petclinic.repository.OwnerRepository;
import io.micronaut.samples.petclinic.repository.PetRepository;
import io.micronaut.samples.petclinic.repository.PetTypeRepository;
import io.micronaut.samples.petclinic.repository.SpecialityRepository;
import io.micronaut.samples.petclinic.repository.VetRepository;
import io.micronaut.samples.petclinic.repository.VetSpecialityRepository;
import io.micronaut.samples.petclinic.repository.VisitRepository;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Populates the database with sample data on application startup.
 */
@Singleton
@Requires(property = "petclinic.sample-data.enabled", value = "true", defaultValue = "true")
public class DataLoader implements ApplicationEventListener<StartupEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(DataLoader.class);

    private final VetRepository vetRepository;
    private final SpecialityRepository specialityRepository;
    private final PetTypeRepository petTypeRepository;
    private final OwnerRepository ownerRepository;
    private final PetRepository petRepository;
    private final VisitRepository visitRepository;
    private final VetSpecialityRepository vetSpecialityRepository;
    private final ClinicRepository clinicRepository;
    private final ClinicServiceOfferingRepository clinicServiceOfferingRepository;

    private static final Map<String, List<ClinicOfferingSeed>> CLINIC_OFFERINGS = Map.ofEntries(
            Map.entry("Downtown Madison Pet Clinic", List.of(
                    offering("WELLNESS_CHECK", "Wellness check", "Annual physical examination and preventive care review.", "45.00", 30),
                    offering("SENIOR_PET_SCREENING", "Senior pet screening", "A focused health screen for aging companions.", "72.00", 45),
                    offering("SAME_DAY_SICK_VISIT", "Same-day sick visit", "Fast evaluation for sudden illness and minor injuries.", "58.00", 30),
                    offering("DENTAL_SPARKLE", "Dental sparkle clean", "Professional cleaning with an oral health review.", "95.00", 60))),
            Map.entry("Capitol Square Pet Clinic", List.of(
                    offering("RABIES_EXPRESS", "Rabies express", "A quick vaccination appointment for busy city pets.", "30.00", 20),
                    offering("APARTMENT_PET_BEHAVIOR", "Apartment pet behavior consult", "Practical guidance for barking, anxiety, and shared walls.", "68.00", 45),
                    offering("PUPPY_SOCIAL_START", "Puppy social start", "Early-care guidance for confident city puppies.", "55.00", 30))),
            Map.entry("University Pet Clinic", List.of(
                    offering("STUDENT_PET_WELLNESS", "Student pet wellness", "Affordable preventive care for student households.", "38.00", 30),
                    offering("NEW_PET_ORIENTATION", "New pet orientation", "A first-visit roadmap for newly adopted companions.", "42.00", 30),
                    offering("EXAM_SEASON_CHECK", "Exam-season check", "A convenient health check before a busy semester.", "35.00", 20))),
            Map.entry("East Madison Pet Clinic", List.of(
                    offering("ALLERGY_SKIN_CONSULT", "Allergy and skin consult", "Investigate itching, hot spots, and seasonal allergies.", "78.00", 45),
                    offering("WEIGHT_WATCH_PLAN", "Healthy weight plan", "A measured nutrition and activity plan for long-term health.", "52.00", 30),
                    offering("MICROCHIP_ID_CLINIC", "Microchip and ID clinic", "Microchipping and personalized identification review.", "36.00", 20))),
            Map.entry("South Madison Pet Clinic", List.of(
                    offering("PUPPY_FIRST_YEAR", "Puppy first-year plan", "A staged wellness plan for growing puppies.", "125.00", 60),
                    offering("KITTEN_FIRST_YEAR", "Kitten first-year plan", "A preventive-care starter plan for kittens.", "115.00", 60),
                    offering("PARASITE_PREVENTION", "Parasite prevention consult", "A prevention plan for fleas, ticks, and intestinal parasites.", "48.00", 30))),
            Map.entry("West Madison Pet Clinic", List.of(
                    offering("CANINE_REHAB_ASSESSMENT", "Canine rehab assessment", "Mobility assessment for active or recovering dogs.", "88.00", 60),
                    offering("CALM_COMPANION_VISIT", "Calm companion visit", "A low-stress appointment for anxious pets.", "62.00", 45),
                    offering("SPORT_DOG_CONDITIONING", "Sport dog conditioning", "Fitness guidance for working and performance dogs.", "84.00", 45))),
            Map.entry("Middleton Pet Clinic", List.of(
                    offering("LAKE_COUNTRY_CHECK", "Lake country adventure check", "Pre-adventure exam for pets who love the outdoors.", "56.00", 30),
                    offering("SENIOR_MOBILITY_SCREEN", "Senior mobility screen", "Early support for stiffness and changing mobility.", "74.00", 45),
                    offering("GROOMING_READY_EXAM", "Grooming-ready exam", "Health check before a major grooming appointment.", "44.00", 30))),
            Map.entry("Fitchburg Pet Clinic", List.of(
                    offering("TRAIL_READY_CHECK", "Trail-ready pet check", "Preventive visit for pets joining hikes and long walks.", "58.00", 30),
                    offering("TICK_BORNE_SCREEN", "Tick-borne disease screen", "Seasonal screening and prevention planning.", "66.00", 30),
                    offering("RESCUE_INTRO_CHECK", "Rescue introduction check", "A thorough first exam for newly rescued pets.", "70.00", 45))),
            Map.entry("Monona Pet Clinic", List.of(
                    offering("LAKE_SIDE_WATER_SAFETY", "Lakeside water safety", "A seasonal check for swimming and boating companions.", "51.00", 30),
                    offering("CAT_COMFORT_EXAM", "Cat comfort exam", "A quiet, feline-friendly examination experience.", "49.00", 30),
                    offering("FELINE_DENTAL_PLAN", "Feline dental plan", "Targeted dental assessment and home-care guidance.", "82.00", 45))),
            Map.entry("McFarland Pet Clinic", List.of(
                    offering("RURAL_COMPANION_CHECK", "Rural companion check", "Wellness care for pets with more room to roam.", "53.00", 30),
                    offering("BARN_CAT_WELLNESS", "Barn cat wellness", "Preventive care for working and semi-outdoor cats.", "47.00", 30),
                    offering("FIELD_DOG_FITNESS", "Field dog fitness", "Conditioning and injury prevention for active dogs.", "79.00", 45))),
            Map.entry("Sun Prairie Pet Clinic", List.of(
                    offering("FAMILY_PUPPY_PLAN", "Family puppy plan", "A practical care plan for a puppy joining a busy family.", "61.00", 40),
                    offering("MULTI_PET_WELLNESS", "Multi-pet wellness visit", "Coordinated preventive care for households with several pets.", "48.00", 30),
                    offering("HOMECARE_NAIL_TRIM", "Home-care nail trim", "A quick trim plus techniques for easier future care.", "24.00", 20))),
            Map.entry("Waunakee Pet Clinic", List.of(
                    offering("COUNTRY_COMPANION_CHECK", "Country companion check", "A complete wellness visit for country-living pets.", "54.00", 30),
                    offering("GERIATRIC_CARE_PLAN", "Geriatric care plan", "Personalized monitoring for senior companions.", "86.00", 60),
                    offering("RABBIT_SMALL_PET_EXAM", "Rabbit and small-pet exam", "Specialized preventive care for small companion animals.", "63.00", 40))),
            Map.entry("Verona Pet Clinic", List.of(
                    offering("HIKER_PET_PREP", "Hiker pet preparation", "Health and prevention planning for trail companions.", "59.00", 35),
                    offering("ALLERGY_RELIEF_CONSULT", "Allergy relief consult", "A seasonal plan for itchy pets and sensitive skin.", "76.00", 45),
                    offering("ACTIVE_DOG_RECOVERY", "Active dog recovery", "Recovery guidance after intense activity or minor strain.", "81.00", 45))),
            Map.entry("Stoughton Pet Clinic", List.of(
                    offering("SMALL_TOWN_WELLNESS", "Small-town wellness", "Friendly, thorough preventive care for every life stage.", "43.00", 30),
                    offering("FAMILY_VACCINE_BUNDLE", "Family vaccine bundle", "Streamlined vaccination visits for multi-pet families.", "92.00", 45),
                    offering("BEHAVIOR_STARTER", "Behavior starter consult", "A first step for common behavior concerns at home.", "64.00", 45))),
            Map.entry("Oregon Pet Clinic", List.of(
                    offering("OUTDOOR_PET_FIRST_AID", "Outdoor pet first aid", "Practical first-aid planning for outdoor adventures.", "57.00", 35),
                    offering("FELINE_DENTAL_SCREEN", "Feline dental screen", "Early detection and prevention for cat dental disease.", "69.00", 40),
                    offering("PARASITE_CONTROL_PLAN", "Parasite control plan", "Seasonal parasite protection tailored to outdoor exposure.", "50.00", 30))),
            Map.entry("DeForest Pet Clinic", List.of(
                    offering("PUPPY_KITTEN_START", "Puppy and kitten start", "A combined starter visit for young household pets.", "58.00", 35),
                    offering("SENIOR_BLOODWORK_REVIEW", "Senior bloodwork review", "A focused review of screening results and next steps.", "91.00", 45),
                    offering("QUICK_VACCINE_VISIT", "Quick vaccine visit", "An efficient appointment for routine boosters.", "31.00", 20))),
            Map.entry("Mount Horeb Pet Clinic", List.of(
                    offering("COUNTRY_TRAIL_CHECK", "Country trail check", "A preventive exam for pets exploring rural trails.", "55.00", 30),
                    offering("FARM_CAT_WELLNESS", "Farm cat wellness", "Practical care for cats living and working around the farm.", "46.00", 30),
                    offering("HERDING_DOG_ASSESSMENT", "Herding dog assessment", "Joint, muscle, and conditioning review for working dogs.", "83.00", 50))),
            Map.entry("Portage Pet Clinic", List.of(
                    offering("TRAVEL_HEALTH_CERT", "Travel health certificate", "Documentation and exam support for pet travel.", "73.00", 40),
                    offering("WATER_DOG_CHECK", "Water dog check", "Seasonal health review for swimming companions.", "52.00", 30),
                    offering("FISHING_CAMP_PET_CHECK", "Fishing-camp pet check", "A practical check before a weekend away outdoors.", "49.00", 30))),
            Map.entry("Janesville Pet Clinic", List.of(
                    offering("COMPREHENSIVE_WELLNESS", "Comprehensive wellness", "A full preventive visit with lifestyle planning.", "60.00", 40),
                    offering("RESCUE_PET_INTAKE", "Rescue pet intake", "A welcoming first examination for adopted pets.", "71.00", 45),
                    offering("SENIOR_PET_PLAN", "Senior pet plan", "A practical monitoring plan for aging pets.", "84.00", 50))),
            Map.entry("Milwaukee Pet Clinic", List.of(
                    offering("CITY_PET_WELLNESS", "City pet wellness", "Preventive care designed for urban companion animals.", "57.00", 30),
                    offering("CONDO_BEHAVIOR_CONSULT", "Condo behavior consult", "Behavior support for compact city living.", "72.00", 45),
                    offering("EXPRESS_VACCINATION", "Express vaccination", "A focused booster appointment for busy schedules.", "34.00", 20),
                    offering("URBAN_DENTAL_CARE", "Urban dental care", "Professional cleaning and city-pet dental advice.", "98.00", 60)))
    );

    /**
     * Creates the data loader with the repositories used to seed sample data.
     *
     * @param vetRepository repository for vets
     * @param specialityRepository repository for specialities
     * @param petTypeRepository repository for pet types
     * @param ownerRepository repository for owners
     * @param petRepository repository for pets
     * @param visitRepository repository for visits
     * @param vetSpecialityRepository repository for vet-speciality join rows
     * @param clinicRepository repository for clinic locations
     * @param clinicServiceOfferingRepository repository for clinic service offerings
     */
    public DataLoader(VetRepository vetRepository,
                      SpecialityRepository specialityRepository,
                      PetTypeRepository petTypeRepository,
                      OwnerRepository ownerRepository,
                      PetRepository petRepository,
                      VisitRepository visitRepository,
                      VetSpecialityRepository vetSpecialityRepository,
                      ClinicRepository clinicRepository,
                      ClinicServiceOfferingRepository clinicServiceOfferingRepository) {
        this.vetRepository = vetRepository;
        this.specialityRepository = specialityRepository;
        this.petTypeRepository = petTypeRepository;
        this.ownerRepository = ownerRepository;
        this.petRepository = petRepository;
        this.visitRepository = visitRepository;
        this.vetSpecialityRepository = vetSpecialityRepository;
        this.clinicRepository = clinicRepository;
        this.clinicServiceOfferingRepository = clinicServiceOfferingRepository;
    }

    /**
     * Seeds the database when the application starts.
     *
     * @param event the startup event
     */
    @Override
    @Transactional
    public void onApplicationEvent(StartupEvent event) {
        LOG.info("Loading sample data...");
        loadData();
        LOG.info("Sample data loaded successfully.");
    }

    private void loadData() {
        // Create specialities
        Speciality radiology = createSpeciality("radiology");
        Speciality surgery = createSpeciality("surgery");
        Speciality dentistry = createSpeciality("dentistry");

        // Create vets
        Vet james = createVet("James", "Carter");
        Vet helen = createVet("Helen", "Leary", radiology);
        Vet linda = createVet("Linda", "Douglas", surgery, dentistry);
        Vet rafael = createVet("Rafael", "Ortega", surgery);
        Vet henry = createVet("Henry", "Stevens", radiology);
        Vet sharon = createVet("Sharon", "Jenkins");

        // Create pet types
        PetType cat = createPetType("cat");
        PetType dog = createPetType("dog");
        PetType lizard = createPetType("lizard");
        PetType snake = createPetType("snake");
        PetType bird = createPetType("bird");
        PetType hamster = createPetType("hamster");

        // Create owners and their pets
        Owner george = createOwner("George", "Franklin", "110 W. Liberty St.", "Madison", "6085551023");
        Pet leo = createPet("Leo", LocalDate.of(2010, 9, 7), cat, george);

        Owner betty = createOwner("Betty", "Davis", "638 Cardinal Ave.", "Sun Prairie", "6085551749");
        Pet basil = createPet("Basil", LocalDate.of(2012, 8, 6), hamster, betty);

        Owner eduardo = createOwner("Eduardo", "Rodriquez", "2693 Commerce St.", "McFarland", "6085558763");
        Pet jewel = createPet("Jewel", LocalDate.of(2010, 3, 7), dog, eduardo);
        Pet rosy = createPet("Rosy", LocalDate.of(2011, 4, 17), dog, eduardo);

        Owner harold = createOwner("Harold", "Davis", "563 Friendly St.", "Windsor", "6085553198");
        Pet iggy = createPet("Iggy", LocalDate.of(2010, 11, 30), lizard, harold);

        Owner peter = createOwner("Peter", "McTavish", "2387 S. Fair Way", "Madison", "6085552765");
        Pet george2 = createPet("George", LocalDate.of(2010, 1, 20), snake, peter);

        Owner jean = createOwner("Jean", "Coleman", "105 N. Lake St.", "Monona", "6085552654");
        Pet samantha = createPet("Samantha", LocalDate.of(2012, 9, 4), cat, jean);
        Pet max = createPet("Max", LocalDate.of(2012, 9, 4), cat, jean);

        Owner jeff = createOwner("Jeff", "Black", "1450 Oak Blvd.", "Monona", "6085555387");
        Pet lucky = createPet("Lucky", LocalDate.of(2011, 8, 6), bird, jeff);

        Owner maria = createOwner("Maria", "Escobito", "345 Maple St.", "Madison", "6085557683");
        Pet mulligan = createPet("Mulligan", LocalDate.of(2007, 2, 24), dog, maria);

        Owner david = createOwner("David", "Schroeder", "2749 Blackhawk Trail", "Madison", "6085559435");
        Pet freddy = createPet("Freddy", LocalDate.of(2010, 3, 9), bird, david);

        Owner carlos = createOwner("Carlos", "Estaban", "2335 Independence La.", "Waunakee", "6085555487");
        Pet lucky2 = createPet("Lucky", LocalDate.of(2010, 6, 24), dog, carlos);
        Pet sly = createPet("Sly", LocalDate.of(2012, 6, 8), cat, carlos);

        // Create some visits
        createVisit(samantha, LocalDate.of(2026, 1, 5), "rabies shot", Duration.ofMinutes(30), Period.ofMonths(6));
        createVisit(samantha, LocalDate.of(2026, 1, 15), "neutered", Duration.ofHours(1), Period.ofMonths(12));
        createVisit(max, LocalDate.of(2026, 1, 9), "rabies shot", Duration.ofMinutes(15), Period.ofMonths(3));
        createVisit(max, LocalDate.of(2026, 1, 12), "neutered", Duration.ofHours(2), Period.ofMonths(24));

        // Additional visits cover different pets, dates, durations, and follow-up thresholds.
        createVisit(leo, LocalDate.of(2017, 2, 8), "annual wellness exam", Duration.ofMinutes(15), Period.ofMonths(1));
        createVisit(basil, LocalDate.of(2018, 2, 14), "vaccination", Duration.ofMinutes(45), Period.ofMonths(6));
        createVisit(jewel, LocalDate.of(2019, 3, 1), "dental cleaning", Duration.ofMinutes(75), Period.ofMonths(9));
        createVisit(rosy, LocalDate.of(2020, 3, 15), "ear infection", Duration.ofMinutes(90), Period.ofMonths(18));
        createVisit(iggy, LocalDate.of(2021, 4, 2), "skin check", Duration.ofMinutes(120), Period.ofMonths(12));
        createVisit(george2, LocalDate.of(2022, 5, 10), "routine checkup", Duration.ofMinutes(150), Period.ofMonths(24));
        createVisit(lucky, LocalDate.of(2023, 6, 18), "wing injury", Duration.ofMinutes(20), Period.ofMonths(3));
        createVisit(mulligan, LocalDate.of(2024, 7, 22), "arthritis review", Duration.ofMinutes(180), Period.ofMonths(36));
        createVisit(freddy, LocalDate.of(2025, 8, 9), "follow-up examination", Duration.ofMinutes(30), Period.ofMonths(6));
        createVisit(lucky2, LocalDate.of(2026, 1, 12), "vaccination", Duration.ofMinutes(60), Period.ofMonths(12));
        createVisit(sly, LocalDate.of(2026, 6, 21), "dental cleaning", Duration.ofMinutes(210), Period.ofMonths(18));
        createVisit(max, LocalDate.of(2026, 8, 25), "allergy consultation", Duration.ofMinutes(45), Period.ofMonths(6));

        loadClinicData();
    }

    private Speciality createSpeciality(String name) {
        Speciality speciality = new Speciality(name);
        return specialityRepository.save(speciality);
    }

    private Vet createVet(String firstName, String lastName, Speciality... specialities) {
        Vet vet = new Vet(firstName, lastName).withSpecialities(Set.copyOf(Arrays.asList(specialities)));
        Vet saved = vetRepository.save(vet);
        for (Speciality speciality : specialities) {
            vetSpecialityRepository.save(new VetSpeciality(saved.id(), speciality.id()));
        }
        return saved;
    }

    private PetType createPetType(String name) {
        PetType petType = new PetType(name);
        return petTypeRepository.save(petType);
    }

    private Owner createOwner(String firstName, String lastName, String address, String city, String telephone) {
        Owner owner = new Owner(firstName, lastName, address, city, telephone);
        return ownerRepository.save(owner);
    }

    private Pet createPet(String name, LocalDate birthDate, PetType type, Owner owner) {
        Pet pet = new Pet(name, birthDate, type, owner);
        return petRepository.save(pet);
    }

    private Visit createVisit(Pet pet, LocalDate date, String description, Duration duration, Period period) {
        Visit visit = new Visit(date, description, pet, duration, period);
        return visitRepository.save(visit);
    }

    private void loadClinicData() {
        List<Clinic> clinics = new ArrayList<>();
        clinics.add(new Clinic("Downtown Madison Pet Clinic", "15 E Main St.", "Madison", -89.3838, 43.0748));
        clinics.add(new Clinic("Capitol Square Pet Clinic", "2 S Carroll St.", "Madison", -89.3844, 43.0742));
        clinics.add(new Clinic("University Pet Clinic", "750 University Ave.", "Madison", -89.3985, 43.0739));
        clinics.add(new Clinic("East Madison Pet Clinic", "2210 E Washington Ave.", "Madison", -89.3545, 43.1020));
        clinics.add(new Clinic("South Madison Pet Clinic", "2300 S Park St.", "Madison", -89.3952, 43.0384));
        clinics.add(new Clinic("West Madison Pet Clinic", "701 N High Point Rd.", "Madison", -89.5186, 43.0753));
        clinics.add(new Clinic("Middleton Pet Clinic", "7428 University Ave.", "Middleton", -89.5137, 43.0972));
        clinics.add(new Clinic("Fitchburg Pet Clinic", "5515 Nobel Dr.", "Fitchburg", -89.4233, 43.0026));
        clinics.add(new Clinic("Monona Pet Clinic", "6000 Monona Dr.", "Monona", -89.3240, 43.0622));
        clinics.add(new Clinic("McFarland Pet Clinic", "4910 Terminal Dr.", "McFarland", -89.2887, 43.0125));
        clinics.add(new Clinic("Sun Prairie Pet Clinic", "300 E Main St.", "Sun Prairie", -89.2137, 43.1836));
        clinics.add(new Clinic("Waunakee Pet Clinic", "100 W Main St.", "Waunakee", -89.4557, 43.1919));
        clinics.add(new Clinic("Verona Pet Clinic", "101 W Verona Ave.", "Verona", -89.5332, 42.9908));
        clinics.add(new Clinic("Stoughton Pet Clinic", "207 S Forrest St.", "Stoughton", -89.2179, 42.9169));
        clinics.add(new Clinic("Oregon Pet Clinic", "117 Spring St.", "Oregon", -89.3848, 42.9261));
        clinics.add(new Clinic("DeForest Pet Clinic", "120 S Stevenson St.", "DeForest", -89.3440, 43.2478));
        clinics.add(new Clinic("Mount Horeb Pet Clinic", "138 E Main St.", "Mount Horeb", -89.7385, 43.0086));
        clinics.add(new Clinic("Portage Pet Clinic", "117 W Cook St.", "Portage", -89.4626, 43.5391));
        clinics.add(new Clinic("Janesville Pet Clinic", "20 S Main St.", "Janesville", -89.0187, 42.6828));
        clinics.add(new Clinic("Milwaukee Pet Clinic", "200 E Wells St.", "Milwaukee", -87.9065, 43.0410));
        for (Clinic clinic : clinicRepository.saveAll(clinics)) {
            seedClinicOfferings(clinic);
        }
    }

    private void seedClinicOfferings(Clinic clinic) {
        List<ClinicOfferingSeed> offerings = CLINIC_OFFERINGS.get(clinic.name());
        if (offerings == null) {
            throw new IllegalStateException("No service catalog configured for clinic: " + clinic.name());
        }
        for (ClinicOfferingSeed offering : offerings) {
            clinicServiceOfferingRepository.upsert(new ClinicServiceOffering(
                    clinic,
                    offering.serviceCode(),
                    offering.name(),
                    offering.description(),
                    offering.price(),
                    offering.durationMinutes()));
        }
    }

    private static ClinicOfferingSeed offering(String serviceCode,
                                               String name,
                                               String description,
                                               String price,
                                               int durationMinutes) {
        return new ClinicOfferingSeed(serviceCode, name, description, new BigDecimal(price), durationMinutes);
    }

    private record ClinicOfferingSeed(String serviceCode,
                                      String name,
                                      String description,
                                      BigDecimal price,
                                      Integer durationMinutes) {
    }
}
