package eu.europeana.api.recommend.model;

public class Labels {

    private String title;
    private String descriptions;

    public Labels(String title, String descriptions) {
        this.title = title;
        this.descriptions = descriptions;
    }

    public Labels(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescriptions() {
        return descriptions;
    }

    public void setDescriptions(String descriptions) {
        this.descriptions = descriptions;
    }
}
