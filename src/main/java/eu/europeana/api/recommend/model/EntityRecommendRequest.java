package eu.europeana.api.recommend.model;

import java.util.List;

public class EntityRecommendRequest {

    private List<Labels> labels;
    private String [] items;

    public List<Labels> getLabels() {
        return labels;
    }

    public void setLabels(List<Labels> labels) {
        this.labels = labels;
    }

    public String[] getItems() {
        return items;
    }

    public void setItems(String[] items) {
        this.items = items;
    }

}
