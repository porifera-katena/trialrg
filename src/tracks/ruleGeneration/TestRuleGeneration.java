package tracks.ruleGeneration;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Random;

import tracks.ruleGeneration.climbRuleGenerator.SharedData;

/**
 * Created by dperez on 19/03/2017.
 */
public class TestRuleGeneration {
	//private int seed = 0;
	public static void main(String[] args) throws Exception {
		//Available Controllers
		String sampleMCTSController = "tracks.singlePlayer.advanced.sampleMCTS.Agent";
		String sampleOLETSController = "tracks.singlePlayer.advanced.olets.Agent";
		String simpleRandomController = "tracks.singlePlayer.simple.simpleRandom.Agent";

		// Available Rule Generator


		String randomRuleGenerator = "tracks.ruleGeneration.randomRuleGenerator.RuleGenerator";
		String constructiveRuleGenerator = "tracks.ruleGeneration.constructiveRuleGenerator.RuleGenerator";
		String geneticRuleGenerator = "tracks.ruleGeneration.geneticRuleGenerator.RuleGenerator";
		String simEvoRuleGenerator = "tracks.ruleGeneration.simEvoRuleGenerator.RuleGenerator";

		// Available games:
		String gamesPath = "examples/gridphysics/";
		String physicsGamesPath = "examples/contphysics/";
		String generateRulePath = gamesPath;

		// All public games (gridphysics)
		String[] games = new String[]{"aliens", "angelsdemons", "assemblyline", "avoidgeorge", "bait", // 0-4
				"beltmanager", "blacksmoke", "boloadventures", "bomber", "bomberman", // 5-9
				"boulderchase", "boulderdash", "brainman", "butterflies", "cakybaky", // 10-14
				"camelRace", "catapults", "chainreaction", "chase", "chipschallenge", // 15-19
				"clusters", "colourescape", "chopper", "cookmepasta", "cops", // 20-24
				"crossfire", "defem", "defender", "digdug", "dungeon", // 25-29
				"eighthpassenger", "eggomania", "enemycitadel", "escape", "factorymanager", // 30-34
				"firecaster", "fireman", "firestorms", "freeway", "frogs", // 35-39
				"garbagecollector", "gymkhana", "hungrybirds", "iceandfire", "ikaruga", // 40-44
				"infection", "intersection", "islands", "jaws", "killBillVol1", // 45-49
				"labyrinth", "labyrinthdual", "lasers", "lasers2", "lemmings", // 50-54
				"missilecommand", "modality", "overload", "pacman", "painter", // 55-59
				"pokemon", "plants", "plaqueattack", "portals", "raceBet", // 60-64
				"raceBet2", "realportals", "realsokoban", "rivers", "roadfighter", // 65-69
				"roguelike", "run", "seaquest", "sheriff", "shipwreck", // 70-74
				"sokoban", "solarfox", "superman", "surround", "survivezombies", // 75-79
				"tercio", "thecitadel", "thesnowman", "waitforbreakfast", "watergame", // 80-84
				"waves", "whackamole", "wildgunman", "witnessprotection", "wrapsokoban", // 85-89
				"zelda", "zenpuzzle"}; // 90, 91

		// Other settings
		boolean visuals = true;
		Random random = new Random();
		int seed = random.nextInt();
		int gameIdx = 11;
		int levelIdx = 1;



		String recordActionsFile = "actions_" + games[gameIdx] + "_lvl" + levelIdx + "_" + seed + ".txt";
		// where to record the actions
		// executed. null if not to save.



		// level names from 0 to 4 (game_lvlN.txt).

		String game = generateRulePath + games[gameIdx] + ".txt";
		String level1 = gamesPath + games[gameIdx] + "_lvl" + levelIdx + ".txt";
		String recordGameFile = generateRulePath + games[gameIdx] + "_ggame_";

		// 1. Generate rules (Interaction and Terminations) for a fixed level

		String generatorPath = "tracks.ruleGeneration.";
		String generatorClass = "RuleGenerator.RuleGenerator";
		String Generators[] = {"genetic","simEvo","climb"};
		String Generator = "";
		int gameIds[] = {0,11,76};
		
		for(int i=0;i<gameIds.length;i++) {
			File file = new File("C:\\"+games[gameIds[i]]);
	        File files[] = file.listFiles();
	        for(File f:files) {
	        	String filename = f.getName();
	        	int genId = -1;
	        	for(int j=0;j<Generators.length;j++) {
	        		if(filename.contains(Generators[j])) {
	        			genId = j;
	        		}
	        	}
	        	
	        	game = generateRulePath + games[gameIds[i]] + ".txt";
				level1 = gamesPath + games[gameIds[i]] + "_lvl" + levelIdx + ".txt";
				recordActionsFile = "actions_" + games[gameIds[i]] + "_lvl" + levelIdx + "_" + random.nextInt() + ".txt";
				FileWriter fw = new FileWriter("C:\\"+games[gameIds[i]]+"\result"+games[gameIds[i]]+".txt", true);
				PrintWriter pw = new PrintWriter(new BufferedWriter(fw));
				
	        	for(int j=0;j<10;j++) {
	        		seed = random.nextInt();
	        		double[] result = RuleGenMachine.runOneGame(game, f.getAbsolutePath(), level1, false, sampleOLETSController, recordActionsFile, seed, 0);
	        		for(double d:result) {
	        			pw.print(d+",");
	        		}
	        		pw.print(":");
	        		result = RuleGenMachine.runOneGame(game, f.getAbsolutePath(), level1, false, simpleRandomController, recordActionsFile, seed, 0);
	        		for(double d:result) {
	        			pw.print(d+",");
	        		}
	        		pw.println();
	        		
	        	}

	            //ファイルに書き出す
	            pw.close();
	            fw.close();
	        }
	        files[0].getName();
		}
		
		
		RuleGenMachine.runOneGame(game, recordGameFile, level1, true, sampleOLETSController, recordActionsFile, seed, 0);
/*
		if(false) {
			for(int i=0;i<100;i++){
				gameIdx = i%gameIds.length;
				game = generateRulePath + games[gameIds[gameIdx]] + ".txt";
				level1 = gamesPath + games[gameIds[gameIdx]] + "_lvl" + levelIdx + ".txt";

				for(int j=0;j<Generators.length;j++) {
					if(i==0 && j<1) {
						break;
					}
					seed = seed+1;


					Generator = generatorPath + Generators[j] + generatorClass;


					recordGameFile = generateRulePath + games[gameIds[gameIdx]] + "_" + Generators[j] + "_" + seed + ".txt";

					try {
						RuleGenMachine.generateRules(game, level1, Generator, recordGameFile, seed);
						String record = recordGameFile+seed+".txt";
					}catch (Exception e) {
						// TODO: handle exception
					}
				}
			}
		}
		else {
			boolean generate = false;
			if (generate) {
				String record = recordGameFile+seed+"const.txt";
				RuleGenMachine.generateRules(game, level1, constructiveRuleGenerator, record, seed);
				for(int i=0;i<10;i++) {
					seed = seed+1;

					record = recordGameFile+seed+".txt";
					//game = generateRulePath + "boulderdash_ggame_1988196601.txt";
					RuleGenMachine.generateRules(game, level1, generatorPath + Generators[1] + generatorClass, record, seed);
				}
			}
			else {
				//recordGameFile = generateRulePath + "boulderdash_ggame_-1115206505.txt";
				for(int j=0;j<6;j++) {
					recordGameFile = "examples/generatedgame/sim/" + "solarfox_ggame_34345473"+ j + ".txt";
					for(int i=-0;i<1;i++) {

						RuleGenMachine.runOneGame(game, recordGameFile, level1, true, sampleOLETSController, recordActionsFile, seed, 0);
					}
				}

				//RuleGenMachine.playOneGame(game, recordGameFile, level1, recordActionsFile, seed);
			}
		}
*/




	}

}

