package ch.fanki;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hello world!
 *
 */
public class App 
{
    private static final Logger LOG = LoggerFactory.getLogger(App.class);
    private static final DateTimeFormatter DTF_METADATA = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");
    private static final SimpleDateFormat SDF_METADATA = new SimpleDateFormat("YYYY:MM:DD hh:mm:ss", Locale.GERMAN);

    public static void main( String[] args ) throws IOException
    {
        Path inputDir = Path.of("D:\\Fotos\\Family\\2023\\Icoming");
        Path outputDir = Path.of("D:\\Fotos\\Family\\2023\\Sorted");

        LOG.info("Sorting files from {} to {}", inputDir, outputDir);
        Files.walk(inputDir).forEach(p -> {
            if (p.toFile().isFile()) {
                try {
                    handlePicture(p, outputDir);
                } catch (Exception e) {
                    LOG.error("Exception Handling File {}", p);
                    e.printStackTrace();
                }
            }
        });
    }

    private static void handlePicture(Path picture, Path outputDirectory) throws ImageReadException, IOException {
        LOG.debug("Handling {}", picture.getFileName());
        LocalDateTime originalDate = getDateFromMetadata(picture);
        if (originalDate == null) {
            LOG.info("Failed to handle {}", picture, originalDate);
        }
        LOG.info("Extracted date [{}] from {}", originalDate, picture.getFileName());
    }

    private static LocalDateTime getDateFromMetadata(Path picture) throws ImageReadException, IOException {
        LocalDateTime originalDate = null;
        ImageMetadata metadata = Imaging.getMetadata(picture.toFile());

        if (metadata instanceof JpegImageMetadata) {
            JpegImageMetadata jpegImageMetadata = (JpegImageMetadata) metadata;
            TiffField field = jpegImageMetadata.findEXIFValue(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
            try {
                originalDate = LocalDateTime.parse((String) field.getValue(), DTF_METADATA);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (metadata instanceof TiffImageMetadata) {
            TiffImageMetadata tiffImageMetadata = (TiffImageMetadata) metadata;
            TiffField field = tiffImageMetadata.findField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
            try {
                originalDate = LocalDateTime.parse((String) field.getValue(), DTF_METADATA);
            } catch (Exception e) {}
        } else if (metadata != null) {
            LOG.info("{}", metadata.getClass());
        }
       

        return originalDate;
    }
}