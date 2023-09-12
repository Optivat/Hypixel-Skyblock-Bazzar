package api;

import java.util.Map;

public class RecipeItem {
    public String name;
    public Map<String, String> recipe;
    public String wiki;
    public String scrollable;
    public String base_rarity;
    public RecipeItem(String name, Map<String, String> recipe, String wiki, String scrollable, String base_rarity) {
        this.name = name;
        this.recipe = recipe;
        this.wiki = wiki;
        this.scrollable = scrollable;
        this.base_rarity = base_rarity;
    }


}
