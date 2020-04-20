package tracks.ruleGeneration;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
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
		String cylindRuleGenerator = "tracks.ruleGeneration.cylinderP406.RuleGenerator";

		// Available games:
		String gamesPath = "examples/gridphysics/";
		String physicsGamesPath = "examples/contphysics/";
		String generateRulePath = "examples/generatedgame/";

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

		// Other settings 77
		boolean visuals = true;
		Random random = new Random();
		int seed = random.nextInt();
		int gameIdx = 0;
		int levelIdx = 1;



		String recordActionsFile = "actions_" + games[gameIdx] + "_lvl" + levelIdx + "_" + seed + ".txt";
		// where to record the actions
		// executed. null if not to save.



		// level names from 0 to 4 (game_lvlN.txt).

		String game = gamesPath + games[gameIdx] + ".txt";
		String level1 = gamesPath + games[gameIdx] + "_lvl" + levelIdx + ".txt";
		String recordGameFile = generateRulePath + games[gameIdx] + "_ggame_";

		// 1. Generate rules (Interaction and Terminations) for a fixed level

		String generatorPath = "tracks.ruleGeneration.";
		String generatorClass = ".RuleGenerator";
		String Generators[] = {"geneticRuleGenerator","simEvoRuleGenerator","climbRuleGenerator","cylinderP406"};
		String Generator = "";
		int gameIds[] = {0,11,76};
		String record = "collChecksimevo"+seed+".txt";
		String gamet = generateRulePath+"100/"+"aliens_ggame_-895003241cylinderP406.txt";
		
		for(int i=0;i<100;i++) {
			seed = random.nextInt();
			record = "collChecksimevo_2020_"+seed+".txt";
			RuleGenMachine.generateRules(game, level1, generatorPath + Generators[3] + generatorClass, record, seed);
		}
		
		File file = new File("examples/generatedgame/100/aliens/");
        File files[] = file.listFiles();
        for(File f:files) {
        	String filename = f.getName();
        	System.out.println(filename);
        	gamet = generateRulePath+"100/aliens/"+filename;
    		RuleGenMachine.runOneGame(game,gamet , level1, true,sampleOLETSController , null, seed, 0);
        }
        
		RuleGenMachine.runOneGame(game,gamet , level1, true,sampleOLETSController , null, seed, 0);
		RuleGenMachine.runOneGame(game,gamet , level1, true,simpleRandomController , null, seed, 0);
		RuleGenMachine.playOneGame(game, gamet, level1, recordActionsFile, seed);
		/*for(int i=0;i<100;i++) {
			seed = random.nextInt();
			record = "collChecksimevo_2020_"+seed+".txt";
			RuleGenMachine.generateRules(game, level1, generatorPath + Generators[3] + generatorClass, record, seed);
		}
		
		seed = random.nextInt();
		RuleGenMachine.generateRules(game, level1, generatorPath + Generators[3] + generatorClass, "collChecksimevo"+seed+".txt", seed);
		*/
		/*
		RuleGenMachine.playOneGame(game, generateRulePath+"aliens_mutated.txt", level1, recordActionsFile, seed);
*/
		/*while(true) {
			//RuleGenMachine.playOneGame(game, game, level1, recordActionsFile, seed);
			RuleGenMachine.playOneGame(game, "/Users/tomoya/eclipse-workspace/rulegen/examples/generatedgame/sim/boulderdash_ggame_215451621.txt", level1, recordActionsFile, seed);
		}*/
		/*
		 * /Users/tomoya/eclipse-workspace/rulegen/examples/generatedgame/sim/boulderdash_ggame_215451621.txt
		 * /Users/tomoya/eclipse-workspace/rulegen/examples/generatedgame/sim/boulderdash_ggame_-1156089430.txt
		 * /Users/tomoya/eclipse-workspace/rulegen/examples/generatedgame/sim/boulderdash_ggame_-1156089432.txt
		 * */
		// RuleGenMachine.runOneGame(game,game , level1, true,sampleOLETSController , null, seed, 0);
		/*for(int i=0;i<100;i++) {
			seed = random.nextInt();
			record = recordGameFile+seed+Generators[3]+".txt";
			System.out.println(generatorPath + Generators[3] + generatorClass);
			RuleGenMachine.generateRules(game, level1, generatorPath + Generators[3] + generatorClass, record, seed);
		}
		RuleGenMachine.runOneGame(game,record , level1, true, sampleOLETSController, "action_rec.txt", seed, 0);
		*/
		//for(int i=0;i<gameIds.length;i++) {
			/*File file = new File("examples/climb");
	        File files[] = file.listFiles();
	        for(File f:files) {
	        	String filename = f.getName();
				RuleGenMachine.runOneGame(game, f.getAbsolutePath(), level1, true, sampleOLETSController, recordActionsFile, seed,0);
	        	
				level1 = gamesPath + games[gameIdx] + "_lvl" + levelIdx + ".txt";
				recordActionsFile = null;//"actions_" + games[gameIds[i]] + "_lvl" + levelIdx + "_" + random.nextInt() + ".txt";

	        }
	        files[0].getName();*/
		//}
		
		
		//RuleGenMachine.runOneGame(game, game, level1, true, sampleOLETSController, recordActionsFile, seed, 0);
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

