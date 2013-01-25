/**
 * 
 */
package org.jokerd.opensocial.facebook;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

import org.jokerd.opensocial.api.model.DomainName;
import org.jokerd.opensocial.cursors.AbstractActivityBuilder;
import org.ubimix.commons.json.JsonObject;
import org.ubimix.commons.json.ext.DateFormatter;
import org.ubimix.commons.json.ext.FormattedDate;

/**
 * @author kotelnikov
 */
public class AbstractFacebookEntityBuilder extends AbstractActivityBuilder {

    private static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(
        "MM/dd/yyyy");

    private static SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat(
        "yyyy-MM-dd'T'HH:mm:ssZ");

    static {
        DATE_TIME_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    protected static void copyFields(
        JsonObject from,
        JsonObject to,
        String... ignoredFields) {
        Set<String> ignored = new HashSet<String>(Arrays.asList(ignoredFields));
        for (String key : from.getKeys()) {
            if (!ignored.contains(key)) {
                Object value = from.getValue(key);
                to.setValue(key, value);
            }
        }
    }

    protected static FormattedDate formatDate(Date date) {
        return DateFormatter.formatDate(date);
    }

    protected static FormattedDate readDate(String date) {
        try {
            if (date == null) {
                return null;
            }
            Date d = DATE_FORMAT.parse(date);
            return formatDate(d);
        } catch (ParseException e) {
            return null;
        }
    }

    protected static FormattedDate readDateTime(String dateTime) {
        try {
            if (dateTime == null) {
                return null;
            }
            Date d = DATE_TIME_FORMAT.parse(dateTime);
            return formatDate(d);
        } catch (ParseException e) {
            return null;
        }
    }

    protected DomainName DOMAIN_NAME = new DomainName("facebook.com");

    /**
     * 
     */
    public AbstractFacebookEntityBuilder() {
    }

    @Override
    protected DomainName getDomainName() {
        return DOMAIN_NAME;
    }

}
