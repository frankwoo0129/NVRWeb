package com.foxconn.filter;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.foxconn.util.AESCipher;
import com.foxconn.wrapper.JSONResponseWrapper;

public class ConfigCipherFilter implements Filter {
	
	private static Logger logger = Logger.getLogger(ConfigCipherFilter.class);
	
	public void init(FilterConfig fConfig) throws ServletException {}

	public void destroy() {}
	
	@SuppressWarnings("unchecked")
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse resp = (HttpServletResponse) response;
		HttpSession session = req.getSession(false);
		JSONObject ret = new JSONObject();
		
		try {
			if (session == null) {
				throw new Exception();
			} else if (!accept((JSONArray) session.getAttribute("roles"))) {
				throw new Exception();
			}
		} catch (Exception e) {
			ret.put("code", HttpServletResponse.SC_UNAUTHORIZED);
			ret.put("msg", "No auth");
			response.setContentType("application/json");
			PrintWriter out = response.getWriter();
			out.println(ret);
			out.close();
			return ;
		}
		
		String id = session.getId();
		if (req.getServletPath().equals("/Servlet/GetConfigServlet")) {
			JSONResponseWrapper responseWrapper = new JSONResponseWrapper(resp);
			chain.doFilter(request, responseWrapper);
			try {
				JSONObject obj = (JSONObject) responseWrapper.toJSON();
				if ((Long) obj.get("code") != 200) {
					logger.debug("Servlet Return Code is " + obj.get("code"));
				} else if (obj.get("list") == null) {
					logger.debug("Servlet Return No list");
				} else {
					JSONArray list = (JSONArray) obj.get("list");
					for (Object inner : list) {
						addNumber(inner, id);
					}
				}
				ret.putAll(obj);
			} catch (Exception e) {
				logger.error("ConfigCipherFilter Error", e);
				ret.put("code", 500);
				ret.put("msg", "Server error");
			}
			response.setContentType("application/json");
			PrintWriter out = response.getWriter();
			out.println(ret);
			out.close();
			return ;
		} else if (request.getParameter("number") != null) {
			try {
				String cipher = request.getParameter("number");
				ConfigRequestWrapper requestWrapper = new ConfigRequestWrapper(req, id, cipher);
				chain.doFilter(requestWrapper, response);
			} catch (Exception e) {
				logger.debug("Parameter 'number' is error", e);
				ret.put("code", 400);
				ret.put("msg", "Parameter 'number' is error");
				response.setContentType("application/json");
				PrintWriter out = response.getWriter();
				out.println(ret);
				out.close();
				return ;
			}
		} else {
			logger.debug("No Parameter 'number'");
			ret.put("code", 400);
			ret.put("msg", "No Parameter 'number'");
			response.setContentType("application/json");
			PrintWriter out = response.getWriter();
			out.println(ret);
			out.close();
			return ;
		}
	}
	
	public boolean accept(JSONArray roles) {
		// TODO
		return true;
	}
	
	@SuppressWarnings("unchecked")
	private void addNumber(Object obj, String id) throws Exception {
		if (obj instanceof JSONObject) {
			JSONObject json = (JSONObject) obj;
			if ((json.get("address") != null) && (json.get("title") != null)) {
				json.put("number", ConfigRequestWrapper.getCipher((String) json.get("address"), (String) json.get("title"), id));
			}
		}
	}
	
	public static class ConfigRequestWrapper extends HttpServletRequestWrapper {
		
		private final String address;
		private final String title;
		private final static String SPLITTER = "==";
		
		public ConfigRequestWrapper(HttpServletRequest request, String id, String cipher) throws Exception {
			super(request);
			if (id.length() < 32)
				throw new RuntimeException("id's length must be greater than 32");
			String plain = AESCipher.decrypt(AESCipher.stringToBytes(cipher), id.substring(0, 16), id.substring(16, 32));
			String[] para = plain.split(SPLITTER);
			if (para.length != 2)
				throw new RuntimeException("Cipher decrypt error, plain: " + plain);
			this.address = para[0];
			this.title = para[1];
		}
		
		@Override
		public String getParameter(String name) {
			if (name.equals("address"))
				return this.address;
			if (name.equals("title"))
				return this.title;
			return super.getParameter(name);
		}
		
		public static String getCipher(String address, String title, String id) throws Exception {
			if (id.length() < 32)
				throw new RuntimeException("id's length must be greater than 32");
			String plain = address + SPLITTER + title;
			return AESCipher.bytesToString(AESCipher.encrypt(plain, id.substring(0, 16), id.substring(16, 32)));
		}
	}
}
