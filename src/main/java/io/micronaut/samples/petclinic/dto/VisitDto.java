package io.micronaut.samples.petclinic.dto;

import io.micronaut.samples.petclinic.model.Visit;
import io.micronaut.serde.annotation.Serdeable;
import org.jspecify.annotations.NonNull;

import java.time.LocalDate;
import java.util.List;

@Serdeable
public record VisitDto(
        Integer id,
        LocalDate date,
        String description,
        String petName,
        String ownerName,
        Long durationInMinutes,
        Long periodInMonths
) {
    public static VisitDto from(@NonNull Visit visit) {
        return new VisitDto(visit.id(),
                visit.date(),
                visit.description(),
                visit.pet() != null ? visit.pet().name() : null,
                visit.pet() != null &&
                        visit.pet().getOwner() != null ? visit.pet().getOwner().getFullName() : null,
                visit.duration().toMinutes(),
                visit.period().toTotalMonths());
    }

    public static List<VisitDto> from(@NonNull List<Visit> visits) {
        return visits.stream().map(VisitDto::from).toList();
    }

    ;
}
