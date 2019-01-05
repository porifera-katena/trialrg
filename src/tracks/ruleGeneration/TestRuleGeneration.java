package tracks.ruleGeneration;

import java.util.Random;

/**
 * Created by dperez on 19/03/2017.
 */
public class TestRuleGeneration {
    //private int seed = 0;
    public static void main(String[] args) throws Exception {
	//Available Controllers
	String sampleMCTSController = "tracks.singlePlayer.advanced.sampleMCTS.Agent";
	String sampleOLETSController = "tracks.singlePlayer.advanced.olets.Agent";

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
        int gameIdx = 0;
        int levelIdx = 1;
        
        String recordActionsFile = "actions_" + games[gameIdx] + "_lvl" + levelIdx + "_" + seed + ".txt";
        // where to record the actions
        // executed. null if not to save.

        
        
         // level names from 0 to 4 (game_lvlN.txt).
        String game = generateRulePath + games[gameIdx] + ".txt";
        String level1 = gamesPath + games[gameIdx] + "_lvl" + levelIdx + ".txt";
        String recordGameFile = generateRulePath + games[gameIdx] + "_ggame_";
        
        // 1. Generate rules (Interaction and Terminations) for a fixed level
        
        
        
    	boolean generate = true;
    	if (generate) {
    		String record = recordGameFile+seed+"const.txt";
        	RuleGenMachine.generateRules(game, level1, constructiveRuleGenerator, record, seed);
    		for(int i=0;i<10;i++) {
            	seed = seed+1;
            	
            	record = recordGameFile+seed+".txt";
            	//game = generateRulePath + "boulderdash_ggame_1988196601.txt";
            	RuleGenMachine.generateRules(game, level1, simEvoRuleGenerator, record, seed);
            }
		}
    	else {
    		recordGameFile = generateRulePath + "boulderdash_ggame_-1115206505.txt";
            //RuleGenMachine.playOneGame(game, recordGameFile, level1, recordActionsFile, seed);
    		
            RuleGenMachine.runOneGame(game, recordGameFile, level1, true, sampleOLETSController, recordActionsFile, seed, 0);
    	}
        
        
    }

}

