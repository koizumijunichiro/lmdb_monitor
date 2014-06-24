package com.turner.lmdb.feed.json;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.turner.lmdb.feed.LmdbFeed;

public class LmdbJson extends LmdbFeed {

	public LmdbJson(String url) {
		super(url);
	}
	public Map<String, String> read()  {
		Gson gson = new Gson();
		JsonMessage jsonMessage = null;
		JsonMessageAlt jsonMessageAlt = null;
		String json = readUrl();
		if ( errors.length()>0) lmdbMap.put("errors", errors);

		if (json != null && json.length()>0) {
			lmdbMap.put("lmdbFile", json);
			try {
				jsonMessage = gson.fromJson( json, JsonMessage.class );
				processJsonMessage(jsonMessage);
			}
			catch (JsonSyntaxException jse){
				jsonMessageAlt = gson.fromJson(json, JsonMessageAlt.class );
				processJsonMessageAlt(jsonMessageAlt);
			}
			catch(Exception e) {
				errors += "Error parsing LMDB Json data:  \n";
				errors += e.toString() + "\n";
				lmdbMap.put("errors", errors);
				return lmdbMap;
			}
		}
		return lmdbMap;
	}

	private void processJsonMessage(JsonMessage jsonMessage){
		String schedItemData="";
		if (jsonMessage.LiveFeed != null) lmdbMap.put("LiveFeed", "LiveFeed");
		if (jsonMessage.LiveFeed.tveLiveSched != null) {
			lmdbMap.put("tveLiveSched","tveLiveSched");
			if (jsonMessage.LiveFeed.tveLiveSched.TimeStamp != null) lmdbMap.put("TimeStamp", jsonMessage.LiveFeed.tveLiveSched.TimeStamp);
			if (jsonMessage.LiveFeed.tveLiveSched.End != null) lmdbMap.put("End", jsonMessage.LiveFeed.tveLiveSched.End);
			if (jsonMessage.LiveFeed.tveLiveSched.Start != null) lmdbMap.put("Start", jsonMessage.LiveFeed.tveLiveSched.Start); 
			if (jsonMessage.LiveFeed.tveLiveSched.SchedItem != null) {
				lmdbMap.put("SchedItem", String.valueOf(jsonMessage.LiveFeed.tveLiveSched.SchedItem.size()));
				for (SchedItem item : jsonMessage.LiveFeed.tveLiveSched.SchedItem) {
					schedItemData += item.toString() + "\n"; 
				}
				lmdbMap.put("SchedItemData", schedItemData);
			}
		}
	}

	private void processJsonMessageAlt(JsonMessageAlt jsonMessageAlt){
		if (jsonMessageAlt.LiveFeed != null) lmdbMap.put("LiveFeed", "LiveFeed");
		if (jsonMessageAlt.LiveFeed.tveLiveSched != null) {
			lmdbMap.put("tveLiveSched","tveLiveSched");
			if (jsonMessageAlt.LiveFeed.tveLiveSched.TimeStamp != null) lmdbMap.put("TimeStamp", jsonMessageAlt.LiveFeed.tveLiveSched.TimeStamp);
			if (jsonMessageAlt.LiveFeed.tveLiveSched.End != null) lmdbMap.put("End", jsonMessageAlt.LiveFeed.tveLiveSched.End);
			if (jsonMessageAlt.LiveFeed.tveLiveSched.Start != null) lmdbMap.put("Start", jsonMessageAlt.LiveFeed.tveLiveSched.Start); 
			if (jsonMessageAlt.LiveFeed.tveLiveSched.SchedItem != null) {
				lmdbMap.put("SchedItem", "1");
				lmdbMap.put("SchedItemData", jsonMessageAlt.LiveFeed.tveLiveSched.SchedItem.toString());
			}
		}
	}
	
	private static class LiveFeed {
		TveLiveSched tveLiveSched;
		String Version;
	}

	private static class TveLiveSched {
		String TimeStamp;
		String End;
		String Start;
		String xmlnsXsi;
		String Provider;
		List<SchedItem> SchedItem;
	}

	private static class SchedItem {
		String AiringID;
		String Rights;
		String Breaks;
		Map<String,Object> Program;
		public String toString() {
			return "AiringID:" + AiringID + " | Name:" + Program.get("Name") + " | FranchiseName:" + Program.get("FranchiseName") + " | StartTime:" + Program.get("StartTime") + " | EndTime:" + Program.get("EndTime");
		}
	}

	private static class JsonMessage {
		LiveFeed	LiveFeed;
	}
	
	//
	private static class LiveFeedAlt {
		TveLiveSchedAlt tveLiveSched;
		String Version;
	}

	 private static class TveLiveSchedAlt {
		String TimeStamp;
		String End;
		String Start;
		String xmlnsXsi;
		String Provider;
		SchedItem SchedItem;
	}
	
	private static class JsonMessageAlt {
		LiveFeedAlt	LiveFeed;
	}
}
//Test data for testing json parsing:
//String json = "{\"LiveFeed\": {		    \"Version\": \"1.0\"		  }		}";
//String json = "{ \"LiveFeed\": { \"TimeStamp\": \"2013-07-01T15:03:00.8471857-04:00\", \"SchedItem\": [ { \"AiringID\": \"472545365\", \"Rights\": \"\", \"Breaks\": \"\", \"Program\": { \"SeriesID\": \"896456\", \"AwayTeamDivisionName\": \"\", \"TitleID\": \"896486\", \"SeasonNumber\": \"2\", \"AwayTeamLeague\": \"\", \"HomeTeamLeague\": \"\", \"SportsName\": \"\", \"Storyline\": \"\", \"TVRatingDescriptors\": { \"Descriptor\": [ \"D\", \"L\" ] }, \"FranchiseName\": \"Cougar Town [SAP]\", \"Participants\": { \"Participant\": [ { \"Name\": \"Courteney Cox\", \"RoleType\": \"Actor\" }, { \"Name\": \"Josh Hopkins\", \"RoleType\": \"Actor\" } ] }, \"AwayTeamName\": \"\", \"HomeTeamName\": \"\", \"Name\": \"You Don't Know How It Feels\", \"FranchiseID\": \"386736\", \"ProgramType\": \"E\", \"TVRating\": \"TV-PG\", \"ProductionNumber\": \"\", \"SeriesName\": \"Cougar Town\", \"Duration\": \"1797\", \"HomeTeamDivisionName\": \"\", \"EndTime\": \"2013-07-01T15:29:57.0000000-04:00\", \"StartTime\": \"2013-07-01T15:00:00.0000000-04:00\" } } ], \"End\": \"2013-07-01T18:59:51.0000000-04:00\", \"Start\": \"2013-07-01T15:00:00.0000000-04:00\", \"xmlns:xsi\": \"http://www.w3.org/2001/XMLSchema-instance\", \"Provider\": \"TBSE\" \"Version\": \"1.0\" } }";
//String json = "{ \"LiveFeed\": { \"tveLiveSched\": { \"TimeStamp\": \"2013-07-01T15:03:00.8471857-04:00\", \"SchedItem\": [ ], \"End\": \"2013-07-01T18:59:51.0000000-04:00\", \"Start\": \"2013-07-01T15:00:00.0000000-04:00\", \"xmlns:xsi\": \"http://www.w3.org/2001/XMLSchema-instance\", \"Provider\": \"TBSE\" }, \"Version\": \"1.0\" } }";
//String json = "{ \"LiveFeed\": { \"tveLiveSched\": { \"TimeStamp\": \"2013-08-04T20:57:02.6662000-04:00\", \"SchedItem\": { \"AiringID\": \"472758681\", \"Rights\": \"\", \"Breaks\": \"\", \"Program\": { \"SeriesID\": \"\", \"AwayTeamDivisionName\": \"\", \"TitleID\": \"452051\", \"SeasonNumber\": \"\", \"AwayTeamLeague\": \"\", \"HomeTeamLeague\": \"\", \"SportsName\": \"\", \"Storyline\": \"It is an ordinary summer day. But then, without warning, something very extraordinary happens. Enormous shadows fall across the land. Strange atmospheric phenomena, ominous and mesmerizing, surface around the globe. All eyes turn upward. The question of whether we\u0027re alone in the universe has finally been answered. And, in a matter of minutes, the lives of every person across the globe are forever changed. With the fate of our planet at stake, the Fourth of July is about to take on an entirely new meaning. No longer will it be an American holiday. It will be known as the day the entire world fought back. The day we did not go gentle into the good night... The day all of us on planet Earth celebrated our independence day.\", \"TVRatingDescriptors\": \"\", \"FranchiseName\": \"Movie (DVS)\", \"Participants\": { \"Participant\": [ { \"Name\": \"Will Smith\", \"RoleType\": \"Actor\" }, { \"Name\": \"Roland Emmerich\", \"RoleType\": \"Director\" }, { \"Name\": \"Bill Pullman\", \"RoleType\": \"Actor\" }, { \"Name\": \"Roland Emmerich\", \"RoleType\": \"Screenplay\" }, { \"Name\": \"Jeff Goldblum\", \"RoleType\": \"Actor\" }, { \"Name\": \"Mary McDonnell\", \"RoleType\": \"Actor\" }, { \"Name\": \"Vivica A. Fox\", \"RoleType\": \"Actor\" }, { \"Name\": \"Randy Quaid\", \"RoleType\": \"Actor\" }, { \"Name\": \"Judd Hirsch\", \"RoleType\": \"Actor\" }, { \"Name\": \"Robert Loggia\", \"RoleType\": \"Actor\" }, { \"Name\": \"Margaret Colin\", \"RoleType\": \"Actor\" }, { \"Name\": \"Adam Baldwin\", \"RoleType\": \"Actor\" } ] }, \"AwayTeamName\": \"\", \"HomeTeamName\": \"\", \"Name\": \"Independence Day\", \"FranchiseID\": \"386335\", \"ProgramType\": \"FF\", \"TVRating\": \"TV-PG\", \"ProductionNumber\": \"\", \"SeriesName\": \"\", \"Duration\": \"12584\", \"HomeTeamDivisionName\": \"\", \"EndTime\": \"2013-08-05T00:59:29.0000000-04:00\", \"StartTime\": \"2013-08-04T21:29:45.0000000-04:00\" } }, \"End\": \"2013-08-05T00:59:29.0000000-04:00\", \"Start\": \"2013-08-04T21:29:45.0000000-04:00\", \"xmlns:xsi\": \"http://www.w3.org/2001/XMLSchema-instance\", \"Provider\": \"TNTW\" }, \"Version\": \"1.0\" } }";