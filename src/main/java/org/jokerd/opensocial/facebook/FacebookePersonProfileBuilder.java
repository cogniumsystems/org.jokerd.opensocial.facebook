package org.jokerd.opensocial.facebook;

import org.jokerd.opensocial.api.model.Account;
import org.jokerd.opensocial.api.model.ActivityObject;
import org.jokerd.opensocial.api.model.Address;
import org.jokerd.opensocial.api.model.Name;
import org.jokerd.opensocial.api.model.Person;
import org.ubimix.commons.json.JsonObject;
import org.ubimix.commons.json.ext.FormattedDate;

/**
 * @author kotelnikov
 */
// http://developers.facebook.com/docs/reference/api/user/
public class FacebookePersonProfileBuilder
    extends
    AbstractFacebookEntityBuilder {

    public FacebookePersonProfileBuilder() {
    }

    public Person getPersonInfo(JsonObject obj) {
        Person person = new Person();
        person.setId(getId(obj.getString("id")));
        person.setDisplayName(obj.getString("name"));
        Name name = new Name();
        name.setFormatted(obj.getString("name"));
        name.setGivenName(obj.getString("first_name"));
        name.setFamilyName(obj.getString("last_name"));
        person.setName(name);
        person.setGender(obj.getString("gender"));
        // FIXME: check the semantic of the account fields
        Account account = new Account();
        account.setFullUserId(getId(obj.getString("username")));
        account.setValue("link", obj.getString("link"));
        person.setAccounts(account);
        FormattedDate birthDay = readDate(obj.getString("birthday"));
        person.setAnniversary(birthDay);
        JsonObject homeTown = obj.getObject("hometown", JsonObject.FACTORY);
        if (homeTown != null) {
            Address address = new Address();
            ActivityObject addressAsActivity = ActivityObject.FACTORY
                .newValue(address);
            addressAsActivity.setId(getId(homeTown.getString("id")));
            String str = homeTown.getString("name");
            String[] array = str.split(",");
            String town = array.length > 0 ? array[0].trim() : null;
            String country = array.length > 1 ? array[1].trim() : null;
            address.setCountry(country);
            address.setLocality(town);
            person.setAddresses(address);
        }
        person.setUpdated(readDateTime(obj.getString("updated_time")));
        return person;
    }
}