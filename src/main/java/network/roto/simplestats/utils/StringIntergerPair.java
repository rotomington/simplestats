package network.roto.simplestats.utils;

public class StringIntergerPair {
    public String string;
    public int interger;

    public StringIntergerPair(String str, int intg){
        this.string = str;
        this.interger = intg;
    }

    public StringIntergerPair decodeSIP (String str) {
        String regex = "[,]";
        StringIntergerPair pair = new StringIntergerPair("", 0);
        String[] array = str.split(regex);
        int i = 0;
        for (String string : array) {
            array[i] = string.replaceAll(",", "");
            i++;
        }
        pair.string = array[0];
        pair.interger = Integer.parseInt(array[1]);
        return pair;
    }

}
