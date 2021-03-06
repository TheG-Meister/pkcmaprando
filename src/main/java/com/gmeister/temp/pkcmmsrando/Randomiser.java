package com.gmeister.temp.pkcmmsrando;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gmeister.temp.pkcmmsrando.map.data.Block;
import com.gmeister.temp.pkcmmsrando.map.data.BlockSet;
import com.gmeister.temp.pkcmmsrando.map.data.Map;
import com.gmeister.temp.pkcmmsrando.map.data.Warp;

public class Randomiser
{
	
	private Random random;
	
	public Randomiser()
	{
		this.random = new Random();
	}
	
	public Randomiser(Random random)
	{
		this.random = random;
	}
	
	public Block[] randomiseBlocksByCollision(BlockSet blockSet, Block[] blocks)
	{
		Random random = new Random(this.random.nextLong());
		Block[] randomisedBlocks = new Block[blocks.length];
		
		for (int i = 0; i < blocks.length; i++) if (blockSet.getBlocks().indexOf(blocks[i]) != 0)
		{
			ArrayList<Block> blockGroup = null;
			for (ArrayList<Block> group : blockSet.getCollGroups()) if (group.contains(blocks[i]))
			{
				blockGroup = group;
				break;
			}
			
			int index = random.nextInt(blockGroup.size());
			//inelegant solution, but works
			while (blockSet.getBlocks().indexOf(blockGroup.get(index)) == 0) index = random.nextInt(blockGroup.size());
			randomisedBlocks[i] = blockGroup.get(index);
			if (randomisedBlocks[i] == null || blockSet.getBlocks().indexOf(randomisedBlocks[i]) == 0) throw new IllegalStateException();
		}
		
		return randomisedBlocks;
	}
	
	public ArrayList<String> shuffleScriptLines(ArrayList<String> script, boolean[] toShuffle)
	{
		Random random = new Random(this.random.nextLong());
		ArrayList<String> shuffledScript = new ArrayList<>();
		
		for (int i = 0; i < toShuffle.length && i < script.size(); i++) if (toShuffle[i]) shuffledScript.add(script.get(i));
		Collections.shuffle(shuffledScript, random);
		for (int i = 0; i < script.size(); i++)
		{
			if (i >= toShuffle.length) shuffledScript.add(script.get(i));
			else if (!toShuffle[i]) shuffledScript.add(i, script.get(i));
		}
		
		return shuffledScript;
	}
	
	public ArrayList<String> shuffleMusicPointers(ArrayList<String> script)
	{
		boolean[] toShuffle = new boolean[script.size()];
		for (int i = 0; i < script.size(); i++) toShuffle[i] = script.get(i).startsWith("\tdba") && !script.get(i).contains("Music_Nothing");
		return this.shuffleScriptLines(script, toShuffle);
	}
	
	public ArrayList<String> shuffleSFXPointers(ArrayList<String> script)
	{
		boolean[] toShuffle = new boolean[script.size()];
		for (int i = 0; i < script.size(); i++) toShuffle[i] = script.get(i).startsWith("\tdba");
		return this.shuffleScriptLines(script, toShuffle);
	}
	
	public void randomiseTrainerLocation(Map map)
	{
		Random random = new Random(this.random.nextLong());
		Pattern commentsPattern = Pattern.compile("\\s*;.*");
		Pattern mapConstPattern = Pattern.compile("\\tmap_const\\s+");
		Pattern commaWhitespacePattern = Pattern.compile("\\s*,\\s*");
		Pattern objectEventPattern = Pattern.compile("\\tobject_event\\s+");
		Pattern numberPattern = Pattern.compile("\\d+");
		
		for (int i = 0; i < map.getScript().size(); i++)
		{
			String line = map.getScript().get(i);
			if (objectEventPattern.matcher(line).find())
			{
				String argsLine = commentsPattern.matcher(line).replaceFirst("");
				argsLine = mapConstPattern.matcher(line).replaceFirst("");
				String[] args = commaWhitespacePattern.split(argsLine);
				
				if (args[9].equals("OBJECTTYPE_TRAINER"))
				{
					int newX = random.nextInt(map.getXCapacity());
					int newY = random.nextInt(map.getYCapacity());
					
					StringBuffer buffer = new StringBuffer();
					Matcher numbers = numberPattern.matcher(line);
					numbers.find();
					numbers.appendReplacement(buffer, String.valueOf(newX));
					numbers.find();
					numbers.appendReplacement(buffer, String.valueOf(newY));
					numbers.appendTail(buffer);
					
					map.getScript().set(i, buffer.toString());
				}
			}
		}
	}
	
	//Change this function such that, for every warp, it accepts a list of potential candidates it can randomise to?
	//Other methods can then run this one by filling up the groups
	//Won't this be a fuck tonne of array lists if there's like 600 warps?
	//Aren't we doing that anyway with the warps sources?
	
	/*
	 * Chunks of code we have here:
	 * Getting a random
	 * Getting the warp sources arrays
	 * Creating the list of old warp to new warp
	 * Various changes to said array depending on rules
	 * Actually applying the changes
	 */
	
	public void shuffleWarpDestinations(ArrayList<Warp> warps)
	{
		Random random = new Random(this.random.nextLong());
		ArrayList<ArrayList<Warp>> warpsSources = new ArrayList<>();
		
		//Create lists of warps that have their destination as each warp
		for (int i = 0; i < warps.size(); i++) warpsSources.add(new ArrayList<>());
		for (Warp warp : warps) if (warp.getDestination() != null) warpsSources.get(warps.indexOf(warp.getDestination())).add(warp);
		
		//Create a list of valid warp indices and a randomly reordered copy of it
		ArrayList<Integer> oldIndices = new ArrayList<>();
		for (int i = 0; i < warps.size(); i++) if (warpsSources.get(i).size() > 0 && warps.get(i).getDestination() != null) oldIndices.add(i);
		ArrayList<Integer> newIndices = new ArrayList<>(oldIndices);
		Collections.shuffle(newIndices, random);
		
		//Remove duplicate assignments from both lists
		ArrayList<Integer> usedIndices = new ArrayList<>();
		for (int i = 0; i < oldIndices.size(); i++)
		{
			while (i < oldIndices.size() && usedIndices.contains(oldIndices.get(i))) oldIndices.remove(i);
			while (i < newIndices.size() && usedIndices.contains(newIndices.get(i))) newIndices.remove(i);
			if (i < oldIndices.size()) usedIndices.add(oldIndices.get(i));
			if (i < newIndices.size()) usedIndices.add(newIndices.get(i));
		}
		
		this.editWarpDestinations(warps, warpsSources, oldIndices, newIndices);
	}
	
	public void shuffleWarpOrders(ArrayList<Warp> warps)
	{
		Random random = new Random(this.random.nextLong());
		ArrayList<ArrayList<Warp>> warpsSources = new ArrayList<>();
		
		//Create lists of warps that have their destination as each warp
		for (int i = 0; i < warps.size(); i++) warpsSources.add(new ArrayList<>());
		for (Warp warp : warps) if (warp.getDestination() != null) warpsSources.get(warps.indexOf(warp.getDestination())).add(warp);
		
		//Create a list of valid warp indices and a randomly reordered copy of it
		ArrayList<Integer> oldIndices = new ArrayList<>();
		for (int i = 0; i < warps.size(); i++) if (warpsSources.get(i).size() > 0 && warps.get(i).getDestination() != null) oldIndices.add(i);
		ArrayList<Integer> shuffledIndices = new ArrayList<>(oldIndices);
		Collections.shuffle(shuffledIndices, random);
		
		//Force warps to link the same maps together as vanilla
		ArrayList<Integer> newIndices = new ArrayList<>();
		for (int oldIndex : oldIndices) for (int shuffledIndex : shuffledIndices)
			if (!newIndices.contains(shuffledIndex) && warpsSources.get(oldIndex).get(0).getDestination().getMap().equals(warps.get(shuffledIndex).getDestination().getMap()))
		{
			newIndices.add(shuffledIndex);
			break;
		}
		
		//Remove duplicate assignments from both lists
		ArrayList<Integer> usedIndices = new ArrayList<>();
		for (int i = 0; i < oldIndices.size(); i++)
		{
			while (i < oldIndices.size() && usedIndices.contains(oldIndices.get(i))) oldIndices.remove(i);
			while (i < newIndices.size() && usedIndices.contains(newIndices.get(i))) newIndices.remove(i);
			if (i < oldIndices.size()) usedIndices.add(oldIndices.get(i));
			if (i < newIndices.size()) usedIndices.add(newIndices.get(i));
		}
		
		this.editWarpDestinations(warps, warpsSources, oldIndices, newIndices);
	}
	
	private void editWarpDestinations(ArrayList<Warp> warps, ArrayList<ArrayList<Warp>> warpsSources, ArrayList<Integer> oldIndices, ArrayList<Integer> newIndices)
	{
		//Create a list of warps to copy destinations from
		ArrayList<Warp> newDests = new ArrayList<>();
		for (int i = 0; i < warps.size(); i++) newDests.add(null);
		
		for (int i = 0; i < oldIndices.size(); i++)
		{
			int oldIndex = oldIndices.get(i);
			int newIndex = newIndices.get(i);
			
			//For each warp that leads to the old warp, change their destination to that of the new warp
			Warp newWarpWithData = warps.get(newIndex);
			for (Warp warp : warpsSources.get(oldIndex)) newDests.set(warps.indexOf(warp), newWarpWithData.getDestination());
			
			//For each warp that leads to the new warp, change their destination to that of the old warp
			Warp oldWarpWithData = warps.get(oldIndex);
			for (Warp warp : warpsSources.get(newIndex)) newDests.set(warps.indexOf(warp), oldWarpWithData.getDestination());
		}
		
		//Update each warp with its new destination
		for (int i = 0; i < warps.size(); i++)
		{
			Warp toEdit = warps.get(i);
			//if block to fix all moving rooms (goldenrod and celadon elevators, pokecenter 2F and fast ship 1F
			if (newDests.get(i) != null) toEdit.setDestination(newDests.get(i));
		}
	}
	
}