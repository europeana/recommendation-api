package eu.europeana.api.recommend.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import eu.europeana.api.recommend.util.RecommendationConstants;

/**
 * @deprecated
 */
@Deprecated(since="Aug 2023")
@JsonPropertyOrder({RecommendationConstants.SESSION_ID, RecommendationConstants.TYPE})
public class UserSignalRequest {

    @JsonProperty(RecommendationConstants.SESSION_ID)
    private String userId;

    private String item;

    @JsonProperty(RecommendationConstants.TYPE)
    private String signal;

    public UserSignalRequest(String userId, String item, String signal) {
        this.userId = userId;
        this.item = item;
        this.signal = signal;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public String getSignal() {
        return signal;
    }

    public void setSignal(String signal) {
        this.signal = signal;
    }
}
