package com.turner.lmdb.feed;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public abstract class LmdbFeed {
		private String lmdbUrl;
		protected Map<String, String> lmdbMap;
		protected String errors;

		public LmdbFeed(String url) {
			lmdbUrl = url;
			lmdbMap = new HashMap<String,String>();
			errors="";
		}
		
		public abstract Map<String, String> read();

		protected String readUrl() {
			BufferedReader reader = null;
			StringBuffer buffer=new StringBuffer();
			try {
				URL url = new URL(lmdbUrl);
				reader = new BufferedReader(new InputStreamReader(url.openStream()));
				buffer = new StringBuffer();
				int read;
				char[] chars = new char[1024];
				while ((read = reader.read(chars)) != -1)
					buffer.append(chars, 0, read); 

				return buffer.toString();
			} 
			catch (MalformedURLException e) {
				errors += "Url is malformed:  " + lmdbUrl;
			}
			catch (IOException e) {
				errors += "Error reading url:  " + lmdbUrl + "\n";
			}
			finally { 
				if (reader != null)
					try {
						reader.close();
					} catch (IOException e) {
						//ignoring IOException when closing the reader
						e.printStackTrace();
					} 
			}
			return "";
		}
}
