package org.jokerd.opensocial.facebook;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jokerd.opensocial.api.model.ActivityEntry;
import org.jokerd.opensocial.api.model.ActivityObject;
import org.jokerd.opensocial.api.model.Group;
import org.jokerd.opensocial.api.model.MediaLink;
import org.jokerd.opensocial.api.model.ObjectId;
import org.jokerd.opensocial.cursors.ActivityEntryUtil;
import org.ubimix.commons.json.JsonArray;
import org.ubimix.commons.json.JsonObject;
import org.ubimix.commons.json.ext.FormattedDate;

/**
 * @author kotelnikov
 */
public class FacebookeActivityBuilder extends AbstractFacebookEntityBuilder {

    public class ActivityBuilder implements IActivityBuilder {

        private final String fType;

        public ActivityBuilder(String type) {
            fType = type;
        }

        @Override
        public ActivityEntry build(JsonObject entry) {
            ActivityEntry result = new ActivityEntry();
            ObjectId entryId = getId(entry.getString("id"));
            result.setId(entryId.toString());

            ActivityObject activityObject = buildActivityObject(entry);
            String title = activityObject.getDisplayName();
            result.setTitle(title);

            setTimeInfo(entry, ActivityObject.FACTORY.newValue(result));

            MediaLink link = getMediaLink(entry, "icon");
            result.setMediaLink(link);

            ActivityObject author = buildAuthorObject(entry);
            result.setActor(author);
            String verb = getVerb();
            result.setVerb(verb);
            result.setObject(activityObject);

            ActivityObject targetInfo = buildTargetInfo(entry);
            result.setTarget(targetInfo);
            return result;
        }

        protected ActivityObject buildActivityObject(JsonObject entry) {
            ActivityObject activity = new ActivityObject();
            activity.setId(getId(entry.getString("id")));
            activity.setObjectType(fType);
            setTimeInfo(entry, activity);
            return activity;
        }

        protected ActivityObject buildAuthorObject(JsonObject entry) {
            JsonObject userInfo = entry.getObject("from", JsonObject.FACTORY);
            ActivityObject obj = new ActivityObject();
            ObjectId id = getId(userInfo.getString("id"));
            obj.setId(id);
            obj.setObjectType(userInfo.getString("category"));
            obj.setDisplayName(userInfo.getString("name"));
            // FIXME: add information about this user to fill in the profile
            return obj;
        }

        protected ActivityObject buildTargetInfo(JsonObject entry) {
            ActivityObject targetInfo = ActivityObject.FACTORY
                .newValue(fTargetInfo.toString());
            // FIXME: use the "application" field as a hint
            // how to extract the provinence information
            return targetInfo;
        }

        protected MediaLink getMediaLink(JsonObject entry, String key) {
            MediaLink link = new MediaLink();
            link.setUrl(entry.getString(key));
            return link;
        }

        protected String getType() {
            return fType;
        }

        protected String getVerb() {
            return "post";
        }

        protected void setComments(JsonObject entry, ActivityObject activity) {
            // TODO Auto-generated method stub
        }

    }

    public interface IActivityBuilder {
        ActivityEntry build(JsonObject entry);
    }

    protected static void setTimeInfo(JsonObject entry, ActivityObject result) {
        FormattedDate createdTime = readDateTime(entry
            .getString("created_time"));
        result.setPublished(createdTime);

        FormattedDate updateTime = readDateTime(entry.getString("updated_time"));
        result.setUpdated(updateTime);
    }

    private final Map<String, IActivityBuilder> fBuilders = new HashMap<String, IActivityBuilder>();

    private final JsonArray fFeed;

    private final ActivityObject fTargetInfo;

    public FacebookeActivityBuilder(JsonObject obj, ActivityObject targetInfo) {
        initBuilders();
        JsonArray feed = obj.getArray("data", false);
        fTargetInfo = targetInfo;
        fFeed = feed;
    }

    private void addBuilder(ActivityBuilder builder) {
        fBuilders.put(builder.getType(), builder);
    }

    public ActivityEntry buildActivityEntry(JsonObject entry) {
        String type = entry.getString("type");
        IActivityBuilder builder = fBuilders.get(type);
        if (builder == null) {
            return null;
        }
        ActivityEntry result = builder.build(entry);
        return result;
    }

    public List<ActivityEntry> getActivities() {
        List<ActivityEntry> result = new ArrayList<ActivityEntry>();
        for (JsonObject entry : fFeed.getList(JsonObject.FACTORY)) {
            ActivityEntry activity = buildActivityEntry(entry);
            if (activity != null) {
                result.add(activity);
            }
        }
        Collections.sort(result, ActivityEntryUtil.ENTRY_COMPARATOR);
        return result;
    }

    private void initBuilders() {
        // Ignored types: achievement, checkin, domain, insights

        // http://developers.facebook.com/docs/reference/api/event/
        addBuilder(new ActivityBuilder("event") {
            @Override
            protected ActivityObject buildActivityObject(JsonObject entry) {
                ActivityObject activity = super.buildActivityObject(entry);
                activity.setDisplayName(entry.getString("name"));
                activity.setContent(entry.getString("description"));
                setTimeInfo(entry, activity);
                // FIXME: copy start/end time
                return activity;
            }

            @Override
            protected ActivityObject buildAuthorObject(JsonObject entry) {
                JsonObject userInfo = entry.getObject(
                    "owner",
                    JsonObject.FACTORY);
                ActivityObject obj = new ActivityObject();
                obj.setId(getId(userInfo.getString("id")));
                obj.setDisplayName(userInfo.getString("name"));
                return obj;
            }
        });
        // http://developers.facebook.com/docs/reference/api/link/
        addBuilder(new ActivityBuilder("link") {
            @Override
            public ActivityEntry build(JsonObject entry) {
                ActivityEntry activityEntry = super.build(entry);
                activityEntry.setContent(entry.getString("message"));
                return activityEntry;
            }

            @Override
            protected ActivityObject buildActivityObject(JsonObject entry) {
                ActivityObject activity = super.buildActivityObject(entry);
                activity.setDisplayName(entry.getString("name"));
                activity.setContent(entry.getString("description"));
                MediaLink icon = getMediaLink(entry, "picture");
                activity.setMediaLink(icon);
                setTimeInfo(entry, activity);
                setComments(entry, activity);
                return activity;
            }

            @Override
            protected String getVerb() {
                return "shared";
            }
        });
        // http://developers.facebook.com/docs/reference/api/message/
        addBuilder(new ActivityBuilder("message") {
            @Override
            protected ActivityObject buildActivityObject(JsonObject entry) {
                ActivityObject activity = super.buildActivityObject(entry);
                activity.setDisplayName(entry.getString("message"));
                setTimeInfo(entry, activity);
                // FIXME: copy "to" field here
                return activity;
            }

            @Override
            protected String getVerb() {
                return "sent";
            }
        });
        // http://developers.facebook.com/docs/reference/api/note/
        addBuilder(new ActivityBuilder("note") {
            @Override
            protected ActivityObject buildActivityObject(JsonObject entry) {
                ActivityObject activity = super.buildActivityObject(entry);
                activity.setDisplayName(entry.getString("subject"));
                activity.setContent(entry.getString("message"));
                setTimeInfo(entry, activity);
                setComments(entry, activity);
                // FIXME: copy "comments" field here
                return activity;
            }

            @Override
            protected String getVerb() {
                return "sent";
            }
        });
        // http://developers.facebook.com/docs/reference/api/photo/
        addBuilder(new ActivityBuilder("photo") {
            @Override
            protected ActivityObject buildActivityObject(JsonObject entry) {
                ActivityObject activity = super.buildActivityObject(entry);
                String title = entry.getString("subject");
                if (title == null) {
                    title = entry.getString("message");
                }
                activity.setDisplayName(title);
                activity.setUrl(entry.getString("link"));
                MediaLink img = getMediaLink(entry, "picture");
                activity.setMediaLink(img);
                activity.setValue("height", entry.getValue("height"));
                activity.setValue("width", entry.getValue("width"));
                setTimeInfo(entry, activity);
                setComments(entry, activity);
                // FIXME: copy "position", "place", "source", "tags" field here
                return activity;
            }

            @Override
            protected String getVerb() {
                return "sent";
            }
        });
        // http://developers.facebook.com/docs/reference/api/post/
        addBuilder(new ActivityBuilder("post") {
            @Override
            public ActivityEntry build(JsonObject entry) {
                ActivityEntry activityEntry = super.build(entry);
                activityEntry.setContent(entry.getString("message"));
                // FIXME: copy "message_tags" fields
                return activityEntry;
            }

            @Override
            protected ActivityObject buildActivityObject(JsonObject entry) {
                ActivityObject activity = super.buildActivityObject(entry);
                activity.setDisplayName(entry.getString("name"));
                activity.setContent(entry.getString("description"));
                activity.setUrl(entry.getString("link"));
                MediaLink icon = getMediaLink(entry, "picture");
                activity.setMediaLink(icon);
                setTimeInfo(entry, activity);
                setComments(entry, activity);
                // FIXME: copy "caption", "source", "privacy", "actions" etc
                // fields
                return activity;
            }

            @Override
            protected String getVerb() {
                return "shared";
            }
        });
        // http://developers.facebook.com/docs/reference/api/status/
        addBuilder(new ActivityBuilder("status") {
            @Override
            protected ActivityObject buildActivityObject(JsonObject entry) {
                ActivityObject activity = super.buildActivityObject(entry);
                String message = entry.getString("message");
                if (message == null) {
                    message = entry.getString("story");
                }
                activity.setDisplayName(message);
                setTimeInfo(entry, activity);
                setComments(entry, activity);
                return activity;
            }

        });
        // http://developers.facebook.com/docs/reference/api/video/
        addBuilder(new ActivityBuilder("video") {
            @Override
            protected ActivityObject buildActivityObject(JsonObject entry) {
                ActivityObject activity = super.buildActivityObject(entry);
                activity.setDisplayName(entry.getString("name"));
                activity.setUrl(entry.getString("source"));
                MediaLink img = getMediaLink(entry, "picture");
                activity.setMediaLink(img);
                activity.setValue("embed_html", entry.getValue("embed_html"));
                setTimeInfo(entry, activity);
                setComments(entry, activity);
                return activity;
            }
        });

        // ???????????????????????????????????

        // http://developers.facebook.com/docs/reference/api/album/
        addBuilder(new ActivityBuilder("album") {
            @Override
            protected ActivityObject buildActivityObject(JsonObject entry) {
                ActivityObject activity = super.buildActivityObject(entry);
                activity.setDisplayName(entry.getString("name"));
                activity.setContent(entry.getString("description"));
                activity.setUrl(entry.getString("link"));
                activity.setObjectType(entry.getString("type"));
                setTimeInfo(entry, activity);
                return activity;
            }
        });
        // http://developers.facebook.com/docs/reference/api/application/
        addBuilder(new ActivityBuilder("application") {
            @Override
            protected ActivityObject buildActivityObject(JsonObject entry) {
                ActivityObject activity = super.buildActivityObject(entry);
                activity.setDisplayName(entry.getString("name"));
                activity.setContent(entry.getString("description"));
                activity.setUrl(entry.getString("link"));
                copyFields(entry, activity, "id", "name", "description", "link");
                return activity;
            }
        });
        // http://developers.facebook.com/docs/reference/api/comment/
        addBuilder(new ActivityBuilder("comment") {
            @Override
            protected ActivityObject buildActivityObject(JsonObject entry) {
                ActivityObject activity = super.buildActivityObject(entry);
                activity.setDisplayName(entry.getString("message"));
                setTimeInfo(entry, activity);
                return activity;
            }
        });
        // http://developers.facebook.com/docs/reference/api/FriendList/
        addBuilder(new ActivityBuilder("friendlists") {
            @Override
            protected ActivityObject buildActivityObject(JsonObject entry) {
                ActivityObject activity = super.buildActivityObject(entry);
                Group group = Group.FACTORY.newValue(activity);
                group.setDisplayName(entry.getString("name"));
                // TODO: ? Add other group fields
                return group;
            }
        });
        // http://developers.facebook.com/docs/reference/api/group/
        addBuilder(new ActivityBuilder("group") {
            @Override
            protected ActivityObject buildActivityObject(JsonObject entry) {
                ActivityObject activity = super.buildActivityObject(entry);
                Group group = Group.FACTORY.newValue(activity);
                group.setDisplayName(entry.getString("name"));
                group.setDescription(entry.getString("description"));
                setTimeInfo(entry, activity);
                // TODO: ? Add other group fields
                return group;
            }
        });
        // http://developers.facebook.com/docs/reference/api/insights/
        addBuilder(new ActivityBuilder("insights") {
            @Override
            protected ActivityObject buildActivityObject(JsonObject entry) {
                ActivityObject activity = super.buildActivityObject(entry);
                activity.setDisplayName(entry.getString("name"));
                setTimeInfo(entry, activity);
                return activity;
            }
        });
    }

}