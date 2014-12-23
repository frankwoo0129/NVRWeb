package com.foxconn.servlet;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Calendar;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.connector.ClientAbortException;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import com.foxconn.wrapper.JSONResponseWrapper;

public class GetJPEGServlet extends HttpServlet {
	
	public static int millis = 500;
	private final static int BUFFER = 60;
	private static Logger logger = Logger.getLogger(GetJPEGServlet.class);
	
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

	/**
	 * Given address, title, and group parameters forward to GetCameraServlet for getting jpegpath.
	 * 
	 * Parameter:
	 * title: camera's title
	 * 
	 * Return:
	 * "image/jpeg" if no error
	 * 
	 * "application/json" if error
	 * code: 200 or 400 or 404 or 500
	 * msg: error msg
	 * 
	 * @see CameraControl#doGet(HttpServletRequest, HttpServletResponse)
	 */
	@SuppressWarnings("unchecked")
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		JSONObject ret = new JSONObject();
		if (request.getParameter("address") == null) {
			ret.put("msg", "No 'address' parameter");
			ret.put("code", HttpServletResponse.SC_BAD_REQUEST);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.setContentType("application/json");
			PrintWriter out = response.getWriter();
			out.println(ret);
			out.close();
			return ;
		}
			
		try {
			JSONResponseWrapper responseWrapper = new JSONResponseWrapper((HttpServletResponse) response);
			this.getServletContext().getContext(SERVICE_PATH).getRequestDispatcher(CAMERA_SERVLET_PATH + "/" + request.getParameter("address")).forward(request, responseWrapper);
			
			JSONObject obj = (JSONObject) responseWrapper.toJSON();
			if (responseWrapper.getStatus() != HttpServletResponse.SC_OK) {
				ret.putAll(obj);
				response.setContentType("application/json");
				PrintWriter out = response.getWriter();
				out.println(ret);
				out.close();
				return ;
			} else {	
				Calendar cal = Calendar.getInstance();
				long now = cal.getTimeInMillis();
				cal.set(Calendar.MINUTE, 0);
				cal.set(Calendar.SECOND, 0);
				long init = cal.getTimeInMillis();
				int count = (int) (now-init)/millis + 1 - BUFFER;
				if (count <= 0)
					count += (3600000/millis);
				
				String jpegpath = (String) obj.get("jpegpath");
				File jpeg = new File(jpegpath, String.format("out%d.jpg", count));
				if (!jpeg.exists()) {
					ret.put("msg", "File Not Found: " + jpeg.getName());
					ret.put("code", HttpServletResponse.SC_NOT_FOUND);
					response.setStatus(HttpServletResponse.SC_NOT_FOUND);
					response.setContentType("application/json");
					PrintWriter out = response.getWriter();
					out.println(ret);
					out.close();
					return ;
				} else if ((now - jpeg.lastModified()) > 600000) {
					ret.put("msg", "File Error: " + jpeg.getName());
					ret.put("code", HttpServletResponse.SC_NOT_FOUND);
					response.setStatus(HttpServletResponse.SC_NOT_FOUND);
					response.setContentType("application/json");
					PrintWriter out = response.getWriter();
					out.println(ret);
					out.close();
					return ;
				} else {
					response.setContentType("image/jpeg");
					OutputStream out = response.getOutputStream();
					FileInputStream fin = new FileInputStream(jpeg);
					BufferedInputStream bin = new BufferedInputStream(fin);
					BufferedOutputStream bout = new BufferedOutputStream(out);
					try {
						int ch =0; ;
						while((ch=bin.read()) != -1){
							bout.write(ch);
						}
					} catch (Exception e) {
					} finally {
						bin.close();
						fin.close();
						bout.close();
						out.close();
					}
				}
			}
		} catch (ClientAbortException e) {
			logger.debug("ClientAbort");
		} catch (Exception e) {
			logger.error(e.getMessage(), e);			
			ret.put("msg", "Server Error, " + e.getClass().getName());
			ret.put("code", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.setContentType("application/json");
			PrintWriter out = response.getWriter();
			out.println(ret);
			out.close();
			return ;
		}
	}
}
