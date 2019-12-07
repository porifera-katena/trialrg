package tracks.ruleGeneration.cylinderP406;
import java.util.*;

import core.game.SLDescription;
import core.game.StateObservation;
import core.competition.CompetitionParameters;
import core.game.Observation;
import core.player.AbstractPlayer;
import core.vgdl.SpriteGroup;
import core.vgdl.VGDLRegistry;
import core.vgdl.VGDLSprite;
import ontology.Types;
import ontology.Types.WINNER;
import tools.ElapsedCpuTimer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.LoggingMXBean;


public class Chromosome implements Comparable<Chromosome>{

	public int id = 0;
	
	public boolean Logging = false;
	public HashMap<String, Integer> Log;

	private ArrayList<Double> fitness;

	private double constrainFitness;
	
	public static Random random;

	private int badFrames;

	private int errorCount;

	private String[][] ruleset;

	private SLDescription sl;

	private ElapsedCpuTimer time;
	
	public static HashMap<String, ArrayList<String>> spriteSetStruct=null;

	private static int FEASIBILITY_STEP_LIMIT = 40;
	private static int REPETITION_AMOUNT = 3;
	private static int EVALUATION_STEP_COUNT = 300;
	public static long EVALUATION_STEP_TIME = 40;

	
	
	private int doNothingLength;
	private StateObservation doNothingState;
	//private StateObservation bestState;
	private ArrayList<Types.ACTIONS> bestSol;
	

	public static AbstractPlayer automatedAgent;
	public static AbstractPlayer naiveAgent;
	public static AbstractPlayer doNothingAgent;
	public static AbstractPlayer randomAgent;

	/**
	 * Chromosome constructor.  Holds the ruleset and initializes agents within
	 * @param ruleset	the ruleset the chromosome contains
	 * @param sl		the game description
	 * @param id		identifier
	 */


	public Chromosome(String[][] ruleset, SLDescription sl,int _id) {
		this.id = _id;
		this.ruleset = ruleset;
		this.sl = sl;
		this.fitness = new ArrayList<Double>();
		fitness.add(0.0);
		fitness.add(0.0);
		fitness.add(0.0);
		this.badFrames = 0;
	}
	public Chromosome(String[][] ruleset, SLDescription sl) {
		this.ruleset = ruleset;
		this.sl = sl;
		this.fitness = new ArrayList<Double>();
		fitness.add(0.0);
		fitness.add(0.0);
		fitness.add(0.0);
		this.badFrames = 0;
	}
	public Chromosome() {
		
	}
	/**
	 * clone the chromosome data
	 */
	public Chromosome clone(){
		String[][] nRuleset = new String[ruleset.length][];
		nRuleset[0] = new String[ruleset[0].length];
		nRuleset[1] = new String[ruleset[1].length];
		for(int i = 0; i < ruleset[0].length; i++) {
			nRuleset[0][i] = ruleset[0][i];
		}
		for(int i = 0; i < ruleset[1].length; i++) {
			nRuleset[1][i] = ruleset[1][i];
		}
		Chromosome c = new Chromosome(nRuleset, sl);
		return c;
	}

	private StateObservation feasibilityTest() {
		// = SharedData.constGen.getSpriteSetStructure();
		StateObservation state = MakeNewGame();		
		errorCount = sl.getErrors().size();
		constrainFitness = 0;
		constrainFitness += (0.5) * 1.0 / (errorCount + 1.0);	
		if(constrainFitness >= 0.5) {
			doNothingLength = Integer.MAX_VALUE;
			for(int i = 0; i < REPETITION_AMOUNT; i++) {
				int temp = this.getAgentResult(state.copy(), FEASIBILITY_STEP_LIMIT, doNothingAgent);
				if(temp < doNothingLength){
					doNothingLength = temp;
				}
			}
			constrainFitness += 0.2 * (doNothingLength / (40.0));

			this.fitness.set(0, constrainFitness);

		}
		return state;
	}

	public boolean calculateFitnessLight(long time) {
		this.badFrames = 0;
		StateObservation stateObs = feasibilityTest();
		if(constrainFitness < 0.7) {


			this.fitness.set(0, constrainFitness);
		}
		else {					
			// Best Agent
			double agentBestScore = Double.NEGATIVE_INFINITY;
			double automatedScoreSum = 0.0;
			double automatedWinSum = 0.0;
			int bestSolutionSize = 0;
			for(int i=0; i<REPETITION_AMOUNT; i++){
				StateObservation tempState = MakeNewGame();
				int temp=0;
				try {
					cleanOpenloopAgents();
					temp = getAgentResult(tempState, EVALUATION_STEP_COUNT, automatedAgent);
				}catch (Exception e) {
					constrainFitness = 0.0;
					this.fitness.set(0, constrainFitness);
					/*this.fitness.set(1, 0.0);
					this.fitness.set(2, 0.0);*/
					//System.out.print(constrainFitness);
					return false;
				}

				if(tempState.getGameScore() > agentBestScore) {
					agentBestScore = tempState.getGameScore();
					///bestState = tempState;
					bestSolutionSize = temp;
				}

				
				automatedScoreSum += tempState.getGameScore();
				if(tempState.getGameWinner() == Types.WINNER.PLAYER_DISQ){
					constrainFitness = 0.0;
					this.fitness.set(0, constrainFitness);
					return false;//automatedWinSum += 1;
				}
				automatedWinSum += GameOverPoint(tempState.getGameWinner());
				

				/*TreeSet s1 = tempState.getEventsHistory();
				Iterator<Event> iter1 = s1.iterator();
				while(iter1.hasNext()) {
					Event e = iter1.next();
					events.add(e.activeTypeId + "" + e.passiveTypeId);
				}*/
				//System.out.print("T");
			}


			int ActionTypeNum = stateObs.getAvailableActions(true).size();
			double summedRandom = -200.0;
			for(int i=0; i<ActionTypeNum + 1; i++){
				double scoreSum = 0.0;
				double winSum = 0.0;
				for(int j=0;j<REPETITION_AMOUNT;j++) {
					StateObservation tempState = MakeNewGame();
					int temp = 0;
					try{
						if (i==0) {
							temp = getAgentResult(tempState, EVALUATION_STEP_COUNT, randomAgent);
						}
						else {
							temp = getSimpleResult(tempState, EVALUATION_STEP_COUNT, stateObs.getAvailableActions(true).get(i-1));
						}
					}catch (Exception e) {
						constrainFitness = 0.0;
						this.fitness.set(0, constrainFitness);
						return false;
					}


					scoreSum += tempState.getGameScore();
					/*if(scoreBest < score) {
						scoreBest = score;
					}*/
					
					if(tempState.getGameWinner() == Types.WINNER.PLAYER_DISQ){
						constrainFitness = 0.0;
						this.fitness.set(0, constrainFitness);
						return false;//automatedWinSum += 1;
					}
					winSum += GameOverPoint(tempState.getGameWinner());

					// gather all unique interactions between objects in the naive agent
					/*TreeSet s1 = randomState.getEventsHistory();
					Iterator<Event> iter1 = s1.iterator();
					while(iter1.hasNext()) {
						Event e = iter1.next();
						events.add(e.activeTypeId + "" + e.passiveTypeId);
					}*/
				}

				// add temp to framesCount
				//frameCount += temp;
				//randomScoreSum += score;
				double avgRandomWin = winSum / REPETITION_AMOUNT;
				double sigRandom = 1 / (1 + Math.pow(Math.E, (0.1) * -scoreSum));
				double tempsummedRandom = 0.9 * avgRandomWin + 0.1 * sigRandom;
				if(summedRandom < tempsummedRandom) {
					summedRandom = tempsummedRandom;
				}
				//System.out.print("N");
			}

			double avgBestScore = automatedScoreSum / REPETITION_AMOUNT;
			double avgBestWin = automatedWinSum / REPETITION_AMOUNT;
			double sigBest = 1 / (1 + Math.pow(Math.E, (0.1) * -avgBestScore));
			double summedBest = 0.9 * avgBestWin + 0.1 * sigBest;

			double gameScore = (summedBest - summedRandom);

			// fitness is calculated by weight summing the 2 variables together

			//double fitness = gameScore + 1 * (rulesTriggered);
			constrainFitness = 1.0;
			this.fitness.set(0, constrainFitness);
			this.fitness.set(1, gameScore);
			//this.fitness.set(2, rulesTriggered);
		} 
		return true;
	}


	private double GameOverPoint(WINNER gameWinner) {
		if(gameWinner == Types.WINNER.PLAYER_WINS){
			return 1;
		}
		else if(gameWinner == Types.WINNER.NO_WINNER) {
			return 0;
		}
		else if(gameWinner == Types.WINNER.PLAYER_LOSES) {
			return -1;
		}
		else {
			return -100;
		}
}

	public int getSimpleResult(StateObservation stateObs, int steps,Types.ACTIONS action) {
		int i =0;
		int k = 0;
		for(i=0;i<steps;i++){
			if(stateObs.isGameOver()){
				break;
			}
			ElapsedCpuTimer timer = new ElapsedCpuTimer();
			timer.setMaxTimeMillis(EVALUATION_STEP_TIME);
			Types.ACTIONS bestAction = action;
			stateObs.advance(bestAction);
			k += checkIfOffScreen(stateObs);

		}
		if(k > 0) {
			// add k to global var keeping track of this
			this.badFrames += k;
		}
		return i;
	}



	private int getAgentResult(StateObservation stateObs, int steps, AbstractPlayer agent){
		int i =0;
		int k = 0;
		for(i=0;i<steps;i++){
			if(stateObs.isGameOver()){
				break;
			}
			ElapsedCpuTimer timer = new ElapsedCpuTimer();
			timer.setMaxTimeMillis(EVALUATION_STEP_TIME);
			Types.ACTIONS bestAction = agent.act(stateObs, timer);
			stateObs.advance(bestAction);
			if(Logging==true) {
				ArrayList<Observation>[][] grid = stateObs.getObservationGrid();
				for(ArrayList<Observation>[] gs: grid) {
					for(ArrayList<Observation> obs :gs) {
						for(int j=0;j<obs.size();j++) {
							for(int j2=j+1;j2<obs.size();j2++) {
								
								Integer a = obs.get(j).itype;
								Integer b = obs.get(j2).itype;
								if(a<b) {
									a = b;
									b = obs.get(j).itype;
								}
								String key = a.toString()+b.toString();
								if(Log.get(key)!=null) {
									Log.put(key, Log.get(key)+1);
								}
								else {
									Log.put(key,1);
								}
								
								
							}
						}
					}
				}
			}
			k += checkIfOffScreen(stateObs);

		}
		if(k > 0) {
			// add k to global var keeping track of this
			this.badFrames += k;
		}
		return i;
	}
	
	private StateObservation MakeNewGame() {
		return sl.testRules(ruleset[0], ruleset[1], spriteSetStruct);
	}
	
	private void cleanOpenloopAgents() {
		((tracks.singlePlayer.advanced.olets.Agent)automatedAgent).mctsPlayer = 
				new tracks.singlePlayer.advanced.olets.SingleMCTSPlayer(new Random(), 
						(tracks.singlePlayer.advanced.olets.Agent) automatedAgent);
	}

	/***
	 * Checks to see if sprites are off screen
	 * @param stateObs the temporary state observation of the game
	 * @return the number of times sprites were off screen
	 */
	private int checkIfOffScreen(StateObservation stateObs) {
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
		int xMax = (sl.getCurrentLevel().length /*SharedData.la.getWidth()*/+1) * stateObs.getBlockSize();
		int yMax = (sl.getCurrentLevel()[0].length/*SharedData.la.getLength()*/+1) * stateObs.getBlockSize();
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

	}

	/**
	 * Compare two chromosome with each other based on their
	 * constrained fitness and normal fitness
	 */
	@Override
	public int compareTo(Chromosome o) {
		//if(this.constrainFitness < 1 || o.constrainFitness < 1){
		if(this.constrainFitness < o.constrainFitness){
			return 1;
		}
		if(this.constrainFitness > o.constrainFitness){
			return -1;
		}
		//return 0;
		//}

		double firstFitness = 0;
		double secondFitness = 0;
		for(int i=0; i<this.fitness.size(); i++){
			firstFitness += this.fitness.get(i);
			secondFitness += o.fitness.get(i);
		}

		if(firstFitness > secondFitness){
			return -1;
		}

		if(firstFitness < secondFitness){
			return 1;
		}

		return 0;
	}
	/**
	 * Returns the fitness of the chromosome
	 * @return fitness the fitness of the chromosome
	 */
	public ArrayList<Double> getFitness() {
		return fitness;
	}
	/**
	 * Get constraint fitness for infeasible chromosome
	 * @return	1 if its feasible and less than 1 if not
	 */
	public double getConstrainFitness(){
		return constrainFitness;
	}
	/**
	 * returns the ruleset of this chromosome
	 * @return
	 */
	public String[][] getRuleset() {
		return ruleset;
	}
	/**
	 * sets the ruleset
	 * @param nRuleset	the new ruleset
	 */
	public void setRuleset(String[][] nRuleset) {
		this.ruleset = nRuleset;
	}
	
	public void setLogging(boolean tf) {
		this.Logging = tf;
		if(tf) {
			Log = new HashMap<String,Integer>();
		}
	}
	
}
