package eu.europeana.api.recommend.model;

/**
 * Container class for holding relevant data from a set search. A set search results can return one or more sets
 */
public class SetSearch {

    private int total;
    private Set[] items;

    public int getTotal() {
        return total;
    }

    public Set[] getItems() {
        return items;
    }
}
