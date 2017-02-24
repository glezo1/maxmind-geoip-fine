package main;

import java.io.BufferedWriter;
import java.io.Console;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import data_structures.IpRangeOpenHashtable;
import data_structures.Ip_start_end_property;
import com.glezo.ipv4.UnparseableIpv4Exception;
import com.glezo.jWget.JWget;
import com.glezo.unzipper.Unzipper;

import jargs.gnu.CmdLineParser;
import jargs.gnu.CmdLineParser.UnknownOptionException;
import org.apache.commons.lang3.ArrayUtils;


public class Main
{
	//--------------------------------------------------------------------------	
	public static void					main(String[] args) throws ClassNotFoundException 
	{		
		//TODO! trap ctrl-c, and make statement.cancel()
		
		String usage=			"Usage: -o       <output_folder> [-i      <input_folder>] -h     <DB_host> -u     <DB_user> -p[<DB_pass>]     -P     <DB_port> [-H]     <database_name>";
		String usageL=			"Usage: --output <output_folder> [--input <input_folder>] --host <DB_host> --user <DB_user> --pass[<DB_pass>] --port <DB_port> [--help] <database_name>";
		String helpString=		"-o/--output			<output_folder>	Specifes the folder where downloaded csv files will be written to.								\n";
		helpString+=			"[-i/--input			<input_folder>]	Specifies the input folder to read csv from, rather than downloading them from maxmind.			\n";
		helpString+=			"										Folder name MUST finish by yyyyMMdd, in order to express the csv data timestamp.				\n";
		helpString+=			"										Folder must contain, at least, files with these names:											\n";
		helpString+=			"										GeoIPASNum2.csv																					\n";
		helpString+=			"										GeoIPCountryWhois.csv																			\n";
		helpString+=			"										GeoLiteCity-Blocks.csv																			\n";
		helpString+=			"										GeoLiteCity-Location.csv																		\n";
		helpString+=			"										region.csv																						\n";
		helpString+=			"-h/--host				<DB_host>		Specifies the host address where the database is listening at.									\n";
		helpString+=			"-u/--user				<DB_user>		Specifies the database user that will be used to connect to the database.						\n";
		helpString+=			"-p[pass]/--pass[pass]					If -p/--pass, password will be prompted (in the shape of MySQL console client -p flag)			\n";
		helpString+=			"-P/--port								Specifies the port where the database is listening at.											\n";
		helpString+=			"[-H/--help]							Print this help and exits.																		\n";
		String versionString=	"0.1 Design and implementation by @glezo1";

		
		CmdLineParser parser = new CmdLineParser(); 
		CmdLineParser.Option destiny =	parser.addStringOption('o',		"output");
		CmdLineParser.Option input =	parser.addStringOption('i',		"input");
		CmdLineParser.Option user = 	parser.addStringOption('u',		"user");
		CmdLineParser.Option pass = 	parser.addBooleanOption('p',	"pass");
		CmdLineParser.Option host = 	parser.addStringOption('h',		"host");
		CmdLineParser.Option port = 	parser.addStringOption('P',		"port");
		CmdLineParser.Option help = 	parser.addBooleanOption('H',	"help");
		
		String password=null;
		boolean finished_args_parsing=false;
		while(!finished_args_parsing)
		{
			try 
			{ 
				parser.parse(args);
				finished_args_parsing=true;
			}
			catch (UnknownOptionException e) 
			{
				if(e.getOptionName().startsWith("-p"))
				{
					String supplied_password_flag=e.getOptionName();
					password=e.getOptionName().substring(2);
					args=ArrayUtils.removeElement(args,supplied_password_flag);
				}
				else if(e.getMessage().startsWith("Unknown option '--pass"))
				{
					String supplied_password_flag=e.getMessage().substring(16).split("'")[0];
					password=supplied_password_flag.substring(6);
					args=ArrayUtils.removeElement(args,supplied_password_flag);
				}
				else
				{
					System.err.println(e.getMessage());
					System.out.println(usage);
					System.out.println(usageL);
					System.exit(1);
				}
			}
			catch (CmdLineParser.OptionException e) 
			{
				System.err.println(e.getMessage());
				System.out.println(usage);
				System.out.println(usageL);
				System.exit(1);
			}
		}
		if(args.length<1)
		{
			System.out.println(versionString);
			System.out.println(usage);
			System.out.println(usageL);
			System.exit(1);			
		}
		String destinyValue		= (String)parser.getOptionValue(destiny);
		String inputValue		= (String)parser.getOptionValue(input);
		String userValue   		= (String)parser.getOptionValue(user);
		Boolean hasPassValue	= (Boolean)parser.getOptionValue(pass);
		String hostValue   		= (String)parser.getOptionValue(host);
		String portValue   		= (String)parser.getOptionValue(port);
		Boolean helpValue   	= (Boolean)parser.getOptionValue(help);
		String dbname			= args[args.length-1];
		
		//check args
		if(helpValue!=null)
		{
			System.out.println(versionString);
			System.out.println(usage);
			System.out.println(usageL);
			System.out.println(helpString);
			System.exit(0);			
		}
		if( (destinyValue==null)||(userValue==null)||(hostValue==null)||(portValue==null))
		{
			System.out.println(versionString);
			System.out.println(usage);
			System.out.println(usageL);
			System.exit(1);			
		}
		if(hasPassValue!=null && hasPassValue)
		{
			Console console = System.console();
			if (console == null) 
			{
				System.out.println("Couldn't get Console instance");
				System.out.println("Running from IDE instead from console?");
				System.exit(1);
		    }
			char passwordArray[] = console.readPassword("Enter DB password: ");
			password=new String(passwordArray);
			
		}
		File destinyFolder=new File(destinyValue);
		if(!destinyFolder.isDirectory())
		{
			System.out.println(usage);
			System.out.println(usageL);
			System.err.println("-o <destiny> must be a valid directory");
			System.exit(2);
		}

		String date_version_string=null;

		File input_folder=null;
		if(inputValue!=null)
		{
			input_folder=new File(inputValue);
			if(!input_folder.isDirectory())
			{
				System.out.println(usage);
				System.out.println(usageL);
				System.err.println("-i <input_folder> must be a valid directory");
				System.exit(2);
			}
			else
			{
				String input_folder_name=input_folder.getName();
				if(input_folder_name.length()<8)
				{
					System.out.println(usage);
					System.out.println(usageL);
					System.err.println("-i <input_folder> must have a name in the shape of *yyyyMMdd");
					System.exit(2);
				}
				else
				{
					SimpleDateFormat sdf_input_folder=new SimpleDateFormat("yyyyMMdd");
					String input_folder_name_date=input_folder_name.substring(input_folder_name.length()-8,input_folder_name.length());
					date_version_string=input_folder_name_date;
					try 
					{
						sdf_input_folder.parse(input_folder_name_date);
					} 
					catch (ParseException e) 
					{
						System.out.println(usage);
						System.out.println(usageL);
						System.err.println("-i <input_folder> must have a name in the shape of *yyyyMMdd");
						System.exit(2);
					}
				}
			}
			
		}
		SimpleDateFormat simpleDateFormat=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		Connection connection=null;
		Statement statement=null;
		try 
		{
			connection=DriverManager.getConnection("jdbc:mysql://"+hostValue+":"+portValue+"/"+dbname+"?user="+userValue+"&password="+password+"&rewriteBatchedStatements=true");	
			statement=connection.createStatement();
		} 
		catch (SQLException e) 
		{
			System.err.println("Couldn't connect to DB! Aborting...");
			System.err.println("Make sure specified database exists and you have executed the attached DDL");
			while(e!=null)
			{
				System.out.println("SQL Exception:");
				System.out.println("State   : "+e.getSQLState());
				System.out.println("Message : "+e.getMessage());
				System.out.println("Error   : "+e.getErrorCode());
				e=e.getNextException();
			}			
			System.exit(2);
		}

		date_version_string=						download_required_files(			simpleDateFormat,	statement,	destinyValue,	destinyFolder,						input_folder,	usage,	usageL);
		String highest_executed_date=				get_highest_data_version_executed(	simpleDateFormat,	statement,									date_version_string);
		int compare_result=1;
		if(highest_executed_date!=null)
		{
			compare_result=date_version_string.compareTo(highest_executed_date);
		}
		if(compare_result==0)
		{
			System.out.println("Data for date-version "+date_version_string+" was the last executed date. Nothing to do, exiting...");
			System.exit(0);
		}
		else if(compare_result<0)
		{
			System.out.println("Data for date-version "+date_version_string+" is lower than was the last executed date ("+highest_executed_date+"). Nothing to do, exiting...");
			System.exit(0);
		}
													truncate_load_tables(				simpleDateFormat,	statement,									date_version_string	);
													load_data_infile(					simpleDateFormat,	statement,	destinyValue,					date_version_string	);		
													preprocessed_joins(					simpleDateFormat,	statement,									date_version_string	);
		IpRangeOpenHashtable asnumber_in_memory=	asnumber_to_memory(					simpleDateFormat,	statement,									date_version_string	);
													truncate_processed_tables(								statement														);
		boolean finished=false;
		int loop_number=-1;
		while(!finished)
		{
			loop_number++;
			System.out.println(simpleDateFormat.format(new Date())+" --LOOP "+loop_number+"-----------------------------------------");
			if(loop_number==0)	{	load_geo_asnum(simpleDateFormat,statement,"TGIP_202_BLOCKS_LOCATIONS_REGIONS_COUNTRY",	asnumber_in_memory,destinyFolder+"/temp.txt",loop_number);	}
			else				{	load_geo_asnum(simpleDateFormat,statement,"TGIP_206_PRE_ORPHANS_PREVIOUS_STEP",			asnumber_in_memory,destinyFolder+"/temp.txt",loop_number);	}
			
			split_geoasnum_1_into_geo_asnum_2and_orphans(simpleDateFormat,statement,loop_number);
			finished=geo_asnum_loop_finished(loop_number,statement);
			if(!finished)
			{
				dump_preorphans_into_previous_step(statement);
			}
		}
		generate_last_board(simpleDateFormat,statement,date_version_string);
		update_historic_board(simpleDateFormat,statement,date_version_string);
		System.out.println(simpleDateFormat.format(new Date()) + " JOB COMPLETED. EXITING.");
		System.exit(0);
	}
	//----------------------------------------------------------------------------------------------------------------------------------------------------
	public static String				download_required_files(SimpleDateFormat simpleDateFormat,Statement statement,String destinyValue,File destinyFolder,File inputFolder,String usage,String usageL)
	{
		String dateBegin=simpleDateFormat.format(new Date());
		String date_version_string=null;

		if(inputFolder==null)
		{
			//download files and folders
			ArrayList<String> requiredFiles=new ArrayList<String>();
			ArrayList<String> downloadPaths=new ArrayList<String>();
			ArrayList<String> filesToUnzip=new ArrayList<String>();
			
			requiredFiles.add("region.csv");
			downloadPaths.add("http://dev.maxmind.com/static/csv/codes/maxmind/region.csv");
			
			//geoIPCountryWhois.csv
			requiredFiles.add("GeoIPCountryCSV.zip");
			downloadPaths.add("http://geolite.maxmind.com/download/geoip/database/GeoIPCountryCSV.zip");
			filesToUnzip.add(destinyValue+"/GeoIPCountryCSV.zip");

			
			//requiredFiles.add("GeoIpv6.csv");
			
			requiredFiles.add("GeoIPASNum2.zip");
			downloadPaths.add("http://download.maxmind.com/download/geoip/database/asnum/GeoIPASNum2.zip");		
			filesToUnzip.add(destinyValue+"/GeoIPASNum2.zip");

			//GeoLiteCity-Blocks.csv , GeoLiteCity-Location.csv
			requiredFiles.add("GeoLiteCity-latest.zip");
			downloadPaths.add("http://geolite.maxmind.com/download/geoip/database/GeoLiteCity_CSV/GeoLiteCity-latest.zip");
			filesToUnzip.add(destinyValue+"/GeoLiteCity-latest.zip");

			System.out.println(simpleDateFormat.format(new Date())+" "+"STEP 1: DOWNLOADING REQUIRED FILES...");
			for(int i=0;i<requiredFiles.size();i++)
			{
				try 
				{
					System.out.print(simpleDateFormat.format(new Date())+" \t\tDownloading "+requiredFiles.get(i)+"...");
					JWget.jwGet(destinyFolder+"/"+requiredFiles.get(i),downloadPaths.get(i));
					System.out.println("...DONE");
				} 
				catch (MalformedURLException e) 
				{
					System.err.println("MalformedURLException when downloading file "+requiredFiles.get(i)+" from "+downloadPaths.get(i));
					System.exit(1);
				} 
				catch (IOException e) 
				{
					System.err.println("IOException when downloading file "+requiredFiles.get(i)+" from "+downloadPaths.get(i));
					System.exit(1);
				}							
			}		
			//unzip where appropiate
			for(int i=0;i<filesToUnzip.size();i++)
			{
				try 
				{
					System.out.print(simpleDateFormat.format(new Date())+" \t\tUnzipping "+filesToUnzip.get(i)+"...");
					Unzipper.unZip(filesToUnzip.get(i),destinyValue);
					System.out.println("...DONE");
				} 
				catch (IOException e) 
				{
					System.err.println("IOException at unzipping file "+filesToUnzip.get(i));
					e.printStackTrace();
					System.exit(1);
				}
				File file = new File(filesToUnzip.get(i));
				file.delete();
			}
			
			//GeoLiteCity-latest.zip shall create a folder like GeoLiteCity-yyyymmdd containing two csv files
			File folder = new File(destinyValue);
			File[] listOfFilesAndFolders = folder.listFiles();  
			ArrayList<File> listOfFolders=new ArrayList<File>();
			for (int i = 0; i < listOfFilesAndFolders.length; i++) 
			{ 
				if(listOfFilesAndFolders[i].isDirectory()) 
				{
					listOfFolders.add(new File(destinyValue+"/"+listOfFilesAndFolders[i].getName()));
				}
			}
			
			//let's copy those files to the upper folder
			for (int i = 0; i < listOfFolders.size(); i++) 
			{
				File current_folder=listOfFolders.get(i);
				if(current_folder.getName().startsWith("GeoLiteCity_"))
				{
					date_version_string=current_folder.getName().substring(12,current_folder.getName().length());
				}

				File[] listOfFiles = current_folder.listFiles();  
				for (int j = 0; j < listOfFiles.length; j++) 
				{
					String file_relative_name=listOfFiles[j].getName();
					File tmp=new File(destinyFolder+"/"+file_relative_name);
					
					try{	Files.delete(tmp.toPath()); }catch (IOException e){}
					try
					{
						Files.copy(listOfFiles[j].toPath(),tmp.toPath());
					} 
					catch (IOException e) 
					{
						System.err.println("IOException at extracting folder!!");
						e.printStackTrace();
						System.exit(1);
					}
					listOfFiles[j].delete();
				}
				listOfFolders.get(i).delete();
			}
			
			//if new csv-dataset, copy to a separate folder
			File date_version_folder=new File(destinyFolder+"/"+date_version_string);
			if(!date_version_folder.exists())
			{
				date_version_folder.mkdirs();
				File[] files_to_preserve=destinyFolder.listFiles();
				for(int i=0;i<files_to_preserve.length;i++)
				{
					File current_file=files_to_preserve[i];
					String current_file_name=current_file.getName();
					File tmp=new File(date_version_folder+"/"+current_file_name);
					if(current_file.isFile() && !current_file_name.equals("temp.txt"))
					{
						try 
						{
							Files.copy(files_to_preserve[i].toPath(),tmp.toPath());
						} 
						catch (IOException e) 
						{
							e.printStackTrace();
						}
					}
				}
			}
		}
		else
		{
			System.out.println(simpleDateFormat.format(new Date())+" "+"STEP 1: COPYING REQUIRED FILES...");

			ArrayList<String> required_files=new ArrayList<String>();
			required_files.add("GeoIPASNum2.csv");
			required_files.add("GeoIPCountryWhois.csv");
			required_files.add("GeoLiteCity-Blocks.csv");
			required_files.add("GeoLiteCity-Location.csv");
			required_files.add("region.csv");
			
			date_version_string=inputFolder.getName().substring(inputFolder.getName().length()-8,inputFolder.getName().length());
			
			for(int i=0;i<required_files.size();i++)
			{
				File current_required_file=new File(inputFolder.getAbsolutePath()+"/"+required_files.get(i));
				if(!current_required_file.isFile())
				{
					System.out.println(usage);
					System.out.println(usageL);
					System.err.println("-i <input_folder> must contain file "+required_files.get(i));
					System.exit(2);
				}
				else
				{
					File f=new File(destinyFolder+"/"+required_files.get(i));
					try 
					{
						Files.delete(f.toPath());
					}
					catch (IOException e) 
					{
					}
					try 
					{
						Files.copy(current_required_file.toPath(),f.toPath());
					} 
					catch (IOException e) 
					{
						System.err.println("IOException when copying files from input to output folder:");
						e.printStackTrace();
						System.exit(3);
					}
				}
			}
		}
		
		
		String dateEnd=simpleDateFormat.format(new Date());
		try 
		{
			if(inputFolder==null)
			{
				statement.execute("INSERT INTO TGIP_000_PROCESS_LOG VALUES ('"+date_version_string+"','download & unzip','download & unzip source files',null,null,'"+dateBegin+"','"+dateEnd+"',TIMEDIFF('"+dateEnd+"','"+dateBegin+"'),null)");
			}
			else
			{
				statement.execute("INSERT INTO TGIP_000_PROCESS_LOG VALUES ('"+date_version_string+"','copying exisint files','copying existing files',null,null,'"+dateBegin+"','"+dateEnd+"',TIMEDIFF('"+dateEnd+"','"+dateBegin+"'),null)");
			}
		} 
		catch (SQLException e) 
		{
			while(e!=null)
			{
				System.out.println("SQL Exception:");
				System.out.println("State   : "+e.getSQLState());
				System.out.println("Message : "+e.getMessage());
				System.out.println("Error   : "+e.getErrorCode());
				e=e.getNextException();
			}
			System.exit(2);
		}
		return date_version_string;
	}
	//----------------------------------------------------------------------------------------------------------------------------------------------------
	public static String				get_highest_data_version_executed(SimpleDateFormat	simpleDateFormat,Statement statement,String date_version_string)
	{
		String queryA="SELECT MAX(data_timestamp) FROM TGIP_401_HISTORIC_BOARD";
		String result=null;
		try
		{
			ResultSet resultset=statement.executeQuery(queryA);
			if(!resultset.isBeforeFirst())
			{
			}
			else
			{
				resultset.first();
				Date highest_executed_data_version_date=resultset.getDate(1);
				if(highest_executed_data_version_date!=null)
				{
					SimpleDateFormat sdf=new SimpleDateFormat("yyyyMMdd");
					result=sdf.format(highest_executed_data_version_date);
				}
			}
			resultset.close();
		}
		catch (SQLException e) 
		{
			while(e!=null)
			{
				System.out.println("SQL Exception:");
				System.out.println("State   : "+e.getSQLState());
				System.out.println("Message : "+e.getMessage());
				System.out.println("Error   : "+e.getErrorCode());
				e=e.getNextException();
			}
			System.exit(2);
		}
		return result;
	}
	//----------------------------------------------------------------------------------------------------------------------------------------------------
	public static void					truncate_load_tables(SimpleDateFormat simpleDateFormat,Statement statement,String date_version_string)
	{
		System.out.println(simpleDateFormat.format(new Date())+" "+"STEP 2.1: TRUNCATING LOAD TABLES...");
		String queryA="TRUNCATE TABLE TGIP_100_LOAD_REGION";
		String queryB="TRUNCATE TABLE TGIP_101_LOAD_GEOIP_COUNTRY_WHOIS";
		String queryC="TRUNCATE TABLE TGIP_102_LOAD_GEOIP_ASNUM";
		String queryD="TRUNCATE TABLE TGIP_103_LOAD_GEOLITE_CITY_BLOCKS";
		String queryE="TRUNCATE TABLE TGIP_104_LOAD_GEOLITE_CITY_LOCATIONS";
		try 
		{
			String dateBegin=simpleDateFormat.format(new Date());
			statement.execute(queryA);
			statement.execute(queryB);
			statement.execute(queryC);
			statement.execute(queryD);
			statement.execute(queryE);
			String dateEnd=simpleDateFormat.format(new Date());
			statement.execute("INSERT INTO TGIP_000_PROCESS_LOG VALUES ('"+date_version_string+"','truncate','truncate all',null,'all','"+dateBegin+"','"+dateEnd+"',TIMEDIFF('"+dateEnd+"','"+dateBegin+"'),null)");
		} 
		catch (SQLException e) 
		{
			while(e!=null)
			{
				System.out.println("SQL Exception:");
				System.out.println("State   : "+e.getSQLState());
				System.out.println("Message : "+e.getMessage());
				System.out.println("Error   : "+e.getErrorCode());
				e=e.getNextException();
			}
			System.exit(2);
		}
	}
	//----------------------------------------------------------------------------------------------------------------------------------------------------
	public static void					load_data_infile(SimpleDateFormat simpleDateFormat,Statement statement,String destinyValue,String date_version_string)
	{
		System.out.println(simpleDateFormat.format(new Date())+" STEP 2.2: LOAD DATA INFILE...");
		String queryA,queryB,queryC,queryD,queryE;
		queryA=			"	LOAD DATA INFILE '"+destinyValue.replace("\\","/")+"/region.csv' "
						+	"INTO TABLE TGIP_100_LOAD_REGION			"
						+	"FIELDS TERMINATED BY ','		"
						+	"OPTIONALLY ENCLOSED BY '\"'	";
		queryB=			"	LOAD DATA INFILE '"+destinyValue.replace("\\","/")+"/GeoIPCountryWhois.csv' "
						+	"INTO TABLE TGIP_101_LOAD_GEOIP_COUNTRY_WHOIS	"
						+	"FIELDS TERMINATED BY ','			"
						+	"ENCLOSED BY '\"'					";
		queryC=			"	LOAD DATA INFILE '"+destinyValue.replace("\\","/")+"/GeoIpAsNum2.csv' "
						+	"INTO TABLE TGIP_102_LOAD_GEOIP_ASNUM		"
						+	"FIELDS TERMINATED BY ','			"
						+	"OPTIONALLY ENCLOSED BY '\"'		"
						+	"(ipIntStart,ipIntEnd,ASNumber)		";	
		queryD=			"	LOAD DATA INFILE '"+destinyValue.replace("\\","/")+"/GeoLiteCity-Blocks.csv' "
						+	"INTO TABLE TGIP_103_LOAD_GEOLITE_CITY_BLOCKS	"
						+	"FIELDS TERMINATED BY ','			"
						+	"ENCLOSED BY '\"'					"
						+	"IGNORE 2 LINES						";
		queryE=			"	LOAD DATA INFILE '"+destinyValue.replace("\\","/")+"/GeoLiteCity-Location.csv' "
						+	"INTO TABLE TGIP_104_LOAD_GEOLITE_CITY_LOCATIONS	"
						+	"FIELDS TERMINATED BY ','				"
						+	"OPTIONALLY ENCLOSED BY '\"'			"
						+	"IGNORE 2 LINES							";
		try 
		{
			String dateBegin=simpleDateFormat.format(new Date());
			statement.execute(queryA);
			String dateEnd=simpleDateFormat.format(new Date());
			ResultSet result=statement.executeQuery("SELECT COUNT(*) FROM TGIP_100_LOAD_REGION");
			result.first();
			int number_of_affected_rows=result.getInt(1);
			statement.execute("INSERT INTO TGIP_000_PROCESS_LOG VALUES ('"+date_version_string+"','load','load region ',"+number_of_affected_rows+",'TGIP_100_LOAD_REGION','"+dateBegin+"','"+dateEnd+"',TIMEDIFF('"+dateEnd+"','"+dateBegin+"'),null)");

			dateBegin=dateEnd;
			statement.execute(queryB);
			dateEnd=simpleDateFormat.format(new Date());
			result=statement.executeQuery("SELECT COUNT(*) FROM TGIP_101_LOAD_GEOIP_COUNTRY_WHOIS");
			result.first();
			number_of_affected_rows=result.getInt(1);
			statement.execute("INSERT INTO TGIP_000_PROCESS_LOG VALUES ('"+date_version_string+"','load','load GeoIpCountryWhois',"+number_of_affected_rows+",'TGIP_101_LOAD_GEOIP_COUNTRY_WHOIS','"+dateBegin+"','"+dateEnd+"',TIMEDIFF('"+dateEnd+"','"+dateBegin+"'),null)");

			dateBegin=dateEnd;
			statement.execute(queryC);
			dateEnd=simpleDateFormat.format(new Date());
			result=statement.executeQuery("SELECT COUNT(*) FROM TGIP_102_LOAD_GEOIP_ASNUM");
			result.first();
			number_of_affected_rows=result.getInt(1);
			statement.execute("INSERT INTO TGIP_000_PROCESS_LOG VALUES ('"+date_version_string+"','load','load GeoIpAsNum2',"+number_of_affected_rows+",'TGIP_102_LOAD_GEOIP_ASNUM','"+dateBegin+"','"+dateEnd+"',TIMEDIFF('"+dateEnd+"','"+dateBegin+"'),null)");

			dateBegin=dateEnd;
			statement.execute(queryD);
			dateEnd=simpleDateFormat.format(new Date());
			result=statement.executeQuery("SELECT COUNT(*) FROM TGIP_103_LOAD_GEOLITE_CITY_BLOCKS");
			result.first();
			number_of_affected_rows=result.getInt(1);
			statement.execute("INSERT INTO TGIP_000_PROCESS_LOG VALUES ('"+date_version_string+"','load','load GeoLiteCity_Blocks',"+number_of_affected_rows+",'TGIP_103_LOAD_GEOLITE_CITY_BLOCKS','"+dateBegin+"','"+dateEnd+"',TIMEDIFF('"+dateEnd+"','"+dateBegin+"'),null)");

			dateBegin=dateEnd;
			statement.execute(queryE);
			dateEnd=simpleDateFormat.format(new Date());
			//argucia para que un producto cartesiano sea, por la idiosincrasia de los datos, un left join. Sí, no me sale la query :(
			//by inserting ipStart,ipEnd,ASNumber as max_unsigned_int,max_unsigned_int,null, I'll make a certain cartesian product work as a left join. botched.
			statement.execute("INSERT INTO TGIP_102_LOAD_GEOIP_ASNUM(ipIntStart,ipIntEnd,ASNumber) VALUES (4294967295,4294967295,null)");
			result=statement.executeQuery("SELECT COUNT(*) FROM TGIP_104_LOAD_GEOLITE_CITY_LOCATIONS");
			result.first();
			number_of_affected_rows=result.getInt(1);
			statement.execute("INSERT INTO TGIP_000_PROCESS_LOG VALUES ('"+date_version_string+"','load','load GeoLiteCity_Locations',"+number_of_affected_rows+",'TGIP_104_LOAD_GEOLITE_CITY_LOCATIONS','"+dateBegin+"','"+dateEnd+"',TIMEDIFF('"+dateEnd+"','"+dateBegin+"'),null)");
		} 
		catch (SQLException e) 
		{
			while(e!=null)
			{
				System.out.println("SQL Exception:");
				System.out.println("State   : "+e.getSQLState());
				System.out.println("Message : "+e.getMessage());
				System.out.println("Error   : "+e.getErrorCode());
				e=e.getNextException();
			}
			System.err.println("Couldnt load data infile to load tables!!");
			System.exit(2);
		}
	}
	//------------------------------------------------------------------------------------------------------------------------------------------
	public static void					preprocessed_joins(SimpleDateFormat simpleDateFormat,Statement statement,String date_version_string)
	{
		//now, lets begin to join them
		String queryA,queryB,queryC,queryD,queryAA,queryBB,queryCC;
		System.out.println(simpleDateFormat.format(new Date())+" "+"STEP 3: PREPROCESSED TABLES...");
		queryA=	"TRUNCATE TABLE TGIP_200_BLOCKS_LOCATIONS							";						
		queryB=	"TRUNCATE TABLE TGIP_201_BLOCKS_LOCATIONS_REGIONS					";						
		queryC=	"TRUNCATE TABLE TGIP_202_BLOCKS_LOCATIONS_REGIONS_COUNTRY			";						
		queryD=	"TRUNCATE TABLE TGIP_203_BLOCKS_LOCATIONS_REGIONS_COUNTRY_ASNUM_1	";						

		queryAA= "INSERT INTO TGIP_200_BLOCKS_LOCATIONS				\n"						
				+"(													\n"			
				+"		ipIntStart									\n"						
				+"		,ipIntEnd									\n"						
				+"		,locId 										\n"		
				+"		,countryISO									\n"				
				+"		,regionId									\n"				
				+"		,city										\n"				
				+"		,postalCode									\n"				
				+"		,latitude									\n"				
				+"		,longitude									\n"				
				+"		,metroCode									\n"				
				+"		,areaCode									\n"
				+")													\n"
				+"SELECT	ta.IpIntStart,ta.ipIntEnd,tb.*			\n" 
				+"FROM		TGIP_103_LOAD_GEOLITE_CITY_BLOCKS ta	\n"
				+"			LEFT JOIN								\n"
				+"			TGIP_104_LOAD_GEOLITE_CITY_LOCATIONS tb	\n"
				+"ON		ta.locId=tb.locId						\n";

		queryBB="INSERT INTO TGIP_201_BLOCKS_LOCATIONS_REGIONS				\n"						
				+"(															\n"			
				+"		ipIntStart											\n"						
				+"		,ipIntEnd											\n"						
				+"		,locId 		-- TODO! debug							\n"					
				+"		,countryISO											\n"
				+"		,region												\n"
				+"		,regionId	 -- TODO! debug							\n"								
				+"		,city												\n"				
				+"		,postalCode											\n"				
				+"		,latitude											\n"				
				+"		,longitude											\n"				
				+"		,metroCode											\n"				
				+"		,areaCode											\n"				
				+")															\n"
				+"SELECT	ta.IpIntStart,									\n"
				+"			ta.ipIntEnd, 									\n"
				+"			ta.locId,	-- TODO! debug						\n"
				+"			ta.countryISO,									\n" 
				+"			tb.region,										\n" 
				+"			ta.regionId, -- TODO! debug						\n"
				+"			ta.city,										\n"	 
				+"			ta.postalCode,									\n"	 
				+"			ta.latitude,									\n"	 
				+"			ta.longitude,									\n"
				+"			ta.metroCode, 									\n"
				+"			ta.areaCode										\n"
				+"FROM	TGIP_200_BLOCKS_LOCATIONS ta						\n"
				+"		LEFT JOIN											\n"
				+"		TGIP_100_LOAD_REGION tb								\n"
				+"ON	(ta.regionId=tb.id AND ta.countryISO=tb.countryISO)	\n";

		queryCC="INSERT INTO TGIP_202_BLOCKS_LOCATIONS_REGIONS_COUNTRY		\n"						
				+"(															\n"			
				+"		ipStart												\n"						
				+"		,ipEnd												\n"						
				+"		,ipIntStart											\n"						
				+"		,ipIntEnd											\n"						
				+"		,locId 		-- TODO! debug							\n"					
				+"		,countryISO											\n"
				+"		,country											\n"
				+"		,region												\n"
				+"		,regionId	 -- TODO! debug							\n"								
				+"		,city												\n"				
				+"		,postalCode											\n"				
				+"		,latitude											\n"				
				+"		,longitude											\n"				
				+"		,metroCode											\n"				
				+"		,areaCode											\n"				
				+")															\n"
				+"SELECT													\n"
				+ "			INET_NTOA(ta.IpIntStart),						\n"
				+ "			INET_NTOA(ta.IpIntEnd),							\n"
				+ "			ta.IpIntStart,									\n"
				+"			ta.ipIntEnd, 									\n"
				+"			ta.locId,	-- TODO! debug						\n"
				+"			ta.countryISO,									\n" 
				+"			tb.country,										\n" 
				+"			ta.region,										\n" 
				+"			ta.regionId, -- TODO! debug						\n"
				+"			ta.city,										\n"	 
				+"			ta.postalCode,									\n"	 
				+"			ta.latitude,									\n"	 
				+"			ta.longitude,									\n"
				+"			ta.metroCode, 									\n"
				+"			ta.areaCode										\n"
				+"FROM	TGIP_201_BLOCKS_LOCATIONS_REGIONS ta				\n"
				+"		LEFT JOIN											\n"
				+"		(													\n"
				+"			SELECT		countryISO,country					\n"
				+"			FROM		TGIP_101_LOAD_GEOIP_COUNTRY_WHOIS	\n"
				+"			GROUP BY	countryISO,country					\n"
				+"		) tb												\n" 
				+"	on ta.countryISO=tb.countryISO							\n";
		try 
		{
			ResultSet result=null;
			System.out.println(simpleDateFormat.format(new Date())+" \t\tTRUNCATING PREPROCESSED TABLES...");
			statement.execute(queryA);
			statement.execute(queryB);
			statement.execute(queryC);
			statement.execute(queryD);

			
			System.out.println(simpleDateFormat.format(new Date())+" \t\tTGIP_200_BLOCKS_LOCATIONS....[queryAA]");
			String dateBegin=simpleDateFormat.format(new Date());
			statement.execute(queryAA);
			String dateEnd=simpleDateFormat.format(new Date());
			result=statement.executeQuery("SELECT COUNT(*) FROM TGIP_200_BLOCKS_LOCATIONS");
			result.first();
			int number_of_affected_rows=result.getInt(1);
			statement.execute("INSERT INTO TGIP_000_PROCESS_LOG VALUES ('"+date_version_string+"','join','join blocks - locations',"+number_of_affected_rows+",'TGIP_200_BLOCKS_LOCATIONS','"+dateBegin+"','"+dateEnd+"',TIMEDIFF('"+dateEnd+"','"+dateBegin+"'),null)");
			
			
			System.out.println(simpleDateFormat.format(new Date())+" \t\tTGIP_201_BLOCKS_LOCATIONS_REGIONS....[queryBB]");
			dateBegin=simpleDateFormat.format(new Date());
			statement.execute(queryBB);
			dateEnd=simpleDateFormat.format(new Date());
			result=statement.executeQuery("SELECT COUNT(*) FROM TGIP_201_BLOCKS_LOCATIONS_REGIONS");
			result.first();
			number_of_affected_rows=result.getInt(1);
			statement.execute("INSERT INTO TGIP_000_PROCESS_LOG VALUES ('"+date_version_string+"','join','join blocks_locations - regions',"+number_of_affected_rows+",'TGIP_201_BLOCKS_LOCATIONS_REGIONS','"+dateBegin+"','"+dateEnd+"',TIMEDIFF('"+dateEnd+"','"+dateBegin+"'),null)");
			
			//1:20 para la subqyert cib ka tabla solo filas de españa
			System.out.println(simpleDateFormat.format(new Date())+" \t\tTGIP_202_BLOCKS_LOCATIONS_REGIONS_COUNTRY...[queryCC]");
			dateBegin=simpleDateFormat.format(new Date());
			statement.execute(queryCC);
			result=statement.executeQuery("SELECT COUNT(*) FROM TGIP_202_BLOCKS_LOCATIONS_REGIONS_COUNTRY");
			result.first();
			number_of_affected_rows=result.getInt(1);
			dateEnd=simpleDateFormat.format(new Date());
			statement.execute("INSERT INTO TGIP_000_PROCESS_LOG VALUES ('"+date_version_string+"','join','join blocks_locations_regions - country',"+number_of_affected_rows+",'TGIP_202_BLOCKS_LOCATIONS_REGIONS_COUNTRY','"+dateBegin+"','"+dateEnd+"',TIMEDIFF('"+dateEnd+"','"+dateBegin+"'),null)");
		}
		catch (SQLException e) 
		{
			while(e!=null)
			{
				System.out.println("SQL Exception:");
				System.out.println("State   : "+e.getSQLState());
				System.out.println("Message : "+e.getMessage());
				System.out.println("Error   : "+e.getErrorCode());
				e=e.getNextException();
			}
			System.exit(2);
		}
		
	}
	//------------------------------------------------------------------------------------------------------------------------------------------
	public static IpRangeOpenHashtable	asnumber_to_memory(SimpleDateFormat simpleDateFormat,Statement statement,String date_version_string)
	{
		System.out.println(simpleDateFormat.format(new Date())+" "+"STEP 4.1:\tREASNUMBER DUMP TO MEMORY...");
		String dateBegin=simpleDateFormat.format(new Date());
		IpRangeOpenHashtable result=null;
		try
		{
			ArrayList<Ip_start_end_property> asnumber_array=new ArrayList<Ip_start_end_property>();
			ResultSet resultset=statement.executeQuery("SELECT ipIntStart,ipIntEnd,ASNumber FROM TGIP_102_LOAD_GEOIP_ASNUM ORDER BY ipIntStart ASC");
			resultset.first();
			boolean finished=false;
			while(!finished)
			{
				long ip_start=resultset.getLong(1);
				long ip_end=resultset.getLong(2);
				String currentRangeASNumber=resultset.getString(3);
				asnumber_array.add(new Ip_start_end_property(ip_start, ip_end, currentRangeASNumber));
				if(resultset.isLast())
				{
					finished=true;
				}
				resultset.next();
			}
			result=new IpRangeOpenHashtable(16,asnumber_array);
			String dateEnd=simpleDateFormat.format(new Date());
			statement.execute("INSERT INTO TGIP_000_PROCESS_LOG VALUES ('"+date_version_string+"','dump','dump asnumber to in-memory open hashtable',null,null,'"+dateBegin+"','"+dateEnd+"',TIMEDIFF('"+dateEnd+"','"+dateBegin+"'),null)");
		}
		catch (SQLException e) 
		{
			while(e!=null)
			{
				System.out.println("SQL Exception:");
				System.out.println("State   : "+e.getSQLState());
				System.out.println("Message : "+e.getMessage());
				System.out.println("Error   : "+e.getErrorCode());
				e=e.getNextException();
			}
			System.exit(2);
		} 
		catch (UnparseableIpv4Exception e) 
		{
			System.out.println("UnparseableIpv4Exception when dumping ASNumber to memory.");
			System.out.println("This is quite embarrasing, and should've never happened.");
			System.out.println(e.getMessage());
			System.exit(2);
		}
		return result;
	}
	//------------------------------------------------------------------------------------------------------------------------------------------
	public static void					truncate_processed_tables(Statement statement)
	{
		try
		{
			statement.execute("TRUNCATE TABLE TGIP_203_BLOCKS_LOCATIONS_REGIONS_COUNTRY_ASNUM_1");
			statement.execute("TRUNCATE TABLE TGIP_204_BLOCKS_LOCATIONS_REGIONS_COUNTRY_ASNUM_2");
			statement.execute("TRUNCATE TABLE TGIP_205_PRE_ORPHANS");
			statement.execute("TRUNCATE TABLE TGIP_206_PRE_ORPHANS_PREVIOUS_STEP");
		}
		catch (SQLException e) 
		{
			while(e!=null)
			{
				System.out.println("SQL Exception:");
				System.out.println("State   : "+e.getSQLState());
				System.out.println("Message : "+e.getMessage());
				System.out.println("Error   : "+e.getErrorCode());
				e=e.getNextException();
			}
			System.exit(2);
		}
	}
	//------------------------------------------------------------------------------------------------------------------------------------------
	public static void					load_geo_asnum(SimpleDateFormat simpleDateFormat,Statement statement,String geo_source_tablename,IpRangeOpenHashtable asnumber_in_memory,String temp_file_path,int iteration)
	{
		System.out.println(simpleDateFormat.format(new Date())+" "+"STEP 4.2:\tcross "+geo_source_tablename+" with REASNUMBER-IN-MEMORY...");
		//load 203: relationship between geo and asnumber dimension
		File tmp_file_file = new File(temp_file_path);
		if(!tmp_file_file.exists()) 
		{
			try 
			{
				tmp_file_file.createNewFile();
			} 
			catch (IOException e) 
			{
				System.out.println("COULDNT CREATE TMP FILE. ABORTING!");
				System.exit(1);
			}
		}
		FileWriter fw=null;
		try 
		{
			fw = new FileWriter(tmp_file_file.getAbsoluteFile());
		} 
		catch (IOException e1) 
		{
			System.out.println("COULDNT CREATE FILEWRITER. ABORTING!");
			System.exit(1);
		}
		BufferedWriter bw = new BufferedWriter(fw);
		
		try
		{
			statement.execute("TRUNCATE TABLE TGIP_300_RANGE_A_RANGES_B");
			System.out.println(simpleDateFormat.format(new Date())+" "+"\t\tSTEP 4.2.1: DOWNLOAD FROM "+geo_source_tablename+"...");
			ResultSet resultset=statement.executeQuery("SELECT ipIntStart,ipIntEnd,'foobar' FROM "+geo_source_tablename+"  ORDER BY ipIntStart ASC");
			boolean finished_dump=!resultset.isBeforeFirst();
			resultset.first();
			if(finished_dump)
			{
				statement.execute("TRUNCATE TABLE TGIP_300_RANGE_A_RANGES_B");
			}
			else
			{
				System.out.println(simpleDateFormat.format(new Date())+" "+"\t\tSTEP 4.2.2: MATCHING GEO VS AS IN MEMORY...");
				while(!finished_dump)
				{
					long ip_start=resultset.getLong(1);
					long ip_end=resultset.getLong(2);
					Ip_start_end_property asn_match=asnumber_in_memory.get_mach_for_ip(ip_start);
					bw.write(ip_start+"\t"+ip_end+"\t"+asn_match.get_double_a()+"\t"+asn_match.get_double_b()+"\n");
					finished_dump=resultset.isLast();
					resultset.next();
				}
				resultset.close();
				bw.close();
				
				System.out.println(simpleDateFormat.format(new Date())+" "+"\t\tSTEP 4.2.3: LOADING INFILE...");
				String query="";
				query+=	"	LOAD DATA INFILE '"+temp_file_path.replace("\\","/")+"'		\n";
				query+=	"	INTO TABLE TGIP_300_RANGE_A_RANGES_B						\n";
				query+=	"	(ipIntStartA,ipIntEndA,@ipIntStartB,@ipIntEndB)				\n";
				query+=	"	SET															\n";
				query+=	"		ipIntStartB	=NULLIF(@ipIntStartB,'null'),				\n";
				query+=	"		ipIntEndB	=NULLIF(@ipIntEndB,'null')					\n";
				statement.execute(query);
			}
			tmp_file_file.delete();
		}
		catch (SQLException e) 
		{
			while(e!=null)
			{
				System.out.println("SQL Exception:");
				System.out.println("State   : "+e.getSQLState());
				System.out.println("Message : "+e.getMessage());
				System.out.println("Error   : "+e.getErrorCode());
				e=e.getNextException();
			}
			System.exit(2);
		} 
		catch (IOException e) 
		{
			System.out.println("IOEXCEPTION AT WRITING TO TEMP FILE. ABORTING!");
			System.exit(1);
		} 
		catch (UnparseableIpv4Exception e) 
		{
			System.out.println("UnparseableIpv4Exception when loading geo-ASNumber.");
			System.out.println("This is quite embarrasing, and should've never happened.");
			System.out.println(e.getMessage());
			System.exit(2);
		}
		
		
		//now we have the 300 relationship table, let's load:
		String queryDD="";
		queryDD="";
		queryDD+=	"INSERT INTO TGIP_203_BLOCKS_LOCATIONS_REGIONS_COUNTRY_ASNUM_1			\n";							
		queryDD+=	"(																		\n";
		queryDD+=	"	ipIntStart															\n";		
		queryDD+=	"	,ipIntEnd															\n";
		queryDD+=	"	,ipStart															\n";
		queryDD+=	"	,ipEnd																\n";
		queryDD+=	"	,ipIntStartB														\n";							
		queryDD+=	"	,ipIntEndB															\n";
		queryDD+=	"	,ipStartB															\n";
		queryDD+=	"	,ipEndB																\n";
		queryDD+=	"	,ASNumber															\n";
		queryDD+=	"	,locId 																\n";		
		queryDD+=	"	,countryISO															\n";
		queryDD+=	"	,country															\n";
		queryDD+=	"	,region																\n";
		queryDD+=	"	,regionId															\n";								
		queryDD+=	"	,city																\n";								
		queryDD+=	"	,postalCode															\n";								
		queryDD+=	"	,latitude															\n";								
		queryDD+=	"	,longitude															\n";								
		queryDD+=	"	,metroCode															\n";								
		queryDD+=	"	,areaCode															\n";								
		queryDD+=	")																		\n";
		queryDD+=	"SELECT																	\n";
		queryDD+=	"	A.ipIntStart														\n";
		queryDD+=	"	,A.ipIntEnd															\n";
		queryDD+=	"	,A.IpStart															\n";
		queryDD+=	"	,A.ipEnd															\n";
		queryDD+=	"	,B.ipIntStartB														\n";
		queryDD+=	"	,B.ipIntEndB														\n";
		queryDD+=	"	,INET_NTOA(B.ipIntStartB)	AS 'ipStartB'							\n";
		queryDD+=	"	,INET_NTOA(B.ipIntEndB)		AS 'ipEndB'								\n";
		queryDD+=	"	,C.ASNumber															\n";
		queryDD+=	"	,A.locId															\n";		
		queryDD+=	"	,A.countryISO														\n";
		queryDD+=	"	,A.country															\n";
		queryDD+=	"	,A.region															\n";
		queryDD+=	"	,A.regionId															\n";								
		queryDD+=	"	,A.city																\n";								
		queryDD+=	"	,A.postalCode														\n";							
		queryDD+=	"	,A.latitude															\n";					
		queryDD+=	"	,A.longitude														\n";					
		queryDD+=	"	,A.metroCode														\n";				
		queryDD+=	"	,A.areaCode															\n";		
		queryDD+=	"FROM	"+geo_source_tablename+" A										\n";
		queryDD+=	"		INNER JOIN TGIP_300_RANGE_A_RANGES_B B							\n";
		queryDD+=	"		ON		A.ipIntStart=B.ipIntStartA								\n";
		queryDD+=	"			AND	A.ipIntEnd	=B.ipIntEndA								\n";
		queryDD+=	"		LEFT JOIN TGIP_102_LOAD_GEOIP_ASNUM C							\n";
		queryDD+=	"		ON		B.ipIntStartB	=C.ipIntStart							\n";
		queryDD+=	"			AND	B.ipIntEndB		=C.ipIntEnd								\n";
		try
		{
			statement.execute("TRUNCATE TABLE TGIP_203_BLOCKS_LOCATIONS_REGIONS_COUNTRY_ASNUM_1");
			System.out.println(simpleDateFormat.format(new Date())+" "+"\t\tSTEP 4.2.4: JOINING INTO TGIP_203_BLOCKS_LOCATIONS_REGIONS_COUNTRY_ASNUM_1...");
			statement.execute(queryDD);
		}
		catch (SQLException e) 
		{
			while(e!=null)
			{
				System.out.println("SQL Exception:");
				System.out.println("State   : "+e.getSQLState());
				System.out.println("Message : "+e.getMessage());
				System.out.println("Error   : "+e.getErrorCode());
				e=e.getNextException();
			}
			System.exit(2);
		}
	}
	//------------------------------------------------------------------------------------------------------------------------------------------
	public static void					split_geoasnum_1_into_geo_asnum_2and_orphans(SimpleDateFormat simpleDateFormat,Statement statement,int iteration)
	{
		String queryB,queryC,queryD,queryE,queryF,queryG,queryH,queryI,queryJ,queryL,queryM,queryN,queryP,queryQ;
		try
		{
			
			//EQUALS
			//-- B  |----|
			//-- A  |----|
			queryB=		"		INSERT IGNORE INTO TGIP_204_BLOCKS_LOCATIONS_REGIONS_COUNTRY_ASNUM_2				\n"
						+"		SELECT																	\n"					
						+"				ipIntStart,ipIntEnd,ipStart,ipEnd,ipIntStartB,ipIntEndB,ipStartB,ipEndB,ASNumber,'equals',ipIntStart,IpIntEnd,locId,countryISO,country,region,regionId,city,postalCode,latitude,longitude,metroCode,areaCode,"+iteration+"						\n"					
						+"		FROM	TGIP_203_BLOCKS_LOCATIONS_REGIONS_COUNTRY_ASNUM_1 \n"
						+"		WHERE	ipIntStart=ipIntStartB AND ipIntEnd=ipIntEndB";
			System.out.println(simpleDateFormat.format(new Date())+" \t\t\t[queryB] equals");
			statement.execute(queryB);

			//DISJUNCT
			//-- B  ....|         |------|
			//-- A         |----|			
			queryC=		"		INSERT IGNORE INTO TGIP_204_BLOCKS_LOCATIONS_REGIONS_COUNTRY_ASNUM_2	\n"
						+"		SELECT																	\n" 
						+"				ipIntStart,ipIntEnd,ipStart,ipEnd,ipIntStartB,ipIntEndB,ipStartB,ipEndB,null,'disjunct',ipIntStart,IpIntEnd,locId,countryISO,country,region,regionId,city,postalCode,latitude,longitude,metroCode,areaCode,"+iteration+"	\n"
						+"		FROM	TGIP_203_BLOCKS_LOCATIONS_REGIONS_COUNTRY_ASNUM_1 				\n"
						+"		WHERE	ipIntEnd<ipIntStartB											\n"
						+"				OR ipIntStartB IS NULL											\n";
			System.out.println(simpleDateFormat.format(new Date())+" \t\t\t[queryC] disjunct");
			statement.execute(queryC);

			//COMPLETELY INCLUDED
			//-- B  |--------|
			//-- A    |----|    (query is where (included and not-equals)
			queryD=		"		INSERT IGNORE INTO TGIP_204_BLOCKS_LOCATIONS_REGIONS_COUNTRY_ASNUM_2				\n"
						+"		SELECT "
						+"			ipIntStart,ipIntEnd,ipStart,ipEnd,ipIntStartB,ipIntEndB,ipStartB,ipEndB,ASNumber,'completely',ipIntStart,IpIntEnd,locId,countryISO,country,region,regionId,city,postalCode,latitude,longitude,metroCode,areaCode,"+iteration+" "
						+"		FROM   TGIP_203_BLOCKS_LOCATIONS_REGIONS_COUNTRY_ASNUM_1 \n"
						+"		WHERE ( (ipIntStartB<=ipIntStart AND ipIntEnd<=ipIntEndB) AND (ipIntStartB<>ipIntStart AND ipIntEndB<>ipIntEnd) )";
			System.out.println(simpleDateFormat.format(new Date())+" \t\t\t[queryD] completely included");
			statement.execute(queryD);

			//COMPLETELY INCLUDED B
			//-- B |---------------------|
			//-- A          |------------|
			queryE=		"		INSERT IGNORE INTO TGIP_204_BLOCKS_LOCATIONS_REGIONS_COUNTRY_ASNUM_2		\n"
						+"		SELECT 															\n"
						+"			ipIntStart,ipIntEnd,ipStart,ipEnd,ipIntStartB,ipIntEndB,ipStartB,ipEndB,ASNumber,'completely-B',ipIntStart,IpIntEnd,locId,countryISO,country,region,regionId,city,postalCode,latitude,longitude,metroCode,areaCode,"+iteration+"	\n"
						+"		FROM   TGIP_203_BLOCKS_LOCATIONS_REGIONS_COUNTRY_ASNUM_1 		\n"
						+"		WHERE (ipIntStartB<ipIntStart AND ipIntEndB=ipIntEnd)	\n";
			System.out.println(simpleDateFormat.format(new Date())+" \t\t\t[queryE] completely included b");
			statement.execute(queryE);


			//PARTIALLY INCLUDED A
			//-- B         |--------|
			//-- A    |-----------------|
			queryF=		"		INSERT IGNORE INTO TGIP_204_BLOCKS_LOCATIONS_REGIONS_COUNTRY_ASNUM_2 "
						+"		(ipIntStart,ipIntEnd,ipStart,ipEnd,ipIntStartB,ipIntEndB,ipStartB,ipEndB,ASNumber,reason,parent_start,parent_end,locId,countryISO,country,region,regionId,city,postalCode,latitude,longitude,metroCode,areaCode) 									"
						+"		SELECT 	ipIntStart,ipIntStartB-1,ipStart,INET_NTOA(ipIntStartB-1),ipIntStartB,ipIntEndB,ipStartB,ipEndB,null,'P-I-A-left',ipIntStart,IpIntEnd,locId,countryISO,country,region,regionId,city,postalCode,latitude,longitude,metroCode,areaCode " 
						+"		FROM   TGIP_203_BLOCKS_LOCATIONS_REGIONS_COUNTRY_ASNUM_1 "
						+"		WHERE (ipIntStart<ipIntStartB AND ipIntEndB<ipIntEnd) ";
			System.out.println(simpleDateFormat.format(new Date())+" \t\t\t[queryF] partially included a-left");
			statement.execute(queryF);
			queryG=		"		INSERT IGNORE INTO TGIP_204_BLOCKS_LOCATIONS_REGIONS_COUNTRY_ASNUM_2 	"
						+"		SELECT 	ipIntStartB,ipIntEndB,ipStartB,ipEndB,ipIntStartB,ipIntEndB,ipStartB,ipEndB,ASNumber,'P-I-A-center',ipIntStart,IpIntEnd,locId,countryISO,country,region,regionId,city,postalCode,latitude,longitude,metroCode,areaCode,"+iteration+" "
						+"		FROM   TGIP_203_BLOCKS_LOCATIONS_REGIONS_COUNTRY_ASNUM_1 \n"
						+"		WHERE (ipIntStart<ipIntStartB AND ipIntEndB<ipIntEnd)";
			System.out.println(simpleDateFormat.format(new Date())+" \t\t\t[queryG] partially included a-center");
			statement.execute(queryG);
			queryH=		"		INSERT IGNORE INTO TGIP_205_PRE_ORPHANS "
						+"		(ipIntStart,ipIntEnd,ipStart,ipEnd,ipIntStartB,ipIntEndB,ipStartB,ipEndB,ASNumber,reason,parent_start,parent_end,locId,countryISO,country,region,regionId,city,postalCode,latitude,longitude,metroCode,areaCode) 									"
						+"		SELECT ipIntEndB+1,ipIntEnd,INET_NTOA(ipIntEndB+1),ipEnd,ipIntStartB,ipIntEndB,ipStartB,ipEndB,null,'P-I-A-right',ipIntStart,IpIntEnd,locId,countryISO,country,region,regionId,city,postalCode,latitude,longitude,metroCode,areaCode "
						+"		FROM   TGIP_203_BLOCKS_LOCATIONS_REGIONS_COUNTRY_ASNUM_1 	\n"
						+"		WHERE (ipIntStart<ipIntStartB AND ipIntEndB<ipIntEnd)";
			System.out.println(simpleDateFormat.format(new Date())+" \t\t\t[queryH] partially included a-right");
			statement.execute(queryH);
			

			//PARTIALLY INCLUDED C
			//-- B |------|
			//-- A |------------|
			queryI=		"		INSERT IGNORE INTO TGIP_204_BLOCKS_LOCATIONS_REGIONS_COUNTRY_ASNUM_2 "
						+"		SELECT " 
						+"			ipIntStart,ipIntEndB,ipStart,INET_NTOA(ipIntEndB),ipIntStartB,ipIntEndB,ipStartB,ipEndB,ASNumber,'P-I-C-left',ipIntStart,IpIntEnd,locId,countryISO,country,region,regionId,city,postalCode,latitude,longitude,metroCode,areaCode,"+iteration+""
						+"		FROM   TGIP_203_BLOCKS_LOCATIONS_REGIONS_COUNTRY_ASNUM_1 \n"
						+"		WHERE (ipIntStart=ipIntStartB AND ipIntEndB<ipIntEnd)";
			System.out.println(simpleDateFormat.format(new Date())+" \t\t\t[queryI] partially included c-left");
			statement.execute(queryI);
			queryJ=		"		INSERT IGNORE INTO TGIP_205_PRE_ORPHANS "
						+"		(ipIntStart,ipIntEnd,ipStart,ipEnd,ipIntStartB,ipIntEndB,ipStartB,ipEndB,ASNumber,reason,parent_start,parent_end,locId,countryISO,country,region,regionId,city,postalCode,latitude,longitude,metroCode,areaCode) 									"
						+"		SELECT "
						+"			ipIntEndB+1,ipIntEnd,INET_NTOA(ipIntEndB+1),ipEnd,ipIntStartB,ipIntEndB,ipStartB,ipEndB,null,'P-I-C-right',ipIntStart,IpIntEnd,locId,countryISO,country,region,regionId,city,postalCode,latitude,longitude,metroCode,areaCode"
						+"		FROM   TGIP_203_BLOCKS_LOCATIONS_REGIONS_COUNTRY_ASNUM_1 \n"
						+"		WHERE (ipIntStart=ipIntStartB AND ipIntEndB<ipIntEnd)";
			System.out.println(simpleDateFormat.format(new Date())+" \t\t\t[queryJ] partially included c-right");
			statement.execute(queryJ);

			//PARTIALLY INCLUDED D
			//-- B |------|
			//-- A     |------------|			
			queryL=		" 		INSERT IGNORE INTO TGIP_204_BLOCKS_LOCATIONS_REGIONS_COUNTRY_ASNUM_2 "
						+"		SELECT " 
						+"			ipIntStart,ipIntEndB,ipStart,ipEndB,ipIntStartB,ipIntEndB,ipStartB,ipEndB,ASNumber,'P-I-D-center',ipIntStart,IpIntEnd,locId,countryISO,country,region,regionId,city,postalCode,latitude,longitude,metroCode,areaCode,"+iteration+" "
						+"		FROM   TGIP_203_BLOCKS_LOCATIONS_REGIONS_COUNTRY_ASNUM_1 \n"
						+"		WHERE (ipIntStartB<ipIntStart AND ipIntEndB<ipIntEnd)";
			System.out.println(simpleDateFormat.format(new Date())+" \t\t\t[queryL] partially included d-center");
			statement.execute(queryL);
			queryM=		"		INSERT IGNORE INTO TGIP_205_PRE_ORPHANS "
						+"		(ipIntStart,ipIntEnd,ipStart,ipEnd,ipIntStartB,ipIntEndB,ipStartB,ipEndB,ASNumber,reason,parent_start,parent_end,locId,countryISO,country,region,regionId,city,postalCode,latitude,longitude,metroCode,areaCode) 									"
						+"		SELECT "
						+"			ipIntEndB+1,ipIntEnd,INET_NTOA(ipIntEndB+1),ipEnd,ipIntStartB,ipIntEndB,ipStartB,ipEndB,null,'P-I-D-right',ipIntStart,IpIntEnd,locId,countryISO,country,region,regionId,city,postalCode,latitude,longitude,metroCode,areaCode "
						+"		FROM   TGIP_203_BLOCKS_LOCATIONS_REGIONS_COUNTRY_ASNUM_1 \n"
						+"		WHERE (ipIntStartB<ipIntStart AND ipIntEndB<ipIntEnd)";
			System.out.println(simpleDateFormat.format(new Date())+" \t\t\t[queryM] partially included d-right");
			statement.execute(queryM);
			
			//PARTIALLY INCLUDED E
			//-- B |-----------------|
			//-- A |------------|			
			queryN=		"		INSERT IGNORE INTO TGIP_204_BLOCKS_LOCATIONS_REGIONS_COUNTRY_ASNUM_2 "
						+"		SELECT "
						+"			ipIntStart,ipIntEnd,ipStart,ipEnd,ipIntStartB,ipIntEndB,ipStartB,ipEndB,ASNumber,'P-I-E-left',ipIntStart,IpIntEnd,locId,countryISO,country,region,regionId,city,postalCode,latitude,longitude,metroCode,areaCode,"+iteration+" "
						+"		FROM   TGIP_203_BLOCKS_LOCATIONS_REGIONS_COUNTRY_ASNUM_1 \n"
						+"		WHERE (ipIntStart=ipIntStartB AND ipIntEnd<ipIntEndB)";
			System.out.println(simpleDateFormat.format(new Date())+" \t\t\t[queryN] partially included e-left");
			statement.execute(queryN);


			//PARTIALLY INCLUDED F
			//-- B     |-----------------|
			//-- A |------------|    
			queryP=		"		INSERT IGNORE INTO TGIP_204_BLOCKS_LOCATIONS_REGIONS_COUNTRY_ASNUM_2 "
						+"		SELECT "
						+"			ipIntStart,ipIntStartB-1,ipStart,INET_NTOA(ipIntStartB-1),ipIntStartB,ipIntEndB,ipStartB,ipEndB,null,'P-I-F-left',ipIntStart,IpIntEnd,locId,countryISO,country,region,regionId,city,postalCode,latitude,longitude,metroCode,areaCode,"+iteration+" "
						+"		FROM   TGIP_203_BLOCKS_LOCATIONS_REGIONS_COUNTRY_ASNUM_1 "
						+"		WHERE ((ipIntStart<ipIntStartB AND ipIntEnd<ipIntEndB) AND (ipIntStartB<ipIntEnd))";
			System.out.println(simpleDateFormat.format(new Date())+" \t\t\t[queryP] partially included f-left");
			statement.execute(queryP);
			queryQ=		"		INSERT IGNORE INTO TGIP_204_BLOCKS_LOCATIONS_REGIONS_COUNTRY_ASNUM_2 "
						+"		SELECT "
						+"			ipIntStartB,ipIntEnd,ipStartB,ipEnd,ipIntStartB,ipIntEndB,ipStartB,ipEndB,ASNumber,'P-I-F-center',ipIntStart,IpIntEnd,locId,countryISO,country,region,regionId,city,postalCode,latitude,longitude,metroCode,areaCode,"+iteration+" "
						+"		FROM   TGIP_203_BLOCKS_LOCATIONS_REGIONS_COUNTRY_ASNUM_1 \n"
						+"		WHERE ((ipIntStart<ipIntStartB AND ipIntEnd<ipIntEndB) AND (ipIntStartB<ipIntEnd))";
			System.out.println(simpleDateFormat.format(new Date())+" \t\t\t[queryQ] partially included f-center");
			statement.execute(queryQ);

		}
		catch (SQLException e) 
		{
			while(e!=null)
			{
				System.out.println("SQL Exception:");
				System.out.println("State   : "+e.getSQLState());
				System.out.println("Message : "+e.getMessage());
				System.out.println("Error   : "+e.getErrorCode());
				e=e.getNextException();
			}
			System.exit(2);
		}
	}
	//------------------------------------------------------------------------------------------------------------------------------------------
	public static boolean				geo_asnum_loop_finished(int loop_number,Statement statement)
	{
		if(loop_number==0)
		{
			return false;
		}
		try
		{
			ResultSet resultset=statement.executeQuery("SELECT COUNT(*) FROM TGIP_206_PRE_ORPHANS_PREVIOUS_STEP");
			resultset.first();
			int num_orphans_previous		=resultset.getInt(1);
			resultset=statement.executeQuery("SELECT COUNT(*) FROM TGIP_205_PRE_ORPHANS");		
			resultset.first();
			int num_orphans					=resultset.getInt(1);
			if(num_orphans!=num_orphans_previous)
			{
				return false;
			}
			resultset=statement.executeQuery("SELECT COUNT(*) FROM TGIP_205_PRE_ORPHANS A INNER JOIN TGIP_206_PRE_ORPHANS_PREVIOUS_STEP B ON A.ipIntStart=B.ipIntStart AND A.ipIntEnd=B.ipIntEnd");		
			resultset.first();
			int common_rows					=resultset.getInt(1);
			if(num_orphans == common_rows)
			{
				return  true;
			}
			else
			{
				return false;
			}
		}
		catch (SQLException e) 
		{
			while(e!=null)
			{
				System.out.println("SQL Exception:");
				System.out.println("State   : "+e.getSQLState());
				System.out.println("Message : "+e.getMessage());
				System.out.println("Error   : "+e.getErrorCode());
				e=e.getNextException();
			}
			System.exit(2);
		}
		return false;
	}
	//------------------------------------------------------------------------------------------------------------------------------------------
	public static void					dump_preorphans_into_previous_step(Statement statement)
	{
		try
		{
			statement.execute("TRUNCATE TABLE TGIP_206_PRE_ORPHANS_PREVIOUS_STEP");
			statement.execute("INSERT INTO TGIP_206_PRE_ORPHANS_PREVIOUS_STEP SELECT * FROM TGIP_205_PRE_ORPHANS");
			statement.execute("TRUNCATE TABLE TGIP_205_PRE_ORPHANS");
		}
		catch (SQLException e) 
		{
			while(e!=null)
			{
				System.out.println("SQL Exception:");
				System.out.println("State   : "+e.getSQLState());
				System.out.println("Message : "+e.getMessage());
				System.out.println("Error   : "+e.getErrorCode());
				e=e.getNextException();
			}
			System.exit(2);
		}
	}
	//------------------------------------------------------------------------------------------------------------------------------------------
	public static void					generate_last_board(SimpleDateFormat simpleDateFormat,Statement statement,String date_version_string)
	{
		System.out.println(simpleDateFormat.format(new Date())+" "+"STEP 5\tGENERATE LAST-STATE BOARD...");
		try
		{
			statement.execute("TRUNCATE TABLE TGIP_400_LAST_BOARD");
			String query="";
			query+=	"	INSERT INTO TGIP_400_LAST_BOARD										\n";
			query+=	"	SELECT																\n";
			query+=	"		STR_TO_DATE('"+date_version_string+"','%Y%m%d')					\n";
			query+=	"		,CONCAT_WS(' - ',ipStart,ipEnd)									\n";
			query+=	"		,A.*															\n";
			query+=	"	FROM	TGIP_204_BLOCKS_LOCATIONS_REGIONS_COUNTRY_ASNUM_2 A			\n";
			String dateBegin=simpleDateFormat.format(new Date());
			statement.execute(query);
			String dateEnd=simpleDateFormat.format(new Date());
			statement.execute("INSERT INTO TGIP_000_PROCESS_LOG VALUES ('"+date_version_string+"','insert','generate last-view board',null,null,'"+dateBegin+"','"+dateEnd+"',TIMEDIFF('"+dateEnd+"','"+dateBegin+"'),null)");
		}
		catch (SQLException e) 
		{
			while(e!=null)
			{
				System.out.println("SQL Exception:");
				System.out.println("State   : "+e.getSQLState());
				System.out.println("Message : "+e.getMessage());
				System.out.println("Error   : "+e.getErrorCode());
				e=e.getNextException();
			}
			System.exit(2);
		}

	}
	//------------------------------------------------------------------------------------------------------------------------------------------
	public static void					update_historic_board(SimpleDateFormat simpleDateFormat,Statement statement,String date_version_string)
	{
		System.out.println(simpleDateFormat.format(new Date())+" "+"STEP 6:\tUPDATE HISTORIC BOARD...");

		//mark invalid records.
		//sadly, mysql wont allow an update <A> where <A>.<B> IN SELECT (.... FROM <A>), so we need a staging table
		String query="TRUNCATE TABLE TGIP_301_NEW_INVALID_RECORDS";
		
		String queryA="";
		queryA+=	"	INSERT INTO TGIP_301_NEW_INVALID_RECORDS																	\n";
		queryA+=	"	SELECT																										\n";
		queryA+=	"		A.ipIntStart																							\n";
		queryA+=	"	FROM																										\n";
		queryA+=	"		tgip_401_historic_board A																				\n";
		queryA+=	"		LEFT JOIN tgip_400_last_board B																			\n";
		queryA+=	"		ON			A.ipIntStart	=	B.ipIntStart															\n";
		queryA+=	"			AND		A.ipIntEnd		=	B.ipIntEnd																\n";
		queryA+=	"			AND(	(A.ASNumber		=	B.ASNumber	)OR(A.ASNumber		IS NULL AND B.ASNumber		IS NULL))	\n";
		queryA+=	"			AND(	(A.locId		=	B.locId		)OR(A.locId			IS NULL AND B.locId			IS NULL))	\n";
		queryA+=	"			AND(	(A.countryISO	=	B.countryISO)OR(A.countryISO	IS NULL AND B.countryISO	IS NULL))	\n";
		queryA+=	"			AND(	(A.country		=	B.country	)OR(A.country		IS NULL AND B.country		IS NULL))	\n";
		queryA+=	"			AND(	(A.region		=	B.region	)OR(A.region		IS NULL AND B.region		IS NULL))	\n";
		queryA+=	"			AND(	(A.regionId		=	B.regionId	)OR(A.regionId		IS NULL AND B.regionId		IS NULL))	\n";
		queryA+=	"			AND(	(A.city			=	B.city		)OR(A.city			IS NULL AND B.city			IS NULL))	\n";
		queryA+=	"			AND(	(A.postalCode	=	B.postalCode)OR(A.postalCode	IS NULL AND B.postalCode	IS NULL))	\n";
		queryA+=	"			AND(	(A.latitude		=	B.latitude	)OR(A.latitude		IS NULL AND B.latitude		IS NULL))	\n";
		queryA+=	"			AND(	(A.longitude	=	B.longitude	)OR(A.longitude		IS NULL AND B.longitude		IS NULL))	\n";
		queryA+=	"			AND(	(A.metroCode	=	B.metroCode	)OR(A.metroCode		IS NULL AND B.metroCode		IS NULL))	\n";
		queryA+=	"			AND(	(A.areaCode		=	B.areaCode	)OR(A.areaCode		IS NULL AND B.areaCode		IS NULL))	\n";
		queryA+=	"	WHERE																										\n";
		queryA+=	"		B.ipIntStart IS NULL																					\n";
		
		String queryAA="";
		queryAA+=	"	UPDATE TGIP_401_HISTORIC_BOARD H,TGIP_301_NEW_INVALID_RECORDS I		\n";
		queryAA+=	"	SET H.data_valid_to='"+date_version_string+"'						\n";
		queryAA+=	"	WHERE	H.data_valid_to IS NULL										\n";
		queryAA+=	"			AND H.ipIntStart=I.ipIntStart								\n";
		
		//insert new records
		String queryB="";
		queryB+=	"	INSERT INTO tgip_401_historic_board																			\n";
		queryB+=	"	SELECT '"+date_version_string+"',null,A.*																	\n";
		queryB+=	"	FROM																										\n";
		queryB+=	"		tgip_400_last_board A																					\n";
		queryB+=	"		LEFT JOIN tgip_401_historic_board B																		\n";
		queryB+=	"		ON			A.ipIntStart	=	B.ipIntStart															\n";
		queryB+=	"			AND		A.ipIntEnd		=	B.ipIntEnd																\n";
		queryB+=	"			AND(	(A.ASNumber		=	B.ASNumber	)OR(A.ASNumber		IS NULL AND B.ASNumber		IS NULL))	\n";
		queryB+=	"			AND(	(A.locId		=	B.locId		)OR(A.locId			IS NULL AND B.locId			IS NULL))	\n";
		queryB+=	"			AND(	(A.countryISO	=	B.countryISO)OR(A.countryISO	IS NULL AND B.countryISO	IS NULL))	\n";
		queryA+=	"			AND(	(A.country		=	B.country	)OR(A.country		IS NULL AND B.country		IS NULL))	\n";
		queryB+=	"			AND(	(A.region		=	B.region	)OR(A.region		IS NULL AND B.region		IS NULL))	\n";
		queryB+=	"			AND(	(A.regionId		=	B.regionId	)OR(A.regionId		IS NULL AND B.regionId		IS NULL))	\n";
		queryB+=	"			AND(	(A.city			=	B.city		)OR(A.city			IS NULL AND B.city			IS NULL))	\n";
		queryB+=	"			AND(	(A.postalCode	=	B.postalCode)OR(A.postalCode	IS NULL AND B.postalCode	IS NULL))	\n";
		queryB+=	"			AND(	(A.latitude		=	B.latitude	)OR(A.latitude		IS NULL AND B.latitude		IS NULL))	\n";
		queryB+=	"			AND(	(A.longitude	=	B.longitude	)OR(A.longitude		IS NULL AND B.longitude		IS NULL))	\n";
		queryB+=	"			AND(	(A.metroCode	=	B.metroCode	)OR(A.metroCode		IS NULL AND B.metroCode		IS NULL))	\n";
		queryB+=	"			AND(	(A.areaCode		=	B.areaCode	)OR(A.areaCode		IS NULL AND B.areaCode		IS NULL))	\n";
		queryB+=	"	WHERE																										\n";
		queryB+=	"			B.ipIntStart IS NULL																				\n";
		
		try
		{
			String dateBegin=simpleDateFormat.format(new Date());
			System.out.println(simpleDateFormat.format(new Date())+" "+"STEP 6.1:\tMARK INVALID RECORDS...");
			statement.execute(query);
			statement.execute(queryA);
			statement.execute(queryAA);
			System.out.println(simpleDateFormat.format(new Date())+" "+"STEP 6.2:\tINSERT NEW RECORDS...");
			statement.execute(queryB);
			String dateEnd=simpleDateFormat.format(new Date());
			statement.execute("INSERT INTO TGIP_000_PROCESS_LOG VALUES ('"+date_version_string+"','pseudo-merge','merge into historic board',null,null,'"+dateBegin+"','"+dateEnd+"',TIMEDIFF('"+dateEnd+"','"+dateBegin+"'),null)");
		}
		catch (SQLException e) 
		{
			while(e!=null)
			{
				System.out.println("SQL Exception:");
				System.out.println("State   : "+e.getSQLState());
				System.out.println("Message : "+e.getMessage());
				System.out.println("Error   : "+e.getErrorCode());
				e=e.getNextException();
			}
			System.exit(2);
		}
	}
	//------------------------------------------------------------------------------------------------------------------------------------------
}
