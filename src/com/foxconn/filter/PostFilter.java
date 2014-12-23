package com.foxconn.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import com.foxconn.wrapper.JSONResponseWrapper;

public class PostFilter implements Filter {
	
	private static Logger logger = Logger.getLogger(PostFilter.class);
	private static String AUTHPATH = "/Servlet/GetAuthServlet";
	
	public void init(FilterConfig fConfig) throws ServletException {
		if (fConfig.getInitParameter("authpath") != null)
			AUTHPATH = fConfig.getInitParameter("authpath");
	}
	
	public void destroy() {}

	@SuppressWarnings("unchecked")
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse resp = (HttpServletResponse) response;
		JSONObject ret = new JSONObject();
		if (req.getServletPath().equals(AUTHPATH)) {
			chain.doFilter(request, response);
			return ;
		} else if (req.getMethod().equalsIgnoreCase("post")) {
			try {
				JSONResponseWrapper responseWrapper = new JSONResponseWrapper(resp);
				request.getRequestDispatcher(AUTHPATH).forward(request, responseWrapper);
				JSONObject obj = (JSONObject) responseWrapper.toJSON();
				logger.debug("Auth: " + responseWrapper.toString());
				if (((Long) obj.get("code") == 200) && ((String) obj.get("user")).equals("admin")) {
					chain.doFilter(request, response);
					return ;
				} else {
					ret.put("code", 401);
					ret.put("msg", "No Post Auth");
					logger.debug(obj);
				}
			} catch (ParseException e) {
				ret.put("code", 500);
				ret.put("msg", "Server error");
				logger.error("Servlet Return is NOT JSON", e);
			} catch (Exception e) {
				ret.put("code", 500);
				ret.put("msg", "Server error");
				logger.error("PostFilter Error", e);
			}
			response.getWriter().println(ret);
			response.getWriter().close();
			return ;
		} else {
			chain.doFilter(request, response);
			return ;
		}
	}

	public boolean accept(String role) {
		if (role.equals("admin"))
			return true;
		else
			return false;
	}

}
