package voteListener;

import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;

import com.mysql.jdbc.Statement;
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

public class VoteListener {
	
	File votersList = new File("Voters_list.xml");
	File topsList = new File("Tops.config");
	File file = new File("C:\\");
	
	final String Top1 = "http://minecraftservers.org/vote/440795";
	final String top1UniqueTag = "table class=" + "\"" + "server-info voters-info" + "\"";
	
	final String Top2 = "https://topcraft.ru/servers/1479/statistics/";
	final String top2UniqueTag = "div class=" + "\"" + "col-xs-6 voterStats" + "\"" + " data-stats-type=" + "\"" + "current_month" + "\"";
	final String top2TableTag = "tbody class=\"voters\"";
	
	
	final String Top3 = "https://mctop.su/servers/604/statistics/";
	final String top3UniqueTag = "div class=\"col-md-10 voterStats\" data-stats-type=\"current_month\"";
	final String top3TableTag = "tbody class=\"voters\"";
	
	final String Top4 = "https://fairtop.in/project/185";
	final String top4UniqueTag = "div class=\"tab-content page-unit\" data-tab=\"votes\"";
	final String top4TableTag = "tbody";
	
	ArrayList<Voter> voters = new ArrayList<Voter>();
	
	public VoteListener()
	{
//		System.out.print((byte) ' ');
		downloadVoterFromSQL();
	}
	
	byte[] getDataFromTop(String path)
	{
		HttpURLConnection connection;
		
		byte result[];
		int count = 0;
		byte b[] = new byte[1024 * 64];
		byte i = 0;
		
		try 
		{
			System.out.println("Connecting to:" + path);
			URL url = new URL(path);
			connection = (HttpURLConnection) url.openConnection();
			connection.addRequestProperty("User-Agent", "VoteListener_v0.1 FreeMinerServers 20/02/2016");			
			connection.setRequestMethod("GET");
		}
		catch(IOException Ex)
		{
			Ex.printStackTrace();
		}
		finally
		{
			connection = null;
		}
		
		try(InputStream input = connection.getInputStream())
		{
			while(i != -1)
			{
				i = (byte) input.read();
//				System.out.print((char) i);
				b[count] = i;
				count++;
			}
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			result = new byte[count];
			
			for(int j = 0; j < count; j++)
			{
				result[j] = b[j];
			}
		}
		
		return result;
	}
	
	// --- ����� ��� ���������� ���������� �� ���� � ����������� �� �������� ---
	/*
	 * ���������  � ����������� � ������� ����������� ���� ������� Html ��������.
	 * uniqueTag - ���������� ������ ��� � �������� ������� �������� ������� � �����������. 
	 */
	
	void topDataAnalyze(String uniqueTag, byte[] scanResult, int namesColumn, int votesColumn)
	{		
		System.out.println("Analyzing data ...");
		getVotersFromArray(HtmlDataAnalyze(scanResult, uniqueTag, false), namesColumn, votesColumn);
		System.out.println("Data analyzing finished.\n");
	}
	
	// --- ������������� ����� ����������� ������ ---
	/*
	 * �� ������ ���� �� ����� ���� ��������� ������ � ������������ ���������� ����� ���������� ������ �� �������� ����������.
	 * tableTag - � ������ ����� ������ ������� ��� ����������� �������,
	 * � uniqueTag - ���������� ��� ���������� � ������� ��������� ������ �������.
	 */
	
	void topDataAnalyze(String uniqueTag, String tableTag, byte[] scanResult, int namesColumn, int votesColumn)
	{		
		System.out.println("> > > Analyzing data ...");
		getVotersFromArray(HtmlDataAnalyze(HtmlDataAnalyze(scanResult, uniqueTag, false), tableTag, false), namesColumn, votesColumn);
		System.out.println("> > > Data analyzing finished.\n");
	}
	
	// --- ����������� ������� ---
	/*
	 * ��������� ���������� ������ �� ��������� ���������� � expandSize ���������� ����� � ���������� �����, ����������� � ���� ����������
	 * �� ����������� - expandableArray.
	 */
	
	byte[] arrayExpand(byte[] expandableArray, int expandSize)
	{
		byte[] tempArray = new byte[expandableArray.length + expandSize];	//������ ������ ����� ���������� ����� ��������� ������� �������.
		for(int i = 0; i < expandableArray.length; i++)	//���������� � ���� ������ ������
		{
			tempArray[i] = expandableArray[i];	//���������� ����� ������ ������ ������� �������
		}
		
//		System.out.println("Array expanded to " + tempArray.length + " elements");
		return tempArray;
	}

	
	// --- ���������� HTML ������ ---
	/*
	 * ����������� �������� ������ - analyzingArray, ��������� c ��������-�������� � ����������� �� ���� ����������,
	 * ������� ��������� ����� ������ - dataBeginTag � dataEndTag. � ������ ������������� ������� ����� ���������� ����� ������� ������ � ���������
	 * ������, ����������� ������ - withKeyTags  
	 * ���������� ������:
	 * tagStarts - ������ ������ ����. ��������� ����� ������ ������� � ����������� � ��� �������� '<'. ����� ��� �������� ��������� � ��������.
	 * tagEnds - ����� ��� ����� ����, �������������� '>'. ���������� ����� ����.
	 * count - � ���� ������, ������� �������� ������ � �������� ������, ��������� �������� ����� ��� ������ ��������� �������.
	 * dataBeginPos - ������ ����� �������, � �������� ����� ������ ������ � �������� ������
	 * keyTagCount - ������� �������� �������� �����, �����, ����� ���� ����� ����� �� ����������� ��� ��� � �� ��� ���������� �� �����������.
	 * scanedData - ��� �������� ������
	 * tagOpend - ����, ��������� �� ��, ��� ����������� ������ ��������� ������ ����. ����� ��� ���������� ���� � ��� �������� ��������� � ��������.
	 * ��� �� ����� ��� ����, ����� �������� ��������� ��������� ������ � ������������ ������, � ������ ���� ������ ������� ���� ��������� � ������ ��������.
	 */
	
	byte[] HtmlDataAnalyze(byte[] array, String dataBeginTag, boolean withKeyTags)
	{
		int tagStarts = 0, tagEnds = 0, count = 0, dataBeginPos = 0, keyTagCount = 0;
		String dataEndTag = getEndTag(dataBeginTag, false);
		byte[] scannedData = new byte[0];
		boolean tagOpened = false;
		boolean requiredData = false;
		
		for(int nowCursor = 0; nowCursor < array.length; nowCursor++) //�������� ����
		{
			if(array[nowCursor] == 60 && !tagOpened) //��������� �� �������� �� ������� ������� �������� '<'
			{
				tagOpened = true;  
				tagStarts = nowCursor + 1; //����� ������ � ������� ����� ����� ������ ������. +1 ��� ���� ����� �� ��������� � ������ ������ '<'
			}
			else if(array[nowCursor] == 62 && tagOpened) //���������, �������� �� ���, ��������� ��� � �������� '>'
			{
				tagEnds = nowCursor; //���������� ��������� ������� ��� �������� ���������
				tagOpened = false;	
				
				char compare[] = new char[tagEnds - tagStarts]; //������������� ������� ��� ���������
				
				
				for(int n = 0; n < tagEnds - tagStarts; n++) //������
				{
					compare[n] = (char) array[tagStarts + n];
				}
				
				if(getEndTag(new String(compare), true).equals(getEndTag(dataBeginTag, true)) && requiredData) 
				{
//					System.out.println("Now key tag count is: " + keyTagCount);
					keyTagCount++;
				}
				
				if(new String(compare).equals(dataEndTag) && requiredData) 
				{
//					System.out.println("Now key tag count is: " + keyTagCount);
					keyTagCount--;
				}
				
				if(new String(compare).equals(dataBeginTag)) //���������� ���� ���������, � ������ ������ ����������� ������ ������ ������� ����������
				{
//					System.out.println("Data marker seccesfuly founded!");
					if(withKeyTags) dataBeginPos = tagStarts - 1; //���� ������� ��������� ���� ����� �������� ���������� ������ � ��������� ������
					else dataBeginPos = nowCursor + 1; // � ��� ���� �������� ���� �� �����
					keyTagCount++;
					requiredData = true;
				}
				
				if(new String(compare).equals(dataEndTag) && requiredData == true && keyTagCount == 0)
				{
					if(withKeyTags) //������ ������ � ������
						{
							scannedData = arrayExpand(scannedData, tagEnds - dataBeginPos + 1);	//����� ����������� ������, �������� ����
							for(int g = dataBeginPos; g < tagEnds + 1; g++) //���� ������ � �������� ������, =1 ��� ������ ������� '>'� ����� ���������
							{
//								System.out.print((char) array[g]);
//								System.out.print("|" + count + ":");
								scannedData[count] = array[g]; //��� ����� ������� ��� ������������ ������� ��������� �������.
								count++;	//��� ���� ���������� �� ���������� �� �������� � ���� ������.
							}
						}
					else //������ ��� �����. -2 ������������� ��������� ������� ����� �������� "<'.
						{
							scannedData = arrayExpand(scannedData, tagStarts - dataBeginPos - 2);
							for(int g = dataBeginPos; g < tagStarts - 2; g++)
							{
//								System.out.print((char) array[g]);
//								System.out.print("|" + count + ":");
								scannedData[count] = array[g];
								count++;
							}
						}
					
//					System.out.println("Data position captured!");
//					System.out.println();
					requiredData = false;
				}
			}
			else if(nowCursor - tagStarts > 128 && tagOpened) tagOpened = false;
		}
		
		return scannedData;
	}
	
	// --- ����� ��� ����������� ������������ ���� ---
	/*
	 * ������� count ��������� ������ ��������� ����.
	 * ����� x ������������ ��� ������, ���� �������������.
	 */
	
	String getEndTag(String beginTag, boolean getKeyTag)
	{
		int count = 0;
		for(char x : beginTag.toCharArray())	//������������������� ����� ����� for - ���� for each � ���������� �����
		{
			if(x != ' ') count++;
			else break;
		}
		if(!getKeyTag)return ("/" + beginTag.substring(0, count));	// ����� substring ���������� ������ ������� beginTeg-� ��������������� ���������. 
		else return beginTag.substring(0, count);
	}
	
	// --- ����� ��� �������� ����� ---
	/*
	 * �������� ���������� �� ���� ����� ���.
	 * ����� ���������� ���������� ����� ������� ��������� �� ��������� ������ ����.
	 */
	
	String getDataFromTag(byte[] array)
	{
		int startPos = 0, endPos = 0;
		String executedStr = new String(array);
		String resultStr = "";
		boolean tagOpened = false;
		
		for(int count = 0; count < executedStr.length(); count++)
		{
			if(executedStr.charAt(count) == '<') 
			{
				endPos = count;
				resultStr += executedStr.substring(startPos, endPos);
				tagOpened = true;
			}
			else if(executedStr.charAt(count) == '>') 
			{
				startPos = count + 1;
				tagOpened = false;
			}
			else if(count + 1 == executedStr.length() && !tagOpened)
			{
				endPos = count + 1;
				resultStr += executedStr.substring(startPos, endPos);
			}
		}
		return resultStr;
	}
	
	// --- ����������� ������� ---
	
	ArrayList<Voter> getVotersFromArray(byte[] array, int nickNameColumnNo, int votesColumnNo)
	{
		ArrayList<Voter> detectedVoters = new ArrayList<Voter>(); 
		
		String rowTag = "tr";
		String rowEndTag = "/tr";
		
		
		byte[] scannedData = new byte[0];
		
		for(int cursorPos = 0, tagBegin = 0, tagEnd = 0, dataBeginPos = 0; cursorPos < array.length; cursorPos++)
		{
			if(array[cursorPos] == 60)
			{
				tagBegin = cursorPos + 1;
			}
			else if(array[cursorPos] == 62)
			{
				tagEnd = cursorPos;
				String compare = getEndTag(new String(array).substring(tagBegin, tagEnd), true);
				
				if(new String(compare).equals(rowTag)) 
				{
					dataBeginPos = cursorPos + 1;
				}
				
				if(new String(compare).equals(rowEndTag))
				{
					int count = 0;
					scannedData = arrayExpand(scannedData, tagBegin - dataBeginPos - 1);
					
					for(int g = dataBeginPos; g < tagBegin - 1; g++) 
					{
//						System.out.print((char) array[g]);
						scannedData[count] = array[g]; 
						count++;
					}
					detectedVoters.add(getVoterFromRaw(scannedData, nickNameColumnNo, votesColumnNo));
				}
			}
		}
		return detectedVoters;
	}
	
	// --- ����� ��������� ������ ����� ������ ������� ---
	
	Voter getVoterFromRaw(byte[] array, int nickNameColumnNo, int votesColumnNo)
	{
		String columnTag = "td";
		String columnEndTag = "/td";
		int columnNo = 1;
		
		for(int cursorPos = 0, tagBegin = 0, tagEnd = 0, dataBeginPos = 0; cursorPos < array.length; cursorPos++)
		{
			if(array[cursorPos] == 60)
			{
				tagBegin = cursorPos + 1;
			}
			else if(array[cursorPos] == 62)
			{
				tagEnd = cursorPos;
				String compare = getEndTag(new String(array).substring(tagBegin, tagEnd), true);
				
				if(compare.equals(columnTag)) dataBeginPos = cursorPos + 1;
				
				if(compare.equals(columnEndTag) && columnNo == nickNameColumnNo)
				{
					byte[] scannedData = new byte[0];
					int count = 0;
					
					scannedData = arrayExpand(scannedData, tagBegin - dataBeginPos);
					
					for(int g = dataBeginPos; g < tagBegin; g++) 
					{
//						System.out.print((char) array[g]);
						scannedData[count] = array[g]; 
						count++;		
					}
					System.out.print(getDataFromTag(scannedData));
					columnNo++;
					System.out.print(": ");
				} 
				else if(new String(compare).equals(columnEndTag) && columnNo == votesColumnNo)
				{
					byte[] scannedData = new byte[0];
					int count = 0;
					
					scannedData = arrayExpand(scannedData, tagBegin - dataBeginPos);
					
					for(int g = dataBeginPos; g < tagBegin; g++) 
					{
//						System.out.print((char) array[g]);
						scannedData[count] = array[g]; 
						count++;
					}
					System.out.print(getDataFromTag(scannedData));
					System.out.println();
					columnNo++;
				}
				else if(new String(compare).equals(columnEndTag) && columnNo != votesColumnNo && columnNo != nickNameColumnNo)
				{
					columnNo++;
				}
			}
			
		}
		
		return null;
	}
	
	// --- �������� ������ �� �����
	Connection SQLconnection;
	Statement statement;
	
	void downloadVoterFromSQL()
	{
		try {
			SQLconnection = DriverManager.getConnection("jdbc:mysql://localhost:3306/votelist", "root", "318972645971368425aA");
			statement = (Statement) SQLconnection.createStatement();
			statement.execute("create table user(" + "id integer primary key auto_increment, " + "name varchar(100));");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally
		{
			try {SQLconnection.close();} catch(Exception Ex) {Ex.printStackTrace();}
			try {statement.close();} catch(Exception Ex) {Ex.printStackTrace();}
		}
	}
	
	void VotersInfo(Voter voter)
	{
		
	}
	
	// --- ��������� ���� �� ������������ � ������� ---
	
	int voterCheck(String nickName)
	{
		for(int voterNo = 0; voterNo < voters.size() && voters.get(0) != null; voterNo++)
		{
			Voter vCheck = voters.get(voterNo);
			if(vCheck.getName() == nickName)
			{
				return voterNo;
			}
		}
		
		return -1;
	}
}

// --- ����� ��� �������� ������ � ���������� ---

class Voter
{
	final String nickName;
	int voteCount;
	
	Voter(String nickName, int voteCount)
	{
		this.nickName = nickName;
		this.voteCount = voteCount;
	}
	
	String getName()
	{
		return nickName;
	}
	
	int getVoteCount()
	{
		return voteCount;
	}
	
	void changeVoteCount(int Value)
	{
		voteCount += Value;
	}
}