import java.io.*;
import java.util.*;

public class main {
    public static void main(String[] args) {
        ArrayList<String[]> linesArr = new ArrayList<String[]>();
        try {
            File myFile = new File("C:\\nlp_final_project\\HW3\\WSJ_POS_CORPUS_FOR_STUDENTS\\SRL2_1178900.txt");  //Opens training corpus file for reading
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

                String[] dataArr = (curr.split("\\s+", 2));
                //System.out.println(dataArr[0]);
                //System.out.println(dataArr[1]);
                if(dataArr.length > 1) {
    	            String temp = dataArr[0];
    	            dataArr[0] = dataArr[1];
    	            dataArr[1] = temp;
    	            linesArr.add(dataArr);
                }
                
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
            parsedUntagged = untaggedLines("C:\\nlp_final_project\\HW3\\WSJ_POS_CORPUS_FOR_STUDENTS\\SRL3_9988untagged.txt");
            ArrayList<String[]> untaggedSents = new ArrayList<String[]>(); // each String[] is a sentence
            untaggedSents = createSentences(parsedUntagged);
            List<List<String[]>> sols = new ArrayList<>();

            for(int i = 0; i < untaggedSents.size(); i++){
                sols.add(assignTagsToSentence(untaggedSents.get(i), occProb, transProb, tagsList));
            }
            writeTaggedToFile("C:\\nlp_final_project\\HW3\\sols.txt", sols);
            
            // scoring output works after adding an empty line to tagged txts
            // sols list is currently in a different order than how its supposed to be read, but its fine
            float score = compareFiles("C:\\nlp_final_project\\HW3\\WSJ_POS_CORPUS_FOR_STUDENTS\\SRL3_9988tagged.txt", sols); 
            System.out.println(score);
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
                String[] sentSplit = sentence.split("~");   //Split the sentence up by "-" (each word has "-" added to it before being added to the sentence)
                sentences.add(sentSplit);   //Add the resulting array of each word in the current sentence to the nested array "sentences"
                sentence = "";  //Reset sentence to an empty string, ready for the next sentence
            } else {
                sentence += "~" + lines.get(i);  //Add "-" for the end sentence split, and add the word to sentence
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
        /*
        for (int i = 0; i < sentence.length; i++) {
            System.out.print(sentence[i] + " ");
        }
        System.out.println();*/
        String prev_tag = "Begin_Sent";
        String prev_word = "BEGIN_SENT";
        for (int i = 0; i < sentence.length; i++) {
            String currWord = sentence[i];
            if (i == 0) {
                String[] firstWord = {sentence[i], prev_tag};
                sol.add(firstWord);
            } else {
                String[] currWordWithTag = {currWord, mostLikelyOOV(currWord, wordTagsHash, transProbHash, prev_tag, prev_word)};
                //String[] currWordWithTag = {currWord, "OOV"};
                //sol.add(currWordWithTag);
                //prev = currWordWithTag[1];
                //prev = "OOV";
                if (currWordWithTag[1] != null) {
                    //System.out.println(currWordWithTag[0] + " - " + currWordWithTag[1] + "\n");
                    sol.add(currWordWithTag);
                    prev_tag = currWordWithTag[1];
                    prev_word = currWordWithTag[0];
                } else {
                    List<String> possibleTags = wordTagsHash.get(sentence[i]);
                    Hashtable<String, Float> possibleTagScores = new Hashtable<>();
                    for (int j = 0; j < possibleTags.size(); j++) {
                        String currPossTag = possibleTags.get(j);
                        float transProb = 0.5f;
                        if(prev_tag != "OOV") {
                            if(transProbHash.get(prev_tag).get(currPossTag) == null) {
                                transProb = 0f;
                            } else {
                                transProb = transProbHash.get(prev_tag).get(currPossTag);
                            }
                        } 
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
                    currWordWithTag[1] = maxTag;
                    sol.add(currWordWithTag);
                    prev_tag = maxTag;
                    prev_word = currWordWithTag[0];
                }
            }
        }
        sol.remove(0);
        /*
        for (int i = 0; i < sol.size(); i++) {
            System.out.print(sol.get(i)[0] + " ");
        }
        System.out.println();*/
        return sol;
    }

    public static void writeTaggedToFile(String filePath, List<List<String[]>> taggedSentences) {
        File newFile = new File(filePath);
        try {
            System.out.println(taggedSentences.size());
            FileWriter myWriter = new FileWriter(newFile, false);
            for (int i = 0; i < taggedSentences.size(); i++) {
                List<String[]> currSentence = taggedSentences.get(i);
                for (int j = 0; j < currSentence.size(); j++) {
                    String currWord = currSentence.get(j)[0];
                    String currTag = currSentence.get(j)[1];
                    myWriter.write(currTag + "\t" + currWord + "\n");
                }
                myWriter.write("\n");
            }
            myWriter.close();
        } catch (IOException e) { 
            System.out.println("IO error.");
            e.printStackTrace();
        }
    }

    public static float compareFiles(String taggedFilePath, List<List<String[]>> solution) {
        float score = 0;
        int counter = 0;
        int numOfWords = 0;
        try {
            File taggedFile = new File(taggedFilePath);
            Scanner taggedScanner = new Scanner(taggedFile);
            String testLine = "";
            for (int i = 0; i < solution.size(); i++) {
                for (int j = 0; j < solution.get(i).size(); j++) {
                    String[] currWord =solution.get(i).get(j);
                    testLine = taggedScanner.nextLine();
                    String[] testSplit = testLine.split("\\s+");
                    //System.out.println("word: " + currWord[0] + " - " + currWord[1] + "=== " + "test: " + testSplit[0] + " - " + testSplit[1]);
                    if (currWord[0].equals(testSplit[1]) && currWord[1].equals(testSplit[0])) {
                        counter++;
                    }
                    numOfWords++;
                }
                taggedScanner.nextLine();
            }
            
            score = (float)counter/(float)numOfWords;
            System.out.println(numOfWords + " " + counter + " " + score);
            taggedScanner.close();
        } catch (FileNotFoundException e) {
            System.out.println("File not found");
            e.printStackTrace();
        }
        return score;
    }

    public static String mostLikelyOOV(String currWord, Hashtable<String, List<String>> wordTagsHash, Hashtable<String, Hashtable<String, Float>> transProbHash, String prev_tag, String prev_word){
        float max = 0f;
        String currTag = "";
        //if (prev_tag.equals("NOW") && currWord.equals("there")) {
        //   currTag = "NIL";
        //} 
        //else 
        //if (prev_tag.equals("PRO") && currWord.equals("have")) {
        //    currTag = "NEC";
        //}
         //else if (prev_word.equals("Let") && currWord.equals("'s")) {
           // currTag = "PRO";
        //} 
        //else if (prev_tag.equals("PER") && currWord.equals("'s")) {
        //    currTag = "REL";    
        //}
        //else
        if (wordTagsHash.get(currWord) == null) {
            Set<String> possTags = transProbHash.get(prev_tag).keySet();
            for (String tag : possTags){
                float tagPoss = transProbHash.get(prev_tag).get(tag);
                if (tagPoss > max){
                    currTag = tag;
                    max = tagPoss;
                }
            }
            return currTag;
        }
        if (currTag == "") {
            return null;
        } else {
            return currTag;
        }
    }// make this return a float(max) to give a probability instead of a pos when we add hard coded rules
}
