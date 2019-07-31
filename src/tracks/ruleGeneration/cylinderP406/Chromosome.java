package tracks.ruleGeneration.cylinderP406;
import java.util.*;

import core.game.SLDescription;
import core.game.StateObservation;

import core.game.Observation;
import core.player.AbstractPlayer;
import ontology.Types;
import ontology.Types.WINNER;
import tools.ElapsedCpuTimer;

import java.util.ArrayList;
import java.util.HashMap;


public class Chromosome implements Comparable<Chromosome>{
	/**
	 * current chromosome fitness if its a feasible
	 */

	public int id = 0;

	private ArrayList<Double> fitness;
	
	public static Random random;
	/**
	 * current chromosome fitness if its an infeasible
	 */
	private double constrainFitness;

	/**
	 * keeps track of how many bad frames there are during playthoughs
	 */
	private int badFrames;

	/**
	 * contains how many errors came out of the build test
	 */
	private int errorCount;

	/**
	 * the ruleset this chromosome contains
	 */
	private String[][] ruleset;
	/**
	 * the SL description of this chromosome
	 */
	private SLDescription sl;
	/**
	 * elapsed time
	 */
	private ElapsedCpuTimer time;
	
	public static HashMap<String, ArrayList<String>> spriteSetStruct=null;
	/**
	 * amount of steps allowed for the naive agent to sit around
	 */
	private static int FEASIBILITY_STEP_LIMIT = 40;
	private static int REPETITION_AMOUNT = 3;
	private static int EVALUATION_STEP_COUNT = 300;
	public static long EVALUATION_STEP_TIME = 40;

	
	
	private int doNothingLength;
	private StateObservation doNothingState;
	//private StateObservation bestState;
	private ArrayList<Types.ACTIONS> bestSol;
	
	/**
	 * the best automated agent
	 */
	public static AbstractPlayer automatedAgent;
	/**
	 * the naive automated agent
	 */
	public static AbstractPlayer naiveAgent;
	/**
	 * the do nothing automated agent
	 */
	public static AbstractPlayer doNothingAgent;
	/**
	 * the random agent
	 */
	public static AbstractPlayer randomAgent;

	/**
	 * Chromosome constructor.  Holds the ruleset and initializes agents within
	 * @param ruleset	the ruleset the chromosome contains
	 * @param sl		the game description
	 * @param time		elapsed time
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
		// copy ruleset into nRuleset. Two for loops, in case 2d array is jagged
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

	/**
	 * first checks to see if there are no build errors, if there are, this is infeasible. 
	 * Otherwise, it will check to see if a do nothing agent dies within the first 40 steps of playing. 
	 * if it does, this is infeasible.
	 * @return
	 */
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

/*
		// reset bad frames
		this.badFrames = 0;
		// unique events that occurred in all the game simulations
		Set<String> events = new HashSet<String>();
		StateObservation stateObs = feasibilityTest();
		if(constrainFitness < 0.7) {
			// failed feasibility
			this.fitness.set(0, constrainFitness);
		}
		else {					
			//Play the game using the best agent
			double score = -200;
			ArrayList<Vector2d> SOs = new ArrayList<>();
			// protects the fitness evaluation from looping forever

			// big vars
			// keeps track of total number of simulated frames
			int frameCount = 0;
			// Best Agent
			double agentBestScore = Double.NEGATIVE_INFINITY;
			double automatedScoreSum = 0.0;
			double automatedWinSum = 0.0;
			int bestSolutionSize = 0;
			for(int i=0; i<SharedData.REPETITION_AMOUNT; i++){
				StateObservation tempState = stateObs.copy();
				cleanOpenloopAgents();
				int temp = getAgentResult(tempState, SharedData.EVALUATION_STEP_COUNT, SharedData.automatedAgent);
				// add temp to framesCount
				frameCount += temp;

				if(tempState.getGameScore() > agentBestScore) {
					agentBestScore = tempState.getGameScore();
					bestState = tempState;
					bestSolutionSize = temp;
				}

				score = tempState.getGameScore();
				automatedScoreSum += score;
				if(tempState.getGameWinner() == Types.WINNER.PLAYER_WINS){
					automatedWinSum += 1;
				} else if(tempState.getGameWinner() == Types.WINNER.NO_WINNER) {
					automatedWinSum += 0.5;
				}

				TreeSet s1 = tempState.getEventsHistory();
				Iterator<Event> iter1 = s1.iterator();
				while(iter1.hasNext()) {
					Event e = iter1.next();
					events.add(e.activeTypeId + "" + e.passiveTypeId);
				}
				score = -200;
			}

			// Random Agent
			score = -200;

			double randomScoreBest = -200.0;
			double randomWinSum = 0.0;
			StateObservation randomState = null;
			int ActionTypeNum = stateObs.getAvailableActions().size() + 1;
			for(int i=0; i<ActionTypeNum; i++){
				StateObservation tempState = stateObs.copy();
				int temp = 0;
				if (i==0) {
					temp = getAgentResult(tempState, bestSolutionSize, SharedData.randomAgent);
				}
				else {
					temp = getSimpleResult(tempState, bestSolutionSize, stateObs.getAvailableActions().get(i-1));

				}
				// add temp to framesCount
				frameCount += temp;
				randomState = tempState;

				score = randomState.getGameScore();


				if(randomScoreBest < score) {
					randomScoreBest = score;
				}
				//randomScoreSum += score;
				if(randomState.getGameWinner() == Types.WINNER.PLAYER_WINS){
					randomWinSum += 1;
				} else if(randomState.getGameWinner() == Types.WINNER.NO_WINNER) {
					randomWinSum += 0.5;
				}

				// gather all unique interactions between objects in the naive agent
				TreeSet s1 = randomState.getEventsHistory();
				Iterator<Event> iter1 = s1.iterator();
				while(iter1.hasNext()) {
					Event e = iter1.next();
					events.add(e.activeTypeId + "" + e.passiveTypeId);
				}
				score = -200;
			}

			// Naive agent
			score = -200;
			StateObservation naiveState = null;
			double naiveScoreSum = 0.0;
			double naiveWinSum = 0.0;
			//playing the game using the naive agent
			for(int i=0; i<SharedData.REPETITION_AMOUNT; i++){
				StateObservation tempState = stateObs.copy();
				int temp = getAgentResult(tempState, bestSolutionSize, SharedData.naiveAgent);
				// add temp to framesCount
				frameCount += temp;
				naiveState = tempState;

				score = naiveState.getGameScore();
				if(score > -100) {
					naiveScoreSum += score;
					if(naiveState.getGameWinner() == Types.WINNER.PLAYER_WINS){
						naiveWinSum += 1;
					} else if(naiveState.getGameWinner() == Types.WINNER.NO_WINNER) {
						naiveWinSum += 0.5;
					}
				}

				// gather all unique interactions between objects in the best agent
				TreeSet s1 = naiveState.getEventsHistory();
				Iterator<Event> iter1 = s1.iterator();
				while(iter1.hasNext()) {
					Event e = iter1.next();
					events.add(e.activeTypeId + "" + e.passiveTypeId);
					}
				score = -200;
			}
			double badFramePercent = badFrames / (1.0 * frameCount);
//			if(badFramePercent > .3) {
//				// if we have bad frames, this is still not a good game
//				constrainFitness += 0.3 * (1 - badFrames / (1.0 * frameCount));
//				this.fitness.set(0, constrainFitness);
//			}
//			else {
				// find average scores and wins across playthroughs
				double avgBestScore = automatedScoreSum / SharedData.REPETITION_AMOUNT;
				double avgNaiveScore = naiveScoreSum / SharedData.REPETITION_AMOUNT;
				//double avgRandomScore = randomScoreSum / ActionTypeNum;

				double avgBestWin = automatedWinSum / SharedData.REPETITION_AMOUNT;
				double avgNaiveWin = naiveWinSum / SharedData.REPETITION_AMOUNT;
				double avgRandomWin = randomWinSum / ActionTypeNum;

				// calc sigmoid function with the score as "t"
				double sigBest = 1 / (1 + Math.pow(Math.E, (0.1) * -avgBestScore));
				double sigNaive = 1 / (1 + Math.pow(Math.E, (0.1) * -avgNaiveScore));
				double sigRandom = 1 / (1 + Math.pow(Math.E, (0.1) * -randomScoreBest));

				// sum weighted win and sig-score values
				double summedBest = 0.9 * avgBestWin + 0.1 * sigBest;
				double summedNaive = 0.9 * avgNaiveWin + 0.1 * sigNaive;
				double summedRandom = 0.9 * avgRandomWin + 0.1 * sigRandom;

				// calc game score differences
				double gameScore = (summedBest - summedNaive) * (summedNaive - summedRandom);
				System.out.print("("+summedBest+" - "+summedNaive+") * ("+summedNaive+" - "+summedRandom+")="+gameScore);
				// allows rounding up due to weird scores
				if(gameScore < -0.0005) {

					gameScore = 0;
				}
				// reward fitness for each unique interaction triggered
				int uniqueCount = events.size();
				// add a normalized unique count to the fitness
				double rulesTriggered = uniqueCount / (ruleset[0].length * 1.0f + 1);

				// fitness is calculated by weight summing the 2 variables together

				double fitness = (gameScore + 1) * (rulesTriggered);
				constrainFitness = 1.0;
				this.fitness.set(0, constrainFitness);
				this.fitness.set(1, fitness);
		} 
	}*/

	public boolean calculateFitnessLight(long time) {

		// reset bad frames
		this.badFrames = 0;
		// unique events that occurred in all the game simulations
		Set<String> events = new HashSet<String>();
		StateObservation stateObs = feasibilityTest();
		if(constrainFitness < 0.7) {
			// failed feasibility
			//System.out.print(constrainFitness);
			this.fitness.set(0, constrainFitness);
		}
		else {					
			//Play the game using the best agent
			int frameCount = 0;
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

				// add temp to framesCount
				frameCount += temp;
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
				//score = -200;
				System.out.print("T");
			}

			// Random Agent
			//double randomScoreBest = -200.0;
			//double randomWinSum = 0.0;
			StateObservation randomState = null;
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
				System.out.print("N");
			}

			//double badFramePercent = badFrames / (1.0 * frameCount);
			//			if(badFramePercent > .3) {
			//				// if we have bad frames, this is still not a good game
			//				constrainFitness += 0.3 * (1 - badFrames / (1.0 * frameCount));
			//				this.fitness.set(0, constrainFitness);
			//			}
			//			else {
			// find average scores and wins across playthroughs
			double avgBestScore = automatedScoreSum / REPETITION_AMOUNT;
			//double avgNaiveScore = naiveScoreSum / SharedData.REPETITION_AMOUNT;
			//double avgRandomScore = randomScoreSum / ActionTypeNum;

			double avgBestWin = automatedWinSum / REPETITION_AMOUNT;
			//double avgNaiveWin = naiveWinSum / SharedData.REPETITION_AMOUNT;
			//double avgRandomWin = randomWinSum / ActionTypeNum;

			// calc sigmoid function with the score as "t"
			double sigBest = 1 / (1 + Math.pow(Math.E, (0.1) * -avgBestScore));
			//double sigNaive = 1 / (1 + Math.pow(Math.E, (0.1) * -avgNaiveScore));
			//double sigRandom = 1 / (1 + Math.pow(Math.E, (0.1) * -randomScoreBest));

			// sum weighted win and sig-score values
			double summedBest = 0.9 * avgBestWin + 0.1 * sigBest;
			//double summedNaive = 0.9 * avgNaiveWin + 0.1 * sigNaive;
			//double summedRandom = 0.9 * avgRandomWin + 0.1 * sigRandom;

			// calc game score differences
			double gameScore = (summedBest - summedRandom);

			// allows rounding up due to weird scores
			/*if(gameScore < -0.0005) {

					gameScore = 0;
				}*/
			// reward fitness for each unique interaction triggered
			//int uniqueCount = events.size();
			// add a normalized unique count to the fitness
			//double rulesTriggered = uniqueCount / (ruleset[0].length * 1.0f + 1);

			// fitness is calculated by weight summing the 2 variables together

			//double fitness = gameScore + 1 * (rulesTriggered);
			constrainFitness = 1.0;
			this.fitness.set(0, constrainFitness);
			this.fitness.set(1, gameScore);
			//this.fitness.set(2, rulesTriggered);
		} 
		/*if(this.fitness.get(0)==1) {
			return true;
		}
		else{
			return false;
		}*/
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
	/**
	 * Play the current level using the naive player
	 * @param stateObs	the current stateObservation object that represent the level
	 * @param steps		the maximum amount of steps that it shouldn't exceed it
	 * @param agent		current agent to play the level
	 * @return			the number of steps that the agent stops playing after (<= steps)
	 */

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
		try {

		}catch (Exception e) {
			// TODO: handle exception
		}
		for(i=0;i<steps;i++){
			if(stateObs.isGameOver()){
				break;
			}
			ElapsedCpuTimer timer = new ElapsedCpuTimer();
			timer.setMaxTimeMillis(EVALUATION_STEP_TIME);
			Types.ACTIONS bestAction = agent.act(stateObs, timer);
			stateObs.advance(bestAction);
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
	
	
	/**
	 * crossover the current chromosome with the input chromosome
	 * @param c	the other chromosome to crossover with
	 * @return	the current children from the crossover process
	 */


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
}
