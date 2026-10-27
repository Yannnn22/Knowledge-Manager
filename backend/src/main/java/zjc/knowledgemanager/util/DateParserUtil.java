package zjc.knowledgemanager.util;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public class DateParserUtil {

    // A more robust formatter that explicitly uses English locale for day/month names
    private static final DateTimeFormatter RFC_1123_FORMATTER = 
            new DateTimeFormatterBuilder()
            .appendPattern("EEE, dd MMM yyyy HH:mm:ss zzz")
            .toFormatter(Locale.ENGLISH);

    private static final DateTimeFormatter STANDARD_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static LocalDateTime parseDateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.trim().isEmpty()) {
            return null;
        }

        // Attempt to parse with the standard format first
        try {
            return LocalDateTime.parse(dateTimeString, STANDARD_FORMATTER);
        } catch (DateTimeParseException e) {
            // If it fails, proceed to the more complex RFC format
        }

        // Handle the non-standard RFC format from the frontend
        try {
            String processedString = dateTimeString;
            // Handle the special case where we have "GMT 00:00:00" or "GMT 23:59:59" at the end
            if (processedString.contains(" GMT ")) {
                // Extract the time part after "GMT "
                String timePart = processedString.substring(processedString.indexOf(" GMT ") + 5);
                // Remove the time part and keep only up to "GMT"
                processedString = processedString.substring(0, processedString.indexOf(" GMT ") + 4);
                
                // Parse the base date
                LocalDateTime baseDateTime = ZonedDateTime.parse(processedString, RFC_1123_FORMATTER).toLocalDateTime();
                
                // If we have a time part, replace the time in the base date
                if (timePart.matches("\\d{2}:\\d{2}:\\d{2}")) {
                    String[] timeParts = timePart.split(":");
                    int hour = Integer.parseInt(timeParts[0]);
                    int minute = Integer.parseInt(timeParts[1]);
                    int second = Integer.parseInt(timeParts[2]);
                    return baseDateTime.withHour(hour).withMinute(minute).withSecond(second);
                }
                
                return baseDateTime;
            }
            
            // Fallback to original logic for other cases
            if (processedString.contains(" GMT")) {
                processedString = processedString.substring(0, processedString.indexOf(" GMT") + 4).trim();
            }
            return ZonedDateTime.parse(processedString, RFC_1123_FORMATTER).toLocalDateTime();
        } catch (DateTimeParseException e) {
            // If both attempts fail, throw a detailed exception
            throw new DateTimeParseException("Unable to parse date-time string with any known format: " + dateTimeString, dateTimeString, 0);
        }
    }
}
