package tracks.ruleGeneration.cylinderP406;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import javax.print.attribute.Size2DSyntax;

import core.game.GameDescription.SpriteData;
import core.game.SLDescription;
import core.game.StateObservation;
import core.generator.AbstractRuleGenerator;
import core.logging.Logger;
import core.player.AbstractPlayer;
import core.termination.Termination;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.LevelAnalyzer;

public class RuleGenerator extends AbstractRuleGenerator{
	public static final String BEST_AGENT_NAME = "tracks.singlePlayer.advanced.olets.Agent";
	/**
	 * The name of a naive agent
	 */
	public static final String NAIVE_AGENT_NAME = "tracks.singlePlayer.advanced.sampleMCTS.Agent";
	/**
	 * The name of the random agent
	 */
	public static final String RANDOM_AGENT_NAME = "tracks.singlePlayer.simple.simpleRandom.Agent";
	/**
	 * The name of the do nothing agent
	 */
	public static final String DO_NOTHING_AGENT_NAME = "tracks.singlePlayer.simple.doNothing.Agent";
	
	public static long EVALUATION_TIME = 10000;
	public static double PICK_PROB = 0.7;
	
	private double worstTime;
	//private double avgTime;
	//private double avgcalcFitTime;
	//private double totalTime;
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
	
	private static Random random;
	
	private String target = null;
	
	public static LevelAnalyzer la;
	
	public boolean LoopFlag=true;

	private String[] availableInteractions = new String[] { 
			"killSprite", "killAll stype=<@stype@>", "killIfHasMore resource=<@resource@> limit=<@limit@>", "killIfHasLess resource=<@resource@> limit=<@limit@>",
			"killIfFromAbove", "killIfOtherHasMore resource=<@resource@> limit=<@limit@>", "spawnBehind stype=<@stype@>", "stepBack", "spawnIfHasMore stype=<@stype@> resource=<@resource@> limit=<@limit@>", "spawnIfHasLess stype=<@stype@> resource=<@resource@> limit=<@limit@>",
			"cloneSprite", "transformTo stype=<@stype@> forceOrientation=<@bool@>", "flipDirection", /*"transformToRandomChild  stype=<@stype@>", "updateSpawnType stype=<@stype@> spawnPoint=<@stype@>",*/
			"removeScore  stype=<@stype@>", "addHealthPoints value=<@value@>", "addHealthPointsToMax value=<@value@>", "reverseDirection", "subtractHealthPoints stype=<@stype@> value=<@value@>",
			"increaseSpeedToAll stype=<@stype@> value=<@value@>", "decreaseSpeedToAll stype=<@stype@> value=<@value@>", "attractGaze", "align", "turnAround", "wrapAround",
			"pullWithIt", "bounceForward",/* "teleportToExit",*/ "collectResource", "setSpeedForAll stype=<@stype@> value=<@value@>",// "undoAll",
			"reverseDirection", "changeResource resource=<@resource@> value=<@value@>" };
	private String[] killingInteractions = new String[] { 
			"killSprite", "killAll stype=<@stype@>", "killIfHasMore resource=<@resource@> limit=<@limit@>", "killIfHasLess resource=<@resource@> limit=<@limit@>",
			"killIfFromAbove", "killIfOtherHasMore resource=<@resource@> limit=<@limit@>"};
	
	private ArrayList<Interaction> terminations;
	private ArrayList<Interaction> interactions;
	
	/**
	 * initialize the agents used during evaluating the chromosome
	 */
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
		
		random = new Random();
		la = new LevelAnalyzer(sl);
		
		usefulSprites = new ArrayList<String>();


		String[][] currentLevel = sl.getCurrentLevel();
		SpriteData[] extraSprites = sl.getGameSprites();
		
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
		
		terminations = new ArrayList<Interaction>();
		interactions = new ArrayList<Interaction>();
		for (String r:initialRule[0]) {
			Interaction I = new Interaction(r);
			interactions.add(I);
		}
		for (String r:initialRule[1]) {
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
		}
	}

	/**
	 * Generates the rules using evolution
	 * @param sl	the SL description
	 * @param time	the time allowed for the generator to loop
	 */
	@Override
	public String[][] generateRules(SLDescription sl, ElapsedCpuTimer time) {
		
		
		
		String[][] r = {toStringArray(interactions),toStringArray(terminations)};
		
		/*if(LoopFlag==false) {
			return r;
		}*/
		int numberOfIterations = 0;
		Chromosome bestChromosome = new Chromosome(r,sl);
		ArrayList<Chromosome> newChromosomes = new ArrayList<Chromosome>();
		Logger.getInstance().active = false;
		worstTime = 4 * EVALUATION_TIME * interactions.size();
		
		evolution: while(true) {
			System.out.println("Generation #" + (numberOfIterations + 1) + ": ");
			newChromosomes.clear();
			for (int i = 0; i < interactions.size(); i++) {
				//ArrayList<Interaction> copy = (ArrayList<Interaction>) interactions.clone();
				//copy.remove(i);
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
				if(target.compareTo(c)>0) {
					interactions = copy;
					if(bestChromosome.compareTo(c)>0) {
						bestChromosome = c;
					}
				}
				System.out.print("*");
			}
		}
		
		/*avgTime = worstTime;
		avgcalcFitTime = 4 * EVALUATION_TIME;
		totalTime = 0;*/
		int calculateFitNum = 0;
		
		int numberOfGoodGens = 0;
		int mutatedInteractions[] = {-1,-1};
/*
		// START EVO LOOP
		ArrayList<Chromosome> chromosomes = new ArrayList<Chromosome>();
		//ArrayList<Chromosome> newChromosomes = new ArrayList<Chromosome>();
		Logger.getInstance().active = false;
		while(time.remainingTimeMillis() > 4 * avgTime && time.remainingTimeMillis() > worstTime && (time.remainingTimeMillis() > InteractionNum * avgcalcFitTime * 2 || calculateFitNum == 0)){
			int remainingcalcFitTime = (int) ( (time.remainingTimeMillis() / avgcalcFitTime) - InteractionNum*2);
			ElapsedCpuTimer timer = new ElapsedCpuTimer();
			System.out.println("Generation #" + (numberOfIterations + 1) + "(" + numberOfGoodGens + ")" + ": ");
			chromosomes.clear();
			for (int i = 0; i < InteractionNum; i++) {
				//if(mutatedInteractions[0]!=i && mutatedInteractions[1]!=i) {
					ArrayList<String> temp = new ArrayList<String>();
					temp = (ArrayList<String>) interactions.clone();
					temp.remove(i);
					temp.add(avatar.get(0).name+" EOS > stepBack");
					String[][] rules = {toStringArray(temp),toStringArray(terminations)};
					Chromosome c = new Chromosome(rules,sl,i);
					if(c.calculateFitnessLight(SharedData.EVALUATION_TIME)) {
						calculateFitNum++;
					}
					chromosomes.add(c);
					if(bestChromosome.compareTo(c)<0) {
						bestChromosome = c;
					}
					System.out.print("*");
				//}
			}
			System.out.println();*/
			/*for (Chromosome c : newChromosomes) {
				chromosomes.add(c);
			}*/
		/*

			Collections.shuffle(chromosomes);
			Collections.sort(chromosomes);
			for(int i=0;i<2;i++) {
				mutatedInteractions[i] = chromosomes.get(i).id;
			}

			for(Chromosome c:chromosomes) {
				System.out.print("[");
				for(Double f:c.getFitness()) {
					System.out.print(f+",");
				}
				System.out.print("]");
			}
			System.out.println();*/
			/******************************/
			//newChromosomes.clear();
			/*for (int i = 0; i < 1; i++) {
				Chromosome c = null;
				//Chromosome worst = chromosomes.get(InteractionNum-1);
				Chromosome worst = chromosomes.get(0);
				for(int j=0;j<remainingcalcFitTime;j++) {
					int id = chromosomes.get(0).id;
					for(int k=0;k<chromosomes.size();k++) {
						if(SharedData.random.nextInt(10)>1) {
							id = chromosomes.get(k).id;
							break;
						}
					}
					
					//System.out.print(i);
					ArrayList<String> temp = new ArrayList<String>();
					temp = (ArrayList<String>) interactions.clone();
					temp.set(id, createInteraction(usefulSprites,id));
					sl.testRules(toStringArray(temp),toStringArray(terminations));
					while(sl.getErrors().size() > 0){
						temp.set(id, createInteraction(usefulSprites,id));
						sl.testRules(toStringArray(temp),toStringArray(terminations));
					}
					
					//temp.remove(mutatedInteractions[(i+1)%2]);
					temp.add(avatar.get(0).name+" EOS > stepBack");
					String[][] rules = {toStringArray(temp),toStringArray(terminations)};
					c = new Chromosome(rules,sl,-1);
					if(c.calculateFitnessLight(SharedData.EVALUATION_TIME)) {
						calculateFitNum++;
					}
					if(bestChromosome.compareTo(c)<0) {
						bestChromosome = c;
					}
					System.out.print("c="+worst.compareTo(c)+" ");
					if (worst.compareTo(c)>0) {
						remainingcalcFitTime = remainingcalcFitTime - (j+1);
						interactions = temp;
						interactions.remove(interactions.size()-1);
						break;
					}
					System.out.print("{");
					for(Double f:c.getFitness()) {
						System.out.print(f+",");
					}
					System.out.print("}");
				}
				//newChromosomes.add(c);

			}*/
			/***********************/
			/*numberOfIterations += 1;
			if(calculateFitNum != 0) {
				numberOfGoodGens++;
				totalTime += timer.elapsedMillis();
				avgTime = totalTime / numberOfGoodGens;
				avgcalcFitTime = totalTime/calculateFitNum;
			}
			
		}

		for (int i = 0; i < 1; i++) {
			interactions.set(chromosomes.get(i).id, "");
		}
		/*
		for(String s:interactions) {
			System.out.println(s);
		}
		for(String s:terminations) {
			System.out.println(s);
		}
		String[][] rules = {toStringArray(interactions),toStringArray(terminations)};*/
		//Chromosome c = new Chromosome(rules,sl,0);
		//c.calculateFitness(0);
		//System.out.println(c.getFitness());

		//String[][] rules = r;//bestChromosome.getRuleset();

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
	
	/*private String createInteraction(ArrayList<String> usefulSprites,int i) {
		if (i==0) {
			return createGoalInteraction(usefulSprites, target);
		}
		else {
			return createInteraction(usefulSprites);
		}
	}*/

	private Interaction createInteraction() {
		String newInteraction = "";
		int i1 = random.nextInt(spritePowerSet.size());
		int i2 = (i1 + 1 + random.nextInt(spritePowerSet.size() - 1)) % spritePowerSet.size();
		do {
			// add score change parameter for interactions
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
		// add the new random interaction that doesn't produce errors
		return new Interaction(spritePowerSet.get(i1),spritePowerSet.get(i2),newInteraction);
		//System.out.println(interaction);
	}
/*
	private String createGoalInteraction(ArrayList<String> usefulSprites,String target) {
		String interaction = "";
		do {
			interaction = (target + " " + usefulSprites.get(SharedData.random.nextInt(usefulSprites.size())) + " > " +
					this.availableKillingInteractions[SharedData.random.nextInt(this.availableKillingInteractions.length)]);
			while (interaction.contains("<@stype@>")) {
				interaction = interaction.replaceFirst("<@stype@>", target);
			}
			if (resource.size()>0) {
				while (interaction.contains("<@resource@>")) {
					interaction = interaction.replaceFirst("<@resource@>", resource.get(SharedData.random.nextInt(resource.size())).name);
				}
			}
			while (interaction.contains("<@limit@>")) {
				interaction = interaction.replaceFirst("<@limit@>", ""+SharedData.random.nextInt(10));
			}
			while (interaction.contains("<@value@>")) {
				interaction = interaction.replaceFirst("<@value@>", ""+SharedData.random.nextInt(10));
			}
			while (interaction.contains("<@bool@>")) {
				if (SharedData.random.nextBoolean()) {
					interaction = interaction.replaceFirst("<@bool@>", "true");
				}
				else {
					interaction = interaction.replaceFirst("<@bool@>", "false");
				}
			}
		}while(interaction.contains("<@"));
		// add the new random interaction that doesn't produce errors
		interaction = interaction + " scoreChange=10";
		System.out.println(interaction);
		return interaction;	
	}*/
/*
	private String[] toStringArray(ArrayList<String> arlist) {
		String[] str = new String[arlist.size()];
		arlist.toArray(str);
		return str;
	}*/
	
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
		for(int i=0;i<arlist.size();i++) {
			if(i!=0) {
				arlist.addAll(Ilist.get(i).ToInteractionString());
			}
		}
		String[] str = new String[arlist.size()];
		arlist.toArray(str);
		return str;
	}
}
