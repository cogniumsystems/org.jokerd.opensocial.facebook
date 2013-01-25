package org.jokerd.opensocial.facebook;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jokerd.opensocial.api.model.ActivityEntry;
import org.jokerd.opensocial.api.model.ActivityObject;
import org.jokerd.opensocial.api.model.Person;
import org.jokerd.opensocial.cursors.ActivityListCursor;
import org.jokerd.opensocial.cursors.IActivityCursor;
import org.jokerd.opensocial.cursors.StreamException;
import org.jokerd.opensocial.oauth.OAuthHelper;
import org.ubimix.commons.cursor.ICursor;
import org.ubimix.commons.cursor.SequentialCursor;
import org.ubimix.commons.json.JsonObject;
import org.ubimix.commons.json.rpc.RpcError;

/**
 * @author kotelnikov
 */
public class FacebookActivitiesCursor
    extends
    SequentialCursor<ActivityEntry, StreamException> implements IActivityCursor {

    private static String HOME_NEWS_URL = "https://graph.facebook.com/me/home";

    private static Logger log = Logger.getLogger(FacebookActivitiesCursor.class
        .getName());

    private String fNextUrl = HOME_NEWS_URL;

    private final OAuthHelper fOAuthHelper;

    private final ActivityObject fProfileInfo;

    public FacebookActivitiesCursor(OAuthHelper oauthHelper) throws IOException {
        fOAuthHelper = oauthHelper;
        FacebookProfileProvider profileProvider = new FacebookProfileProvider(
            fOAuthHelper);
        Person person = profileProvider.getUserProfile();
        fProfileInfo = new ActivityObject();
        fProfileInfo.setId(person.getIdAsString());
        fProfileInfo
            .setDisplayName("Facebook News: " + person.getDisplayName());
        fProfileInfo.setUrl(person.getUrl());
    }

    private StreamException handleError(String msg, Throwable t) {
        if (t instanceof StreamException) {
            return (StreamException) t;
        }
        log.log(Level.FINE, msg, t);
        return new StreamException(t);
    }

    @Override
    protected ICursor<ActivityEntry, StreamException> loadNextCursor(
        ICursor<ActivityEntry, StreamException> cursor) throws StreamException {
        try {
            if (fNextUrl == null) {
                return null;
            }

            HashMap<String, String> map = new HashMap<String, String>();
            String json = fOAuthHelper.call(fNextUrl, map.entrySet());
            JsonObject response = JsonObject.FACTORY.newValue(json);
            RpcError error = response.getObject("error", RpcError.FACTORY);
            if (error != null) {
                throw new StreamException(error.getMessage());
            }
            JsonObject paging = response
                .getObject("paging", JsonObject.FACTORY);
            fNextUrl = paging != null ? paging.getString("next") : null;
            FacebookeActivityBuilder builder = new FacebookeActivityBuilder(
                response,
                fProfileInfo);
            List<ActivityEntry> activities = builder.getActivities();
            if (activities.isEmpty()) {
                return null;
            }

            ActivityListCursor nextCursor = new ActivityListCursor(activities);
            return nextCursor;

        } catch (Throwable t) {
            throw handleError("Can not load a new activity cursor.", t);
        }
    }

}