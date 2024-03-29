package eu.europeana.api.recommend.model;

import eu.europeana.api.recommend.common.RecordId;

import java.util.*;

/**
 * Container class for holding relevant set data, retrieved from Set API
 * Note that we use a slightly different Entity definition in the Recommendations Updater
 */
public class Set {

    private String id;
    private Map<String, String> title;
    private Map<String, String> description;
    private String isDefinedBy;
    private String[] items;

    private Set() {
        // empty constructor for Jackson serialization
    }

    public Set(String id, Map<String, String> title, Map<String, String> description, String isDefinedBy, String[] items) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.isDefinedBy = isDefinedBy;
        this.items = items;
    }

    public String getId() {
        return id;
    }

    public Map<String, String> getTitle() {
        if (title == null) {
            return Collections.emptyMap();
        }
        return this.title;
    }

    public Map<String, String> getDescription() {
        if (description == null) {
            return Collections.emptyMap();
        }
        return this.description;
    }

    public String getIsDefinedBy() {
        return isDefinedBy;
    }

    public String[] getItems() {
        return items;
    }

    /**
     * Return a list of record ids of all items that are part of this set
     * @return list of record ids
     */
    public List<RecordId> getItemsRecordId() {
        if (this.getItems() == null) {
            return Collections.emptyList();
        }
        List<RecordId> result = new ArrayList<>(this.getItems().length);
        for (String fullId : this.getItems()) {
            result.add(new RecordId(fullId));
        }
        return result;
    }

    @Override
    public String toString() {
        return "Set{" +
                "id='" + id + '\'' +
                ", title=" + title +
                ", description=" + description +
                ", isDefinedBy='" + isDefinedBy + '\'' +
                ", items=" + Arrays.toString(items) +
                '}';
    }
}
