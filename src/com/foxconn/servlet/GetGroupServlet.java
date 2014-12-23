package com.foxconn.servlet;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

public class GetGroupServlet extends HttpServlet {
	
	private static Logger logger = Logger.getLogger(GetGroupServlet.class);
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		logger.info("Group Servlet init");
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		super.doGet(request, response);
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		super.doPost(request, response);
	}

}
