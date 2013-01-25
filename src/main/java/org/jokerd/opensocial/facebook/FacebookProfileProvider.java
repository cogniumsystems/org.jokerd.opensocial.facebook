/**
 * 
 */
package org.jokerd.opensocial.facebook;

import java.io.IOException;
import java.util.HashMap;

import org.jokerd.opensocial.api.model.Person;
import org.jokerd.opensocial.oauth.OAuthHelper;
import org.ubimix.commons.json.JsonObject;

/**
 * @author kotelnikov
 */
public class FacebookProfileProvider {

    private static String PROFILE_URL = "https://graph.facebook.com/me/";

    private final OAuthHelper fOAuthHelper;

    /**
     * 
     */
    public FacebookProfileProvider(OAuthHelper oauthHelper) {
        fOAuthHelper = oauthHelper;
    }

    public Person getUserProfile() throws IOException {
        HashMap<String, String> map = new HashMap<String, String>();
        String json = fOAuthHelper.call(PROFILE_URL, map.entrySet());
        JsonObject obj = JsonObject.FACTORY.newValue(json);
        FacebookePersonProfileBuilder builder = new FacebookePersonProfileBuilder();
        Person person = builder.getPersonInfo(obj);
        return person;
    }
}
