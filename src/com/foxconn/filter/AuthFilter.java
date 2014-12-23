package com.foxconn.filter;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import com.foxconn.wrapper.JSONResponseWrapper;

public class AuthFilter implements Filter {
	
	private static Logger logger = Logger.getLogger(AuthFilter.class);
	private static String AUTHPATH = "/Servlet/GetAuthServlet";
	private static String CONFIGPATH = "/Servlet/GetConfigServlet";
	
	public void init(FilterConfig fConfig) throws ServletException {}
	
	public void destroy() {}

	@SuppressWarnings("unchecked")
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse resp = (HttpServletResponse) response;
		HttpSession session = req.getSession(false);
		JSONObject ret = new JSONObject();
		
		if (req.getServletPath().equals(AUTHPATH)) {
			chain.doFilter(request, response);
			return ;
		} else if (session == null) {
			try {
				JSONResponseWrapper responseWrapper = new JSONResponseWrapper(resp);
				request.getRequestDispatcher(AUTHPATH).forward(request, responseWrapper);
				JSONObject obj = (JSONObject) responseWrapper.toJSON();
				if ((Long)obj.get("code") != 200) {
					ret.put("code", 401);
					ret.put("msg", obj.get("msg"));
				} else {
					chain.doFilter(request, response);
					return ;
				}
			} catch (ParseException e) {
				ret.put("code", 500);
				ret.put("msg", "Server error");
				logger.error("Servlet Return is NOT JSON", e);
			} catch (Exception e) {
				ret.put("code", 500);
				ret.put("msg", "Server error");
				logger.error("AuthFilter Error", e);
			}
			response.getWriter().println(ret);
			response.getWriter().close();
			return ;
		} else if (req.getServletPath().equals(CONFIGPATH)){
			JSONResponseWrapper responseWrapper = new JSONResponseWrapper(resp);
			chain.doFilter(request, responseWrapper);
			try {
				JSONObject json = (JSONObject) responseWrapper.toJSON();
				JSONArray roles = (JSONArray) session.getAttribute("roles");
				if (roles == null) {
					ret.put("code", 401);
					ret.put("msg", "No roles");
				} else if ((Long) json.get("code") != 200) {
					ret.putAll(json);
				} else {
					Set<String> set = new HashSet<String>();
					for (Object obj : roles) {
						String role = (String) obj;
						set.add(role);
					}
					if (!set.contains("all") && !set.contains("All")) {
						JSONArray list = (JSONArray) json.get("list");
						JSONArray newlist = new JSONArray();
						for (Object obj : list) {
							JSONObject inner = (JSONObject) obj;
							if (set.contains(inner.get("group"))) {
								newlist.add(obj);
							}
						}
						json.put("list", newlist);
					}
					ret.putAll(json);
				}
			} catch (ParseException e) {
				ret.put("code", 500);
				ret.put("msg", "Server Error");
				logger.error("Servlet Return is NOT JSON", e);
			} catch (Exception e) {
				ret.put("code", 500);
				ret.put("msg", "Server Error");
				logger.error("AuthFilter Error", e);
			}
			response.getWriter().println(ret);
			response.getWriter().close();
			return ;
		} else {
			chain.doFilter(request, response);
			return ;
		}
	}

}
