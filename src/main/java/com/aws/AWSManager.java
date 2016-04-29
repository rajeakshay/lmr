/**
 * @author Abhijeet Sharma, Deepen Mehta
 * @version 1.0  
 * @since April 8, 2016 
 */

package com.aws;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;

import org.apache.commons.lang3.StringUtils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.util.IOUtils;
import com.main.ClientMain;
import com.main.Context;
import com.main.ServerMain;
import com.map.Mapper;
import com.reduce.Reducer;
import com.sort.SortObject;
import com.utils.BufferedReaderIterable;
import com.utils.FileMerger;
import com.utils.FileUtils;
import com.utils.CollectionUtils;

/**
 * This class helps us manage connections to AWS services
 */
public class AWSManager {
	AmazonS3 s3;	
	final static short MAX_RETRY = 3;

	public AWSManager() {
		this.s3 = configureS3();
	}

	// Default region is US_EAST_1
	public AmazonS3 configureS3(){
		/*
		 * Create your credentials file at ~/.aws/credentials (C:\Users\USER_NAME\.aws\credentials for Windows users)
		 * and save the following lines after replacing the underlined values with your own.
		 *
		 * [default]
		 * aws_access_key_id = YOUR_ACCESS_KEY_ID
		 * aws_secret_access_key = YOUR_SECRET_ACCESS_KEY
		 */
		AWSCredentials credentials = null;
		try {
			credentials = new ProfileCredentialsProvider().getCredentials();
		} catch (Exception e) {
			throw new AmazonClientException(
					"Cannot load the credentials from the credential profiles file. " +
							"Please make sure that your credentials file is at the correct " +
							"location (~/.aws/credentials), and is in valid format.",
							e);
		}
		AmazonS3 s3 = new AmazonS3Client(credentials);
		Region usEast1 = Region.getRegion(Regions.US_EAST_1);
		s3.setRegion(usEast1);
		return s3;
	}

	/**
	 * This method gets all the files from S3 and sorts the files according to their size.These files are then partitioned
	 *  based on the number of slave nodes.File in each partition is then read and then the concerned value
	 * (temperature here) is put in an ArrayList.
	 * @param clientId
	 * @return
	 */
	public ArrayList<SortObject> getAllFiles(int clientId)  {
		ArrayList<SortObject> sortRecords = new ArrayList<SortObject>();
		short retryCount = 0;
		boolean retry = false;
		/* (Run by Client) */
		do{
			try {
				// fetching filenames depending on count
				ObjectListing objectListing = s3.listObjects(new ListObjectsRequest()
						.withBucketName(ClientMain.OUTPUT_BUCKET)
						.withPrefix(ClientMain.MAP_PATH + "/")
						.withDelimiter("/"));
				TreeMap<Long, String> filenamesMap = new TreeMap<Long, String>();
				for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
					filenamesMap.put(objectSummary.getSize(), objectSummary.getKey());			
				}
				TreeSet<String> filenamesTree = new TreeSet<String>(filenamesMap.values());
				filenamesTree.remove(ClientMain.MAP_PATH+"/");

				// save all filenames to a list
				ArrayList<String> fileList = new ArrayList<String>();
				fileList.addAll(filenamesTree);

				List<List<String>> listPartitions = CollectionUtils.partition(fileList, ClientMain.N_INSTANCES);
				String filenames = "";
				for (List<String> elementList : listPartitions){
					if (elementList.size() > clientId){
						filenames = StringUtils.join(new String[]{filenames, elementList.get(clientId)}, ",");
					}
				}

				filenames = filenames.substring(1);
				// fetching only part of actual data
				String[] filenamesList = filenames.split(",");
				int counter = 0;
				for (String filename : filenamesList) {
					S3Object s3object = this.s3.getObject(new GetObjectRequest(ClientMain.OUTPUT_BUCKET, filename));
					BufferedReader reader;
					try {
						reader = new BufferedReader(new InputStreamReader(s3object.getObjectContent()));
						String line;
						while ((line = reader.readLine()) != null) {
							String[] parsedString = line.split("\t");
							String key = parsedString[0];
							String val = StringUtils.join(Arrays.asList(parsedString).subList(1, parsedString.length), "\t");
							sortRecords.add(new SortObject(key, val));
						}
					} catch (IOException e) {
					}
					counter += 1;
				}
			}
			catch (AmazonServiceException ase) {
				System.out.println("Caught an AmazonServiceException, which " +
						"means your request made it " +
						"to Amazon S3, but was rejected with an error response" +
						" for some reason.");
				System.out.println("Error Message:    " + ase.getMessage());
				System.out.println("HTTP Status Code: " + ase.getStatusCode());
				System.out.println("AWS Error Code:   " + ase.getErrorCode());
				System.out.println("Error Type:       " + ase.getErrorType());
				System.out.println("Request ID:       " + ase.getRequestId());
				if (retryCount == MAX_RETRY){
					retry = false;
					System.out.println("Max Retry Limit achieved.Aborting Program: " + retryCount);
				}
				else{
					retry = true;
					retryCount += 1;
					System.out.println("Retrying step. Retry Count: " + retryCount);
				}
			}  
			catch (AmazonClientException ace) {
				System.out.println("Caught an AmazonClientException, which " +
						"means the client encountered " +
						"an internal error while trying to " +
						"communicate with S3, " +
						"such as not being able to access the network.");
				System.out.println("Error Message: " + ace.getMessage());
				if (retryCount == MAX_RETRY){
					retry = false;
					System.out.println("Max Retry Limit achieved.Aborting Program: " + retryCount);
				}
				else{
					retry = true;
					retryCount += 1;
					System.out.println("Retrying step. Retry Count: " + retryCount);
				}
			}
		}
		while(retry);		
		return sortRecords;
	}

	/**
	 * This method uploads a file and an ec2 file (with the partitions information) to the S3 output. The method tries
	 * uploading the file to s3 till it reaches a maximum count.
	 * @param uploadFileName
	 * @param ec2FileName
	 */
	public void sendFileToS3(String uploadFileName, String ec2FileName){
		short retryCount = 0;
		short MAX_RETRY = 3;
		boolean retry = false;
		do{
			try {
				File file = new File(uploadFileName);
				if (ClientMain.OUTPUT_BUCKET == null){
					this.s3.putObject(new PutObjectRequest(ServerMain.OUTPUT_BUCKET, ec2FileName, file));
				}
				else{
					this.s3.putObject(new PutObjectRequest(ClientMain.OUTPUT_BUCKET, ec2FileName, file));
				}
			} 
			catch (AmazonServiceException ase) {
				System.out.println("Caught an AmazonServiceException, which " +
						"means your request made it " +
						"to Amazon S3, but was rejected with an error response" +
						" for some reason.");
				System.out.println("Error Message:    " + ase.getMessage());
				System.out.println("HTTP Status Code: " + ase.getStatusCode());
				System.out.println("AWS Error Code:   " + ase.getErrorCode());
				System.out.println("Error Type:       " + ase.getErrorType());
				System.out.println("Request ID:       " + ase.getRequestId());
				if (retryCount == MAX_RETRY){
					retry = false;
					System.out.println("Max Retry Limit achieved.Aborting Program: " + retryCount);
				}
				else{
					retry = true;
					retryCount += 1;
					System.out.println("Retrying step. Retry Count: " + retryCount);
				}
			} 
			catch (AmazonClientException ace) {
				System.out.println("Caught an AmazonClientException, which " +
						"means the client encountered " +
						"an internal error while trying to " +
						"communicate with S3, " +
						"such as not being able to access the network.");
				System.out.println("Error Message: " + ace.getMessage());
				if (retryCount == MAX_RETRY){
					retry = false;
					System.out.println("Max Retry Limit achieved.Aborting Program: " + retryCount);
				}
				else{
					retry = true;
					retryCount += 1;
					System.out.println("Retrying step. Retry Count: " + retryCount);
				}
			}
		}
		while(retry);
	}

	/**
	 * This method reads files from S3 output bucket, and merges the contents of the files from different slave nodes.
	 * @param clientId
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public String readFilesfromS3andConcat(int clientId) throws FileNotFoundException, IOException {
		short retryCount = 0;
		short MAX_RETRY = 3;
		boolean retry = false;
		do{
			try {
				// creating a folder
				FileUtils.createDir(ClientMain.SORT_PATH+"/"+String.valueOf(clientId));
				ObjectListing objectListing = s3.listObjects(new ListObjectsRequest()
						.withBucketName(ClientMain.OUTPUT_BUCKET)
						.withPrefix(ClientMain.SORT_PATH+"/"+clientId + "/")
						.withDelimiter("/"));
				TreeSet<String> filenamesTree = new TreeSet<String>();
				for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
					filenamesTree.add(objectSummary.getKey());			
				}
				filenamesTree.remove(ClientMain.SORT_PATH+"/"+clientId+"/");
				int counter = 0;
				ArrayList<String> localfileNames = new ArrayList<>();
				for (String filename : filenamesTree) {
					S3Object s3object = this.s3.getObject(new GetObjectRequest(ClientMain.OUTPUT_BUCKET, filename));
					try {
						localfileNames.add(filename.split("/")[4]);
						S3ObjectInputStream objectContent = s3object.getObjectContent();
						IOUtils.copy(objectContent, new FileOutputStream(ClientMain.SORT_PATH+"/"+clientId+"/"+filename.split("/")[4]));
					} catch (IOException e) {
						e.printStackTrace();
					}
					counter += 1;
				}
				int x = 0;
				while (x < ClientMain.N_INSTANCES)
				{
					if(x == 0){
						FileMerger.merger(ClientMain.SORT_PATH + "/" + clientId+"/"+localfileNames.get(1),
								ClientMain.SORT_PATH + "/" + clientId+"/"+localfileNames.get(0),
								ClientMain.SORT_PATH + "/" + clientId+"/"+"finalPart-"+clientId+"-1");
						x+=2;
					}
					else{
						FileMerger.merger(ClientMain.SORT_PATH + "/" + clientId+"/"+localfileNames.get(x), 
								ClientMain.SORT_PATH + "/" + clientId+"/"+"finalPart-"+clientId+"-"+(x-1),
								ClientMain.SORT_PATH + "/" + clientId+"/"+"finalPart-"+clientId+"-"+x);
						x+=1;
					}
				}
				return ClientMain.SORT_PATH + "/" + clientId + "/" + "finalPart-" + clientId + "-" + (x-1);
			} 
			catch (ArrayIndexOutOfBoundsException ae) {
				ae.printStackTrace();
				if (retryCount == MAX_RETRY){
					retry = false;
					System.out.println("Max Retry Limit achieved.Aborting Program: " + retryCount);
				}
				else{
					retry = true;
					retryCount += 1;
					System.out.println("Retrying step. Retry Count: " + retryCount);
				}
			}
			catch (AmazonServiceException ase) {
				System.out.println("Caught an AmazonServiceException, which " +
						"means your request made it " +
						"to Amazon S3, but was rejected with an error response" +
						" for some reason.");
				System.out.println("Error Message:    " + ase.getMessage());
				System.out.println("HTTP Status Code: " + ase.getStatusCode());
				System.out.println("AWS Error Code:   " + ase.getErrorCode());
				System.out.println("Error Type:       " + ase.getErrorType());
				System.out.println("Request ID:       " + ase.getRequestId());
				if (retryCount == MAX_RETRY){
					retry = false;
					System.out.println("Max Retry Limit achieved.Aborting Program: " + retryCount);
				}
				else{
					retry = true;
					retryCount += 1;
					System.out.println("Retrying step. Retry Count: " + retryCount);
				}
			} 
			catch (AmazonClientException ace) {
				System.out.println("Caught an AmazonClientException, which " +
						"means the client encountered " +
						"an internal error while trying to " +
						"communicate with S3, " +
						"such as not being able to access the network.");
				System.out.println("Error Message: " + ace.getMessage());
				if (retryCount == MAX_RETRY){
					retry = false;
					System.out.println("Max Retry Limit achieved.Aborting Program: " + retryCount);
				}
				else{
					retry = true;
					retryCount += 1;
					System.out.println("Retrying step. Retry Count: " + retryCount);
				}
			}
		}
		while(retry);
		return null;		
	}

	/**
	 * Mapper method to fetch all files
	 * @param clientId
	 * @param mapper
	 */
	public void mapAllFiles(int clientId, Mapper mapper) {
		Context context = new Context();
		short retryCount = 0;
		boolean retry = false;
		/* (Run by Client) */
		do{
			try {
				// fetching filenames depending on count
				ObjectListing objectListing = s3.listObjects(new ListObjectsRequest()
						.withBucketName(ClientMain.INPUT_BUCKET)
						.withPrefix(ClientMain.INPUT_FOLDER + "/")
						.withDelimiter("/"));
				TreeMap<Long, String> filenamesMap = new TreeMap<Long, String>();
				for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
					filenamesMap.put(objectSummary.getSize(), objectSummary.getKey());			
				}
				TreeSet<String> filenamesTree = new TreeSet<String>(filenamesMap.values());
				filenamesTree.remove(ClientMain.INPUT_FOLDER + "/");
				// save all filenames to a list
				ArrayList<String> fileList = new ArrayList<String>();
				fileList.addAll(filenamesTree);
				List<List<String>> listPartitions = CollectionUtils.partition(fileList, ClientMain.N_INSTANCES);
				String filenames = "";
				for (List<String> elementList : listPartitions){
					if (elementList.size() > clientId){
                        filenames = StringUtils.join(new String[]{filenames, elementList.get(clientId)}, ",");
					}
				}
				filenames = filenames.substring(1);
				// fetching only part of actual data
				String[] filenamesList = filenames.split(",");
				int counter = 0;
				for (String filename : filenamesList) {
					String currentFile = filename.substring(filename.lastIndexOf('/') + 1).split("\\.")[0];
					ClientMain.CURRENT_FILE = currentFile;
					S3Object s3object = this.s3.getObject(new GetObjectRequest(ClientMain.INPUT_BUCKET, filename));
					BufferedReader reader;
					try {
						reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(s3object.getObjectContent())));
						String line;
						while ((line = reader.readLine()) != null) {
							mapper.map(null, line, context);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
					counter += 1;
				}
			}
			catch (AmazonServiceException ase) {
				System.out.println("Caught an AmazonServiceException, which " +
						"means your request made it " +
						"to Amazon S3, but was rejected with an error response" +
						" for some reason.");
				System.out.println("Error Message:    " + ase.getMessage());
				System.out.println("HTTP Status Code: " + ase.getStatusCode());
				System.out.println("AWS Error Code:   " + ase.getErrorCode());
				System.out.println("Error Type:       " + ase.getErrorType());
				System.out.println("Request ID:       " + ase.getRequestId());
				if (retryCount == MAX_RETRY){
					retry = false;
					System.out.println("Max Retry Limit achieved.Aborting Program: " + retryCount);
				}
				else{
					retry = true;
					retryCount += 1;
					System.out.println("Retrying step. Retry Count: " + retryCount);
				}
			}  
			catch (AmazonClientException ace) {
				System.out.println("Caught an AmazonClientException, which " +
						"means the client encountered " +
						"an internal error while trying to " +
						"communicate with S3, " +
						"such as not being able to access the network.");
				System.out.println("Error Message: " + ace.getMessage());
				if (retryCount == MAX_RETRY){
					retry = false;
					System.out.println("Max Retry Limit achieved.Aborting Program: " + retryCount);
				}
				else{
					retry = true;
					retryCount += 1;
					System.out.println("Retrying step. Retry Count: " + retryCount);
				}
			}
		}
		while(retry);	
	}

	/**
	 * Reducer method that performs the reduce operation on sorted output
	 * @param clientNum
	 * @param reducer
	 * @throws FileNotFoundException
	 */
	public void reduceKey(int clientNum, Reducer reducer) throws FileNotFoundException {
		Context context = new Context();
		File file = new File(ClientMain.REDUCE_PATH);
		File[] files = file.listFiles();
		Arrays.sort(files, new Comparator<File>(){
			@Override
			public int compare(File f1, File f2) {
				return f1.getName().compareToIgnoreCase(f2.getName());
			}			
		});
		ClientMain.CURRENT_FILE = "abs-final-output-" + ClientMain.CLIENT_NUM;
		for (File f: files){
			if (f.getName().startsWith("" + clientNum)){
				//+f.getName().split("_")[1];
				BufferedReaderIterable b = new BufferedReaderIterable(f);
				reducer.reduce(f.getName().split("_")[1], b, context);
			}
		}
	}
}