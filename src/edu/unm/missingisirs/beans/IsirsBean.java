package edu.unm.missingisirs.beans;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class IsirsBean {
	public HashMap<String, ArrayList<String>> listBySsnMap = new HashMap<String, ArrayList<String>>();
	public ArrayList<String> fafsaTransToLoadList = new ArrayList<String>();
	public ArrayList<String> fafsaTransToNotLoadList = new ArrayList<String>();
	public ArrayList<String> fafsaTransToNotLoadLessList = new ArrayList<String>();
	public ArrayList<String> fafsaTransKidsNotInDB = new ArrayList<String>();
	public ArrayList<String> fafsasThatHaveNeverBeenLoadedList = new ArrayList<String>();
	public ArrayList<String> fafsasFromIsirFilesThatAreAlreadyInSuspenseList = new ArrayList<String>();
	public HashMap<String, HashSet<Integer>> fafsaTransactionsInSuspenseBySsnMap = new HashMap<String, HashSet<Integer>>();

}
