package tracks.ruleGeneration.cylinderP406;

import java.util.ArrayList;

public class Interaction {
	
	public ArrayList<String> first;
	public ArrayList<String> second;
	public String interactionType;
	
	public String pair = "";
	
	public Interaction(ArrayList<String> _first,ArrayList<String> _second,String _interactionType) {
		first = _first;
		second = _second;
		interactionType = _interactionType;
	}
	
	public Interaction(String _interactionType) {
		first = null;
		second = null;
		interactionType = _interactionType;
	}
	
	public ArrayList<String> ToInteractionString(){
		ArrayList<String> Is = new ArrayList<String>();
		if(first==null || second == null) {
			Is.add(interactionType);
			return Is;
		}
		else {
			for(String f:first) {
				String I = f;
				for(String s:second) {
					I = I + " " + s;
				}
				I = I + " < " + interactionType;
				Is.add(I);
			}
			return Is;
		}
	}

}
