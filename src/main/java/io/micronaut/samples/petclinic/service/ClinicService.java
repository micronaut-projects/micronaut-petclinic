package io.micronaut.samples.petclinic.service;

import io.micronaut.data.model.Sort;
import io.micronaut.data.model.geo.LineString;
import io.micronaut.data.model.geo.Point;
import io.micronaut.data.model.geo.Polygon;
import io.micronaut.samples.petclinic.exception.NotFoundEntityException;
import io.micronaut.samples.petclinic.model.Clinic;
import io.micronaut.samples.petclinic.model.Owner;
import io.micronaut.samples.petclinic.model.Pet;
import io.micronaut.samples.petclinic.model.PetType;
import io.micronaut.samples.petclinic.model.Visit;
import io.micronaut.samples.petclinic.dto.VisitSearchCriteria;
import io.micronaut.samples.petclinic.repository.ClinicRepository;
import io.micronaut.samples.petclinic.repository.OwnerRepository;
import io.micronaut.samples.petclinic.repository.PetRepository;
import io.micronaut.samples.petclinic.repository.PetTypeRepository;
import io.micronaut.samples.petclinic.repository.VisitRepository;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final ClinicRepository clinicRepository;
    private final OwnerLoyaltyService ownerLoyaltyService;

    /**
     * Creates the service facade with its repository and loyalty dependencies.
     *
     * @param ownerRepository repository for owners
     * @param petRepository repository for pets
     * @param petTypeRepository repository for pet types
     * @param visitRepository repository for visits
     * @param clinicRepository repository for clinic locations
     * @param ownerLoyaltyService profile-specific owner and visit loyalty hooks
     */
    public ClinicService(OwnerRepository ownerRepository,
                         PetRepository petRepository,
                         PetTypeRepository petTypeRepository,
                         VisitRepository visitRepository,
                         ClinicRepository clinicRepository,
                         OwnerLoyaltyService ownerLoyaltyService) {
        this.ownerRepository = ownerRepository;
        this.petRepository = petRepository;
        this.petTypeRepository = petTypeRepository;
        this.visitRepository = visitRepository;
        this.clinicRepository = clinicRepository;
        this.ownerLoyaltyService = ownerLoyaltyService;
    }

    // ========== Owner Operations ==========

    /**
     * Find an owner by ID.
     * @param id the owner ID
     * @return the persisted owner
     * @throws NotFoundEntityException if the owner does not exist
     */
    public Owner findOwnerById(Integer id) throws NotFoundEntityException {
        return ownerRepository.findById(id).orElseThrow(NotFoundEntityException::new);
    }

    /**
     * Find owners by last name (case-insensitive partial match).
     * @param lastName the last name to search for
     * @return collection of matching owners
     */
    public Collection<Owner> findOwnerByLastName(String lastName) {
        return ownerRepository.findByLastNameContainsIgnoreCase(lastName, Sort.of(Sort.Order.asc("lastName")));
    }

    /**
     * Find all owners.
     * @return collection of all owners
     */
    public Collection<Owner> findAllOwners() {
        return ownerRepository.findAll(Sort.of(Sort.Order.asc("lastName")));
    }

    /**
     * Saves an owner, creating its loyalty account in the same transaction when enabled.
     * Updating an owner leaves the separate loyalty balance unchanged.
     * @param owner the owner to save
     * @return the persisted owner returned by the repository
     */
    @Transactional
    public Owner saveOwner(Owner owner) {
        if (owner.isNew()) {
            Owner saved = ownerRepository.save(owner);
            ownerLoyaltyService.createAccount(saved);
            return saved;
        } else {
            return ownerRepository.update(owner);
        }
    }

    /**
     * Deletes an owner and its loyalty data in the same transaction.
     * @param owner the persisted owner to delete
     */
    @Transactional
    public void deleteOwner(Owner owner) {
        ownerLoyaltyService.deleteOwnerData(owner);
        ownerRepository.delete(owner);
    }

    // ========== Pet Operations ==========

    /**
     * Find a pet by ID.
     * @param id the pet ID
     * @return the persisted pet
     * @throws NotFoundEntityException if the pet does not exist
     */
    public @NonNull Pet findPetById(Integer id) throws NotFoundEntityException {
        return petRepository.findById(id).orElseThrow(NotFoundEntityException::new);
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
     * Saves a visit, awarding loyalty points only for a newly inserted visit.
     * The visit and credit share one transaction; editing a visit earns no further points.
     * @param visit the visit to save
     * @return the persisted visit returned by the repository
     */
    @Transactional
    public Visit saveVisit(Visit visit) {
        if (!visit.isNew()) {
            return visitRepository.update(visit);
        }

        Visit saved = visitRepository.save(visit);
        ownerLoyaltyService.awardVisit(saved.pet().getOwner());
        return saved;
    }

    /**
     * Deletes a visit by ID without reversing any previously earned loyalty points.
     * @param id the visit ID
     */
    @Transactional
    public void deleteVisit(Integer id) {
        visitRepository.deleteById(id);
    }

    /**
     * Finds clinic by id.
     * @param id clinic id.
     * @return clinic if found.
     * @throws NotFoundEntityException if the clinic does not exist
     */
    public Clinic findClinicById(Integer id) throws NotFoundEntityException {
        return clinicRepository.findById(id).orElseThrow(NotFoundEntityException::new);
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
        return findClinicsNear(longitude, latitude, radiusMeters, null, null);
    }

    /**
     * Finds physical clinic branches near a coordinate and applies optional
     * Boolean availability filters.
     *
     * @param longitude the longitude coordinate
     * @param latitude the latitude coordinate
     * @param radiusMeters the search radius in meters
     * @param acceptingNewPatients optional new-patient availability filter
     * @param emergencyService optional emergency-service filter
     * @return nearby clinics matching the requested filters
     */
    public List<Clinic> findClinicsNear(double longitude,
                                        double latitude,
                                        double radiusMeters,
                                        Boolean acceptingNewPatients,
                                        Boolean emergencyService) {
        List<Clinic> clinics = clinicRepository.findByLocationNear(new Point(longitude, latitude), radiusMeters);
        return filterByAvailability(clinics, acceptingNewPatients, emergencyService);
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
        return findClinicsWithinBounds(minLongitude, minLatitude, maxLongitude, maxLatitude, null, null);
    }

    /**
     * Finds clinics inside a bounding box and applies optional Boolean filters.
     *
     * @param minLongitude western bound
     * @param minLatitude southern bound
     * @param maxLongitude eastern bound
     * @param maxLatitude northern bound
     * @param acceptingNewPatients optional new-patient availability filter
     * @param emergencyService optional emergency-service filter
     * @return clinics inside the box matching the requested filters
     */
    public List<Clinic> findClinicsWithinBounds(double minLongitude,
                                                double minLatitude,
                                                double maxLongitude,
                                                double maxLatitude,
                                                Boolean acceptingNewPatients,
                                                Boolean emergencyService) {
        List<Clinic> clinics = clinicRepository.findByLocationGeoWithin(
                toBoundingBox(minLongitude, minLatitude, maxLongitude, maxLatitude));
        return filterByAvailability(clinics, acceptingNewPatients, emergencyService);
    }

    /**
     * Finds clinics whose location falls within the supplied polygon.
     *
     * @param coordinates polygon shell coordinates
     * @return clinics inside the polygon
     */
    public List<Clinic> findClinicsWithinPolygon(List<Point> coordinates) {
        return findClinicsWithinPolygon(coordinates, null, null);
    }

    /**
     * Finds clinics inside a polygon and applies optional Boolean filters.
     *
     * @param coordinates polygon shell coordinates
     * @param acceptingNewPatients optional new-patient availability filter
     * @param emergencyService optional emergency-service filter
     * @return clinics inside the polygon matching the requested filters
     */
    public List<Clinic> findClinicsWithinPolygon(List<Point> coordinates,
                                                 Boolean acceptingNewPatients,
                                                 Boolean emergencyService) {
        List<Clinic> clinics = clinicRepository.findByLocationGeoWithin(toPolygon(coordinates));
        return filterByAvailability(clinics, acceptingNewPatients, emergencyService);
    }

    /**
     * Finds clinics whose service area intersects the supplied line.
     *
     * @param coordinates line coordinates
     * @return clinics intersecting the line
     */
    public List<Clinic> findClinicsIntersectingLine(List<Point> coordinates) {
        return findClinicsIntersectingLine(coordinates, null, null);
    }

    /**
     * Finds clinics whose service areas intersect a line and applies optional
     * Boolean filters.
     *
     * @param coordinates line coordinates
     * @param acceptingNewPatients optional new-patient availability filter
     * @param emergencyService optional emergency-service filter
     * @return intersecting clinics matching the requested filters
     */
    public List<Clinic> findClinicsIntersectingLine(List<Point> coordinates,
                                                    Boolean acceptingNewPatients,
                                                    Boolean emergencyService) {
        List<Clinic> clinics = clinicRepository.findByServiceAreaGeoIntersects(toLineString(coordinates));
        return filterByAvailability(clinics, acceptingNewPatients, emergencyService);
    }

    /**
     * Applies availability predicates through the repository's Boolean query
     * methods, then preserves the order returned by the spatial query.
     */
    private List<Clinic> filterByAvailability(List<Clinic> clinics,
                                              Boolean acceptingNewPatients,
                                              Boolean emergencyService) {
        if (acceptingNewPatients == null && emergencyService == null) {
            return clinics;
        }

        List<Clinic> availabilityMatches;
        if (acceptingNewPatients != null && emergencyService != null) {
            availabilityMatches = clinicRepository.findByAcceptingNewPatientsAndEmergencyService(
                    acceptingNewPatients, emergencyService);
        } else if (acceptingNewPatients != null) {
            availabilityMatches = clinicRepository.findByAcceptingNewPatients(acceptingNewPatients);
        } else {
            availabilityMatches = clinicRepository.findByEmergencyService(emergencyService);
        }

        Set<Integer> matchingIds = availabilityMatches.stream()
                .map(Clinic::id)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return clinics.stream()
                .filter(clinic -> clinic.id() != null && matchingIds.contains(clinic.id()))
                .toList();
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
