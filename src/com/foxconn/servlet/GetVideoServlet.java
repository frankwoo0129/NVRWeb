package com.foxconn.servlet;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.connector.ClientAbortException;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.foxconn.wrapper.JSONResponseWrapper;

public class GetVideoServlet extends HttpServlet {
	
	private static SimpleDateFormat inFormat = new SimpleDateFormat("yyyyMMddHHmm");
	private static SimpleDateFormat outFormat = new SimpleDateFormat("yyyyMMddHHmmss");
	private static SimpleDateFormat showFormat = new SimpleDateFormat("HH:mm");
	private static Logger logger = Logger.getLogger(GetVideoServlet.class);
	
	private static String titleFormat = "%s~%s";
	private static String linkFormat = "%s/%s/%s";
	private static String STORAGE_PATH = "/media";
	private static String SERVICE_PATH = "/NVRService";
	private static String CAMERA_SERVLET_PATH = "/Servlet/GetCameraServlet";
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		STORAGE_PATH = (this.getServletContext().getInitParameter("STORAGE_CONTEXT") != null)
				? this.getServletContext().getInitParameter("STORAGE_CONTEXT") : "/media";
		SERVICE_PATH = (this.getServletContext().getInitParameter("SERVICE_CONTEXT") != null)
				? this.getServletContext().getInitParameter("SERVICE_CONTEXT") : "/NVRService";
		CAMERA_SERVLET_PATH = (this.getServletContext().getInitParameter("CAMERA_SERVLET") != null)
				? this.getServletContext().getInitParameter("CAMERA_SERVLET") : "/Servlet/GetCameraServlet";
	}

	/**
	 * Parameter:
	 * group:
	 * title:
	 * address:
	 * from:
	 * to:
	 * 
	 * Or PathInfo, like: /Servlet/GetVideoServlet/XXXXXXX/YYYYY
	 * PathInfo = /XXXXXX/YYYYY
	 * 
	 * Return:
	 * return mp4 file or mp4 file list
	 * code:
	 * list:
	 * 
	 * Or forward to mp4 file if using PathInfo
	 * 
	 * Example:
	 * {"code":200, "list":
	 * [{"title":"09:00~10:00","url":"http://localhost:8080/NVRCenter/Servlet/GetVideoServlet/{group}/{title}/{filename}"},
	 * {"title":"10:00~11:00","url":"http://localhost:8080/NVRCenter/Servlet/GetVideoServlet/{group}/{title}/{filename}"}
	 * ]}
	 */
	@SuppressWarnings("unchecked")
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		JSONObject ret = new JSONObject();
		String mode = request.getServletPath();
		
		if (request.getPathInfo() == null) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			ret.put("code", HttpServletResponse.SC_BAD_REQUEST);
			ret.put("msg", "No path info");
		} else if (mode.contains("Video")) {
			String address = null;
			String day = null;
			String from = null;
			String to = null;
					
			// pathinfo = /{IP}/{day}/{from}/{to}
			String[] pathinfo = request.getPathInfo().split("/");
			if (pathinfo.length > 1)
				address = pathinfo[1];
			if (pathinfo.length > 2)
				day = pathinfo[2];
			if (pathinfo.length > 3)
				from = pathinfo[3];
			if (pathinfo.length > 4)
				to = pathinfo[4];
			
			logger.debug("address: " + address);
			logger.debug("day: " + day);
			logger.debug("from: " + from);
			logger.debug("to: " + to);
				
			if (address == null) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				ret.put("code", HttpServletResponse.SC_BAD_REQUEST);
				ret.put("msg", "address is NULL");
			} else {		
				try {
					JSONResponseWrapper responseWrapper = new JSONResponseWrapper(response);
					this.getServletContext().getContext(SERVICE_PATH).getRequestDispatcher(CAMERA_SERVLET_PATH + "/" + address).forward(request, responseWrapper);
					JSONObject obj = (JSONObject) responseWrapper.toJSON();
					if (responseWrapper.getStatus() != HttpServletResponse.SC_OK) {
						ret.putAll(obj);
					} else {			
						String videopath = (String) obj.get("videopath");
						File dir = Paths.get(videopath).toFile();
						if (!dir.exists()) {
							logger.error(String.format("directory: %s is NOT exist", videopath));
							response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
							ret.put("code", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
							ret.put("msg", "Server Error");
						} else {
							videopath = videopath.replace(this.getServletContext().getContext(STORAGE_PATH).getRealPath(""), "");
							if (videopath.startsWith("/"))
								videopath = videopath.substring(1);
							VideoFilter vf = null;
							if (day == null) {
								vf = new VideoFilter(0, Calendar.getInstance().getTimeInMillis());
							} else if (from == null) {
								Date start = inFormat.parse(day + "0000");
								vf = new VideoFilter(start.getTime(), Calendar.getInstance().getTimeInMillis());
							} else if (to == null) {
								Date start = inFormat.parse(day + from);
								vf = new VideoFilter(start.getTime(), Calendar.getInstance().getTimeInMillis());
							} else {
								Date start = inFormat.parse(day + from);
								Date end = inFormat.parse(day + to);
								vf = new VideoFilter(start.getTime(), end.getTime());
							}
							File[] files = dir.listFiles(vf);
							Arrays.sort(files, new VideoFileSort());
							JSONArray records = new JSONArray();
							for (File f : files) {
								JSONObject inner = new JSONObject();
								String startTitle = f.getName().substring(8, 10) + ":" + f.getName().substring(10, 12);
								String endTitle = showFormat.format(new Date(f.lastModified()));
								inner.put("title", String.format(titleFormat, startTitle, endTitle));
								inner.put("url", String.format(linkFormat, request.getRequestURL().substring(0, request.getRequestURL().indexOf("/" + address)).replace("GetVideoServlet", "GetFilmServlet"), videopath, f.getName()));
								records.add(inner);
							}
							ret.put("list", records);
							ret.put("code", HttpServletResponse.SC_OK);
						}
					}
				} catch (ParseException e) {
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					ret.put("code", HttpServletResponse.SC_BAD_REQUEST);
					ret.put("msg", String.format("date format error. day=%s, from=%s, to=%s", day, from, to));
				} catch (Exception e) {
					logger.error(e.getClass().getName(), e);
					response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					ret.put("code", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					ret.put("msg", "Server Error");
				}
			}
		} else if (mode.contains("Film")) {
			String file = request.getPathInfo();
			logger.debug("file: " + file);
			if (file != null) {
				try {
					if (this.getServletContext().getContext(STORAGE_PATH) == null) {
						logger.error("Media Storage Error");
						ret.put("msg", "Storage Error");
						ret.put("code", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					} else {
						this.getServletContext().getContext(STORAGE_PATH).getRequestDispatcher(file).forward(request, response);
						return ;
					}
				} catch (ClientAbortException e) {
					logger.debug("ClientAbort");
				} catch (Exception e) {
					ret.put("msg", "url error");
					ret.put("code", HttpServletResponse.SC_BAD_REQUEST);
					logger.error(e.getClass().getName());
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				}
			} else {
				ret.put("msg", "No file url");
				ret.put("code", HttpServletResponse.SC_BAD_REQUEST);
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			}
		} else {
			ret.put("msg", "url error, " + request.getServletPath());
			ret.put("code", HttpServletResponse.SC_BAD_REQUEST);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		}
		
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.println(ret);
		out.close();
		return ;
	}
	
	private class VideoFilter implements FileFilter {
		
		private final long startTimeMillis;
		private final long endTimeMillis;
		
		public VideoFilter(long start, long end) {
			this.startTimeMillis = start;
			this.endTimeMillis = end;
		}
		
		@Override
		public boolean accept(File file) {
			/*
			 * Example:
			 * 20140522090010.mp4
			 */
			String s = file.getName().substring(0, file.getName().lastIndexOf('.'));// 20140522090010
			long now = Calendar.getInstance().getTimeInMillis();
			try {
				long time = outFormat.parse(s).getTime();
				if (time > endTimeMillis)
					return false;
				else if (file.lastModified() < startTimeMillis)
					return false;
				else if ((now - file.lastModified()) < 60000)
					return false;
				else
					return true;
			} catch (Exception e) {
				return false;
			}
		}
	}
	
	private class VideoFileSort implements Comparator<File> {
		@Override
		public int compare(File lhs, File rhs) {
			try {
				String lhsname = lhs.getName().substring(0, lhs.getName().lastIndexOf('.'));
				String rhsname = rhs.getName().substring(0, rhs.getName().lastIndexOf('.'));
				long lhstime = outFormat.parse(lhsname).getTime();
				long rhstime = outFormat.parse(rhsname).getTime();
				return ((Long) (lhstime - rhstime)).intValue();
			} catch (Exception e) {
				throw new RuntimeException("Compare Error");
			}
		}
	}
}
