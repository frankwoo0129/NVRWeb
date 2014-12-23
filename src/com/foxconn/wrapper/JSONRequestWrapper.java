package com.foxconn.wrapper;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class JSONRequestWrapper extends HttpServletRequestWrapper {
	
	private JSONObject parameter = new JSONObject();

	@SuppressWarnings("unchecked")
	public JSONRequestWrapper(HttpServletRequest request, JSONObject parameter) {
		super(request);
		this.parameter.putAll(parameter);
	}

	@Override
	public String getParameter(String name) {
		Object obj = parameter.get(name);
		try {
			return String.valueOf(obj);
		} catch (Exception e) {
			return super.getParameter(name);
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Map<String, String[]> getParameterMap() {
		Map<String, String[]> map = new HashMap<String, String[]>();
		Set set = parameter.keySet();
		Iterator iter = set.iterator();
		while (iter.hasNext()) {
			Object obj = iter.next();
			try {
				String name = String.valueOf(obj);
				String[] values = this.getParameterValues(name);
				if (values == null)
					continue;
				map.put(name, values);
			} catch (Exception e) {
				continue;
			}
		}
		return map;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Enumeration<String> getParameterNames() {
		Vector<String> vector = new Vector<String>();
		Set set = parameter.keySet();
		Iterator iter = set.iterator();
		while (iter.hasNext()) {
			Object obj = iter.next();
			try {
				vector.add(String.valueOf(obj));
			} catch (Exception e) {
				continue;
			}
		}
		return vector.elements();
	}

	@Override
	public String[] getParameterValues(String name) {
		Object obj = parameter.get(name);
		if (obj != null && obj instanceof JSONArray) {
			JSONArray list = (JSONArray) obj;
			List<String> l = new ArrayList<String>();
			if (list.size() == 0)
				return null;
			for (Object inner : list) {
				try {
					l.add(String.valueOf(inner));
				} catch (Exception e) {
					continue;
				}
			}
			return l.toArray(new String[l.size()]);
		} else {
			return super.getParameterValues(name);
		}
	}
	
}
