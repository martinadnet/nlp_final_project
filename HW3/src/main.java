import java.io.*;
import java.util.*;

import javax.lang.model.util.ElementScanner14;

public class main {
    public static void main(String[] args) {
        ArrayList<String[]> linesArr = new ArrayList<String[]>();
        try {
            File myFile = new File("C:\\nlp_final_project\\HW3\\WSJ_POS_CORPUS_FOR_STUDENTS\\cut_silver.txt");  //Opens training corpus file for reading
            Scanner myScanner = new Scanner(myFile);
            String prev = myScanner.nextLine(); 
            String[] tempDataArr = prev.split("\\s+"); 
            String[] firstEle = {tempDataArr[0], "Begin_Sent"}; 
            linesArr.add(firstEle);

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
            
            ArrayList<String> parsedUntagged = new ArrayList<String>();
            parsedUntagged = untaggedLines("C:\\nlp_final_project\\HW3\\WSJ_POS_CORPUS_FOR_STUDENTS\\untagged_part.txt");
            ArrayList<String[]> untaggedSents = new ArrayList<String[]>();
            untaggedSents = createSentences(parsedUntagged);
            List<List<String[]>> sols = new ArrayList<>();

            for(int i = 0; i < untaggedSents.size(); i++){
                sols.add(assignTagsToSentence(untaggedSents.get(i), occProb, transProb, tagsList));
            }
            writeTaggedToFile("C:\\nlp_final_project\\HW3\\sols.txt", sols);
            
            float score = compareFiles("C:\\nlp_final_project\\HW3\\WSJ_POS_CORPUS_FOR_STUDENTS\\cut_part.txt", sols); 
            System.out.println(score);
            myScanner.close();          //Close the file scanner



        } catch (FileNotFoundException e) { //File reading exception
            System.out.println("File not found.");
            e.printStackTrace();
        }
    }

    public static void findWeirdTags(Hashtable<String, Hashtable<String, Float>>occHash) {
        for (var mainTags: occHash.entrySet()) {
            String mainTag = mainTags.getKey();
            if (mainTag.length() > 3 || mainTag.length() < 3) {
                System.out.println(mainTag + ":\n" + occHash.get(mainTag));
            }
        }
    }

    public static void findWeirdOccs(Hashtable<String, Hashtable<String, Integer>>occHash) {
        for (var mainTags: occHash.entrySet()) {
            String mainTag = mainTags.getKey();
            for (var words: mainTags.getValue().entrySet()) {
                String word = words.getKey();
                if (word.equals("French"))  
                    System.out.println(mainTag + ":\n" + occHash.get(mainTag));
            }
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
        } catch (FileNotFoundException e) { 
            System.out.println("File not found.");
            e.printStackTrace();
        }
        return untaggedLines;
    }

    //Creating an occurence matrix out of the array of words and their tags
    //The matrix will look like this: {tag1: {word1: # of times it has tag1, word2: # of times it has tag1}}, tag2: {...}, ...}
    public static Hashtable<String, Hashtable<String, Integer>> createOccHash(ArrayList<String[]> lines) {
        Hashtable<String, Hashtable<String, Integer>> pos = new Hashtable<>();  
        for(int i = 0; i < lines.size()-1; i++) {
            if (pos.get(lines.get(i)[1]) == null) { //If key for current tag doesnt exist yet 
                Hashtable<String, Integer> nestedHash = new Hashtable<>();  //Create a hashtable for this word
                nestedHash.put(lines.get(i)[0], 1); //Add the word as the key, with value 1 (first occurence of the word with that tag)
                pos.put(lines.get(i)[1], nestedHash);   //Add the tag as the key, and hashtable above (word: 1) as value, into the occurence matrix
            } else if (pos.get(lines.get(i)[1]) != null && pos.get(lines.get(i)[1]).get(lines.get(i)[0]) == null) { //Tag exists as a key, but word does not exist under that tag yet
                (pos.get(lines.get(i)[1])).put(lines.get(i)[0],1);  
            } else { 
                String tag = lines.get(i)[1];  
                String word = lines.get(i)[0];
                int oldVal = pos.get(tag).get(word);
                pos.get(tag).replace(word, oldVal, oldVal+1); 
            }
        }
        return pos; //Return occurence matrix
    }

    //Create a probability matrix, for "transition probability"
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
            } else {  
                int oldVal = states.get(tag).get(nextTag);  
                states.get(tag).replace(nextTag, oldVal, oldVal+1); 
            }
        }
        return states;
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
        return sentences;
    }

    //Creates a probability hashtable out of the occurence hashtable
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
        String prev_tag = "Begin_Sent";
        String prev_word = "BEGIN_SENT";
        for (int i = 0; i < sentence.length; i++) {
            String currWord = sentence[i];
            if (i == 0) {
                String[] firstWord = {sentence[i], prev_tag};
                sol.add(firstWord);
            } else {
                String[] currWordWithTag = {currWord, mostLikelyOOV(currWord, wordTagsHash, transProbHash, occProbHash, prev_tag, prev_word)};
                if (currWordWithTag[1] != null) {
                    sol.add(currWordWithTag);
                    prev_tag = currWordWithTag[1];
                    prev_word = currWordWithTag[0];
                } else {
                    String maxTag = findMaxTag(occProbHash, transProbHash, wordTagsHash, currWord, prev_tag); 
                    currWordWithTag[1] = maxTag;
                    sol.add(currWordWithTag);
                    prev_tag = maxTag;
                    prev_word = currWordWithTag[0];
                }
            }
        }
        sol.remove(0);
        return sol;
    }

    public static String findMaxTag(Hashtable<String, Hashtable<String, Float>> occProbHash, Hashtable<String, Hashtable<String, Float>> transProbHash, Hashtable<String, List<String>> wordTagsHash, String word, String prev_tag) {
        List<String> possibleTags = wordTagsHash.get(word);
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
            float occProb = occProbHash.get(currPossTag).get(word);
            float tagProb = transProb*occProb;
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
        return maxTag;
    }

    public static String mostLikelyOOV(String currWord, Hashtable<String, List<String>> wordTagsHash, Hashtable<String, Hashtable<String, Float>> transProbHash, Hashtable<String, Hashtable<String, Float>> occProbHash, String prev_tag, String prev_word){
        float max = 0f;
        String currTag = "";
        List<String> pronouns = Arrays.asList("he", "she", "I", "they", "it", "we", "He", "She", "They", "It", "We");
        
        if (prev_tag.equals("NOW") && (currWord.equals("there") || currWord.equals("There"))) {
          currTag = "NIL";
        } 
        else 
        if (prev_tag.equals("PRO") && (currWord.equals("have") || currWord.equals("Have"))) {
            currTag = "NEC";
        }
        else
        
         if ((prev_word.equals("Let") || prev_word.equals("let")) && currWord.equals("'s")) {
            currTag = "PRO";
        } 
        else
        if (prev_tag.equals("PER") && currWord.equals("'s")) {
            currTag = "REL";    
        } else 
        if (prev_tag.equals("ROL") && Character.isUpperCase(currWord.charAt(0))){
            currTag = "PER";
        }
        else 
        if ((prev_word.equals("speaks") || prev_word.equals("talk") || prev_word.equals("talks") || prev_word.equals("Speak") || prev_word.equals("Speaks") || prev_word.equals("Talk") || prev_word.equals("Talks") || prev_word.equals("speak")) && Character.isUpperCase(currWord.charAt(0))) {
           currTag = "CON";
        } else 
        if (currWord.equals("?")) {
            currTag = "QUE";
        } else 
        if ((prev_word.equals("that") || prev_word.equals("That")) && currWord.equals("'s")) {
            currTag = "NOW";
        } else
        if ((prev_word.equals("what") || prev_word.equals("What")) && currWord.equals("'s")) {
            currTag = "ENS";
        } else
        if ((prev_word.equals("how") || prev_word.equals("How")) && currWord.equals("'s")) {
            currTag = "NOW";
        } else
        if (wordTagsHash.get(currWord) == null && wordTagsHash.get(currWord.toLowerCase()) == null) {
            Set<String> possTags = transProbHash.get(prev_tag).keySet();
            for (String tag : possTags){
                float tagPoss = transProbHash.get(prev_tag).get(tag);
                if (tagPoss > max){
                    currTag = tag;
                    max = tagPoss;
                }
            }
        } else 
        if (wordTagsHash.get(currWord) == null && wordTagsHash.get(currWord.toLowerCase()) != null) {
            String currWordLower = currWord.toLowerCase();
            currTag = findMaxTag(occProbHash, transProbHash, wordTagsHash, currWordLower, prev_tag);
        }
        if (currTag.equals("")) {
            return null;
        } 
        else if (currTag.equals("French")) {
            return "PER";    
        } 
        else {
             return currTag;
        }
    }

    public static void writeTaggedToFile(String filePath, List<List<String[]>> taggedSentences) {
        File newFile = new File(filePath);
        try {
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
                    if (currWord[0].equals(testSplit[1]) && currWord[1].equals(testSplit[0])) {
                        counter++;
                    }
                    numOfWords++;
                }
                taggedScanner.nextLine();
            }
            
            score = (float)counter/(float)numOfWords;
            System.out.println("Number of words: " + numOfWords + " - Number correctly tagged: " + counter + " - Percent accuracy: " + score*100 + "%"); 
    
            taggedScanner.close();
        } catch (FileNotFoundException e) {
            System.out.println("File not found");
            e.printStackTrace();
        }
        return score;
    }

    
}
