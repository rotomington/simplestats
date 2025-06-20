package network.roto.simplestats.utils;

public class Perks {
    public String id;
    public String name;
    public String icon;
    public int maxLevel;
    public int cost;
    public String levelUpCommand;
    public String levelDownCommand;

    public Perks (String id, String name, String icon, int maxLevel, int cost, String levelUpCommand, String levelDownCommand) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.maxLevel = maxLevel;
        this.cost = cost;
        this.levelUpCommand = levelUpCommand;
        this.levelDownCommand = levelDownCommand;
    }

    public Perks decodePerks (String str) {
        String regex = "[,]";
        Perks perk = new Perks("", "", "", 0, 0, "","");
        String[] array = str.split(regex);
        int i = 0;
        for (String string : array) {
            array[i] = string.replaceAll(",", "");
            i++;
        }
        perk.id = array[0];
        perk.name = array[1];
        perk.icon = array[2];
        perk.maxLevel = Integer.parseInt(array[3]);
        perk.cost = Integer.parseInt(array[4]);
        perk.levelUpCommand = array[5];
        perk.levelDownCommand = array[6];
        return perk;
    }
}
