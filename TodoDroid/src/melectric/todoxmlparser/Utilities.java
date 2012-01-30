package melectric.todoxmlparser;

public class Utilities {
    public static String ReplaceApostrophe(String searchString){
        int index = 0;
        String StrOut = "";

        //account for any apostrophes in the parameter
        for (index = searchString.indexOf("'"); index != -1; index = searchString.indexOf("\'")) {
        // Copy up to the apostrophe
        StrOut += searchString.substring(0, index);

        // Add double apostrophe
        StrOut += "''";
        searchString = searchString.substring(index + 1);
        //Chop off "used" part
        }
        StrOut += searchString;
        // Add the left over part. (Whole thing, if there was no ')
        return StrOut;
        }
}
