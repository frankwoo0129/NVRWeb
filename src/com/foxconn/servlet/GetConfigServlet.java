package com.foxconn.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.foxconn.wrapper.JSONResponseWrapper;

public class GetConfigServlet extends HttpServlet {
	
	private static Logger logger = Logger.getLogger(GetConfigServlet.class);
	private static String SERVICE_PATH = "/NVRService";
	private static String CAMERA_SERVLET_PATH = "/Servlet/GetCameraServlet";
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		SERVICE_PATH = (this.getServletContext().getInitParameter("SERVICE_CONTEXT") != null)
				? this.getServletContext().getInitParameter("SERVICE_CONTEXT") : "/NVRService";
		CAMERA_SERVLET_PATH = (this.getServletContext().getInitParameter("CAMERA_SERVLET") != null)
				? this.getServletContext().getInitParameter("CAMERA_SERVLET") : "/Servlet/GetCameraServlet";
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		JSONObject ret = new JSONObject();
		
		try {
			JSONResponseWrapper responseWrapper = new JSONResponseWrapper(response);
			this.getServletContext().getContext(SERVICE_PATH).getRequestDispatcher(CAMERA_SERVLET_PATH).forward(request, responseWrapper);
			JSONObject obj = (JSONObject) responseWrapper.toJSON();
			
			if (responseWrapper.getStatus() != HttpServletResponse.SC_OK) {
				ret.putAll(obj);
			} else {
				JSONArray list = (JSONArray) obj.get("list");
				JSONArray newList = new JSONArray();
				for (Object o : list) {
					JSONObject json = (JSONObject) o;
					JSONObject newJson = new JSONObject(json);
					newJson.remove("user");
					newJson.remove("passwd");
					newList.add(newJson);
				}
				ret.put("list", newList);
				ret.put("code", HttpServletResponse.SC_OK);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			ret.put("msg", "Server Error, " + e.getClass().getName());
			ret.put("code", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.println(ret);
		out.close();
	}
	
}