package com.foxconn.filter;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.foxconn.util.JSONReader;
import com.foxconn.wrapper.JSONResponseWrapper;

public class GroupFilter implements Filter {
	
	private static Logger logger = Logger.getLogger(GroupFilter.class);
	private static JSONArray grouplist = null;
	private static Object lock = new Object();
	private static long lastModified = 0L;
	private File groupFile = null;
	
	public void init(FilterConfig fConfig) throws ServletException {
		if (fConfig.getInitParameter("groupfile") == null) {
			logger.info("No group file path");
			return ;
		}
		groupFile = new File(fConfig.getServletContext().getRealPath("/WEB-INF"), fConfig.getInitParameter("groupfile"));
		loadGroupFile();
	}
	
	private void loadGroupFile() throws ServletException {
		if (groupFile != null && groupFile.exists()) {
			synchronized(lock) {
				try {
					if (lastModified == groupFile.lastModified()) {
						logger.debug("Group File is NOT changed");
						return ;
					} else {
						lastModified = groupFile.lastModified();
						grouplist = (JSONArray) JSONReader.readJSONinFile(groupFile);
						logger.info("Read Group File Successfully");
					}
				} catch (Exception e) {
					logger.error("Read Group File Error", e);
				}
			}
		} else {
			logger.warn("Group File is NOT exist");
			return ;
		}
	}
	
	public void destroy() {}

	@SuppressWarnings("unchecked")
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		JSONObject ret = new JSONObject();
		try {
			JSONResponseWrapper responsewrapper = new JSONResponseWrapper((HttpServletResponse) response);
			chain.doFilter(request, responsewrapper);
			JSONObject obj = (JSONObject) responsewrapper.toJSON();
			if ((Long) obj.get("code") != 200) {
				ret.putAll(obj);
			} else {
				JSONArray alllist = (JSONArray) obj.get("list");
				JSONArray jsonout = new JSONArray();
				
//				Map<String, JSONArray> groupmap = new HashMap<String, JSONArray>();
//				for (Object inner : alllist) {
//					JSONObject json = (JSONObject) inner;
//					String group = (String) json.get("group");
//					JSONArray list = groupmap.get(group);
//					if (list == null)
//						list = new JSONArray();
//					list.add(json);
//					groupmap.put(group, list);
//				}
//			
//				Set<Entry<String, JSONArray>> set = groupmap.entrySet();
//				Iterator<Entry<String, JSONArray>> iter = set.iterator();
//				while (iter.hasNext()) {
//					Entry<String, JSONArray> entry = iter.next();
//					JSONObject json = new JSONObject();
//					json.put("group", entry.getKey());
//					JSONArray list = entry.getValue();
//					if (list != null && list.size() != 0) {
//						json.put("list", list);
//						jsonout.add(json);
//					}
//				}			
			
				loadGroupFile();
				if (grouplist != null) {
					HashMap<String, JSONObject> map = new HashMap<String, JSONObject>();
					for (Object inner : alllist) {
						JSONObject jsonobj = (JSONObject) inner;
						String address = (String) jsonobj.get("address");
						if (address != null)
							map.put(address, jsonobj);
					}
				
					for (Object inner : grouplist) {
						JSONObject oldobj = (JSONObject) inner;
						JSONObject newobj = new JSONObject();
										
						JSONArray oldlist = (JSONArray) oldobj.get("list");
						JSONArray newlist = new JSONArray();
						for (Object inner2 : oldlist) {
							String address = (String) inner2;
							if (map.get(address) != null)
								newlist.add(map.get(address));
						}
					
						if (newlist.size() != 0) {
							newobj.put("group", oldobj.get("group"));
							newobj.put("list", newlist);
							jsonout.add(newobj);
						}
					}
				} else {
					logger.warn("Group File is NOT exist");
				}
				ret.put("list", jsonout);
				ret.put("code", obj.get("code"));
			}
		} catch (Exception e) {
			ret.put("code", 500);
			ret.put("msg", "Server error");
			logger.error("GroupFilter Error", e);
		}
		response.getWriter().println(ret);
		response.getWriter().close();
	}
}
