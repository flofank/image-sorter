package ch.fanki;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageSorter 
{
    private static final Logger LOG = LoggerFactory.getLogger(ImageSorter.class);
    private static final DateTimeFormatter DTF_METADATA = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");
    private static final DateTimeFormatter DTF_FILE_PREFIX = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final List<RegexDateExtractor> REGEX_EXTRACTORS = List.of(
        new RegexDateExtractor("PHOTO-(\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{2}).*", DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")),
        new RegexDateExtractor("threema-(\\d{8}-\\d{9}).*", DateTimeFormatter.ofPattern("yyyyMMdd-HHmmssSSS")),
        new RegexDateExtractor("image-(\\d{8}-\\d{6}).*", DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")),
        new RegexDateExtractor("IMG-(\\d{8})-WA.*", new DateTimeFormatterBuilder()
        .appendPattern("yyyyMMdd")
        .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
        .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
        .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
        .toFormatter())
    );

    public static void main( String[] args ) throws IOException
    {
        Path inputDir = Path.of(args[0]);
        Path outputDir = Path.of(args[1]);
        

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
           originalDate = getDateFromFileName(picture);
        }
        if (originalDate != null) {
            movePicture(picture, outputDirectory, originalDate);
        } else {
            LOG.error("Failed to handle {}", picture, originalDate);
        }
    }

    private static void movePicture(Path picture, Path outputDirectory, LocalDateTime originalDate) {
        Path targetPath = outputDirectory.resolve(DTF_FILE_PREFIX.format(originalDate) + "_" + picture.getFileName().toString());
        LOG.info("Moving {} to [{}]", picture.getFileName(), targetPath);
        try {
            Files.move(picture, targetPath);
        } catch (FileAlreadyExistsException ex) {
            LOG.warn("Skipped [{}]. File [{}] already exists", picture.getFileName().toString(), targetPath);
        } catch (IOException e) {
            LOG.error("Failure moving picture", e);
        }
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
    
    private static LocalDateTime getDateFromFileName(Path picture) {
        for (RegexDateExtractor extractor : REGEX_EXTRACTORS) {
            LocalDateTime date = extractor.extractDate(picture.getFileName().toString());
            if (date != null) {
                return date;
            }
        }
        return null;
    }

    public static class RegexDateExtractor {
        private DateTimeFormatter dateTimeFormatter;
        private Pattern fileNamePattern;

        public RegexDateExtractor(String fileNamePattern, DateTimeFormatter dateTimeFormatter) {
            this.dateTimeFormatter = dateTimeFormatter;
            this.fileNamePattern = Pattern.compile(fileNamePattern);
        }

        public LocalDateTime extractDate(String fileName) {
            Matcher m = fileNamePattern.matcher(fileName);
            if (m.matches()) {
                try {
                    return LocalDateTime.parse(m.group(1), dateTimeFormatter);
                } catch (Exception e) {}
            }
            return null;
        }
    }
}