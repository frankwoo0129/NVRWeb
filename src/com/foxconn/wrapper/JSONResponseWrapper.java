package com.foxconn.wrapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class JSONResponseWrapper extends HttpServletResponseWrapper {

	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		String name = "B04-3F.*AA";
		System.out.println("B04-3FAAAAAA".matches(name));

		File src = new File(
				"C:/Users/FrankWOO/git/NVRCenter/NVRCenter/WebContent/WEB-INF",
				"ipcams_admin.json");
		
		if (src.exists()) {
			BufferedReader br = null;
			try {
				br = new BufferedReader(new FileReader(src));
				String s = null;
				StringBuffer sb = new StringBuffer();
				while ((s = br.readLine()) != null) {
					sb.append(s);
				}
				JSONArray list = (JSONArray) new JSONParser().parse(sb.toString());
				for (Object obj : list) {
					JSONObject json = (JSONObject) obj;
					String title = (String) json.get("title");
					json.put("group", title.substring(0, 6));
				}
				
				PrintWriter out = new PrintWriter(new FileWriter(src));
				out.println(list);
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

	}

	private StringWriter writer = new StringWriter();

	public JSONResponseWrapper(HttpServletResponse response) {
		super(response);
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		return new PrintWriter(writer);
	}

	public Object toJSON() throws ParseException {
		return new JSONParser().parse(this.writer.toString());
	}

	@Override
	public String toString() {
		return this.writer.toString();
	}

}
