
package com.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Class to merge files according to lexicographical order of keys present in each of them.
 * Assumption:
 * 1. Input files are already sorted in lexicographical order.
 * 2. Keys appear in the first column, rest of the data follows after a delimiter.
 */
public class FileMerger
{

    // Delimiter for data in columns (eg. tab, space, comma etc.). Default is a tab ("\t").
    private static String SEPARATOR = "\t";

	/**
	 * Method to merge two files into one with keys in lexicographical order.
	 * @param currentFile Path to the current file to be merged
	 * @param previousFile Path to the previous file to be merged with current file
	 * @param mergedFile Path to the merged file
	 * @throws IOException
	 */
	public static void merger (String currentFile, String previousFile, String mergedFile) throws IOException
	{
		String nextName1;
		String nextName2;
		File file1 = new File(currentFile);
		File file2 = new File (previousFile);
		File combinedFile = new File (mergedFile);
		if (file1.exists() && file2.exists()) {
			BufferedReader stream1 = new BufferedReader (new FileReader(file1));
			BufferedReader stream2 = new BufferedReader (new FileReader(file2));
			PrintWriter ostream = new PrintWriter(combinedFile);
			nextName1 = stream1.readLine();
			nextName2 = stream2.readLine();

            while(nextName1 != null && nextName2 !=null){
                String[] fileLine1 = nextName1.split(SEPARATOR);
                String[] fileLine2 = nextName2.split(SEPARATOR);
                try {
                    // Read the keys in first column of both files
                    String f1 = fileLine1[0];
                    String f2 = fileLine2[0];

                    if(fileLine1[0].equals("")){
                        // Skip empty line in first file
                        nextName1 = stream1.readLine();
                    }
                    else if(fileLine2[0].equals("")){
                        // Skip empty line in second file
                        nextName2 = stream2.readLine();
                    }
                    else{
                        // Do lexicographic comparison of keys
                        if(f1.compareToIgnoreCase(f2) <= 0) {
                            ostream.println(nextName1);
                            nextName1 = stream1.readLine();
                        } else {
                            ostream.println(nextName2);
                            nextName2 = stream2.readLine();
                        }
                    }
                }
                catch (ArrayIndexOutOfBoundsException e) {
                    // Skip that line and continue
                    System.out.println("=============================");
                    System.out.println("Line in file 1: " + nextName1);
                    System.out.println("Line in file 2: " + nextName2);
                    System.out.println("=============================");
                    e.printStackTrace();
                }
            }

            if(nextName1 == null){
                // File 1 has ended, so copy all lines from file 2
                while(nextName2 != null){
                    // Avoid copying blank lines
                    if(!nextName2.equals(""))
                        ostream.println(nextName2);
                    nextName2 = stream2.readLine();
                }
            }
            else{
                // File 2 has ended, so copy all lines from file 1
                while(nextName1 != null){
                    // Avoid copying blank lines
                    if(!nextName1.equals(""))
                        ostream.println(nextName1);
                    nextName1 = stream1.readLine();
                }
            }

            // Cleanup
			ostream.close();
			stream1.close();
			stream2.close();
		}
	}
}   