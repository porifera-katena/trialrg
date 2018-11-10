package tracks.ruleGeneration.simEvoGenerator;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.stream.IntStream;

import core.game.Game;
import core.game.GameDescription.SpriteData;
import core.game.SLDescription;
import core.game.StateObservation;
import core.generator.AbstractRuleGenerator;
import core.logging.Logger;
import core.player.AbstractPlayer;
import core.vgdl.VGDLFactory;
import core.vgdl.VGDLParser;
import core.vgdl.VGDLRegistry;
import ontology.Types;
import ontology.effects.Effect;
import tools.ElapsedCpuTimer;
import tools.IO;

public class RuleGenerator extends AbstractRuleGenerator{

	private Random random;

	private final int INTERACTION_NUM = 8;
	private final int INTERACTION_WASTE = 3;
	
	private final int TERMINATION_NUM = 3;
	
	private int ITERATION;

	private static int FEASIBILITY_STEP_LIMIT = 40;

	private int EVOLUTION_NUM = Integer.MAX_VALUE;

	private String[] interactions = new String[] { "killSprite", "killAll", "killIfHasMore", "killIfHasLess",
			"killIfFromAbove", "killIfOtherHasMore", "spawnBehind", "stepBack", "spawnIfHasMore", "spawnIfHasLess",
			"cloneSprite", "transformTo", "undoAll", "flipDirection", "transformToRandomChild", "updateSpawnType",
			"removeScore", "addHealthPoints", "addHealthPointsToMax", "reverseDirection", "subtractHealthPoints",
			"increaseSpeedToAll", "decreaseSpeedToAll", "attractGaze", "align", "turnAround", "wrapAround",
			"pullWithIt", "bounceForward", "teleportToExit", "collectResource", "setSpeedForAll", "undoAll",
			"reverseDirection", "changeResource" };

	public RuleGenerator(SLDescription sl, ElapsedCpuTimer time) {
		this.random = new Random();
		this.ITERATION = INTERACTION_NUM + TERMINATION_NUM - 1;
		/*for(SpriteData s : sl.getGameSprites())
			System.out.println(s);*/

		constructAgent(sl);
	}


	@Override
	public String[][] generateRules(SLDescription sl, ElapsedCpuTimer time) {
		Logger.getInstance().active = false;
		ArrayList<String> interactions = new ArrayList<String>();
		ArrayList<String> terminations = new ArrayList<String>();
		SpriteData sprites[] = sl.getGameSprites();
		terminations.add("SpriteCounter stype=" + getAvatar(sprites) + " limit=0 win=False");

		while(interactions.size()<INTERACTION_NUM) {
			interactions.add(createInteraction(sprites));
			sl.testRules(toStringArray(interactions),toStringArray(terminations));
			if(sl.getErrors().size()>0) {
				interactions.remove(interactions.size()-1);
			}
		}
		while(terminations.size()<TERMINATION_NUM) {
			terminations.add(createTermiination(sprites));
			sl.testRules(toStringArray(interactions),toStringArray(terminations));
			if(sl.getErrors().size()>0) {
				terminations.remove(terminations.size()-1);
			}
		}
		
		ArrayList<SLDescription> SLs = new ArrayList<SLDescription>();

		for(int i=0;i<ITERATION;i++) {
			VGDLFactory.GetInstance().init();
			VGDLRegistry.GetInstance().init();

			Game toPlay = new VGDLParser().parseGame(SharedData.gamefile);
			String[] lines = new IO().readFile(SharedData.levelfile);
			try {
				SLs.add(new SLDescription(toPlay, lines, SharedData.seed));
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
		
		double worstTime = 0;
		double avgTime = worstTime;
		double totalTime = 0;
		int numberOfIterations = 0;
		for(int evo=0;evo<EVOLUTION_NUM;evo++) {
			ElapsedCpuTimer timer = new ElapsedCpuTimer();
			
			System.out.println("Generation : " + numberOfIterations);
			ArrayList<StateObservation> states = new ArrayList<StateObservation>();
			for(int i=0;i<INTERACTION_NUM;i++) {
				ArrayList<String> minusedInteraction = (ArrayList<String>) interactions.clone();
				minusedInteraction.remove(i);
				states.add(SLs.get(i).testRules(toStringArray(minusedInteraction), toStringArray(terminations)));
			}
			for(int i=1;i<TERMINATION_NUM;i++) {
				ArrayList<String> minusedtermination = (ArrayList<String>) terminations.clone();
				minusedtermination.remove(i);
				states.add(SLs.get(i).testRules(toStringArray(interactions), toStringArray(minusedtermination)));
			}
			//double results[] = new double[states.size()];
			//double fitness[] = new double[INTERACTION_NUM];
			System.out.println(states.size() +"="+ interactions.size() +"+"+ terminations.size() + "-1");

			Double[][] scores = new Double[(ITERATION)][];
			if(feasibilityTest(sl, interactions, terminations)[0]<1.0) {
				IntStream.range(0, ITERATION).parallel().forEach(i -> {
					scores[i] = feasibilityTest(states.get(i));
				});
			}
			else {
				IntStream.range(0, ITERATION).parallel().forEach(i -> {
					scores[i] = getFitness(states.get(i));
				});
			}
			ArrayList<ScoreData> scoreData = new ArrayList<ScoreData>();
			
			System.out.print("\nScore[");
			for(int i=0;i<ITERATION;i++) {
				scoreData.add(new ScoreData(i, scores[i]));
				System.out.print(scores[i][1]+"_"+ scores[i][0] + ", ");
			}
			System.out.println("]");
			
			Collections.shuffle(scoreData);//??
			Collections.sort(scoreData);
			
			if(!(time.remainingTimeMillis() > 4 * avgTime && time.remainingTimeMillis() > worstTime)) {
				int id = scoreData.get(ITERATION-1-1).id;
				if(id<INTERACTION_NUM) {
					interactions.remove(id);
				}
				else {
					terminations.remove(id-INTERACTION_NUM+1);
				}
				String[][] rules = {toStringArray(interactions),toStringArray(terminations)};
				return rules;
			}
			
			for(int i=0;i<INTERACTION_WASTE;i++) {
				int id = scoreData.get(ITERATION-i-1).id;
				if(id<INTERACTION_NUM) {
					interactions.set(id,"");
				}
				else {
					terminations.set(id-INTERACTION_NUM+1,"");
				}
			}
			for(int i=0;i<INTERACTION_WASTE;i++) {
				interactions.remove(""); 
				terminations.remove("");
			}


			while(interactions.size()<INTERACTION_NUM) {
				interactions.add(createInteraction(sprites));
				sl.testRules(toStringArray(interactions),toStringArray(terminations));
				if(sl.getErrors().size()>0) {
					interactions.remove(interactions.size()-1);
				}
			}
			while(terminations.size()<TERMINATION_NUM) {
				terminations.add(createTermiination(sprites));
				sl.testRules(toStringArray(interactions),toStringArray(terminations));
				if(sl.getErrors().size()>0) {
					terminations.remove(terminations.size()-1);
				}
			}
			numberOfIterations++;
			totalTime += timer.elapsedMillis();
			avgTime = totalTime / numberOfIterations;
			if(timer.elapsedMillis() > worstTime )worstTime = timer.elapsedMillis();
		}
		/*for(int i=0;i<INTERACTION_NUM;i++) {
			//scoreData.add(new ScoreData(i, scores[i]));
			System.out.println("Sorted = " + scoreData.get(i).score[1] + "__" + scoreData.get(i).score[0]);
		}*/

		System.out.println();

		//StateObservation SObs = sl.testRules(toStringArray(interactions),toStringArray(terminations));


		//System.out.println(toStringArray(interactions)[0]+"??");
		String[][] rules = {toStringArray(interactions),toStringArray(terminations)};
		return rules;
	}

	private Double[] feasibilityTest(StateObservation state) {		
		//int errorCount = sl.getErrors().size();
		//StateObservation state = sl.testRules(toStringArray(interactions),toStringArray(terminations));
		double constrainFitness = 0;
		//constrainFitness += (0.5) * 1.0 / (errorCount + 1.0);	
		int goodFrame = Integer.MAX_VALUE;

		AbstractPlayer doNothingAgent = null;
		try{
			Class agentClass = Class.forName(SharedData.DO_NOTHING_AGENT_NAME);
			Constructor agentConst = agentClass.getConstructor(new Class[]{StateObservation.class, ElapsedCpuTimer.class});
			doNothingAgent = (AbstractPlayer)agentConst.newInstance(state, null);
		}
		catch(Exception e){
			e.printStackTrace();
		}
		for(int i = 0; i < SharedData.REPETITION_AMOUNT; i++) { //**************```````````++++++++++++++++++++++++
			//System.out.print("?");
			int temp = getAgentResult(state.copy(), FEASIBILITY_STEP_LIMIT, doNothingAgent);

			if(temp < goodFrame){
				goodFrame = temp;
			}
			//System.out.print("!");
		}
		Double[] result = {(goodFrame / (40.0)),0.0,0.0};
		//constrainFitness = (goodFrame / (40.0));

		//this.fitness.set(0, constrainFitness);
		System.out.print("+");
		return result;
	}

	private Double[] feasibilityTest(SLDescription sl,ArrayList<String> interactions, ArrayList<String> terminations) {		
		//int errorCount = sl.getErrors().size();
		StateObservation state = sl.testRules(toStringArray(interactions),toStringArray(terminations));
		return feasibilityTest(state);
	}

	private Double[] getFitness(StateObservation state) {
		Double[][] temp = {{Double.NEGATIVE_INFINITY,-100.0,0.0},{Double.NEGATIVE_INFINITY,-100.0,0.0}};
		for(int j=0;j<SharedData.REPETITION_AMOUNT;j++) {
			temp[0] = getScore(state.copy(), SharedData.BEST_AGENT_NAME,SharedData.EVALUATION_STEP_COUNT);
			if((temp[0][1] > temp[1][1]) || (temp[0][1] == temp[1][1] && temp[0][0] > temp[1][0]) ) {
				temp[1] = temp[0].clone();
			}
		}
		//state =  sl.testRules(toStringArray(interactions),toStringArray(terminations));
		Double[][] temp1 = {{Double.NEGATIVE_INFINITY ,-100.0, 0.0},{Double.NEGATIVE_INFINITY,-100.0,0.0}};
		for(int j=0;j<SharedData.REPETITION_AMOUNT;j++) {
			temp1[0] = getScore(state.copy(), SharedData.RANDOM_AGENT_NAME,temp[1][2].intValue());
			if((temp1[0][1] > temp1[1][1]) || (temp1[0][1] == temp1[1][1] && temp1[0][0] > temp1[1][0]) ) {
				temp1[1] = temp1[0].clone();
			}
		}
		if(temp1[1][1]==0)temp1[1][1] = -1.0;
		temp[1][0] = temp[1][0] - temp1[1][0];
		temp[1][1] = temp[1][1] - temp1[1][1];
		return temp[1];

	}

	private Double[] getFitness(SLDescription sl,ArrayList<String> interactions, ArrayList<String> terminations) {
		StateObservation state =  sl.testRules(toStringArray(interactions),toStringArray(terminations));
		return getFitness(state);
	}

	private Double[] getScore(StateObservation state,String AgentName,int step) {
		AbstractPlayer Agent = null;
		try{
			Class agentClass = Class.forName(AgentName);
			Constructor agentConst = agentClass.getConstructor(new Class[]{StateObservation.class, ElapsedCpuTimer.class});
			Agent = (AbstractPlayer)agentConst.newInstance(state, null);
		}
		catch(Exception e){
			e.printStackTrace();
		}
		StateObservation tempstate = state;
		int temp = getAgentResult(tempstate, step, Agent);
		Double[] score = {tempstate.getGameScore(),(double)tempstate.getGameWinner().key(),(double) temp};
		System.out.print("?");
		return score;
	}

	private int getAgentResult(StateObservation stateObs, int steps, AbstractPlayer agent){
		int i =0;
		int k = 0;
		//System.out.println("getAgentResults");

		for(i=0;i<steps;i++){
			if(stateObs.isGameOver()){
				break;
			}
			ElapsedCpuTimer timer = new ElapsedCpuTimer();
			timer.setMaxTimeMillis(SharedData.EVALUATION_STEP_TIME);
			Types.ACTIONS bestAction = agent.act(stateObs, timer);
			stateObs.advance(bestAction);
			//k += checkIfOffScreen(stateObs);

		}
		if(k > 0) {
			// add k to global var keeping track of this
			i = i-k;
		}
		//System.out.println("getAgentResults-end");
		return i;
	}

	private void cleanOpenloopAgents() {
		((tracks.singlePlayer.advanced.olets.Agent)SharedData.automatedAgent).mctsPlayer = 
				new tracks.singlePlayer.advanced.olets.SingleMCTSPlayer(new Random(), 
						(tracks.singlePlayer.advanced.olets.Agent) SharedData.automatedAgent);
	}

	/*private int checkIfOffScreen(StateObservation stateObs) {
		ArrayList<Observation> allSprites = new ArrayList<Observation>();
		ArrayList<Observation>[] temp = stateObs.getNPCPositions();
		if(temp != null) {
			for(ArrayList<Observation> list : temp) {
				allSprites.addAll(list);
			}	
		}
		temp = stateObs.getImmovablePositions();
		if(temp != null) {
			for(ArrayList<Observation> list : temp) {
				allSprites.addAll(list);
			}
		}

		temp = stateObs.getMovablePositions();
		if(temp != null) {
			for(ArrayList<Observation> list : temp) {
				allSprites.addAll(list);
			}
		}

		// calculate screen size
		int xMin = -1 * stateObs.getBlockSize();
		int yMin = -1 * stateObs.getBlockSize();

		// add a 1 pixel buffer
		int xMax = (SharedData.la.getWidth()+1) * stateObs.getBlockSize();
		int yMax = (SharedData.la.getLength()+1) * stateObs.getBlockSize();
		int counter = 0;
		// check to see if any sprites are out of screen
		boolean frameBad = false;
		for(Observation s : allSprites) {
			if(s.position.x < xMin || s.position.x > xMax || s.position.y < yMin || s.position.y > yMax) {
				if(!frameBad) {
					counter++;
					frameBad = true;
				}
			}
		}
		return counter;

	}*/

	private String[] toStringArray(ArrayList<String> arlist) {
		String[] str = new String[arlist.size()];
		arlist.toArray(str);
		return str;
	}

	private String createInteraction(SpriteData[] Sprites) {
		int i1 = this.random.nextInt(Sprites.length);
		int i2 = (i1 + 1 + this.random.nextInt(Sprites.length - 1)) % Sprites.length;
		// add score change parameter for interactions
		String scoreChange = "";
		if(this.random.nextBoolean()){
			scoreChange += "scoreChange=" + (this.random.nextInt(5) - 2);
		}
		// add the new random interaction that doesn't produce errors
		return (Sprites[i1].name + " " + Sprites[i2].name + " > " +
				this.interactions[this.random.nextInt(this.interactions.length)] + " " + scoreChange);
	}

	private String createTermiination(SpriteData[] Sprites) {
		if (this.random.nextBoolean()) {
			return "Timeout limit=" + (800 + this.random.nextInt(500)) + " win=True";
		} else {
			String chosen = Sprites[this.random.nextInt(Sprites.length)].name;
			String result = "win=True";
			if(this.random.nextInt(10)==0) {
				result = "win=False";
			}
			return "SpriteCounter stype=" + chosen + " limit=0 " + result;
		}
		// Add a losing termination condition
		//"SpriteCounter stype=" + this.avatar + " limit=0 win=False";
	}

	private void resetCount(SLDescription sl) {
		Game game = sl.getCurrentGame();
		for(Effect e :game.getAllEffects()) {
			e.resetCount();
		}
	}

	/*private void resetCount(StateObservation SObs) {
		Game game = SObs.get;
		for(Effect e :game.getAllEffects()) {
			e.resetCount();
		}
	}*/

	private String getAvatar(SpriteData[] Sprites) {
		for (SpriteData s:Sprites) {
			if (s != null && s.isAvatar) {
				return s.name;
			}
		}
		return "";
	}


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


}

class ScoreData implements Comparable<ScoreData>{
	public int id;
	public Double[] score;

	public ScoreData(int _id,Double[] _score) {
		this.id = _id;
		this.score = _score;
	}

	@Override
	public int compareTo(ScoreData o) {
		if((double)this.score[1] == (double)o.score[1]) {
			//System.out.print("{" +((Double)(this.score[0] - o.score[0])).intValue() + "}");
			return ((Double)(this.score[0] - o.score[0])).intValue();
		}
		else {
			//System.out.print("<" +((Double)(this.score[1] - o.score[1])).intValue()+">" + this.score[1] +"*"+ o.score[1]);
			return ((Double)(this.score[1] - o.score[1])).intValue();
		}
	}

}
