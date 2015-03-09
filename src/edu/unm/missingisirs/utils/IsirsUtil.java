package edu.unm.missingisirs.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

public class IsirsUtil {

	public static HashMap<String, ArrayList<String>> loadHashMap(String saveDirectory) {
		HashMap<String, ArrayList<String>> listBySsnMap = new HashMap<String, ArrayList<String>>();
		// Opening up current directory to get the ISIR files
		File dir = new File(saveDirectory);
		File[] files = dir.listFiles();
		// loop through files
		for (File f : files) {
			if (f.isFile()) {
				BufferedReader inputStream = null;
				// start reading in lines from files
				try {
					inputStream = new BufferedReader(new FileReader(f));
					String line;

					// read in lines from file
					while ((line = inputStream.readLine()) != null && line.length() >= 10) {
						// grab the students SSN from line
						String studentSSN = line.substring(1, 10);

						// check whether studentSSN is a number
						if (IsirsUtil.isNumeric(studentSSN)) {

							// create an arraylist of students based off of their SSN
							ArrayList<String> studentList = listBySsnMap.get(studentSSN);
							// if arraylist is empty, add SSN and list to hashmap
							if (studentList == null) {
								studentList = new ArrayList<String>();

								// Adding the line to the ArrayList
								studentList.add(line);

								// Add the ArrayList to the HashMap
								listBySsnMap.put(studentSSN, studentList);

							}// end if statement
							else {
								// The same line may exist in multiple files
								// We want to make sure we are only adding that
								// line to our HashMap's ArrayList ONCE.
								boolean okToAdd = true;

								for (String fafsaTrans : studentList) {
									if (fafsaTrans.equals(line)) {
										okToAdd = false;
									}
								}
								if (okToAdd) {
									studentList.add(line);
								}
							}// end else statement
						}// end if
					}
				}// end while loop

				catch (Exception e) {
					e.printStackTrace();
				}// end catch
			}// end if statement
		}// end for loop
		return listBySsnMap;
	}

	public static void findFafsaTransactionsInSuspense(HashMap<String, HashSet<Integer>> fafsaTransactionsInSuspenseBySsnMap, String aidYear, String username, String password)
			throws ClassNotFoundException, SQLException {
		// String url = "jdbc:oracle:thin:@stella.unm.edu:1523:DEVL";
		String url = "jdbc:oracle:thin:@sct3.unm.edu:1523:BANP";
		Connection conn = null;
		InputStream fis = null;

		conn = createDatabaseConnection(username, password, url);

		String getSuspenseRecords = "SELECT RCRTMP1_SSN, ROTSTAT_SAR_TRAN_NO FROM RCRTMP1, ROTSTAT WHERE ROTSTAT_AIDY_CODE = '" + aidYear
				+ "' AND RCRTMP1_PIDM = ROTSTAT_PIDM AND ROTSTAT_AIDY_CODE = RCRTMP1_AIDY_CODE";
		// StirlingCrow - 2014-04-15: WTF? Why was this in here? Commenting this out... and replacing the query to what it was before.
		// String getSuspenseRecords = "select rcrapp1_ssn, rcrapp1_used_trans_no from rcrapp1 where rcrapp1_aidy_code = '"+aidYear+"'";
		// SELECT RCRTMP1_SSN, ROTSTAT_SAR_TRAN_NO FROM RCRTMP1, ROTSTAT WHERE ROTSTAT_AIDY_CODE = '" + aidYear + "' AND RCRTMP1_PIDM = ROTSTAT_PIDM AND ROTSTAT_AIDY_CODE = RCRTMP1_AIDY_CODE";

		PreparedStatement stmt1 = conn.prepareStatement(getSuspenseRecords);
		ResultSet resultSet1 = null;
		resultSet1 = stmt1.executeQuery(getSuspenseRecords);

		// return the students max fafsa transaction number
		while (resultSet1.next()) {
			String ssnInSuspense = resultSet1.getString(1);
			Integer fafsaTransInSuspense = resultSet1.getInt(2); // set maxFafsaNumberInBanner to result in int 1

			// There may be more than one transaction in suspense for the
			// same person. The following adds the information to
			// the HashMap so we can track who is in suspense and
			// what fafsas are in suspense
			if (fafsaTransactionsInSuspenseBySsnMap.containsKey(ssnInSuspense)) {
				HashSet<Integer> fafsaTransactionsInSuspenseSet = fafsaTransactionsInSuspenseBySsnMap.get(ssnInSuspense);
				fafsaTransactionsInSuspenseSet.add(fafsaTransInSuspense);
			} else {
				HashSet<Integer> fafsaTransactionsInSuspenseSet = new HashSet<Integer>();
				fafsaTransactionsInSuspenseSet.add(fafsaTransInSuspense);

				fafsaTransactionsInSuspenseBySsnMap.put(ssnInSuspense, fafsaTransactionsInSuspenseSet);
			}

		}// end while loop

		conn.close();
		conn = null;

	}

	
	/**
	 * Loops through the loaded Fafsas in the Hashmap,
	 * determines if the Fafsa exists in the database,
	 * also determines if the Fafsa in the database is
	 * greater than the Fafsa.  Adds the Fafsa into
	 * the appropriate ArrayList
	 * 
	 * @param listBySsnMap
	 * @param fafsaTransToLoadList Contains Fafsas that have a higher number than the max fafsa 
	 * 							in the db.  Also contains Fafsas of students that don't exist in the db.
	 * @param fafsaTransToNotLoadList - These Fafsas are less than the Fafsas in the DB
	 * @param fafsaTransKidsNotInDB - Fafsas of students not in the DB.  These also go into fafsaTransToLoadList
	 * @param aidYear
	 * @param username
	 * @param password
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public static void findFafsasToLoad(HashMap<String, ArrayList<String>> listBySsnMap, ArrayList<String> fafsaTransToLoadList, ArrayList<String> fafsaTransToNotLoadList,
			ArrayList<String> fafsaTransKidsNotInDB, String aidYear, String username, String password, ArrayList<String> fafsasFromIsirFilesThatAreAlreadyInSuspenseList,
			HashMap<String, HashSet<Integer>> fafsaTransactionsInSuspenseBySsnMap) throws ClassNotFoundException, SQLException {
		//String url = "jdbc:oracle:thin:@stella.unm.edu:1523:DEVL";
				String url = "jdbc:oracle:thin:@sct3.unm.edu:1523:BANP";
				Connection conn = null;
				InputStream fis = null;

				//This counter will keep track of how many time we access the database
				int counter = 0;

				//Begin looping through every person in the HashMap...
				for(Map.Entry<String, ArrayList<String>> entry : listBySsnMap.entrySet())
				{
				    System.out.println(counter);
					counter++;

					String ssn = entry.getKey(); //get SSN from hashmap
					ArrayList<String> fafsaList = entry.getValue();




				    if (conn == null)
				    {
				    	System.out.println("Creating DB Connection");
				    	conn = createDatabaseConnection(username, password, url);
				    }

				    //Does the person exist in banner?
				    //If they do, return the PIDM
				    //If they don't, return "NO USER"
				    String pidm = doesPersonExistInBanner(conn, ssn);
				    counter++;



				    //If the user doesn't exist in Banner then
				    //add them to the load list...
				    if (pidm.equals("NO USER"))
				    {
				        processFafsaTransactionWhenSsnIsNotInBanner(listBySsnMap,
								fafsaTransToLoadList, fafsaTransKidsNotInDB,
								fafsasFromIsirFilesThatAreAlreadyInSuspenseList,
								fafsaTransactionsInSuspenseBySsnMap, ssn);

				    }
				    else
				    {	
					   //If the ssn does exist in banner...
				    	counter = processFafsaTranascationsWhenSsnExsistsInBanner(
								fafsaTransToLoadList, fafsaTransToNotLoadList, aidYear,
								fafsasFromIsirFilesThatAreAlreadyInSuspenseList,
								fafsaTransactionsInSuspenseBySsnMap, conn, counter, ssn,
								fafsaList, pidm);
				    } 

				    if (counter > 500)
				      {
				    	  counter = 0;
				    	  System.out.println("Closing DB Connection");
				    	  conn.close();
				    	  conn = null;
				      }
				  }//end for loop

	}

	private static int processFafsaTranascationsWhenSsnExsistsInBanner(
			ArrayList<String> fafsaTransToLoadList,
			ArrayList<String> fafsaTransToNotLoadList,
			String aidYear,
			ArrayList<String> fafsasFromIsirFilesThatAreAlreadyInSuspenseList,
			HashMap<String, HashSet<Integer>> fafsaTransactionsInSuspenseBySsnMap,
			Connection conn, int counter, String ssn,
			ArrayList<String> fafsaList, String pidm) throws SQLException {
		//If the student exists in Banner...

		//initialize maxFafsaNumberInBanner
		Integer maxFafsaNumberInBanner = 0; 

		//retrieve max fasfa transaction number from rcvappl by using students pidm
		String getMaxFafsaSql = "select max( rcrapp1_used_trans_no) from rcrapp1 where rcrapp1_pidm = '" + pidm + "' and rcrapp1_infc_code= 'EDE' and rcrapp1_aidy_code = '" + aidYear + "'";
		counter++;     


		 PreparedStatement stmt1 = conn.prepareStatement(getMaxFafsaSql);
		 ResultSet resultSet1  = null;
		 resultSet1 = stmt1.executeQuery(getMaxFafsaSql);
		 int countFafsa = 0;


		  //return the students max fafsa transaction number
		  while(resultSet1.next())
		  {
		     maxFafsaNumberInBanner = resultSet1.getInt(1); //set maxFafsaNumberInBanner to result in int 1
		     countFafsa++;
 
		  }//end while loop


		  //If there are no FAFSAs in the system then we definitely
		  //need to load one of the FAFSAs in the fafsaList
		  if (countFafsa == 0)
		  {
		      determineFafsasToLoadIfNoFafsasExistInBanner(
					fafsaTransToLoadList,
					fafsasFromIsirFilesThatAreAlreadyInSuspenseList,
					fafsaTransactionsInSuspenseBySsnMap, ssn, fafsaList);


		  }
		  else
		  {	  

		      determineFafsasToLoadIfFafsaTransactionsExistInBanner(fafsaTransToLoadList, fafsaTransToNotLoadList,
					fafsasFromIsirFilesThatAreAlreadyInSuspenseList,
					fafsaTransactionsInSuspenseBySsnMap, ssn, fafsaList,
					maxFafsaNumberInBanner);

		  }
		return counter;
	}


	private static void determineFafsasToLoadIfFafsaTransactionsExistInBanner(
			ArrayList<String> fafsaTransToLoadList,
			ArrayList<String> fafsaTransToNotLoadList,
			ArrayList<String> fafsasFromIsirFilesThatAreAlreadyInSuspenseList,
			HashMap<String, HashSet<Integer>> fafsaTransactionsInSuspenseBySsnMap,
			String ssn, ArrayList<String> fafsaList,
			Integer maxFafsaNumberInBanner) {
		//If there is a FAFSA loaded in the system then we need to 
		  //determine if we should load any of the fafsa transactions
		  //from the ISIR files
		  if (fafsaList.size() > 1)
		  {
		      String fafsaToLoad = null;
		      int maxFafsaTrans = 0;


		      //Looping through the fafsa transactions...
		      for (String fafsa : fafsaList)
		      {
		          //What is the transaction number?
		    	  Integer transNumber = Integer.parseInt(fafsa.substring(12,14)); //Changed from 14,16


		    		  	//If the transaction number is greater than
		    		  	//the transaction number in Banner
		    		  	//then replace the maxFafsaTrans and
		    		  	//make this the fafsa to load
		    		  	if (transNumber > maxFafsaNumberInBanner)
		    		  	{
		    		  		maxFafsaTrans = transNumber;
		    		  		fafsaToLoad = fafsa;
		    		  	} 
		    		  	else if (transNumber > maxFafsaTrans)
		    		  	{   
		    		  		//We might have another transaction that
		    		  		//is higher in number in our fafsaList.  If so,
		    		  		//make that the maxFafsaTrans and load that one...
		    		  		maxFafsaTrans = transNumber;
		    		  		fafsaToLoad = fafsa;
		    		  	} 



		        }//end looping to find max fafsa transaction...

		        //If one of our fafsas from the Isir files has
		        //a greater number than the fafsa transaction in Banner
		        //then we need to load that...
		        if (maxFafsaTrans > maxFafsaNumberInBanner)
		        {
		            //Add the Fafsa with the highest transaction... but
		        	//first we need to check if that fafsa is already in
		        	//suspense

		        	//Is this fafsa transaction already in suspense??
			     	if (fafsaTransactionsInSuspenseBySsnMap.containsKey(ssn))
			     	{
			     		Integer fafsaTransNumber = Integer.parseInt(fafsaToLoad.substring(12, 14));
			     		HashSet<Integer> fafsaTransInSuspenseSet = fafsaTransactionsInSuspenseBySsnMap.get(ssn);

			     		//If the fafsa is already in suspense then add it
			     		//to the "Already In Suspense List" and don't add it
			     		//to any other lists...  Otherwise, add it to
			     		//the fafsaTransToLoadList...
			     		if (fafsaTransInSuspenseSet.contains(fafsaTransNumber))
			     		{
			     			fafsasFromIsirFilesThatAreAlreadyInSuspenseList.add(fafsaToLoad);

			     		}
			     		else
			     		{
			     			//If the fafsa transaction is not in suspense, add
			     			//it to the fafsaTransToLoadList...
			     			fafsaTransToLoadList.add(fafsaToLoad);
			     		}

			     	}
			     	else
			     	{
			     		fafsaTransToLoadList.add(fafsaToLoad);
			     	}

		        }
		        else //If our fafsas from our Isir files are not greater than the max Banner Fafsa transaction...
		        {   
		        	for (String fafsa : fafsaList)
		            {
		                fafsaTransToNotLoadList.add(fafsa);

		            }//end for
		        }
		  	}//end "if FafsaList is greater than 1"
		    else
		    {
		          //If we only have one fafsa transaction in our
		          //arraylist then we need to see if that transaction
		          //number is greater than what is in banner 

		          String fafsaTrans = fafsaList.get(0);

		          Integer transNumber = Integer.parseInt(fafsaTrans.substring(12,14));




		          if (transNumber > maxFafsaNumberInBanner)
		          {
		        	  String fafsaToLoad = fafsaList.get(0);
		        	  //Is this fafsa transaction already in suspense??
		   		      if (fafsaTransactionsInSuspenseBySsnMap.containsKey(ssn))
		   		     	{
		   		     		Integer fafsaTransNumber = Integer.parseInt(fafsaToLoad.substring(12, 14));
		   		     		HashSet<Integer> fafsaTransInSuspenseSet = fafsaTransactionsInSuspenseBySsnMap.get(ssn);

		   		     		//If the fafsa is already in suspense then add it
		   		     		//to the "Already In Suspense List" and don't add it
		   		     		//to any other lists...  Otherwise, add it to
		   		     		//the fafsaTransToLoadList...
		   		     		if (fafsaTransInSuspenseSet.contains(fafsaTransNumber))
		   		     		{
		   		     			fafsasFromIsirFilesThatAreAlreadyInSuspenseList.add(fafsaToLoad);

		   		     		}
		   		     		else
		   		     		{
		   		     			//If the fafsa transaction is not in suspense, add
		   		     			//it to the fafsaTransToLoadList...
		   		     			fafsaTransToLoadList.add(fafsaToLoad);
		   		     		}

		   		     	}
		   		     	else
		   		     	{
		   		     		fafsaTransToLoadList.add(fafsaToLoad);
		   		     	}

		          }
		          else
		          {
		              fafsaTransToNotLoadList.add( fafsaList.get(0));

		          }
		    }
	}


	private static void determineFafsasToLoadIfNoFafsasExistInBanner(
			ArrayList<String> fafsaTransToLoadList,
			ArrayList<String> fafsasFromIsirFilesThatAreAlreadyInSuspenseList,
			HashMap<String, HashSet<Integer>> fafsaTransactionsInSuspenseBySsnMap,
			String ssn, ArrayList<String> fafsaList) 

	{
		System.out.println(ssn + "has no loaded FAFSAs.");

		  //If there is more than one Fafsa in the ISIR files
		  //then we need to determine which one of those we 
		  //need to load...
		  if (fafsaList.size() > 1)
		  {
		      //Put the fafsa to be loaded in our master list
		      String fafsaToLoad = null;
		      int maxFafsaTrans = 0;


		      //If there are multiple fafsas then we need to evaluate which one
		      //has the highest transaction level
		      for (String fafsa : fafsaList)
		      {
		          int transNumber = Integer.getInteger(fafsa.substring(12,14)); //Changed from 14,16


		          if (transNumber > maxFafsaTrans)
		          {
		              maxFafsaTrans = transNumber;
		              fafsaToLoad = fafsa;
		          }//end if


		      }//end for

		      //Add the Fafsa with the highest transaction - but
		      //before we can do that let's check to make sure this fafsa
		      //to load is not in suspense already

		     //Is this fafsa transaction already in suspense??
		     if (fafsaTransactionsInSuspenseBySsnMap.containsKey(ssn))
		     {
		    	 Integer fafsaTransNumber = Integer.parseInt(fafsaToLoad.substring(12, 14));
		         HashSet<Integer> fafsaTransInSuspenseSet = fafsaTransactionsInSuspenseBySsnMap.get(ssn);

		         //If the fafsa is already in suspense then add it
		         //to the "Already In Suspense List" and don't add it
		         //to any other lists...  Otherwise, add it to
		         //the fafsaTransToLoadList...
		         if (fafsaTransInSuspenseSet.contains(fafsaTransNumber))
		         {
		         	fafsasFromIsirFilesThatAreAlreadyInSuspenseList.add(fafsaToLoad);

		         }
		         else
		         {
		        	//If the fafsa transaction is not in suspense, add
			         //it to the fafsaTransToLoadList...
		             fafsaTransToLoadList.add(fafsaToLoad);
		         }

		     }
		     else
		     {
		    	//If the fafsa transaction is not in suspense, add
		         //it to the fafsaTransToLoadList...
		         fafsaTransToLoadList.add(fafsaToLoad);
		     }



		  }//end if
		  else
		  {
		      //If there's only one Fafsa transaction then
		      //we will only pull that one out.  But... first we 
			  //need to see if it is in suspense...
			  String fafsaToLoad = fafsaList.get(0);

			  //Is this fafsa transaction already in suspense??
		      if (fafsaTransactionsInSuspenseBySsnMap.containsKey(ssn))
		      {
		      	 Integer fafsaTransNumber = Integer.parseInt(fafsaToLoad.substring(12, 14));
		         HashSet<Integer> fafsaTransInSuspenseSet = fafsaTransactionsInSuspenseBySsnMap.get(ssn);

		         //If the fafsa is already in suspense then add it
		         //to the "Already In Suspense List" and don't add it
		         //to any other lists...  Otherwise, add it to
		         //the fafsaTransToLoadList...
		         if (fafsaTransInSuspenseSet.contains(fafsaTransNumber))
		         {
		           	fafsasFromIsirFilesThatAreAlreadyInSuspenseList.add(fafsaToLoad);

		         }
		         else
		         {
		        	 fafsaTransToLoadList.add(fafsaToLoad);
		         }

		       }
		      else
		      {
		    	  fafsaTransToLoadList.add(fafsaToLoad);
		      }


		  }//end else if there is only one fafsa
	}


	private static void processFafsaTransactionWhenSsnIsNotInBanner(
			HashMap<String, ArrayList<String>> listBySsnMap,
			ArrayList<String> fafsaTransToLoadList,
			ArrayList<String> fafsaTransKidsNotInDB,
			ArrayList<String> fafsasFromIsirFilesThatAreAlreadyInSuspenseList,
			HashMap<String, HashSet<Integer>> fafsaTransactionsInSuspenseBySsnMap,
			String ssn) 
	{
		ArrayList<String> theFafsaList = listBySsnMap.get(ssn);

		for (String fafsa : theFafsaList) 
		{
			// Is this fafsa transaction already in suspense??
			if (fafsaTransactionsInSuspenseBySsnMap.containsKey(ssn)) 
			{
				Integer fafsaTransNumber = Integer.parseInt(fafsa.substring(12, 14));
				HashSet<Integer> fafsaTransInSuspenseSet = fafsaTransactionsInSuspenseBySsnMap.get(ssn);

				// If the fafsa is already in suspense then add them
				// to the "Already In Suspense List" and don't add them
				// to any other lists... Otherwise, add them to
				// the other lists(fafsaTransToLoad,
				// fafsaTransKidsNotInDB)
				if (fafsaTransInSuspenseSet.contains(fafsaTransNumber)) 
				{
					fafsasFromIsirFilesThatAreAlreadyInSuspenseList.add(fafsa);

				}
				else
				{
					fafsaTransToLoadList.add(fafsa);
					fafsaTransKidsNotInDB.add(fafsa);
				}

			}
			else
			{
				fafsaTransToLoadList.add(fafsa);
				fafsaTransKidsNotInDB.add(fafsa);
			}



		}
	}


	/**
	 * Creates a database connection with the given user name and
	 * password an returns the database connection.
	 * 
	 * @param username
	 * @param password
	 * @param url
	 * @return
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	private static Connection createDatabaseConnection(String username,
			String password, String url) throws ClassNotFoundException,
			SQLException 
	{
		Connection conn;
		Class.forName("oracle.jdbc.driver.OracleDriver");
		conn = DriverManager.getConnection(url, username, password);
		conn.setAutoCommit(false);
		return conn;
	}
    
    
    //checks whether a string is a number
    public static boolean isNumeric(String str)
    {
        return str.matches("-?\\d+(.\\d+)?");
    }
    
   /**
     * Loads a HashMap that is organized as such:
     *
     * Key: SSN of Student
     * Value: An ArrayList that contains all of the FAFSA Transactions
     *
     * @param listBySsnMap
     */
    private static void loadHashMap(HashMap<String, ArrayList<String>> listBySsnMap)
    {
        //Opening up current directory to get the ISIR files
        File dir = new File(".");
        File[] files = dir.listFiles();
        //loop through files
        for (File f : files)
        {
            if (f.isFile())
            {
                BufferedReader inputStream = null;
                //start reading in lines from files
                try
                {
                	inputStream = new BufferedReader(
                            new FileReader(f));
                    String line;
                   
                                        
                    //read in lines from file
                    while ((line = inputStream.readLine()) != null && line.length() >= 10)
                    {
                        //grab the students SSN from line
                    	String studentSSN = line.substring(1,10);
                    	
                    	//check whether studentSSN is a number
                        if(IsirsUtil.isNumeric(studentSSN))
                        {                      	

                        	//create an arraylist of students based off of their SSN
                        	ArrayList<String> studentList = listBySsnMap.get(studentSSN);
                        	//if arraylist is empty, add SSN and list to hashmap
                        	if(studentList == null)
                        	{
                        		studentList = new ArrayList<String>();
                             
                        		//Adding the line to the ArrayList
                        		studentList.add(line);
                             
                        		//Add the ArrayList to the HashMap
                        		listBySsnMap.put(studentSSN, studentList);
 
                        	}//end if statement
                        	else
                        	{
                        		//The same line may exist in multiple files
                        		//We want to make sure we are only adding that
                        		//line to our HashMap's ArrayList ONCE.
                        		boolean okToAdd = true;
                        		
                        		for (String fafsaTrans : studentList)
                        		{
                        			if (fafsaTrans.equals(line))
                        			{
                        				okToAdd = false;
                        			}
                        		}
                        		
                        		if (okToAdd)
                        		{
                        			studentList.add(line);
                        		}
                        		
                        	}//end else statement
                        }//end if
                        
                    }
                    }//end while loop
                
                
                catch(Exception e)
                {
                    e.printStackTrace();
                }//end catch
            }//end if statement
        }//end for loop
    } //end main
	/**
	 * Finds Fafsa Transactions (that are in the ISIR files) that are: -LESS than the max transaction in Banner -Not in Banner -Not in the list that we will load
	 * 
	 * which means... these are previous FAFSA transactions that will never get loaded.
	 * 
	 * @param listBySsnMap
	 * @param fafsaTransToLoadList
	 * @param fafsaTransKidsNotInDB
	 * @param fafsasThatHaveNeverBeenLoadedList
	 * @param aidYear
	 * @param username
	 * @param password
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public static void findFafsaTransactionsThatHaveNeverBeenLoaded(HashMap<String, ArrayList<String>> listBySsnMap, ArrayList<String> fafsaTransToLoadList, ArrayList<String> fafsaTransKidsNotInDB,
			ArrayList<String> fafsasThatHaveNeverBeenLoadedList, String aidYear, String username, String password) throws ClassNotFoundException, SQLException {
		// Building a List of the fafsas that we currently have.
		TreeMap<String, HashSet<Integer>> fafsaTransThatWeHaveBySsnMap = new TreeMap<String, HashSet<Integer>>();

		// Loading a List of students not in banner
		ArrayList<String> ssnNotInBanner = new ArrayList<String>();

		for (String fafsaToLoad : fafsaTransKidsNotInDB) {
			String studentSSN = fafsaToLoad.substring(1, 10);

			ssnNotInBanner.add(studentSSN);
		}

		// TransToLoad HashMap:
		// Key: SSN, Value:
		HashMap<String, Integer> fafsasToLoadBySsnMap = new HashMap<String, Integer>();

		for (String fafsaToLoad : fafsaTransToLoadList) {
			String studentSSN = fafsaToLoad.substring(1, 10);

			// If the student isn't in Banner we don't care
			// about them for the report
			if (ssnNotInBanner.contains(studentSSN)) {
				continue;
			}

			String transNumberString = fafsaToLoad.substring(12, 14);
			Integer transNumber = Integer.parseInt(transNumberString);

			fafsasToLoadBySsnMap.put(studentSSN, transNumber);

		}

		// String url = "jdbc:oracle:thin:@stella.unm.edu:1523:DEVL";
		String url = "jdbc:oracle:thin:@sct3.unm.edu:1523:BANP";
		Connection conn = null;
		InputStream fis = null;

		// This counter will keep track of how many time we access the database
		int counter = 0;

		for (Map.Entry<String, ArrayList<String>> entry : listBySsnMap.entrySet()) {
			System.out.println(counter);

			String ssn = entry.getKey(); // get SSN from hashmap

			if (conn == null) {
				System.out.println("Creating DB Connection");
				conn = createDatabaseConnection(username, password, url);
			}

			// Does the person exist in banner?
			// If they do, return the PIDM
			// If they don't, return "NO USER"
			String pidm = doesPersonExistInBanner(conn, ssn);
			counter++;

			// If the user doesn't exist in Banner then
			// add them to the load list...
			if (pidm.equals("NO USER")) {
				// Go to the next student...
				continue;
			}// end if

			// Grab the Fafsas for the student
			ArrayList<String> fafsaList = entry.getValue();

			// retrieve max fasfa transaction number from rcvappl by using students pidm
			String getMaxFafsaSql = "select distinct( rcrapp1_used_trans_no) from rcrapp1 where rcrapp1_pidm = '" + pidm + "' and rcrapp1_infc_code= 'EDE' and rcrapp1_aidy_code = '" + aidYear + "'";
			// select distinct(RCVAPPL_USED_TRANS_NO) from RCVAPPL where RCVAPPL_PIDM = '" + pidm + "' and RCVAPPL_INFC_CODE = 'EDE' and RCVAPPL_AIDY_CODE = '" + aidYear + "'";
			counter++;

			PreparedStatement stmt1 = conn.prepareStatement(getMaxFafsaSql);
			ResultSet resultSet1 = null;
			resultSet1 = stmt1.executeQuery(getMaxFafsaSql);

			HashSet<Integer> fafsaTransactionsInBannerSet = new HashSet<Integer>();
			fafsaTransThatWeHaveBySsnMap.put(ssn, fafsaTransactionsInBannerSet);

			// return the students max fafsa transaction number
			while (resultSet1.next()) {
				Integer fafsaTransInBanner = resultSet1.getInt(1); // set maxFafsaNumberInBanner to result in int 1
				fafsaTransactionsInBannerSet.add(fafsaTransInBanner);

			}// end while loop

			if (counter > 500) {
				counter = 0;
				System.out.println("Closing DB Connection");
				conn.close();
				conn = null;
			}

		}// End Loop Of Getting Transactions In Banner and Transactions We Will Load

		// Loop through the Fafsas to Load and add them to our
		// overall map/set
		for (Map.Entry<String, Integer> entry : fafsasToLoadBySsnMap.entrySet()) {
			String ssn = entry.getKey();
			Integer fafsaToLoad = entry.getValue();

			if (fafsaTransThatWeHaveBySsnMap.containsKey(ssn)) {
				HashSet fafsaTransactionsInBannerSet = fafsaTransThatWeHaveBySsnMap.get(ssn);

				fafsaTransactionsInBannerSet.add(fafsaToLoad);
			}
		}

		// Now we should have all of the Fafsa Transactions (loaded and that will
		// be loaded in Banner)
		// Let's loop through our basic HashMap again to determine if any of those
		// Fafsas are not in that list
		for (Map.Entry<String, ArrayList<String>> entry : listBySsnMap.entrySet()) {
			String ssn = entry.getKey();
			ArrayList<String> fafsaList = entry.getValue();

			if (fafsaTransThatWeHaveBySsnMap.containsKey(ssn)) {
				HashSet fafsasThatWeHave = fafsaTransThatWeHaveBySsnMap.get(ssn);

				for (String fafsaFromIsirFile : fafsaList) {
					String fafsaTransString = fafsaFromIsirFile.substring(12, 14);
					Integer fafsaTrans = Integer.parseInt(fafsaTransString);

					if (fafsasThatWeHave.contains(fafsaTrans)) {
						System.out.println("We have this one.");
					} else {
						fafsasThatHaveNeverBeenLoadedList.add(fafsaFromIsirFile);
					}

				}
			}
		}

	}

	public static void printReportAndFileToLoad(ArrayList<String> fafsaTransToLoadList, ArrayList<String> fafsaTransToNotLoadList, ArrayList<String> fafsaTransToNotLoadLessList,
			ArrayList<String> fafsaTransKidsNotInDB, String username, ArrayList<String> fafsasThatHaveNeverBeenLoadedList, ArrayList<String> fafsasFromIsirFilesThatAreAlreadyInSuspenseList, String saveDirectory)
			throws IOException {
		// create a timestamp to differentiate files
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmm").format(Calendar.getInstance().getTime());

		// places files in specific folders in FA directory
		String reportDir = saveDirectory + timeStamp + "/";
		System.out.println("Creating file at the location: " + reportDir);
		boolean success = (new File(reportDir)).mkdir();
		if (success) {
			System.out.println("Writing to : " + reportDir);
			// writes students to a report to a file in FA directory
			FileWriter file2 = new FileWriter(reportDir + "reportForISIR_" + username + ".txt");
			PrintWriter report = new PrintWriter(file2);

			// writes students who have not been loaded to a file in FA directory to load into BANNER
			FileWriter file3 = new FileWriter(reportDir + "IDSA15OP.txt");
			PrintWriter loadFile = new PrintWriter(file3);

			report.println(" ");
			report.println("These FAFSAs need to be loaded:");
			report.println("--------------------------------");

			TreeMap<String, String> treeSetByLastName3 = new TreeMap<String, String>();

			for (String fafsaTransaction : fafsaTransToLoadList) {
				String nameOfPerson = fafsaTransaction.substring(14, 40);
				String ssnOfPerson = fafsaTransaction.substring(1, 10);
				String keyInMap = nameOfPerson + " " + ssnOfPerson;

				treeSetByLastName3.put(keyInMap, fafsaTransaction);

			}// end for

			for (Map.Entry<String, String> entry : treeSetByLastName3.entrySet()) {
				String fafsaTransaction = entry.getValue();
				report.println(fafsaTransaction);
				loadFile.println(fafsaTransaction);
			}

			treeSetByLastName3.clear();

			// After the for loop
			// for (String fafsaTransaction: fafsaTransToLoadList)
			// {
			// report.println(fafsaTransaction);
			// loadFile.println(fafsaTransaction);
			// }//end for

			report.println(" ");
			report.println("These FAFSAs belong to students that are not in Banner (but we will load them):");
			report.println("--------------------------------");

			// Loading a treeset so we can list our results
			// alphabetically
			TreeMap<String, String> treeSetByLastName = new TreeMap<String, String>();

			for (String fafsaTransaction : fafsaTransKidsNotInDB) {
				String nameOfPerson = fafsaTransaction.substring(14, 40);
				String ssnOfPerson = fafsaTransaction.substring(1, 10);
				String keyInMap = nameOfPerson + " " + ssnOfPerson;

				treeSetByLastName.put(keyInMap, fafsaTransaction);

			}// end for

			for (Map.Entry<String, String> entry : treeSetByLastName.entrySet()) {
				String fafsaRecord = entry.getValue();
				report.println(fafsaRecord);

			}
			// //After the for loop
			// for (String fafsaTransaction: fafsaTransKidsNotInDB)
			// {
			// report.println("NO SSN in SPBPERS - " + fafsaTransaction);
			//
			// }//end for

			treeSetByLastName.clear();

			report.println(" ");
			report.println("These FAFSAs already exist:");
			report.println("--------------------------------");

			TreeMap<String, String> treeSetByLastName2 = new TreeMap<String, String>();

			for (String fafsaTransaction : fafsaTransToNotLoadList) {
				String nameOfPerson = fafsaTransaction.substring(14, 40);
				String ssnOfPerson = fafsaTransaction.substring(1, 10);
				String keyInMap = nameOfPerson + " " + ssnOfPerson;

				treeSetByLastName2.put(keyInMap, fafsaTransaction);

			}// end for

			for (Map.Entry<String, String> entry : treeSetByLastName2.entrySet()) {
				String fafsaTransaction = entry.getValue();
				report.println(fafsaTransaction);
			}

			treeSetByLastName2.clear();

			report.println(" ");
			report.println("These are FAFSAs that will never get loaded because they are less than the MAX fafsa transaction (or we will load a higher Fafsa Trans Number):");
			report.println("--------------------------------");

			TreeMap<String, ArrayList<String>> fafsasThatHaveNeverBeenLoadedTreeMap = new TreeMap<String, ArrayList<String>>();

			for (String fafsaTransaction : fafsasThatHaveNeverBeenLoadedList) {
				String nameOfPerson = fafsaTransaction.substring(14, 40);
				String ssnOfPerson = fafsaTransaction.substring(1, 10);
				String keyInMap = nameOfPerson + " " + ssnOfPerson;

				//If the map already has a list of the fafsas that will never be loaded...
				if (fafsasThatHaveNeverBeenLoadedTreeMap.containsKey(keyInMap))
				{
					ArrayList<String> fafsasNeverLoadedList = fafsasThatHaveNeverBeenLoadedTreeMap.get(keyInMap);
					fafsasNeverLoadedList.add(fafsaTransaction);
					
				}
				else
				{
					//Else the map does not contain a list of the fafsas for the particular student...
					ArrayList<String> fafsasNeverLoadedList = new ArrayList<String>();
					
					fafsasNeverLoadedList.add(fafsaTransaction);
					fafsasThatHaveNeverBeenLoadedTreeMap.put(keyInMap, fafsasNeverLoadedList);
				}

			}// end for

			for(Map.Entry<String, ArrayList<String>> entry : fafsasThatHaveNeverBeenLoadedTreeMap.entrySet())
			{
				 ArrayList<String> fafsaTransactionList = entry.getValue();
				 
				 for (String fafsa: fafsaTransactionList)
				 {
					 report.println(fafsa);
				 }
				 
				 
			}

			report.println(" ");
			report.println("These are FAFSAs that will not be loaded because they currently exist in Suspense:");
			report.println("--------------------------------");

			// Loading a treeset so we can list our results
			// alphabetically
			TreeMap<String, String> treeSetByLastName5 = new TreeMap<String, String>();

			for (String fafsaTransaction : fafsasFromIsirFilesThatAreAlreadyInSuspenseList) {
				String nameOfPerson = fafsaTransaction.substring(14, 40);
				String ssnOfPerson = fafsaTransaction.substring(1, 10);
				String keyInMap = nameOfPerson + " " + ssnOfPerson;

				treeSetByLastName5.put(keyInMap, fafsaTransaction);

			}// end for

			for (Map.Entry<String, String> entry : treeSetByLastName5.entrySet()) {
				String fafsaRecord = entry.getValue();
				report.println(fafsaRecord);

			}

			file2.close();

		}

	}
	
	
	/**
	 * Determines if a user exists in Banner based on the SSN.
	 * If a user exists in Banner then we return the PIDM. If not
	 * we return the string "NO USER"
	 * 
	 * @param conn
	 * @param ssn
	 * @return
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	private static String doesPersonExistInBanner(Connection conn, String ssn) throws ClassNotFoundException, SQLException
	{
		//retreive students pidm and bannerID from gzbpfpm based off of SSN
	    String sql = "select rcrapp1_pidm from rcrapp1 where rcrapp1_ssn = '"+ ssn +"'";

	    PreparedStatement stmt = conn.prepareStatement(sql);
	    ResultSet resultSet = null;
	    //initialize pidm
	    String pidm = null;

	    resultSet = stmt.executeQuery(sql);

  

	    int count = 0;

	    //return the students pidm and bannerID
	    while(resultSet.next())
	    {
	         pidm = resultSet.getString(1); //set pidm to result in string 1

	         count++;
	    }//end while loop

	    if (count > 0)
	    {
	    	return pidm;
	    }
	    else
	    {
	    	return "NO USER";
	    }
	}
}
