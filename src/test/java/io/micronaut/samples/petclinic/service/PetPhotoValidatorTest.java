package io.micronaut.samples.petclinic.service;

import io.micronaut.samples.petclinic.exception.FileTooLargeException;
import io.micronaut.samples.petclinic.exception.InvalidFileFormatException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PetPhotoValidatorTest {
    private final PetPhotoValidator validator = new PetPhotoValidator(1024);

    @Test
    void acceptsSupportedDeclaredTypesWithoutInspectingContents() throws Exception {
        byte[] bytes = "showcase upload".getBytes(StandardCharsets.UTF_8);
        for (String format : new String[]{"jpeg", "png", "gif"}) {
            var photo = validator.validate(bytes, "image/" + format);
            assertThat(photo.mediaType().getName()).isEqualTo("image/" + format);
            assertThat(photo.extension()).isEqualTo(format);
            assertThat(photo.bytes()).containsExactly(bytes);
        }
        assertThat(validator.validate(bytes, "IMAGE/PNG").extension()).isEqualTo("png");
    }

    @Test
    void ownsAnImmutableCopy() throws Exception {
        byte[] bytes = {1, 2, 3};
        var photo = validator.validate(bytes, "image/png");
        bytes[0] = 0;
        byte[] returned = photo.bytes();
        assertThat(returned).containsExactly(1, 2, 3);
        returned[0] = 0;
        assertThat(photo.bytes()).containsExactly(1, 2, 3);
    }

    @Test
    void rejectsUnsupportedOrMissingContentType() {
        byte[] bytes = {1, 2, 3};
        for (String type : new String[]{"text/plain", "application/pdf", "image/svg+xml", ""}) {
            assertThrows(InvalidFileFormatException.class, () -> validator.validate(bytes, type));
        }
        assertThrows(InvalidFileFormatException.class, () -> validator.validate(bytes, null));
    }

    @Test
    void rejectsOversizedDataAndAcceptsExactLimit() throws Exception {
        assertThrows(FileTooLargeException.class, () -> validator.validate(new byte[1025], "image/png"));
        byte[] bytes = new byte[1024];
        assertThat(validator.validate(bytes, "image/png").bytes()).containsExactly(bytes);
    }
}
