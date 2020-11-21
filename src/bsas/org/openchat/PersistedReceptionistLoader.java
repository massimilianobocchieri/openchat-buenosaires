package bsas.org.openchat;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.time.LocalDateTime;

class PersistedReceptionistLoader {
    public static final String INVALID_RECORD = "Invalid record";

    private final LineNumberReader lineReader;
    private RestReceptionist receptionist;
    private String lastId;
    private LocalDateTime lastNow;
    private JsonObject actionAsJson;
    private JsonObject parameters;
    private JsonObject returned;
    private String line;
    private String actionName;

    public PersistedReceptionistLoader(Reader reader) {
        lineReader = new LineNumberReader(reader);
    }

    public static RestReceptionist loadFrom(Reader reader) throws IOException {
        return new PersistedReceptionistLoader(reader).execute();
    }

    public static String invalidRecordErrorMessage(int lineNumber) {
        return INVALID_RECORD + " at line " + lineNumber;
    }

    public RestReceptionist execute() throws IOException {

        createReceptionist();

        while (hasLineToParse()) {
            createAction();
            executeAction();
        }

        return receptionist;
    }

    public void createReceptionist() {
        receptionist = new RestReceptionist(
                new OpenChatSystem(() -> lastNow),
                () -> lastId);
    }

    public boolean hasLineToParse() throws IOException {
        line = lineReader.readLine();
        return line != null;
    }

    public void executeAction() {
        if (isRegisterUserAction()) {
            executeRegisterUserAction();
        } else if (isFollowingsAction()) {
            executeFollowingsAction();
        } else if (isAddPublicationAction()) {
            executeAddPublicationAction();
        } else if (isLikePublicationAction()) {
            executeLikePublicationAction();
        } else
            invalidAction();
    }

    public void invalidAction() {
        throw new RuntimeException(invalidRecordErrorMessage(lineReader.getLineNumber()));
    }

    public void executeLikePublicationAction() {
        receptionist.likePublicationIdentifiedAs(
                parameters.getString(RestReceptionist.POST_ID_KEY, null),
                parameters);
    }

    public boolean isLikePublicationAction() {
        return isAction(ActionPersistentReceptionist.LIKE_PUBLICATION_ACTION_NAME);
    }

    public boolean isAction(String likePublicationActionName) {
        return actionName.equals(likePublicationActionName);
    }

    public void executeAddPublicationAction() {
        lastId = returned.getString(RestReceptionist.POST_ID_KEY, null);
        lastNow = LocalDateTime.from(RestReceptionist.DATE_TIME_FORMATTER.parse(
                returned.getString(RestReceptionist.DATE_TIME_KEY, null)));
        receptionist.addPublication(
                parameters.getString(RestReceptionist.USER_ID_KEY, null),
                parameters);
    }

    public boolean isAddPublicationAction() {
        return isAction(ActionPersistentReceptionist.ADD_PUBLICATION_ACTION_NAME);
    }

    public void executeFollowingsAction() {
        receptionist.followings(parameters);
    }

    public boolean isFollowingsAction() {
        return isAction(ActionPersistentReceptionist.FOLLOWINGS_ACTION_NAME);
    }

    public void executeRegisterUserAction() {
        lastId = returned.getString(RestReceptionist.ID_KEY, null);
        receptionist.registerUser(parameters);
    }

    public boolean isRegisterUserAction() {
        return isAction(ActionPersistentReceptionist.REGISTER_USER_ACTION_NAME);
    }

    public void createAction() {
        try {
            actionAsJson = Json.parse(line).asObject();
            parameters = actionAsJson.get(ActionPersistentReceptionist.PARAMETERS_KEY).asObject();
            returned = actionAsJson.get(ActionPersistentReceptionist.RETURN_KEY).asObject();
        } catch (RuntimeException e) {
            throw new RuntimeException(invalidRecordErrorMessage(lineReader.getLineNumber()), e);
        }
        actionName = actionAsJson.getString(ActionPersistentReceptionist.ACTION_NAME_KEY, "");
    }
}
