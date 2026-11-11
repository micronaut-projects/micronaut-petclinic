package io.micronaut.samples.petclinic.service;

import io.micronaut.samples.petclinic.exception.FileTooLargeException;
import io.micronaut.samples.petclinic.exception.InvalidFileFormatException;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PetPhotoValidatorTest {
    private final PetPhotoValidator validator = new PetPhotoValidator(1024, 32);

    @Test
    void acceptsDecodableImageAndOwnsAnImmutableCopy() throws Exception {
        byte[] bytes = png(2, 2);
        var photo = validator.validate(bytes, "image/png");
        byte original = bytes[0];
        bytes[0] = 0;
        byte[] returned = photo.bytes();
        assertThat(returned[0]).isEqualTo(original);
        returned[0] = 0;
        assertThat(photo.bytes()[0]).isEqualTo(original);
        assertThat(photo.extension()).isEqualTo("png");
    }

    @Test
    void acceptsJpegAndGifWithTheirDeclaredContentTypes() throws Exception {
        for (String format : new String[]{"jpeg", "gif"}) {
            var output = new ByteArrayOutputStream();
            ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), format, output);
            var photo = validator.validate(output.toByteArray(), "image/" + format);
            assertThat(photo.mediaType().getName()).isEqualTo("image/" + format);
            assertThat(photo.extension()).isEqualTo(format);
        }
    }

    @Test
    void rejectsTruncatedImageWithValidSignature() throws Exception {
        byte[] header = java.util.Arrays.copyOf(png(2, 2), 8);
        assertThrows(InvalidFileFormatException.class, () -> validator.validate(header, "image/png"));
    }

    @Test
    void rejectsImageBeyondDimensionLimit() throws Exception {
        byte[] bytes = png(33, 1);
        assertThrows(InvalidFileFormatException.class, () -> validator.validate(bytes, "image/png"));
    }

    @Test
    void rejectsWrongContentTypeAndOversizedData() throws Exception {
        byte[] bytes = png(2, 2);
        assertThrows(InvalidFileFormatException.class, () -> validator.validate(bytes, "image/jpeg"));
        assertThrows(FileTooLargeException.class, () -> validator.validate(new byte[1025], "image/png"));
    }

    private static byte[] png(int width, int height) throws Exception {
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "png", output);
        return output.toByteArray();
    }
}
