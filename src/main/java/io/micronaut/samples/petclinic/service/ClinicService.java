package io.micronaut.samples.petclinic.service;

import io.micronaut.cache.annotation.CacheInvalidate;
import io.micronaut.cache.annotation.Cacheable;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.model.Sort;
import io.micronaut.data.model.geo.LineString;
import io.micronaut.data.model.geo.Point;
import io.micronaut.data.model.geo.Polygon;
import io.micronaut.samples.petclinic.model.Clinic;
import io.micronaut.samples.petclinic.model.Owner;
import io.micronaut.samples.petclinic.model.Pet;
import io.micronaut.samples.petclinic.model.PetType;
import io.micronaut.samples.petclinic.model.Speciality;
import io.micronaut.samples.petclinic.model.Vet;
import io.micronaut.samples.petclinic.model.VetWithSpecialities;
import io.micronaut.samples.petclinic.model.Visit;
import io.micronaut.samples.petclinic.dto.VisitSearchCriteria;
import io.micronaut.samples.petclinic.repository.ClinicRepository;
import io.micronaut.samples.petclinic.repository.OwnerRepository;
import io.micronaut.samples.petclinic.repository.PetRepository;
import io.micronaut.samples.petclinic.repository.PetTypeRepository;
import io.micronaut.samples.petclinic.repository.SpecialityRepository;
import io.micronaut.samples.petclinic.repository.VetRepository;
import io.micronaut.samples.petclinic.repository.VetSpecialityRepository;
import io.micronaut.samples.petclinic.repository.VisitRepository;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

import java.time.Duration;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import io.micronaut.data.model.Sort;

/**
 * Service class providing business logic for the Pet Clinic application.
 * Acts as a facade over the repository layer.
 */
@Singleton
public class ClinicService {

    private static final double GEOMETRY_EPSILON = 0.0000000001;

    private final OwnerRepository ownerRepository;
    private final PetRepository petRepository;
    private final PetTypeRepository petTypeRepository;
    private final VisitRepository visitRepository;
    private final VetRepository vetRepository;
    private final SpecialityRepository specialityRepository;
    private final VetSpecialityRepository vetSpecialityRepository;
    private final ClinicRepository clinicRepository;

    /**
     * Creates the service facade with its repository dependencies.
     *
     * @param ownerRepository repository for owners
     * @param petRepository repository for pets
     * @param petTypeRepository repository for pet types
     * @param visitRepository repository for visits
     * @param vetRepository repository for vets
     * @param specialityRepository repository for specialities
     * @param vetSpecialityRepository repository for vet-speciality join rows
     * @param clinicRepository repository for clinic locations
     * @param visitIntervalRepository repository for Oracle interval queries
     */
    public ClinicService(OwnerRepository ownerRepository,
                         PetRepository petRepository,
                         PetTypeRepository petTypeRepository,
                         VisitRepository visitRepository,
                         VetRepository vetRepository,
                         SpecialityRepository specialityRepository,
                         VetSpecialityRepository vetSpecialityRepository,
                         ClinicRepository clinicRepository) {
        this.ownerRepository = ownerRepository;
        this.petRepository = petRepository;
        this.petTypeRepository = petTypeRepository;
        this.visitRepository = visitRepository;
        this.vetRepository = vetRepository;
        this.specialityRepository = specialityRepository;
        this.vetSpecialityRepository = vetSpecialityRepository;
        this.clinicRepository = clinicRepository;
    }

    // ========== Owner Operations ==========

    /**
     * Find an owner by ID.
     * @param id the owner ID
     * @return the owner, if found
     */
    public Optional<Owner> findOwnerById(Integer id) {
        return ownerRepository.findById(id);
    }

    /**
     * Find owners by last name (case-insensitive partial match).
     * @param lastName the last name to search for
     * @return collection of matching owners
     */
    public Collection<Owner> findOwnerByLastName(String lastName) {
        return ownerRepository.findByLastNameContainingIgnoreCase(lastName, Sort.of(Sort.Order.asc("lastName")));
    }

    /**
     * Find all owners.
     * @return collection of all owners
     */
    public Collection<Owner> findAllOwners() {
        return ownerRepository.findAll(Sort.of(Sort.Order.asc("lastName")));
    }

    /**
     * Save an owner (create or update).
     * @param owner the owner to save
     * @return the persisted owner returned by the repository
     */
    @Transactional
    public Owner saveOwner(Owner owner) {
        if (owner.isNew()) {
            return ownerRepository.save(owner);
        } else {
            return ownerRepository.update(owner);
        }
    }

    /**
     * Delete an owner by ID.
     * @param id the owner ID
     */
    @Transactional
    public void deleteOwner(Integer id) {
        ownerRepository.deleteById(id);
    }

    // ========== Pet Operations ==========

    /**
     * Find a pet by ID.
     * @param id the pet ID
     * @return the pet, if found
     */
    public Optional<Pet> findPetById(Integer id) {
        return petRepository.findById(id);
    }

    /**
     * Find all pets.
     * @return list of all pets
     */
    public List<Pet> findAllPets() {
        return petRepository.findAll();
    }

    /**
     * Save a pet (create or update).
     * @param pet the pet to save
     * @return the persisted pet returned by the repository
     */
    @Transactional
    public Pet savePet(Pet pet) {
        if (pet.isNew()) {
            return petRepository.save(pet);
        } else {
            return petRepository.update(pet);
        }
    }

    /**
     * Delete a pet by ID.
     * @param id the pet ID
     */
    @Transactional
    public void deletePet(Integer id) {
        petRepository.deleteById(id);
    }

    // ========== Pet Type Operations ==========

    /**
     * Find all pet types.
     * @return list of all pet types
     */
    public List<PetType> findPetTypes() {
        return petTypeRepository.findAllOrderByName();
    }

    /**
     * Find a pet type by ID.
     * @param id the pet type ID
     * @return the pet type, if found
     */
    public Optional<PetType> findPetTypeById(Integer id) {
        return petTypeRepository.findById(id);
    }

    // ========== Visit Operations ==========

    /**
     * Find a visit by ID.
     * @param id the visit ID
     * @return the visit, if found
     */
    public Optional<Visit> findVisitById(Integer id) {
        return visitRepository.findById(id);
    }

    /**
     * Find all visits for a pet.
     * @param petId the pet ID
     * @return collection of visits
     */
    public Collection<Visit> findVisitsByPetId(Integer petId) {
        return visitRepository.findByPetId(petId);
    }

    /**
     * Searches visits using the supplied optional filters.
     *
     * @param criteria the search filters
     * @return matching visits ordered from newest to oldest
     */
    public List<Visit> searchVisits(VisitSearchCriteria criteria) {
        return visitRepository.findByDateBetweenAndDurationLessThanEqualsAndPeriodLessThanEquals(
                criteria.fromDate(),
                criteria.toDate(),
                Duration.ofMinutes(criteria.maxDurationMinutes()),
                Period.ofMonths(criteria.maxFollowUpMonths())
        );
    }

    /**
     * Save a visit (create or update).
     * @param visit the visit to save
     * @return the persisted visit returned by the repository
     */
    @Transactional
    public Visit saveVisit(Visit visit) {
        if (visit.isNew()) {
            return visitRepository.save(visit);
        } else {
            return visitRepository.update(visit);
        }
    }

    /**
     * Delete a visit by ID.
     * @param id the visit ID
     */
    @Transactional
    public void deleteVisit(Integer id) {
        visitRepository.deleteById(id);
    }

    // ========== Vet Operations ==========

    /**
     * Find all veterinarians.
     * Cached for performance.
     * @return collection of all vets
     */
    @Cacheable("vets")
    public Collection<Vet> findAllVets() {
        return vetRepository.findAllWithSpecialities().stream()
                .map(ClinicService::toVet)
                .collect(Collectors.toList());
    }

    private static Vet toVet(VetWithSpecialities vet) {
        return new Vet(vet.id(), vet.firstName(), vet.lastName(), parseSpecialities(vet.specialityRows()));
    }

    private static Set<Speciality> parseSpecialities(String specialityRows) {
        if (specialityRows == null || specialityRows.isBlank()) {
            return Set.of();
        }
        Set<Speciality> specialities = new LinkedHashSet<>();
        for (String specialityRow : specialityRows.split("\\|")) {
            String[] parts = specialityRow.split(":", 2);
            if (parts.length == 2) {
                specialities.add(new Speciality(Integer.valueOf(parts[0]), parts[1]));
            }
        }
        return specialities;
    }

    /**
     * Find a vet by ID.
     * @param id the vet ID
     * @return the vet, if found
     */
    public Optional<Vet> findVetById(Integer id) {
        return vetRepository.findById(id);
    }

    /**
     * Save a vet (create or update) and invalidate the cached vet list.
     *
     * @param vet the vet to save
     * @return the persisted vet returned by the repository
     */
    @Transactional
    @CacheInvalidate(value = "vets", all = true)
    public Vet saveVet(Vet vet) {
        if (vet.isNew()) {
            return vetRepository.save(vet);
        } else {
            return vetRepository.update(vet);
        }
    }

    // ========== Speciality Operations ==========

    /**
     * Find all specialities.
     * @return list of all specialities
     */
    public List<Speciality> findAllSpecialities() {
        return specialityRepository.findAllOrderByName();
    }

    /**
     * Find a speciality by ID.
     * @param id the speciality ID
     * @return the speciality, if found
     */
    public Optional<Speciality> findSpecialityById(Integer id) {
        return specialityRepository.findById(id);
    }

    /**
     * Finds physical clinic branches near a WGS 84 coordinate.
     *
     * @param longitude the longitude coordinate
     * @param latitude the latitude coordinate
     * @param radiusMeters the search radius in meters
     * @return nearby clinics
     */
    public List<Clinic> findClinicsNear(double longitude, double latitude, double radiusMeters) {
        return clinicRepository.findByLocationNear(new Point(longitude, latitude), radiusMeters);
    }

    /**
     * Finds clinics whose location falls within the supplied bounding box.
     *
     * @param minLongitude western bound
     * @param minLatitude southern bound
     * @param maxLongitude eastern bound
     * @param maxLatitude northern bound
     * @return clinics inside the polygon
     */
    public List<Clinic> findClinicsWithinBounds(double minLongitude,
                                                double minLatitude,
                                                double maxLongitude,
                                                double maxLatitude) {
        return clinicRepository.findByLocationGeoWithin(toBoundingBox(minLongitude, minLatitude, maxLongitude, maxLatitude));
    }

    /**
     * Finds clinics whose location falls within the supplied polygon.
     *
     * @param coordinates polygon shell coordinates
     * @return clinics inside the polygon
     */
    public List<Clinic> findClinicsWithinPolygon(List<Point> coordinates) {
        return clinicRepository.findByLocationGeoWithin(toPolygon(coordinates));
    }

    /**
     * Finds clinics whose service area intersects the supplied line.
     *
     * @param coordinates line coordinates
     * @return clinics intersecting the line
     */
    public List<Clinic> findClinicsIntersectingLine(List<Point> coordinates) {
        return clinicRepository.findByServiceAreaGeoIntersects(toLineString(coordinates));
    }

    private static Polygon toBoundingBox(double minLongitude,
                                         double minLatitude,
                                         double maxLongitude,
                                         double maxLatitude) {
        return new Polygon(List.of(toBoundingBoxShell(minLongitude, minLatitude, maxLongitude, maxLatitude)));
    }

    private static LineString toBoundingBoxShell(double minLongitude,
                                                 double minLatitude,
                                                 double maxLongitude,
                                                 double maxLatitude) {
        return new LineString(List.of(
                new Point(minLongitude, minLatitude),
                new Point(minLongitude, maxLatitude),
                new Point(maxLongitude, maxLatitude),
                new Point(maxLongitude, minLatitude),
                new Point(minLongitude, minLatitude)
        ));
    }

    private static Polygon toPolygon(List<Point> coordinates) {
        LineString shell = toClosedLineString(coordinates);
        validateSimplePolygon(shell);
        return new Polygon(List.of(shell));
    }

    private static LineString toLineString(List<Point> coordinates) {
        if (coordinates == null || coordinates.size() < 2) {
            throw new IllegalArgumentException("A line search requires at least two coordinates");
        }
        return new LineString(new ArrayList<>(coordinates));
    }

    private static LineString toClosedLineString(List<Point> coordinates) {
        if (coordinates == null || coordinates.size() < 3) {
            throw new IllegalArgumentException("A polygon search requires at least three coordinates");
        }
        List<Point> shell = new ArrayList<>(coordinates);
        Point first = shell.getFirst();
        Point last = shell.getLast();
        if (first.x() != last.x() || first.y() != last.y()) {
            shell.add(first);
        }
        return new LineString(shell);
    }

    /**
     * Ensures a polygon shell is simple before it is passed to spatial predicates.
     *
     * @param shell closed polygon boundary
     */
    private static void validateSimplePolygon(LineString shell) {
        if (hasSelfIntersection(shell.points())) {
            throw new IllegalArgumentException("A polygon boundary cannot cross itself");
        }
    }

    /**
     * Checks whether any non-adjacent polygon boundary segments cross each other.
     *
     * @param shell closed polygon boundary points
     * @return {@code true} when the shell crosses itself
     */
    private static boolean hasSelfIntersection(List<Point> shell) {
        List<Point> points = new ArrayList<>(shell);
        Point first = points.getFirst();
        Point last = points.getLast();
        if (first.x() == last.x() && first.y() == last.y()) {
            points.removeLast();
        }
        if (points.size() < 4) {
            return false;
        }
        for (int i = 0; i < points.size(); i++) {
            Point firstStart = points.get(i);
            Point firstEnd = points.get((i + 1) % points.size());
            for (int j = i + 1; j < points.size(); j++) {
                if (areAdjacentSegments(i, j, points.size())) {
                    continue;
                }
                Point secondStart = points.get(j);
                Point secondEnd = points.get((j + 1) % points.size());
                if (segmentsIntersect(firstStart, firstEnd, secondStart, secondEnd)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks whether two polygon edges share a vertex and therefore may touch.
     *
     * @param firstIndex first segment index
     * @param secondIndex second segment index
     * @param segmentCount total segment count
     * @return {@code true} when the segments are neighbors
     */
    private static boolean areAdjacentSegments(int firstIndex, int secondIndex, int segmentCount) {
        return Math.abs(firstIndex - secondIndex) == 1
                || firstIndex == 0 && secondIndex == segmentCount - 1;
    }

    /**
     * Checks whether two line segments intersect, including collinear overlap.
     *
     * @param firstStart first segment start
     * @param firstEnd first segment end
     * @param secondStart second segment start
     * @param secondEnd second segment end
     * @return {@code true} when the segments intersect
     */
    private static boolean segmentsIntersect(Point firstStart,
                                             Point firstEnd,
                                             Point secondStart,
                                             Point secondEnd) {
        double d1 = direction(firstStart, firstEnd, secondStart);
        double d2 = direction(firstStart, firstEnd, secondEnd);
        double d3 = direction(secondStart, secondEnd, firstStart);
        double d4 = direction(secondStart, secondEnd, firstEnd);
        if (((d1 > GEOMETRY_EPSILON && d2 < -GEOMETRY_EPSILON)
                || (d1 < -GEOMETRY_EPSILON && d2 > GEOMETRY_EPSILON))
                && ((d3 > GEOMETRY_EPSILON && d4 < -GEOMETRY_EPSILON)
                || (d3 < -GEOMETRY_EPSILON && d4 > GEOMETRY_EPSILON))) {
            return true;
        }
        return isZero(d1) && isPointOnSegment(secondStart, firstStart, firstEnd)
                || isZero(d2) && isPointOnSegment(secondEnd, firstStart, firstEnd)
                || isZero(d3) && isPointOnSegment(firstStart, secondStart, secondEnd)
                || isZero(d4) && isPointOnSegment(firstEnd, secondStart, secondEnd);
    }

    /**
     * Computes the orientation of a point relative to a directed line segment.
     *
     * @param start segment start
     * @param end segment end
     * @param point point to test
     * @return positive, negative, or zero depending on the side of the segment
     */
    private static double direction(Point start, Point end, Point point) {
        return (end.x() - start.x()) * (point.y() - start.y())
                - (end.y() - start.y()) * (point.x() - start.x());
    }

    /**
     * Checks whether a collinear point lies within a segment's bounding box.
     *
     * @param point point to test
     * @param start segment start
     * @param end segment end
     * @return {@code true} when the point lies on the segment
     */
    private static boolean isPointOnSegment(Point point, Point start, Point end) {
        return point.x() >= Math.min(start.x(), end.x()) - GEOMETRY_EPSILON
                && point.x() <= Math.max(start.x(), end.x()) + GEOMETRY_EPSILON
                && point.y() >= Math.min(start.y(), end.y()) - GEOMETRY_EPSILON
                && point.y() <= Math.max(start.y(), end.y()) + GEOMETRY_EPSILON;
    }

    /**
     * Compares floating-point geometry values using a small tolerance.
     *
     * @param value value to test
     * @return {@code true} when the value is close enough to zero
     */
    private static boolean isZero(double value) {
        return Math.abs(value) <= GEOMETRY_EPSILON;
    }

}
