import java.io.*;
import java.util.*;

public class main {
    public static void main(String[] args) {
        ArrayList<String[]> linesArr = new ArrayList<String[]>();
        try {
            File myFile = new File("C:\\HW3\\WSJ_POS_CORPUS_FOR_STUDENTS\\WSJ_02-21.pos");  //Opens training corpus file for reading
            Scanner myScanner = new Scanner(myFile);    //Create scanner to read file
            String prev = myScanner.nextLine(); //Getting first line (first word + tag of the corpus)
            String[] tempDataArr = prev.split("\\s+"); //Splits first line into word and tag
            String[] firstEle = {tempDataArr[0], "Begin_Sent"}; //Replaces first word's tag with "Begin_Sent" (means this word is the beginning of a sentence)
            linesArr.add(firstEle); //Adding first element to linesArr. These three lines is just because I couldnt get the custom Begin Sent for the first word in the while loop bellow, so i did it outside

            String curr = myScanner.nextLine();
            String[] secondEle = curr.split("\\s+");
            linesArr.add(secondEle);
            
            String next = myScanner.nextLine();
            while(true) { 
                prev = curr; //Assigns prev to current line
                curr = next;   //Moves current line down one
                if (curr.isEmpty() && myScanner.hasNextLine()) {
                    prev = curr;
                    curr = myScanner.nextLine();
                } 
                if(prev.isEmpty()) {    //This should be a check for beginning of sentence. If previous line is empty, current line is the start of a new sentence 
                    String[] beg = {"BEGIN_SENT", "Begin_Sent"};
                    linesArr.add(beg);                        
                } 

                String[] dataArr = (curr.split("\\s+"));
                linesArr.add(dataArr);
                
                if(myScanner.hasNextLine() && next.isEmpty()) { //This checks if we're at the end of a sentence (next line is blank or "empty")
                    String[] end = {"END_SENT", "End_Sent"}; 
                    linesArr.add(end);
                } 

                if (!myScanner.hasNextLine()) {
                    break;
                } else {
                    next = myScanner.nextLine();
                }
            }   //At the end of this while loop, linesArr is an arraylist of arrays, where each array is a word and its appropriate tag.
            
            Hashtable<String, Hashtable<String, Integer>> occ = createOccHash(linesArr); //Create an occurence matrix out of linesArr
            Hashtable<String, Hashtable<String, Integer>> prob = createTransHash(linesArr);  
            Hashtable<String, Hashtable<String, Float>> occProb = new Hashtable<>();
            occProb = createOccProbHash(occ);
            Hashtable<String, Hashtable<String, Float>> transProb = new Hashtable<>();
            transProb = createOccProbHash(prob);
            Hashtable<String, List<String>> tagsList = new Hashtable<>();
            tagsList = createWordTagsHash(occ);
            
            ArrayList<String> parsedUntagged = new ArrayList<String>(); // 
            parsedUntagged = untaggedLines("C:\\HW3\\WSJ_POS_CORPUS_FOR_STUDENTS\\WSJ_23.words");
            ArrayList<String[]> untaggedSents = new ArrayList<String[]>(); // each String[] is a sentence
            untaggedSents = createSentences(parsedUntagged);
            List<List<String[]>> sols = new ArrayList<>();

            for(int i = 0; i < untaggedSents.size(); i++){
                sols.add(assignTagsToSentence(untaggedSents.get(i), occProb, transProb, tagsList));
            }
            writeTaggedToFile("C:\\HW3\\sols.txt", sols);
            
            myScanner.close();          //Close the file scanner


        } catch (FileNotFoundException e) { //File reading exception
            System.out.println("File not found.");
            e.printStackTrace();
        }
    }

    //untagged corpus parser
    public static ArrayList<String> untaggedLines(String filePath) {
        ArrayList<String> untaggedLines = new ArrayList<>();
        try {
            File myFile = new File(filePath);
            Scanner myScanner = new Scanner(myFile);
            untaggedLines.add("BEGIN_SENT");
            while(myScanner.hasNextLine()) {
                String curr = myScanner.nextLine();
                if (curr.isEmpty() && myScanner.hasNextLine()) {
                    untaggedLines.add("END_SENT");
                    untaggedLines.add("BEGIN_SENT");
                } else if (!curr.isEmpty()) {
                    untaggedLines.add(curr);
                }
            }
            untaggedLines.add("END_SENT");
            myScanner.close();
        } catch (FileNotFoundException e) { //File reading exception
            System.out.println("File not found.");
            e.printStackTrace();
        }
        return untaggedLines;
    }

    //Creating an occurence matrix out of the array of words and their tags
    //The matrix will look like this: {tag1: {word1: # of times it has tag1, word2: # of times it has tag1}}, tag2: {...}, ...}
    public static Hashtable<String, Hashtable<String, Integer>> createOccHash(ArrayList<String[]> lines) { //Pass in linesArr, created in main, as the parameter
        Hashtable<String, Hashtable<String, Integer>> pos = new Hashtable<>();  //Hashtable of a hashtable, think of it as dict of dict
       
        //lines[i][0] = word at i
        //lines[i][1] = tag at i
        for(int i = 0; i < lines.size()-1; i++) {
            //System.out.println(lines.get(i)[0] + " - " + lines.get(i)[1]);
            //System.out.println("yo");
            if (pos.get(lines.get(i)[1]) == null) { //If key for current tag doesnt exist yet 
                Hashtable<String, Integer> nestedHash = new Hashtable<>();  //Create a hashtable for this word
                nestedHash.put(lines.get(i)[0], 1); //Add the word as the key, with value 1 (first occurence of the word with that tag)
                pos.put(lines.get(i)[1], nestedHash);   //Add the tag as the key, and hashtable above (word: 1) as value, into the occurence matrix
                //So lets say the occurence matrix doesnt have "verb" as a key yet, and you encounter "run". Then you make a hashtable of {run: 1}, since 
                //we found an instance of run. You create a new hashtable with the tag as the key {verb: ___}, and the previous hashtable as the value. Now
                //we have {verb: {run: 1}}, successfully initializing the tag's hashtable
            } else if (pos.get(lines.get(i)[1]) != null && pos.get(lines.get(i)[1]).get(lines.get(i)[0]) == null) { //Tag exists as a key, but word does not exist under that tag yet
                (pos.get(lines.get(i)[1])).put(lines.get(i)[0],1);  //Add {word: 1} to that word's tag tashtable
                //So we find the verb run, and verb is {verb: {walk: 2, speak: 3}}. Then the above adds {run: 1} to it, making it {verb: {walk: 2, speak: 3, run: 1}}
            } else { //Tag exists as a key, and word exists as a key within that tag's nested hashtable
                String tag = lines.get(i)[1];   //Just making it simpler so the int oldval line isnt the length of my monitor
                String word = lines.get(i)[0];
                int oldVal = pos.get(tag).get(word); //Find the current value of the word under its current tag
                pos.get(tag).replace(word, oldVal, oldVal+1); //Replace that value with value + 1, incrementing it
            }
        }
        //System.out.println(pos.get("DT").toString());
        return pos; //Return occurence matrix
    }

    //Create a probability matrix, for "transition probability"
    //Same as occurence matrix, except both keys and values are tags
    //We're trying to find what are the odds that a tag is followed by another tag
    //So matrix looks like: {tag1: {tag2: # of times tag2 follows tag1, tag3: # of times tag3 follows tag1, ...}, tag2: {...}}
    public static Hashtable<String, Hashtable<String, Integer>> createTransHash(ArrayList<String[]> lines) {
        Hashtable<String, Hashtable<String, Integer>> states = new Hashtable<>();
        for(int i = 0; i < lines.size()-2; i++) {
            String tag = lines.get(i)[1]; //Get current word's tag
            String nextTag = lines.get(i+1)[1]; //Get next word's tag
            if (states.get(tag) == null) {  //If current tag does not exist as a key yet, need to initialize it
                Hashtable<String, Integer> nestedHash = new Hashtable<>();  //Create the hashtable for curr tag's first element ({nextTag: 1})
                nestedHash.put(lines.get(i+1)[1], 1);   //Populate first hashtable with {next word's tag: 1}
                states.put(tag, nestedHash);    //Put {curr word's tag: {next word's tag: 1}} into the transition matrix
            } else if (states.get(tag) != null && states.get(tag).get(nextTag) == null) {   //Curr word tag exists as a key, but next word tag does not exist as an element of that key
                states.get(tag).put(nextTag, 1);    //Insert {next word tag: 1} into {curr word tag: {...}}
            } else {    //Curr word tag exists as key, next word tag exists as value of that key
                int oldVal = states.get(tag).get(nextTag);  //Get current value for next word tag
                states.get(tag).replace(nextTag, oldVal, oldVal+1); //Replace it with value +1
            }
        }
        return states;
        //System.out.println(states.toString());
    }

    //Create sentences out of linesArr from an UNTAGGED corpus, basically just group words into sentences in a nested array
    public static ArrayList<String[]> createSentences(ArrayList<String> lines) {
        ArrayList<String[]> sentences = new ArrayList<>(); //Init arraylist for all the sentences
        String sentence = "";   //Init an empty sentence to hold the words
        for(int i = 0; i < lines.size(); i++) { //Iterating through all the words and their tags
            if (lines.get(i) == "BEGIN_SENT") { //We're at the beginning of a sentence
                sentence += lines.get(i);    //Add the word to the sentence, (dont use "-" here cuz they're only in the middle of the sentence)
            } else if (lines.get(i) == "END_SENT") { //We're at the end of the sentence
                String[] sentSplit = sentence.split("-");   //Split the sentence up by "-" (each word has "-" added to it before being added to the sentence)
                sentences.add(sentSplit);   //Add the resulting array of each word in the current sentence to the nested array "sentences"
                sentence = "";  //Reset sentence to an empty string, ready for the next sentence
            } else {
                sentence += "-" + lines.get(i);  //Add "-" for the end sentence split, and add the word to sentence
            }
        }
        for(int i = 0; i < sentences.get(0).length; i++) {
            System.out.print(sentences.get(0)[i] + " "); //test
        }
        //System.out.println(sentences.get(0)[0]);
        return sentences; //return the nested array of sentences
    }

    //Creates a probability hashtable out of the occurence hashtable
    //So instead of {verb: {run: 3}, noun: {run: 6}}
    //We not have {verb: {run: .33}, noun: {run: .66}}
    //This is useful for calculating word scores later on
    public static Hashtable<String, Hashtable<String, Float>> createOccProbHash(Hashtable<String, Hashtable<String, Integer>> occHash) { 
        Hashtable<String, Hashtable<String, Float>> occProb = new Hashtable<>();
        Hashtable<String, Integer> totals = new Hashtable<>();

        
        for (var mainTag: occHash.entrySet()) {
            for (var words: mainTag.getValue().entrySet()) {
                String word = words.getKey();
                int count = words.getValue();
                if (totals.get(word) == null) {
                    totals.put(word, count);
                } else {
                    int oldVal = totals.get(word);
                    totals.replace(word, oldVal+count);
                }
            }
        }

        /*  print totals
        for (var tags: totals.entrySet()) {
            String tag = tags.getKey();
            int val = tags.getValue();
            System.out.println(tag + " - " + val);
        } */

        for (var mainTags: occHash.entrySet()) {
            String mainTag = mainTags.getKey();
            for (var words: mainTags.getValue().entrySet()) {
                String word = words.getKey();
                int count = words.getValue();
                int total = totals.get(word);
                float prob = ((float)count)/total;
                if(occProb.get(mainTag) == null) {
                    Hashtable<String, Float> currWord = new Hashtable<>();
                    currWord.put(word, prob);
                    occProb.put(mainTag, currWord);
                } else if (occProb.get(mainTag).get(word) == null) {
                    Hashtable<String, Float> currWord = new Hashtable<>();
                    currWord.put(word, prob);
                    occProb.get(mainTag).putAll(currWord);
                } else {
                    occProb.get(mainTag).put(word, prob);
                }
            }
        }
        return occProb;

        /*print prob matrix
            for (var tagEntry : occProb.entrySet()) {
                String tag = tagEntry.getKey();
                // ...
                System.out.println(tag + " ------ ");
                for (var occTagEntry : tagEntry.getValue().entrySet()) {
                    String occTag = occTagEntry.getKey();
                    float eg = occTagEntry.getValue();
                    System.out.println(occTag + " - " + eg);
                    // ...
                }
            }*/
    }
    
    public static Hashtable<String, List<String>> createWordTagsHash(Hashtable<String, Hashtable<String, Integer>> occHash) {
        Hashtable<String, List<String>> wordTags = new Hashtable<>();

        for (var mainTags: occHash.entrySet()) {
            String mainTag = mainTags.getKey();
            for (var words: mainTags.getValue().entrySet()) {
                String word = words.getKey();
                if (wordTags.get(word) == null) {
                    List<String> newWordTag = new ArrayList<String>();
                    newWordTag.add(mainTag);
                    wordTags.put(word, newWordTag);
                } else {
                    wordTags.get(word).add(mainTag);
                }
            }
        }
        return wordTags;
    }

    public static List<String[]> assignTagsToSentence(String[] sentence, Hashtable<String, Hashtable<String, Float>> occProbHash, Hashtable<String, Hashtable<String, Float>> transProbHash, Hashtable<String, List<String>> wordTagsHash) {
        List<String[]> sol = new ArrayList<>();
        String prev = "Begin_Sent";
        for (int i = 0; i < sentence.length; i++) {
            String currWord = sentence[i];
            if (i == 0) {
                String[] firstWord = {sentence[i], prev};
                sol.add(firstWord);
            } else if (wordTagsHash.get(currWord) == null){
                String[] currWordWithTag = {currWord, "OOV"};
                sol.add(currWordWithTag);
                prev = "OOV";
            } else {
                List<String> possibleTags = wordTagsHash.get(sentence[i]);
                Hashtable<String, Float> possibleTagScores = new Hashtable<>();
                for (int j = 0; j < possibleTags.size(); j++) {
                    String currPossTag = possibleTags.get(j);
                    float transProb = transProbHash.get(prev).get(currPossTag);
                    float occProb = occProbHash.get(currPossTag).get(currWord);
                    float tagProb = transProb * occProb;
                    possibleTagScores.put(currPossTag, tagProb);
                }
                float currMax = 0;
                String maxTag = "";
                for (var tags: possibleTagScores.entrySet()) {
                    String tag = tags.getKey();
                    float prob = tags.getValue();
                    if (prob >= currMax) {
                        currMax = prob;
                        maxTag = tag;
                    }
                }
                String[] currWordWithTag = {currWord, maxTag};
                sol.add(currWordWithTag);
                prev = maxTag;
            }
        }
        sol.remove(0);
        sol.remove(sol.size()-1);
        return sol;
    }

    public static void writeTaggedToFile(String filePath, List<List<String[]>> taggedSentences) {
        File newFile = new File(filePath);
        try {
            FileWriter myWriter = new FileWriter(newFile);
            for (int i = 0; i < taggedSentences.size(); i++) {
                List<String[]> currSentence = taggedSentences.get(i);
                for (int j = 0; j < currSentence.size(); j++) {
                    String currWord = currSentence.get(i)[0];
                    String currTag = currSentence.get(i)[1];
                    myWriter.write(currWord + "\t" + currTag + "\n");
                }
                myWriter.write("\n");
            }
            myWriter.close();
        } catch (IOException e) { 
            System.out.println("IO error.");
            e.printStackTrace();
        }
    }
}

//WHAT IS MISSING *****
//So we have an occurence and probability matrix created using a pre-tagged training word bank
//So we first use the two methods to create those matrices ON THE TRAINING CORPUS
//We then use the createSentence matrix on a non-tagged development word bank, which groups everything into neat sentences
//Now we need an algorithm to use these two matrices to assign scores to each word's possible tags, and choose the highest score
//1. Each sentence begins with "Begin_Sent" as their tag, so automatically assign first word's tag as "Begin_Sent"
//  -This is just for the assignment though, irl we should include an EMPTY tag and include empty lines in the sentences
//  -That way we also have a transition probability for empty lines and we can determine the beginning tag that way, since it always follows an empty line
//2. For the next word, we look at the occurence matrix and see all the possible tags it could be, along with the percent probability that it is that tag
//  -This is done by taking occurence matrix, seeing that walk is a verb 100 times and a noun 50 times, so {walk: {verb: .66, noun: .33}}
//  -This is the actual use of the occurence matrix
//  -And yes, you need to create a seperate matrix for these per word tag probabilities (similar concept to the ones I made above though)
//3. Now look at the transition matrix for each tag you found in the occurence matrix, and look at the previous word's tag
//  -Since we're looking at current word, and previous word's tag is done, we just need to look up all the tags current word can have
//  -Then find the previous word's tag value for each of current word's tags, and find the probability juse like with the occurence matrix
//  -So say walk can be a verb or a noun, and the previous word is an adjective
//  -Then we look up {verb: {adjective: 25}, noun: {adjective: 75}}, so the transition score for verb is .25 and for adjective it's .75
//  -Helpful to just create a probability matrix out of the transition matrix, so that the above statement is already {verb: {adj: .25}, noun: {adj: .75}}
//4. Now you should have two probabilities per tag that the word can be: an occurence probability and a transition probability
//5. Multiply these two for each tag, choose the highest score, and assign the word that tag
//6. Lastly, need to write all of these tags to the development corpus and check against an answer key