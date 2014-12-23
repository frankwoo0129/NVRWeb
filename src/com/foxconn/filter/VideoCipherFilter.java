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
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.catalina.connector.ClientAbortException;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import com.foxconn.util.AESCipher;
import com.foxconn.util.HttpServletException;
import com.foxconn.wrapper.JSONResponseWrapper;

public class VideoCipherFilter implements Filter {
	
	private static Logger logger = Logger.getLogger(VideoCipherFilter.class);
	
	public void init(FilterConfig fConfig) throws ServletException {}
   	
	public void destroy() {}

	@SuppressWarnings("unchecked")
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpSession session = ((HttpServletRequest) request).getSession(false);
		JSONObject ret = new JSONObject();
		try {
			if (session == null) {
				throw new HttpServletException(HttpServletResponse.SC_UNAUTHORIZED, "No auth");
			} else {
				String id = session.getId();
				// url = "./Servlet/GetVideoServlet/27gmq36eheo8akdirrf5d03yfi7wf4csmuyu2kyf89qqnao8rovjemefehr3owe7q23l7ytdyd8hz8fdhlzjmolfnql6pe2gfm2a"
				// file = "/27gmq36eheo8akdirrf5d03yfi7wf4csmuyu2kyf89qqnao8rovjemefehr3owe7q23l7ytdyd8hz8fdhlzjmolfnql6pe2gfm2a"
				String file = ((HttpServletRequest) request).getPathInfo();
				String servletpath = ((HttpServletRequest) request).getServletPath();
				
				if (file != null) {
					try {
						logger.info("file: " + file);
						String plain = AESCipher.decrypt(AESCipher.stringToBytes(file.substring(1)), id.substring(0, 16), id.substring(16, 32));
						logger.info("plain: " + plain);
						request.getRequestDispatcher(servletpath + plain).forward(request, response);
					} catch (ClientAbortException e) {
						logger.debug("ClientAbort");
					} catch (Exception e) {
						logger.debug("VideoCipher Decrypt Error, " + e.getClass().getName(), e);
						throw new HttpServletException(HttpServletResponse.SC_BAD_REQUEST, "url error");
					}
				} else {
					JSONResponseWrapper responseWrapper = new JSONResponseWrapper((HttpServletResponse) response);
					chain.doFilter(request, responseWrapper);
					
					logger.debug("Before VideoCipherFilter: " + responseWrapper.toString());
					ret = (JSONObject) responseWrapper.toJSON();
					if (ret.get("code") != null && (Long) ret.get("code") == 200) {
						if (ret.get("list") != null && (ret.get("list") instanceof JSONArray)) {
							JSONArray list = (JSONArray) ret.get("list");
							for (Object obj : list) {
								JSONObject json = (JSONObject) obj;
								// url = "http://10.120.135.241:8080/NVRCenter/Servlet/GetVideoServlet/{group}/{title}/{filename}"
								String url = (String) json.get("url");
								// plain = "/{group}/{title}/{filename}"
								String plain = url.substring(url.lastIndexOf(servletpath) + servletpath.length());
								// cipher = "27gmq36eheo8akdirrf5d03yfi7wf4csmuyu2kyf89qqnao8rovjemefehr3owe7q23l7ytdyd8hz8fdhlzjmolfnql6pe2gfm2a"
								String cipher = AESCipher.bytesToString(AESCipher.encrypt(plain, id.substring(0, 16), id.substring(16, 32)));
								json.put("url", url.replace(plain,  "/" + cipher));
							}
						} else {
							logger.debug("Servlet Return No list");
						}
					} else {
						logger.debug("Servlet return code is " + ret.get("code"));
					}
					logger.debug("After VideoCipherFilter: " + ret);
				}
			}
		} catch (ParseException | ClassCastException e) {
			logger.error("Servlet response is NOT json", e);
			ret.put("code", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			ret.put("msg", "Server Error");
		} catch (HttpServletException e) {
			ret.put("code", e.getStatus());
			ret.put("msg", e.getMessage());
		} catch (Exception e) {
			logger.error("Filter Error", e);
			ret.put("code", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			ret.put("msg", "Server Error");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.println(ret);
		out.close();
		return ;
	}
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		String url = "/Servlet/GetVideoServlet/{group}/{title}/{Camera.VIDEO}/{filename}";
		String servlet = "/Servlet/GetVideoServlet";
		String plain = url.substring(url.lastIndexOf(servlet) + servlet.length() + 1);
		String cipher = "xxx";
		System.out.println("Plain:" + plain);
		System.out.println(url.replace(plain, cipher));
		System.out.println(url);
		
		url = "http://10.120.135.241:8080/NVRCenter/Servlet/GetVideoServlet/{group}/{title}/{filename}";
		System.out.println(url.substring(url.lastIndexOf(servlet) + servlet.length()));
		
		JSONArray list = new JSONArray();
		JSONObject obj = new JSONObject();
		obj.put("url", "xxx");
		list.add(obj);
		for (Object o : list) {
			JSONObject json = (JSONObject) o;
			json.put("url", "aaa");
		}
		System.out.println(list);
		
		try {
			String plaintext = "/B04-3F/B04-3f D01/video/aaa.mp4";
			String ID = "E9A49C611D7779BB9871CB62A5106DE6";
			String IV = ID.substring(0, 16);
			String encryptionKey = ID.substring(16, 32);
			System.out.println("==Java==");
			System.out.println("plain:   " + plaintext);
			byte[] ciphertext = AESCipher.encrypt(plaintext, IV, encryptionKey);
			System.out.print("cipher:  ");
			String test = AESCipher.bytesToString(ciphertext);
			System.out.println(test);
			System.out.println(test.length());
//			String decrypted = decrypt(cipher, encryptionKey);
			String decrypted = AESCipher.decrypt(AESCipher.stringToBytes(test), IV, encryptionKey);
			System.out.println("decrypt: " + decrypted);
			System.out.println(decrypted.length());
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
	
}
