//package errorAnalyzer;
import java.io.*;
import java.util.*;
public class compare {

	public static void main(String[] args) throws IOException {
        ArrayList<String[]> prev = new ArrayList<>();
        ArrayList<String[]> wrong = new ArrayList<String[]>();
        ArrayList<String[]> right = new ArrayList<String[]>();

        try {
        	BufferedReader reader1 = new BufferedReader(new FileReader("C:\\nlp_final_project\\HW3\\sols.txt"));
        	BufferedReader reader2 = new BufferedReader(new FileReader("C:\\nlp_final_project\\HW3\\WSJ_POS_CORPUS_FOR_STUDENTS\\cut_part.txt"));
        	String line1 = reader1.readLine();
        	String line2 = reader2.readLine();
            String prevRight = "";
        	while (line1 != null || line2 != null)
            {
                if(line1 == null || line2 == null)
                {
                    break;
                }
                else if(! line1.equals(line2))
                {
                    String[] p = prevRight.split("\\s+");
                    prev.add(p);
                	String[] w = line1.split("\\s+");
                    wrong.add(w);
                	String[] r = line2.split("\\s+");
                    right.add(r);
                }
                prevRight = line2;
                 
                line1 = reader1.readLine();
                 
                line2 = reader2.readLine();
            }
        	writeToFile("C:\\nlp_final_project\\HW3\\baselineTest.txt", prev, wrong, right);
            
            reader1.close();
            reader2.close();
            
        } catch (FileNotFoundException e) { 
            System.out.println("File not found.");
            e.printStackTrace();
        }
	}
	
	public static void writeToFile(String filePath1, ArrayList<String[]> prev, ArrayList<String[]> wrong, ArrayList<String[]> right) {
        File newFile = new File(filePath1);
        try {
            FileWriter myWriter = new FileWriter(newFile, false);
            for (int i = 0; i < wrong.size(); i++) {
                String[] curr = wrong.get(i);
                String currWord = curr[0];
                String currTag = curr[1];
                String[] prevWord = prev.get(i);
                if (prevWord.length > 1) {
                    myWriter.write("Prev: " + prevWord[1] + "\t" + prevWord[0] + "\n");
                }
                myWriter.write("Wrong: " + currTag + "\t" + currWord + "\n");
                curr = right.get(i);
                currWord = curr[0];
                currTag = curr[1];
                myWriter.write("Right: " + currTag + "\t" + currWord + "\n");
                myWriter.write("\n");
            }
            myWriter.close();
        } catch (IOException e) { 
            System.out.println("IO error.");
            e.printStackTrace();
        }
    }

}