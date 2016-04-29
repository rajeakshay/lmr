
package com.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Class to lexicographically merge files according to key.
 */
public class FileMerger
{
	/**
	 * Method to merge two files into one.
	 * @param currentFile
	 * @param previousFile
	 * @param mergedFile
	 * @throws IOException
	 */
	public static void merger (String currentFile, String previousFile, String mergedFile) throws IOException
	{
		String filename1 = currentFile;
		String filename2 = previousFile;
		String combinedFileName = mergedFile;
		String nextName1 = "";
		String nextName2 = "";
		File file1 = new File(filename1);
		File file2 = new File (filename2);  
		File combinedFile = new File (combinedFileName);
		if (file1.exists() && file2.exists()) {
			BufferedReader list1 = new BufferedReader (new FileReader(file1));
			BufferedReader list2 = new BufferedReader (new FileReader(file2)); 
			PrintWriter outputFile = new PrintWriter(combinedFile);
			nextName1 = list1.readLine();
			nextName2 = list2.readLine();
			while (nextName1 != null || nextName2 !=null ) {
				if(nextName1 != null && nextName2 != null) {
					String[] fileLine1 = nextName1.split("\t");
					String[] fileLine2 = nextName2.split("\t");
					try {
						String f1 = fileLine1[0];
						String f2 = fileLine2[0];
						if(f1.compareToIgnoreCase(f2) <= 0) {
							outputFile.println(nextName1);
							nextName1 = list1.readLine();
						} else {
							outputFile.println(nextName2);
							nextName2 = list2.readLine();
						}
					}  catch (ArrayIndexOutOfBoundsException e) {
						e.printStackTrace();
						System.out.println(nextName1);
						System.out.println(nextName2);
						nextName1 = list1.readLine();
						nextName2 = list2.readLine();
					}
				}
				else if(nextName1 != null && nextName2 == null) {
					outputFile.println(nextName1);
					nextName1 = list1.readLine();
				}
				else if(nextName1 == null && nextName2 != null) {
					outputFile.println(nextName2);
					nextName2 = list2.readLine();
				}				
			}
			outputFile.close();
			list1.close();
			list2.close();
		}
	}
}   