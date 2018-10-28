package testing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class trying {
    public static void main(String[] args) throws Exception {
    	ArrayList<String> interactionSet = new ArrayList<>();
    	interactionSet.add("asd");
    	interactionSet.add("asd");
    	interactionSet.add("asd");
    	interactionSet.add("asd");
    	interactionSet.add("dsd");
    	interactionSet = (ArrayList<String>) interactionSet.stream().distinct().collect(Collectors.toCollection(ArrayList::new));
    	System.out.print(interactionSet);
    }
}