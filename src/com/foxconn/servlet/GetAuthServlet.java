package com.foxconn.servlet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.foxconn.util.JSONReader;

public class GetAuthServlet extends HttpServlet {
	
	private final static String AUTHPARAMETER = "auth";
	
	private static Logger logger = Logger.getLogger(GetAuthServlet.class);
	private static Map<String, String> map = new HashMap<String, String>();
	private static Map<String, JSONArray> roles = new HashMap<String, JSONArray>();
	private static long lastModified = 0L;
	
	private void loadAuthFile() throws ServletException {
		logger.debug("loadAuthFile");
		if (this.getInitParameter(AUTHPARAMETER) == null) {
			logger.error("No '" + AUTHPARAMETER + "' parameter");
			throw new ServletException("AuthServlet setting error");
		} else {
			File file = new File(this.getServletContext().getRealPath("/WEB-INF"), this.getInitParameter(AUTHPARAMETER));
			try {
				if (lastModified == file.lastModified()) {
					logger.debug("AuthFile is NOT changed");
				} else {
					lastModified = file.lastModified();
					JSONArray auth = (JSONArray) JSONReader.readJSONinFile(file);
					map.clear();
					roles.clear();
					for (Object obj : auth) {
						JSONObject json = (JSONObject) obj;
						String user = (String) json.get("user");
						String passwd = (String) json.get("passwd");
						JSONArray roles = (JSONArray) json.get("roles");
						GetAuthServlet.roles.put(user, roles);
						GetAuthServlet.map.put(user, passwd);
					}
					logger.info("Load AuthFile Successfully");
				}
			} catch (Exception e) {
				logger.error("Load AuthFile Error", e);
				throw new ServletException("AuthServlet setting error");
			}
		}
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doService(request, response);
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doService(request, response);
	}
	
	/**
	 * Parameter:
	 * login:
	 * password:
	 * 
	 * Return:
	 * code: 200 or 401
	 * msg:
	 * login:
	 * 
	 * Example:
	 * {"code":200, "login":"admin"}
	 * {"code":401}
	 */
	@SuppressWarnings("unchecked")
	private void doService(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		HttpSession session = request.getSession(false);
		JSONObject ret = new JSONObject();
		String user = request.getParameter("user");
		String password = request.getParameter("password");
		
		if (request.getParameter("logout") != null) {
			if (session != null)
				session.invalidate();
			session = null;
			response.setStatus(HttpServletResponse.SC_OK);
			ret.put("code", HttpServletResponse.SC_OK);
			ret.put("msg", "logout");
			logger.debug("logout");
		} else if (session == null) {
			loadAuthFile();
			if (user == null) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				ret.put("code", HttpServletResponse.SC_BAD_REQUEST);
				ret.put("msg", "user is NULL");
				logger.debug("user is NULL");
			} else if (map.get(user) == null) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				ret.put("code", HttpServletResponse.SC_UNAUTHORIZED);
				ret.put("msg", "No auth");
				logger.debug("No auth");
			} else if(password == null) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				ret.put("code", HttpServletResponse.SC_BAD_REQUEST);
				ret.put("msg", "password is NULL");
				logger.debug("password is NULL");
			} else if (password.equals(map.get(user))) {
				session = request.getSession(true);
				session.setAttribute("user", user);
				session.setAttribute("roles", GetAuthServlet.roles.get(user));
				response.setStatus(HttpServletResponse.SC_OK);
				ret.put("code", HttpServletResponse.SC_OK);
				ret.put("user", user);
				logger.debug("'" + user + "' is logging in");
			} else {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				ret.put("code", HttpServletResponse.SC_UNAUTHORIZED);
				ret.put("msg", "No auth");
				logger.debug("No auth");
			}
		} else if (session.getAttribute("user") != null){
			response.setStatus(HttpServletResponse.SC_OK);
			ret.put("code", HttpServletResponse.SC_OK);
			ret.put("user", session.getAttribute("user"));
			logger.debug("'" + session.getAttribute("user") + "' is logging in");
		} else {
			session.invalidate();
			session = null;
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			ret.put("code", HttpServletResponse.SC_UNAUTHORIZED);
			ret.put("msg", "No auth");
		}
		
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.println(ret);
		out.close();
	}
	
}
