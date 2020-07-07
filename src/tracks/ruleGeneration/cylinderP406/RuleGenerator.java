package tracks.ruleGeneration.cylinderP406;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import core.game.GameDescription.SpriteData;
import core.competition.CompetitionParameters;
import core.game.SLDescription;
import core.game.StateObservation;
import core.generator.AbstractRuleGenerator;
import core.logging.Logger;
import core.player.AbstractPlayer;
import core.vgdl.VGDLRegistry;
import tools.ElapsedCpuTimer;
import tools.LevelAnalyzer;

/*
 * cylinderP406
 * 2019/07/31
 * */

public class RuleGenerator extends AbstractRuleGenerator{
	public static final String BEST_AGENT_NAME = "tracks.singlePlayer.advanced.olets.Agent";
	public static final String NAIVE_AGENT_NAME = "tracks.singlePlayer.advanced.sampleMCTS.Agent";
	public static final String RANDOM_AGENT_NAME = "tracks.singlePlayer.simple.simpleRandom.Agent";
	public static final String DO_NOTHING_AGENT_NAME = "tracks.singlePlayer.simple.doNothing.Agent";
	
	public static long EVALUATION_TIME = 10000;
	public static double PICK_PROB = 0.7;
	
	private double worstTime;
	private int CheckNum=0;
	
	private tracks.ruleGeneration.constructiveRuleGenerator.RuleGenerator constGen;
	
	private int InteractionNumMin = 8;
	
	private ArrayList<String> usefulSprites;
	private ArrayList<String> avatarSprites;
	private ArrayList<String> movableSprites;
	private ArrayList<String> immovableSprites;
	private ArrayList<String> doorSprites;
	private ArrayList<String> resourceSprites;
	private ArrayList<String> npcSprites;
	private ArrayList<String> borderSprites;
	
	private ArrayList<ArrayList<String>> spritePowerSet;
	
	public HashMap<Integer,String> itypeToName;
	public ArrayList<String> collidedPairs;
	
	private static Random random;
	
	//private String target = null;
	
	public static LevelAnalyzer la;
	
	public boolean LoopFlag=true;
	
	private String[] availableInteractions = new String[] { 
			"killSprite","killIfFromAbove","stepBack","cloneSprite","transformTo stype=<@stype@> forceOrientation=<@bool@>","flipDirection"
	};

	/*private String[] availableInteractions_OLD = new String[] { 
			"killSprite", "killAll stype=<@stype@>", "killIfHasMore resource=<@resource@> limit=<@limit@>", "killIfHasLess resource=<@resource@> limit=<@limit@>",
			"killIfFromAbove", "killIfOtherHasMore resource=<@resource@> limit=<@limit@>", "spawnBehind stype=<@stype@>", "stepBack", "spawnIfHasMore stype=<@stype@> resource=<@resource@> limit=<@limit@>", "spawnIfHasLess stype=<@stype@> resource=<@resource@> limit=<@limit@>",
			"cloneSprite", "transformTo stype=<@stype@> forceOrientation=<@bool@>", "flipDirection", /*"transformToRandomChild  stype=<@stype@>", "updateSpawnType stype=<@stype@> spawnPoint=<@stype@>",
			"removeScore  stype=<@stype@>", "addHealthPoints value=<@value@>", "addHealthPointsToMax value=<@value@>", "reverseDirection", "subtractHealthPoints stype=<@stype@> value=<@value@>",
			"increaseSpeedToAll stype=<@stype@> value=<@value@>", "decreaseSpeedToAll stype=<@stype@> value=<@value@>", "attractGaze", "align", "turnAround", "wrapAround",
			"pullWithIt", "bounceForward",/* "teleportToExit", "collectResource", "setSpeedForAll stype=<@stype@> value=<@value@>",// "undoAll",
			"reverseDirection", "changeResource resource=<@resource@> value=<@value@>" };*/
	/*private String[] killingInteractions = new String[] { 
			"killSprite", "killAll stype=<@stype@>", "killIfHasMore resource=<@resource@> limit=<@limit@>", "killIfHasLess resource=<@resource@> limit=<@limit@>",
			"killIfFromAbove", "killIfOtherHasMore resource=<@resource@> limit=<@limit@>"};*/
	
	private ArrayList<Interaction> terminations;
	private ArrayList<Interaction> interactions;
	
	private void constructAgent(SLDescription sl){
		try{
			Class agentClass = Class.forName(BEST_AGENT_NAME);
			Constructor agentConst = agentClass.getConstructor(new Class[]{StateObservation.class, ElapsedCpuTimer.class});
			Chromosome.automatedAgent = (AbstractPlayer)agentConst.newInstance(sl.testRules(new String[]{}, new String[]{}), null);
		}
		catch(Exception e){
			e.printStackTrace();
		}

		try{
			Class agentClass = Class.forName(NAIVE_AGENT_NAME);
			Constructor agentConst = agentClass.getConstructor(new Class[]{StateObservation.class, ElapsedCpuTimer.class});
			Chromosome.naiveAgent = (AbstractPlayer)agentConst.newInstance(sl.testRules(new String[]{}, new String[]{}), null);
		}
		catch(Exception e){
			e.printStackTrace();
		}

		try{
			Class agentClass = Class.forName(DO_NOTHING_AGENT_NAME);
			Constructor agentConst = agentClass.getConstructor(new Class[]{StateObservation.class, ElapsedCpuTimer.class});
			Chromosome.doNothingAgent = (AbstractPlayer)agentConst.newInstance(sl.testRules(new String[]{}, new String[]{}), null);
		}
		catch(Exception e){
			e.printStackTrace();
		}
		try{
			Class agentClass = Class.forName(RANDOM_AGENT_NAME);
			Constructor agentConst = agentClass.getConstructor(new Class[]{StateObservation.class, ElapsedCpuTimer.class});
			Chromosome.randomAgent = (AbstractPlayer)agentConst.newInstance(sl.testRules(new String[]{}, new String[]{}), null);
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

	/**
	 * This is an evolutionary rule generator
	 * @param sl	contains information about sprites and current level
	 * @param time	amount of time allowed to generate
	 */
	public RuleGenerator(SLDescription sl, ElapsedCpuTimer time) {
		collidedPairs = new ArrayList<String>();
		random = new Random();
		la = new LevelAnalyzer(sl);
		
		usefulSprites = new ArrayList<String>();


		String[][] currentLevel = sl.getCurrentLevel();
		SpriteData[] extraSprites = sl.getGameSprites();
		
		itypeToName = new HashMap<Integer,String>();
		for(SpriteData sData :sl.getGameSprites()) {
			itypeToName.put(VGDLRegistry.GetInstance().getRegisteredSpriteValue(sl.decodeName(sData.name,CompetitionParameters.randomSeed)), sData.name);
		}
		
		
		// Just get the useful sprites from the current level
		for (int y = 0; y < currentLevel.length; y++) {
			for (int x = 0; x < currentLevel[y].length; x++) {
				String[] parts = currentLevel[y][x].split(",");
				for (int i = 0; i < parts.length; i++) {
					if (parts[i].trim().length() > 0) {
						// Add the sprite if it doesn't exisit
						if (!usefulSprites.contains(parts[i].trim())) {
							usefulSprites.add(parts[i].trim());
						}
					}
				}
			}
		}
		String target = usefulSprites.get(random.nextInt(usefulSprites.size()-1));
		
		boolean flag = true;
		while(flag) {
			flag = false;
			ArrayList<String> temp = new ArrayList<String>();
			for(String u_s : usefulSprites) {
				System.out.println(u_s);
				String[] parameters = getSpriteData(extraSprites,u_s).toString().split(" ",0);
				for(String p:parameters) {
					if(p.matches(".*stype=.*")) {
						p = p.replace("stype=", "");
						if(!usefulSprites.contains(p)) {
							temp.add(p);
							flag = true;
						}
					}
				}
			}
			usefulSprites.addAll(temp);
		}

		
		usefulSprites.add("EOS");

		for(String s:usefulSprites) {
			System.out.println(s);
		}
		avatarSprites = spreiteDataToString(la.getAvatars(false));
		movableSprites = spreiteDataToString(la.getMovables(false));
		immovableSprites = spreiteDataToString(la.getImmovables(false));
		doorSprites = spreiteDataToString(la.getPortals(false));
		resourceSprites = spreiteDataToString(la.getResources(false));
		borderSprites = spreiteDataToString(la.getBorderObjects(0, 10000));
		npcSprites = spreiteDataToString(la.getNPCs(false));
		
		spritePowerSet = new ArrayList<ArrayList<String>>();
		for(String s:usefulSprites) {
			ArrayList<String> set = new ArrayList<String>();
			set.add(s);
			spritePowerSet.add(set);
		}
		spritePowerSet.add(avatarSprites);
		spritePowerSet.add(movableSprites);
		spritePowerSet.add(immovableSprites);
		spritePowerSet.add(doorSprites);
		spritePowerSet.add(resourceSprites);
		spritePowerSet.add(borderSprites);
		spritePowerSet.add(npcSprites);
		
		constructAgent(sl);
		
		constGen = new tracks.ruleGeneration.constructiveRuleGenerator.RuleGenerator(sl, time);
		String[][] initialRule = constGen.generateRules(sl, time);
		Chromosome.spriteSetStruct=constGen.getSpriteSetStructure();
		Chromosome.random = random;
		
		String[][] curreentRule = {{""},{""}};
		Chromosome currentChromosome = new Chromosome(curreentRule,sl);
		currentChromosome.setLogging(true);
		currentChromosome.calculateFitnessLight(EVALUATION_TIME);
		
		List<Entry<String, Integer>> list_entries = new ArrayList<Entry<String, Integer>>(currentChromosome.Log.entrySet());
		Collections.sort(list_entries, new Comparator<Entry<String, Integer>>() {
			public int compare(Entry<String, Integer> obj1, Entry<String, Integer> obj2) {
				return obj1.getValue().compareTo(obj2.getValue());
			}
		});
		collidedPairs.clear();
		for(Entry<String, Integer> entry : list_entries) {
			collidedPairs.add(entry.getKey());
		}
		
		
		terminations = new ArrayList<Interaction>();
		interactions = new ArrayList<Interaction>();
		for (String r:collidedPairs) {
			Interaction I = createInteraction();
			interactions.add(I);
		}
		for (String r:initialRule[1]) {
			for(String p:r.split(" ",0)) {
				if(p.matches(".*stype=.*")) {
					p = p.replace("stype=", "");
					if(!usefulSprites.contains(p)) {
						r=r.replace(p, target);
					}
				}
			}
			Interaction I = new Interaction(r);
			terminations.add(I);
		}
		
		while(interactions.size()<InteractionNumMin) {
			interactions.add(createInteraction());
			sl.testRules(toStringArray(interactions),toStringArray(terminations));
			while(sl.getErrors().size() > 0){
				interactions.remove(interactions.size()-1);
				interactions.add(createInteraction());
				sl.testRules(toStringArray(interactions),toStringArray(terminations));
			}
			collidedPairs.remove(interactions.get(interactions.size()-1).pair);
		}
	}

	/**
	 * Generates the rules using evolution
	 * @param sl	the SL description
	 * @param time	the time allowed for the generator to loop
	 */
	@Override
	public String[][] generateRules(SLDescription sl, ElapsedCpuTimer time) {
		FileWriter filewriter = null;

		File file = new File(CompetitionParameters.randomSeed+"log.txt");
		try {
			filewriter = new FileWriter(file);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		
		String[][] r = {toStringArray(interactions),toStringArray(terminations)};
		
		int numberOfIterations = 0;
		Chromosome bestChromosome = new Chromosome(r,sl);
		ArrayList<Chromosome> newChromosomes = new ArrayList<Chromosome>();
		Logger.getInstance().active = false;
		worstTime = 4 * EVALUATION_TIME/* * interactions.size()*/;
		
		evolution: while(true) {
			numberOfIterations++;
			System.out.println("\nGeneration #" + numberOfIterations + ": ");
			newChromosomes.clear();
			String[][] curreentRule = {toStringArray(interactions),toStringArray(terminations)};
			Chromosome currentChromosome = new Chromosome(curreentRule,sl);
			currentChromosome.setLogging(true);
			currentChromosome.calculateFitnessLight(EVALUATION_TIME);
			try {
				filewriter.write(time.elapsedMillis()+","+currentChromosome.getFitness().get(0).toString()+","+currentChromosome.getFitness().get(1).toString()+","+currentChromosome.getFitness().get(2).toString());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			List<Entry<String, Integer>> list_entries = new ArrayList<Entry<String, Integer>>(currentChromosome.Log.entrySet());
			Collections.sort(list_entries, new Comparator<Entry<String, Integer>>() {
				public int compare(Entry<String, Integer> obj1, Entry<String, Integer> obj2) {
					return obj1.getValue().compareTo(obj2.getValue());
				}
			});
			collidedPairs.clear();
			for(Entry<String, Integer> entry : list_entries) {
				collidedPairs.add(entry.getKey());
			}
			for(Interaction I :interactions) {
				if(I.pair != "") {
					if(collidedPairs.contains(I.pair)) {
						collidedPairs.remove(I.pair);
					}
				}
			}
			System.out.println(collidedPairs.size());
			
			for (int i = 0; i < interactions.size(); i++) {
				//ArrayList<Interaction> copy = (ArrayList<Interaction>) interactions.clone();
				//temp.add(avatar.get(0).name+" EOS > stepBack");
				String[][] rule = {toStringArray(interactions,i),toStringArray(terminations)};
				Chromosome c = new Chromosome(rule,sl,i);
				if(c.calculateFitnessLight(EVALUATION_TIME)) {
					if(checkTimeLimit(time)) {
						break evolution;
					}
				}
				if(bestChromosome.compareTo(c)<0) {
					bestChromosome = c;
				}
				newChromosomes.add(c);
			}
			Collections.shuffle(newChromosomes);
			Collections.sort(newChromosomes);
			
			for(int i=0; i < 200; i++) {
				Chromosome target = newChromosomes.get(0);
				for(int j=0; j<newChromosomes.size(); j++) {
					if(random.nextDouble()<=PICK_PROB) {
						target = newChromosomes.get(j);
						break;
					}
				}
				ArrayList<Interaction> copy = (ArrayList<Interaction>) interactions.clone();
				copy.set(target.id, createInteraction());
				String[][] rule = {toStringArray(copy),toStringArray(terminations)};
				Chromosome c = new Chromosome(rule,sl);
				if(c.calculateFitnessLight(EVALUATION_TIME)) {
					if(checkTimeLimit(time)) {
						break evolution;
					}
				}
				if(currentChromosome.compareTo(c)>0) {
					interactions = copy;
					if(bestChromosome.compareTo(c)>0) {
						bestChromosome = c;
						continue evolution;
					}
				}
				System.out.print("*");
			}
		}
		try {
			filewriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return bestChromosome.getRuleset();
	}

	

	private SpriteData getSpriteData(SpriteData[] gameSprites, String spriteName){
		for(int i=0; i<gameSprites.length; i++){
			if(gameSprites[i].name.equals(spriteName)){
				return gameSprites[i];
			}
		}
		return null;
	}
	
	private ArrayList<String> spreiteDataToString(SpriteData[] Sprites){
		ArrayList<String> spriteNames = new ArrayList<String>();
		for(SpriteData s:Sprites){
			if(usefulSprites.contains(s.name)) {
				spriteNames.add(s.name);
			}
		}
		return spriteNames;
	}
	
	private boolean checkTimeLimit(ElapsedCpuTimer time) {
		CheckNum++;
		long avgTime = time.elapsedMillis()/CheckNum;
		long remainingTime = time.remainingTimeMillis();
		return (remainingTime<avgTime || remainingTime<worstTime);
	}
	
	private Interaction createInteraction() {
		
		if(collidedPairs.size()==0) {
			return OLD_createInteraction();
		}
		String pair = collidedPairs.get(0);
		for(String p :collidedPairs) {
			if(random.nextDouble()<0.6) {
				pair = p;
			}
		}
		return createInteraction(pair);
	}
	
	private Interaction createInteraction(String _pair) {
		
		String pair = _pair;
		
		int i1 = Integer.parseInt(pair.split(",")[0]);
		int i2 = Integer.parseInt(pair.split(",")[1]);
		if(random.nextBoolean()) {
			int tmp = i1;
			i1 = i2;
			i2 = tmp;
		}
		ArrayList<String> first = new ArrayList<String>();
		ArrayList<String> second = new ArrayList<String>();
		first.add(itypeToName.get(i1));
		second.add(itypeToName.get(i2));
		String newInteraction = "";
		do {
			String scoreChange = "";
			if(random.nextBoolean()){
				scoreChange += "scoreChange=" + (random.nextInt(5) - 2)+" ";
			}
			newInteraction = (this.availableInteractions[random.nextInt(this.availableInteractions.length)] + " " + scoreChange);
			while (newInteraction.contains("<@stype@>")) {
				newInteraction = newInteraction.replaceFirst("<@stype@>", usefulSprites.get(random.nextInt(usefulSprites.size()-1)));
			}
			if (resourceSprites.size()>0) {
				while (newInteraction.contains("<@resource@>")) {
					newInteraction = newInteraction.replaceFirst("<@resource@>", resourceSprites.get(random.nextInt(resourceSprites.size())));
				}
			}
			while (newInteraction.contains("<@limit@>")) {
				newInteraction = newInteraction.replaceFirst("<@limit@>", ""+random.nextInt(10));
			}
			while (newInteraction.contains("<@value@>")) {
				newInteraction = newInteraction.replaceFirst("<@value@>", ""+random.nextInt(10));
			}
			while (newInteraction.contains("<@bool@>")) {
				if (random.nextBoolean()) {
					newInteraction = newInteraction.replaceFirst("<@bool@>", "true");
				}
				else {
					newInteraction = newInteraction.replaceFirst("<@bool@>", "false");
				}
			}
		}while(newInteraction.contains("<@"));
		Interaction I = new Interaction(spritePowerSet.get(i1),spritePowerSet.get(i2),newInteraction);
		I.pair = pair;
		return I;
	}

	private Interaction OLD_createInteraction() {
		String newInteraction = "";
		int i1 = random.nextInt(spritePowerSet.size());
		int i2 = (i1 + 1 + random.nextInt(spritePowerSet.size() - 1)) % spritePowerSet.size();
		do {
			String scoreChange = "";
			if(random.nextBoolean()){
				scoreChange += "scoreChange=" + (random.nextInt(5) - 2)+" ";
			}
			newInteraction = (this.availableInteractions[random.nextInt(this.availableInteractions.length)] + " " + scoreChange);
			while (newInteraction.contains("<@stype@>")) {
				newInteraction = newInteraction.replaceFirst("<@stype@>", usefulSprites.get(random.nextInt(usefulSprites.size()-1)));
			}
			if (resourceSprites.size()>0) {
				while (newInteraction.contains("<@resource@>")) {
					newInteraction = newInteraction.replaceFirst("<@resource@>", resourceSprites.get(random.nextInt(resourceSprites.size())));
				}
			}
			while (newInteraction.contains("<@limit@>")) {
				newInteraction = newInteraction.replaceFirst("<@limit@>", ""+random.nextInt(10));
			}
			while (newInteraction.contains("<@value@>")) {
				newInteraction = newInteraction.replaceFirst("<@value@>", ""+random.nextInt(10));
			}
			while (newInteraction.contains("<@bool@>")) {
				if (random.nextBoolean()) {
					newInteraction = newInteraction.replaceFirst("<@bool@>", "true");
				}
				else {
					newInteraction = newInteraction.replaceFirst("<@bool@>", "false");
				}
			}
		}while(newInteraction.contains("<@"));
		return new Interaction(spritePowerSet.get(i1),spritePowerSet.get(i2),newInteraction);
	}

	
	private String[] toStringArray(ArrayList<Interaction> Ilist) {
		ArrayList<String> arlist = new ArrayList<String>();
		for(Interaction I:Ilist) {
			arlist.addAll(I.ToInteractionString());
		}
		String[] str = new String[arlist.size()];
		arlist.toArray(str);
		return str;
	}
	private String[] toStringArray(ArrayList<Interaction> Ilist, int target2) {
		ArrayList<String> arlist = new ArrayList<String>();
		for(int i=0;i<Ilist.size();i++) {
			if(i!=target2) {
				arlist.addAll(Ilist.get(i).ToInteractionString());
			}
		}
		String[] str = new String[arlist.size()];
		arlist.toArray(str);
		return str;
	}
}
