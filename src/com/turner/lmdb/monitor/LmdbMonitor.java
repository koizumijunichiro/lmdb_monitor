package com.turner.lmdb.monitor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.turner.lmdb.feed.LmdbFeed;
import com.turner.lmdb.feed.json.LmdbJson;
import com.turner.lmdb.feed.xml.LmdbXml;

public class LmdbMonitor {
	private int jsonCount;
	private Map<String, String> jsonUrls;
	private int xmlCount;
	private Map<String, String> xmlUrls;
	private Map<String, String> lmdbData;
	private Date now;
	private String feedDir;
	private String logDir;
	private static String to;
	private static String cc;
	private static String from;
	private static String host;
	private static String port;
	private static String subject;
	private static String msgHead;
	private static String msgFoot;

	LmdbMonitor(String properties) throws IOException {
		Properties props=new Properties();
		jsonCount=0;
		jsonUrls = new TreeMap<String,String>();
		xmlCount=0;
		xmlUrls = new TreeMap<String,String>();
		this.now = new Date();
		props.load(new FileInputStream(properties));

		//json urls
		if (props.containsKey("jsonCount"))
			jsonCount=Integer.parseInt(props.getProperty("jsonCount"));		
		for (int i=0; i<jsonCount; i++)
			if (props.containsKey(String.valueOf("jsonUrl"+i))) jsonUrls.put("jsonUrl"+i, props.getProperty("jsonUrl"+i));

		//xml urls
		if (props.containsKey("xmlCount"))
			xmlCount=Integer.parseInt(props.getProperty("xmlCount"));
		for (int i=0; i<xmlCount; i++)
			if (props.containsKey("xmlUrl"+i)) xmlUrls.put("xmlUrl"+i, props.getProperty("xmlUrl"+i));
		
		//email params
		to=props.getProperty("to");
		from=props.getProperty("from");
		host=props.getProperty("host");
		port=props.getProperty("port");
		subject=props.getProperty("subject");
		msgHead=props.getProperty("msgHead");
		msgFoot=props.getProperty("msgFoot");
		
		//I/O directories
		feedDir = props.getProperty("feedDir", "~/");
		logDir = props.getProperty("logDir", "~/");
	}

	String testFeed() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSZ");
		String errors="";
		//LiveFeed 
		if (!lmdbData.containsKey("LiveFeed"))
			return "Error - LiveFeed element is null\n";
		//LiveFeed.tveLiveSched
		if (!lmdbData.containsKey("tveLiveSched"))
			return "Error - tveLiveSched element is null\n";

		//LiveFeed.tveLiveSched.TimeStamp
		try {
			if (!lmdbData.containsKey("TimeStamp")) {
				errors+="TimeStamp is null.\n";
			}
			else if (!(isValidTimestamp(lmdbData.get("TimeStamp").replaceAll(":00$", "00")))) {
				errors += "Error - TimeStamp " + lmdbData.get("TimeStamp").replaceAll(":00$", "00") + 
						" is not within 10 minutes of current time of " + sdf.format(now) + ".\n";
			}
		} catch (ParseException pe){
			pe.printStackTrace();
			errors += "Error - Timestamp " + lmdbData.get("TimeStamp").replaceAll(":00$", "00") + " cannot be parsed.\n";
		}

		//LiveFeed.tveLiveSched.End
		try {
			if (!lmdbData.containsKey("End")) {
				errors += "Error - End time is null.\n";
			}
			else if (!isValidEndTime(lmdbData.get("End").replaceAll(":00$", "00"))) {
				errors += "Error - End time " + lmdbData.get("End").replaceAll(":00$", "00") + " is inconsistent with a current time of " 
						+ sdf.format(now) + ".  End time should be at least 4 hours in the future.\n";
			}
		} catch (ParseException pe) {
			pe.printStackTrace();
			errors += "Error - End date " + lmdbData.get("End").replaceAll(":00$", "00") + " cannot be parsed.\n";
		}

		//LiveFeed.tveLiveSched.Start
//		try {
//			if (!lmdbData.containsKey("Start")) {
//				errors += "Error - Start time is null.\n";
//			}
//			else if (!isValidStartTime(lmdbData.get("Start").replaceAll(":00$", "00"))) {
//				errors += "Error - Start time " + lmdbData.get("Start").replaceAll(":00$", "00") + " is inconsistent with a current time of " 
//						+ sdf.format(now) + ".  Start time should be at most 130 minutes behind current time.\n";
//			}
//		} catch (ParseException pe) {
//			pe.printStackTrace();
//			errors += "Error - Start date " + lmdbData.get("Start").replaceAll(":00$", "00") + " cannot be parsed.\n";
//		}

		//LiveFeed.tveLiveSched.SchedItem[]
		if (!lmdbData.containsKey("SchedItem")) {
			errors += "Error - SchedItem is null.\n";
		}
		else if (Integer.parseInt(lmdbData.get("SchedItem"))==1) {
			errors += "Error - There is only 1 SchedItem airing entry in the current feed.\n";
			if (lmdbData.containsKey("SchedItemData"))
				errors += "\t"+lmdbData.get("SchedItemData");
		}
		else if (Integer.parseInt(lmdbData.get("SchedItem"))==0) { 
			errors += "Error - There are no SchedItem entries in the current feed.\n";
		}
		return errors;
	}
	/*
	 * A valid end time should be at least 3hrs 50min in the future
	 */
	private Boolean isValidEndTime(String end) throws ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSZ");
		Date endDate = sdf.parse( end );
		Date nowPlusFourHours = new Date(now.getTime() + 1000*60*60*4 - 1000*60*10);
		//Date nowPlusThreeHours = new Date(now.getTime() + 1000*60*60*3 - 1000*60*10);  
		return (endDate.compareTo(nowPlusFourHours) >= 0);
	}

	/*
	 * A valid start time should fall within 90 minutes of now ( start >= (now - 90min) )
	 */
	private Boolean isValidStartTime(String start) throws ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSZ");
		Date startDate = sdf.parse( start );
		Date nowMinusNinety = new Date(now.getTime() - (1000*60*130));
		return ( startDate.compareTo(nowMinusNinety) >= 0 );
	}

	/*
	 * A valid time stamp should fall in the range (now - 10min) and (now + 10min)
	 */
	private Boolean isValidTimestamp(String stamp) throws ParseException {
		//We cannot parse dates w/ non-zero, fractional seconds such as 2013-07-11T12:51:00.5854441-0400 so I'm eliminating from the timestamp.
		stamp = stamp.replaceAll(".\\d{7}", "");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		Date timestamp = sdf.parse( stamp );
		Date nowMinusTenMinutes = new Date(now.getTime() - (1000*60*10));
		Date nowPlusTenMinutes = new Date(now.getTime() + (1000*60*10));
		return ( (timestamp.compareTo(nowMinusTenMinutes) >=0) && (timestamp.compareTo(nowPlusTenMinutes) <=0) );
	}
	
	private void saveFile(String message, String name){
		SimpleDateFormat sdfFeeds = new SimpleDateFormat("yyyyMMddHHmm");
		SimpleDateFormat sdfLogs = new SimpleDateFormat("yyyyMMdd");
		String fileName = (!name.equals("lmdb.log")?feedDir + sdfFeeds.format(now) + "-" + name:logDir + sdfLogs.format(now) + "-" + name);
		try {
			File file = new File(fileName);
			if (!file.exists()) file.createNewFile();
			BufferedWriter out = new BufferedWriter(new FileWriter(file.getAbsoluteFile(), true));
			out.write(message);
			out.close();
		} catch (IOException e) {
			System.out.println("Error writing output file:  " + fileName);
			System.out.println( e.getMessage());
		}
	}

	static String sendNotification(String to, String cc, String from, String host, String port, String subject, String msgHead, String errors, String msgFoot) { 
		if ( (to == null || to.isEmpty()) && (to == null || to.isEmpty()) ) return "Error email not sent.  No recipients specified.\n";
		Properties properties = new Properties();
		properties.setProperty("mail.smtp.host", host);
		properties.setProperty("port", port);
		Session session = Session.getDefaultInstance(properties);
		try{
			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(from));
			if ( !(cc == null || cc.isEmpty()) )
				message.addRecipient(Message.RecipientType.CC, new InternetAddress(cc));
			if ( !(to == null && to.isEmpty()) ) 
				message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
			message.setSubject("lmdb monitor");
			message.setText(msgHead + "\n" + errors);
			Transport.send(message);
		}catch (MessagingException mex) {
			String mesg = "MessagingException encountered:  error email message not sent.\n"; 
			mesg += "MessagingException.getMessage():  " + mex.getMessage() +"\n";
			mesg += "Email parameters - to:" + to + "|cc:" + cc + "|from:" + from + "|host:" + host + "|port:" + port + "\n";
			return mesg;
		}     catch (Throwable t) {
		      t.printStackTrace();
		      return "Exception thrown when sending mail:  error email message not sent.\n\n" + t.getMessage() + "\n";
	    }
		return "Error email message sent successfully\n\n";
	}

	public String toString() {
		return	"tbsEJson:" + jsonUrls.get("TbsEast.json") + " | \n" +
				"tbsWJson:" + jsonUrls.get("TbsWest.json") + " | \n" +
				"tntEJson:" + jsonUrls.get("TntEast.json") + " | \n" +
				"tntWJson:" + jsonUrls.get("TntWest.json") + " | \n" +
				"tbsEXml:"  + xmlUrls.get("TbsEast.xml") + " | \n" +
				"tbsWXml:"  + xmlUrls.get("TbsWest.xml") + " | \n" +
				"tntEXml:"  + xmlUrls.get("TntEast.xml") + " | \n" +
				"tntWXml:"  + xmlUrls.get("TntWest.xml") + " | \n" +
				"logDir:"   + logDir   + "\n";
	}

	void initializeNow() {
		this.now = new Date();
	}

	public static void main (String[] args) {
		String logMessage="------------------------------------------\n" + new Date().toString() + "\n\n";
		String emailMessage="";
		String errors = "";
		LmdbMonitor monitor = null;
		try {
			monitor = new LmdbMonitor(args[0]);
		} catch (IOException e) {
			sendNotification(to, cc, from, host, port, subject, msgHead, "Error reading properties file " + args[0] + "\n", msgFoot);
			e.printStackTrace();
		}
		for (Entry<String, String> jsonUrl : monitor.jsonUrls.entrySet())
		{
			logMessage += "=====\nReading " + jsonUrl.getValue() + "\n\n";
			LmdbJson lmdbJson = new LmdbJson( jsonUrl.getValue() ); 
			monitor.lmdbData = lmdbJson.read();
			if (monitor.lmdbData.containsKey("errors")) 
				errors += monitor.lmdbData.get("errors") + "\n";
			if (monitor.lmdbData.containsKey("lmdbFile")) {
				logMessage += "TimeStamp:  " + (monitor.lmdbData.containsKey("TimeStamp")?monitor.lmdbData.get("TimeStamp"):"null")+"\n";
				logMessage += "End:  " + (monitor.lmdbData.containsKey("End")?monitor.lmdbData.get("End"):"null")+"\n";
				logMessage += "Start:  " + (monitor.lmdbData.containsKey("Start")?monitor.lmdbData.get("Start"):"null")+"\n";
				logMessage += "SchedItem.count():  " + (monitor.lmdbData.containsKey("SchedItem")?monitor.lmdbData.get("SchedItem"):"null")+"\n\n";
				monitor.saveFile(monitor.lmdbData.get("lmdbFile"), jsonUrl.getValue().substring(jsonUrl.getValue().lastIndexOf('/')+1));
			}
			if (monitor.lmdbData.containsKey("lmdbFile"))
				errors += monitor.testFeed();
			if (errors.length()!=0) {
				logMessage += errors;
				emailMessage += jsonUrl.getValue() + "\n" + errors + "\n";
			}
			monitor.saveFile(logMessage, "lmdb.log");
			logMessage = "";
			errors = "";
		}
		
		for (Entry<String, String> xmlUrl : monitor.xmlUrls.entrySet())
		{ 
			logMessage += "=====\nReading " + xmlUrl.getValue() + "\n\n";
			LmdbFeed lmdbXml = new LmdbXml( xmlUrl.getValue() ); 
			monitor.lmdbData = lmdbXml.read();
			if (monitor.lmdbData.containsKey("errors"))
				errors += monitor.lmdbData.get("errors") + "\n";
			if (monitor.lmdbData.containsKey("lmdbFile")) {
				logMessage += "TimeStamp:  " + (monitor.lmdbData.containsKey("TimeStamp")?monitor.lmdbData.get("TimeStamp"):"null")+"\n";
				logMessage += "End:  " + (monitor.lmdbData.containsKey("End")?monitor.lmdbData.get("End"):"null")+"\n";
				logMessage += "Start:  " + (monitor.lmdbData.containsKey("Start")?monitor.lmdbData.get("Start"):"null")+"\n";
				logMessage += "SchedItem.count():  " + (monitor.lmdbData.containsKey("SchedItem")?monitor.lmdbData.get("SchedItem"):"null")+"\n\n";
				monitor.saveFile(monitor.lmdbData.get("lmdbFile"), xmlUrl.getValue().substring(xmlUrl.getValue().lastIndexOf('/')+1));
			}
			if (monitor.lmdbData.containsKey("lmdbFile"))
				errors += monitor.testFeed();
			if (errors.length()!=0) {
				logMessage += errors;
				emailMessage += xmlUrl.getValue() + "\n" + errors + "\n";
			}
			monitor.saveFile(logMessage, "lmdb.log");
			logMessage = "";
			errors = "";
		}
		if (emailMessage.length()!=0) {
			logMessage = sendNotification(to, cc, from, host, port, subject, msgHead, emailMessage, msgFoot);
			if (logMessage.length()!=0) 
				monitor.saveFile(logMessage, "lmdb.log");
		}
	}
}
