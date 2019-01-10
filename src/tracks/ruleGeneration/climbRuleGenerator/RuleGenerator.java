package tracks.ruleGeneration.climbRuleGenerator;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

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
	/** The best chromosome fitness across generations **/
	private ArrayList<Double> bestFitness;
	/** number of feasible chromosomes across generations **/
	private ArrayList<Integer> numOfFeasible;
	/** number of infeasible chromosomes across generations **/
	private ArrayList<Integer> numOfInFeasible;

	private int InteractionNum = 8;

	private ArrayList<SpriteData> avatar;
	private ArrayList<SpriteData> door;
	private ArrayList<SpriteData> resource;
	private ArrayList<SpriteData> fleeing;
	private ArrayList<SpriteData> npc;

	private String target = null;

	public boolean LoopFlag=true;

	private String[] availableInteractions = new String[] { 
			"killSprite", "killAll stype=<@stype@>", "killIfHasMore resource=<@resource@> limit=<@limit@>", "killIfHasLess resource=<@resource@> limit=<@limit@>",
			"killIfFromAbove", "killIfOtherHasMore resource=<@resource@> limit=<@limit@>", "spawnBehind stype=<@stype@>", "stepBack", "spawnIfHasMore stype=<@stype@> resource=<@resource@> limit=<@limit@>", "spawnIfHasLess stype=<@stype@> resource=<@resource@> limit=<@limit@>",
			"cloneSprite", "transformTo stype=<@stype@> forceOrientation=<@bool@>", "flipDirection", /*"transformToRandomChild  stype=<@stype@>", "updateSpawnType stype=<@stype@> spawnPoint=<@stype@>",*/
			"removeScore  stype=<@stype@>", "addHealthPoints value=<@value@>", "addHealthPointsToMax value=<@value@>", "reverseDirection", "subtractHealthPoints stype=<@stype@> value=<@value@>",
			"increaseSpeedToAll stype=<@stype@> value=<@value@>", "decreaseSpeedToAll stype=<@stype@> value=<@value@>", "attractGaze", "align", "turnAround", "wrapAround",
			"pullWithIt", "bounceForward",/* "teleportToExit",*/ "collectResource", "setSpeedForAll stype=<@stype@> value=<@value@>",// "undoAll",
			"reverseDirection", "changeResource resource=<@resource@> value=<@value@>" };
	private String[] availableKillingInteractions = new String[] { 
			"killSprite", "killAll stype=<@stype@>", "killIfHasMore resource=<@resource@> limit=<@limit@>", "killIfHasLess resource=<@resource@> limit=<@limit@>",
			"killIfFromAbove", "killIfOtherHasMore resource=<@resource@> limit=<@limit@>"
	};
	/**
	 * initialize the agents used during evaluating the chromosome
	 */
	private void constructAgent(SLDescription sl){
		try{
			Class agentClass = Class.forName(SharedData.BEST_AGENT_NAME);
			Constructor agentConst = agentClass.getConstructor(new Class[]{StateObservation.class, ElapsedCpuTimer.class});
			SharedData.automatedAgent = (AbstractPlayer)agentConst.newInstance(sl.testRules(new String[]{}, new String[]{}), null);
		}
		catch(Exception e){
			e.printStackTrace();
		}

		try{
			Class agentClass = Class.forName(SharedData.NAIVE_AGENT_NAME);
			Constructor agentConst = agentClass.getConstructor(new Class[]{StateObservation.class, ElapsedCpuTimer.class});
			SharedData.naiveAgent = (AbstractPlayer)agentConst.newInstance(sl.testRules(new String[]{}, new String[]{}), null);
		}
		catch(Exception e){
			e.printStackTrace();
		}

		try{
			Class agentClass = Class.forName(SharedData.DO_NOTHING_AGENT_NAME);
			Constructor agentConst = agentClass.getConstructor(new Class[]{StateObservation.class, ElapsedCpuTimer.class});
			SharedData.doNothingAgent = (AbstractPlayer)agentConst.newInstance(sl.testRules(new String[]{}, new String[]{}), null);
		}
		catch(Exception e){
			e.printStackTrace();
		}
		try{
			Class agentClass = Class.forName(SharedData.RANDOM_AGENT_NAME);
			Constructor agentConst = agentClass.getConstructor(new Class[]{StateObservation.class, ElapsedCpuTimer.class});
			SharedData.randomAgent = (AbstractPlayer)agentConst.newInstance(sl.testRules(new String[]{}, new String[]{}), null);
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
		SharedData.usefulSprites = new ArrayList<String>();
		SharedData.random = new Random();
		SharedData.la = new LevelAnalyzer(sl);

		String[][] currentLevel = sl.getCurrentLevel();
		// Just get the useful sprites from the current level
		for (int y = 0; y < currentLevel.length; y++) {
			for (int x = 0; x < currentLevel[y].length; x++) {
				String[] parts = currentLevel[y][x].split(",");
				for (int i = 0; i < parts.length; i++) {
					if (parts[i].trim().length() > 0) {
						// Add the sprite if it doesn't exisit
						if (!SharedData.usefulSprites.contains(parts[i].trim())) {
							SharedData.usefulSprites.add(parts[i].trim());
						}
					}
				}
			}
		}
		SharedData.usefulSprites.add("EOS");
		constructAgent(sl);
		SharedData.constGen = new tracks.ruleGeneration.constructiveRuleGenerator.RuleGenerator(sl, time);
		SharedData.constGen.generateRules(sl, time);
	}

	/**
	 * Generates the rules using evolution
	 * @param sl	the SL description
	 * @param time	the time allowed for the generator to loop
	 */
	@Override
	public String[][] generateRules(SLDescription sl, ElapsedCpuTimer time) {
		SpriteData[] sprites = sl.getGameSprites();

		ArrayList<String> usefulSprites = new ArrayList<String>();

		String[][] currentLevel = sl.getCurrentLevel();
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
			for(SpriteData s : sprites) {
				if(usefulSprites.contains(s.name)) {
					String[] parameters = s.toString().split(" ",0);
					for(String p:parameters) {
						if(p.matches(".*stype=.*")) {
							p = p.replace("stype=", "");
							if(!usefulSprites.contains(p)) {
								usefulSprites.add(p);
								flag = true;
							}
						}
					}
				}
			}
		}
		usefulSprites.add("EOS");

		for(String s:usefulSprites) {
			System.out.println(s);
		}

		avatar = new ArrayList<SpriteData>();
		door = new ArrayList<SpriteData>();
		resource = new ArrayList<SpriteData>();
		fleeing = new ArrayList<SpriteData>();
		npc = new ArrayList<SpriteData>();
		for(SpriteData s : sprites) {
			System.out.println(s.name +"("+s.type+")");
			if(s.type.toLowerCase().matches(".*avatar.*")) {
				avatar.add(s);
			}
			if(s.type.toLowerCase().matches(".*door.*")) {
				System.out.println(s.type+"##");
				door.add(s);
			}
			if(s.type.toLowerCase().matches(".*resource.*")) {
				resource.add(s);
			}
			if(s.type.toLowerCase().matches(".*fleeing.*")) {
				fleeing.add(s);
			}
			if(s.type.toLowerCase().matches(".*chaser.*") || s.type.toLowerCase().matches(".*bomber.*") || s.type.toLowerCase().matches(".*npc.*") || s.type.toLowerCase().matches(".*spreader.*")) {
				if(usefulSprites.contains(s.name)) {
					npc.add(s);
				}
			}
			if(!usefulSprites.contains(s.name)) {
				s = null;
			}

		}

		ArrayList<String> terminations = new ArrayList<String>();
		ArrayList<String> interactions = new ArrayList<String>();

		if(door.size()>0) {
			ArrayList<SpriteData> targetSprites = door;
			int j = SharedData.random.nextInt(targetSprites.size());
			for(int i=0;i<targetSprites.size();i++) {
				j = (j+1)%targetSprites.size();
				if (usefulSprites.contains(targetSprites.get(j).name)) {
					target = targetSprites.get(j).name;
					terminations.add("SpriteCounter stype=" + targetSprites.get(j).name + " limit=0 win=True");
					break;
				}
			}
		}
		if(resource.size()>0 && target == null) {
			ArrayList<SpriteData> targetSprites = resource;
			int j = SharedData.random.nextInt(targetSprites.size());
			for(int i=0;i<targetSprites.size();i++) {
				j = (j+1)%targetSprites.size();
				if (usefulSprites.contains(targetSprites.get(j).name)) {
					target = targetSprites.get(j).name;
					terminations.add("SpriteCounter stype=" + targetSprites.get(j).name + " limit=0 win=True");
					break;
				}
			}
		}
		if(fleeing.size()>0 && target == null) {
			ArrayList<SpriteData> targetSprites = fleeing;
			int j = SharedData.random.nextInt(targetSprites.size());
			for(int i=0;i<targetSprites.size();i++) {
				j = (j+1)%targetSprites.size();
				if (usefulSprites.contains(targetSprites.get(j).name)) {
					target = targetSprites.get(j).name;
					terminations.add("SpriteCounter stype=" + targetSprites.get(j).name + " limit=0 win=True");
					break;
				}
			}
		}
		if(npc.size()>0 && target == null) {
			int i = SharedData.random.nextInt(npc.size());
			terminations.add("SpriteCounter stype=" + npc.get(i).name + " limit=0 win=True");
			target = npc.get(i).name;
		}
		if(target == null){
			target = usefulSprites.get(SharedData.random.nextInt(usefulSprites.size()-1));
			terminations.add("SpriteCounter stype=" + target + " limit=0 win=True");
		}
		terminations.add("SpriteCounter stype=" + avatar.get(0).name + " limit=0 win=False");
		/*door resource fleeing chaser bomber&Random&Spreader*/
		for(int i=0;i<10;i++) {
			interactions.add(createInteraction(usefulSprites,i));
			sl.testRules(toStringArray(interactions),toStringArray(terminations));
			while(sl.getErrors().size() > 0){
				interactions.remove(i);
				interactions.add(createInteraction(usefulSprites,i));
				sl.testRules(toStringArray(interactions),toStringArray(terminations));
			}
		}
		/*for (int i = 0; i < InteractionNum; i++) {
			ArrayList<String> temp = new ArrayList<String>();
			temp = (ArrayList<String>) interactions.clone();
			temp.remove(i);
			temp.add(avatar.get(0).name+" EOS > stepBack");
			String[][] rules = {toStringArray(temp),toStringArray(terminations)};
			Chromosome c = new Chromosome(rules,sl,i);
			if(c.calculateFitnessLight(SharedData.EVALUATION_TIME)) {
				break;
			}
			chromosomes.add(c);
			System.out.print("*");

		}*/
		String[][] r = {toStringArray(interactions),toStringArray(terminations)};

		Chromosome bestChromosome = new Chromosome(r,sl);
		bestChromosome.calculateFitnessLight(SharedData.EVALUATION_TIME);

		double worstTime = 4 * SharedData.EVALUATION_TIME * InteractionNum;
		double avgTime = worstTime;
		double avgcalcFitTime = 4 * SharedData.EVALUATION_TIME;
		double totalTime = 0;
		int calculateFitNum = 0;
		int numberOfIterations = 0;
		int numberOfGoodGens = 0;
		int mutatedInteractions[] = {-1,-1};

		// START EVO LOOP
		//ArrayList<Chromosome> chromosomes = new ArrayList<Chromosome>();
		//ArrayList<Chromosome> newChromosomes = new ArrayList<Chromosome>();
		Logger.getInstance().active = false;
		while(time.remainingTimeMillis() > 4 * avgTime && time.remainingTimeMillis() > worstTime && (time.remainingTimeMillis() > InteractionNum * avgcalcFitTime * 2 || calculateFitNum == 0)){
			int remainingcalcFitTime = (int) ( (time.remainingTimeMillis() / avgcalcFitTime) - InteractionNum*2);
			ElapsedCpuTimer timer = new ElapsedCpuTimer();
			System.out.println("Generation #" + (numberOfIterations + 1) + "(" + numberOfGoodGens + ")" + ": ");
			for (int i = 0; i < InteractionNum; i++) {
				//if(mutatedInteractions[0]!=i && mutatedInteractions[1]!=i) {
				ArrayList<String> temp = (ArrayList<String>) interactions.clone();
				int id = SharedData.random.nextInt(temp.size());
				temp.set(id,createInteraction(usefulSprites, id));
				temp.add(avatar.get(0).name+" EOS > stepBack");
				String[][] rules = {toStringArray(temp),toStringArray(terminations)};
				Chromosome c = new Chromosome(rules,sl,i);
				if(c.calculateFitnessLight(SharedData.EVALUATION_TIME)) {
					calculateFitNum++;
				}
				if(bestChromosome.compareTo(c)<0) {
					bestChromosome = c;
				}
				System.out.print("*");
				//}
			}
			System.out.println();
			/***********************/
			numberOfIterations += 1;
			if(calculateFitNum != 0) {
				numberOfGoodGens++;
				totalTime += timer.elapsedMillis();
				avgTime = totalTime / numberOfGoodGens;
				avgcalcFitTime = totalTime/calculateFitNum;
			}

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

		String[][] rules = bestChromosome.getRuleset();

		return rules;
	}


	private String createInteraction(ArrayList<String> usefulSprites,int i) {
		if (i==0) {
			return createGoalInteraction(usefulSprites, target);
		}
		else {
			return createInteraction(usefulSprites);
		}
	}

	private String createInteraction(ArrayList<String> usefulSprites) {
		String interaction = "";
		do {
			int i1 = SharedData.random.nextInt(usefulSprites.size());
			int i2 = (i1 + 1 + SharedData.random.nextInt(usefulSprites.size() - 1)) % usefulSprites.size();
			// add score change parameter for interactions
			String scoreChange = "";
			if(SharedData.random.nextBoolean()){
				scoreChange += "scoreChange=" + (SharedData.random.nextInt(5) - 2)+" ";
			}
			interaction = (usefulSprites.get(i1) + " " + usefulSprites.get(i2) + " > " +
					this.availableInteractions[SharedData.random.nextInt(this.availableInteractions.length)] + " " + scoreChange);
			while (interaction.contains("<@stype@>")) {
				interaction = interaction.replaceFirst("<@stype@>", usefulSprites.get(SharedData.random.nextInt(usefulSprites.size()-1)));
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
		System.out.println(interaction);
		return interaction;
	}

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
	}

	private String[] toStringArray(ArrayList<String> arlist) {
		String[] str = new String[arlist.size()];
		arlist.toArray(str);
		return str;
	}
	/*
	private ArrayList<Chromosome> getFirstPopulation(SLDescription sl, String name, int amount, int mutations){
	    	ArrayList<Chromosome> chromosomes = new ArrayList<Chromosome>();
	    	try{
        	    	Class genClass = Class.forName(name);
        	    	Constructor genConst = genClass.getConstructor(new Class[]{SLDescription.class, ElapsedCpuTimer.class});
        	    	AbstractRuleGenerator ruleGen = (AbstractRuleGenerator)genConst.newInstance(sl, null);
            	 	for(int i = 0; i < amount; i++) {
        	 		Chromosome c = new Chromosome(ruleGen.generateRules(sl, null), sl);
        	 		c.cleanseChromosome();
        	 		c.calculateFitnessLight(SharedData.EVALUATION_TIME);
        	 		for(int j = 0; j < mutations; j++) {
        				c.mutate();
        			}
        	 		chromosomes.add(c);
        	 	}
	    	}
	    	catch(Exception e){
	    	    e.printStackTrace();
	    	}
    	 	return chromosomes;
	}*/
	/**
	 * Get the next population based on the current feasible infeasible population
	 * @param fPopulation	array of the current feasible chromosomes
	 * @param iPopulation	array of the current infeasible chromosomes
	 * @return				array of the new chromosomes at the new population
	 *//*
	private ArrayList<Chromosome> getNextPopulation(ArrayList<Chromosome> fPopulation, ArrayList<Chromosome> iPopulation){
		ArrayList<Chromosome> newPopulation = new ArrayList<Chromosome>();

		//collect some statistics about the current generation
		ArrayList<Double> fitnessArray = new ArrayList<Double>();
		for(int i=0;i<fPopulation.size();i++){
			fitnessArray.add(fPopulation.get(i).getFitness().get(0));
		}

		Collections.sort(fitnessArray);
		if(fitnessArray.size() > 0){
			bestFitness.add(fitnessArray.get(fitnessArray.size() - 1));
		}
		else{
			bestFitness.add((double) 0);
		}
		numOfFeasible.add(fPopulation.size());
		numOfInFeasible.add(iPopulation.size());


		// CLEANSING PART
		// cleanse the population 
		for(Chromosome c : fPopulation) {
			c.cleanseChromosome();
		}
		for(Chromosome c: iPopulation) {
			c.cleanseChromosome();
		}

		while(newPopulation.size() < SharedData.POPULATION_SIZE){
			//choosing which population to work on with 50/50 probability
			//of selecting either any of them
			ArrayList<Chromosome> population = fPopulation;
			if(fPopulation.size() <= 0){
				population = iPopulation;
			}
			if(SharedData.random.nextDouble() < 0.5){
				population = iPopulation;
				if(iPopulation.size() <= 0){
					population = fPopulation;
				}
			}


			//select the parents using roulettewheel selection
			Chromosome parent1 = rankSelection(population);//rouletteWheelSelection(population);
			Chromosome parent2 = rankSelection(population);//rouletteWheelSelection(population);
			Chromosome child1 = parent1.clone();
			Chromosome child2 = parent2.clone();
			//do cross over
			if(SharedData.random.nextDouble() < SharedData.CROSSOVER_PROB){
				ArrayList<Chromosome> children = parent1.crossover(parent2);
				child1 = children.get(0);
				child2 = children.get(1);


				//do mutation to the children
				if(SharedData.random.nextDouble() < SharedData.MUTATION_PROB){
					child1.mutate();
				}
				if(SharedData.random.nextDouble() < SharedData.MUTATION_PROB){
					child2.mutate();
				}
			}

			//mutate the copies of the parents
			else if(SharedData.random.nextDouble() < SharedData.MUTATION_PROB){
				child1.mutate();
			}
			else if(SharedData.random.nextDouble() < SharedData.MUTATION_PROB){
				child2.mutate();
			}

			//add the new children to the new population
			newPopulation.add(child1);
			newPopulation.add(child2);
		}


		//calculate fitness of the new population chromosomes
		for(int i=0;i<newPopulation.size();i++){
		    	newPopulation.get(i).calculateFitness(SharedData.EVALUATION_TIME);
		    	if(newPopulation.get(i).getConstrainFitness() < 1){
				System.out.println("\tChromosome #" + (i+1) + " Constrain Fitness: " + newPopulation.get(i).getConstrainFitness());
        		}
        		else{
        			System.out.println("\tChromosome #" + (i+1) + " Fitness: " + newPopulation.get(i).getFitness());
        		}
		}

			//add the best chromosome(s) from old population to the new population
		Collections.sort(newPopulation);
		for(int i=SharedData.POPULATION_SIZE - SharedData.ELITISM_NUMBER;i<newPopulation.size();){
			newPopulation.remove(i);
		}

		if(fPopulation.isEmpty()){
			Collections.sort(iPopulation);
			for(int i=0;i<SharedData.ELITISM_NUMBER;i++){
				newPopulation.add(iPopulation.get(i));
			}
		}
		else{
			Collections.sort(fPopulation);
			for(int i=0;i<SharedData.ELITISM_NUMBER;i++){
				newPopulation.add(fPopulation.get(i));

			}
		}

		return newPopulation;
	}
	  */
	/**
	 * Performs rank selection on the given population
	 * @param population 	the population to be performed upon
	 * @return
	 */
	/*private Chromosome rankSelection(ArrayList<Chromosome> population) {
		double[] probabilities = new double[population.size()];
		probabilities[0] = 1.0;
		for(int i = 1; i < population.size(); i++) {
			probabilities[i] = probabilities[i-1] + i;
		}
		for(int i = 0; i < probabilities.length; i++) {
			probabilities[i] = probabilities[i] / probabilities[probabilities.length - 1];
		}

		double chosen = SharedData.random.nextDouble();
		for(int i = 0; i < probabilities.length; i++) {
			if(chosen < probabilities[i]) {
				return population.get(i);
			}
		}
		return population.get(0);

	}*/

	/*
	public String[][] generateRulesfo(SLDescription sl, ElapsedCpuTimer time) {

		//initialize the statistics objects
 		bestFitness = new ArrayList<Double>();
		numOfFeasible = new ArrayList<Integer>();
		numOfInFeasible = new ArrayList<Integer>();

		System.out.println("Generation #0: ");
		ArrayList<Chromosome> fChromosomes = new ArrayList<Chromosome>();
		ArrayList<Chromosome> iChromosomes = new ArrayList<Chromosome>();
		ArrayList<Chromosome> allChromosomes = new ArrayList<Chromosome>();

		allChromosomes.addAll(getFirstPopulation(sl, "tracks.ruleGeneration.constructiveRuleGenerator.RuleGenerator", 
			(int)(SharedData.POPULATION_SIZE * SharedData.INIT_CONSTRUCT_PERCENT), 0));
		allChromosomes.addAll(getFirstPopulation(sl, "tracks.ruleGeneration.randomRuleGenerator.RuleGenerator", 
			(int)(SharedData.POPULATION_SIZE * SharedData.INIT_RANDOM_PERCENT), 0));
		allChromosomes.addAll(getFirstPopulation(sl, "tracks.ruleGeneration.constructiveRuleGenerator.RuleGenerator", 
			(int)(SharedData.POPULATION_SIZE * SharedData.INIT_MUT_PERCENT), SharedData.INIT_MUTATION_AMOUNT));


		//some variables to make sure not getting out of time
		double worstTime = 4 * SharedData.EVALUATION_TIME * SharedData.POPULATION_SIZE;
		double avgTime = worstTime;
		double totalTime = 0;
		int numberOfIterations = 0;

		// START EVO LOOP
		while(time.remainingTimeMillis() > 4 * avgTime && time.remainingTimeMillis() > worstTime){
			ElapsedCpuTimer timer = new ElapsedCpuTimer();
			System.out.println("Generation #" + (numberOfIterations + 1) + ": ");
			fChromosomes.clear();
			iChromosomes.clear();
			for(Chromosome c:allChromosomes){
				if(c.getConstrainFitness() < 1){
					iChromosomes.add(c);
				}
				else{
					fChromosomes.add(c);
				}
			}
			//get the new population and split it to a the feasible and infeasible populations
			ArrayList<Chromosome> chromosomes = getNextPopulation(fChromosomes, iChromosomes);
			numberOfIterations += 1;
			totalTime += timer.elapsedMillis();
			avgTime = totalTime / numberOfIterations;
			Collections.sort(chromosomes);
			System.out.println("Best Chromosome Fitness: " + chromosomes.get(0).getFitness());
		}


		//return the best infeasible chromosome
		if(fChromosomes.isEmpty()){
			for(int i=0;i<iChromosomes.size();i++){
				iChromosomes.get(i).calculateFitness(SharedData.EVALUATION_TIME);
			}

			Collections.sort(iChromosomes);
			System.out.println("Best Fitness: " + iChromosomes.get(0).getConstrainFitness());
			return iChromosomes.get(0).getRuleset();
		}

		//return the best feasible chromosome otherwise and print some statistics
		for(int i=0;i<fChromosomes.size();i++){
			fChromosomes.get(i).calculateFitness(SharedData.EVALUATION_TIME);
		}
		Collections.sort(fChromosomes);
		System.out.println("Best Chromosome Fitness: " + fChromosomes.get(0).getFitness());
		System.out.println(bestFitness);
		System.out.println(numOfFeasible);
		System.out.println(numOfInFeasible);
		return fChromosomes.get(0).getRuleset();

	}*/

}
