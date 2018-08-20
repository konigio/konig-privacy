package io.konig.privacy.deidentification.config;

import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import io.konig.privacy.deidentification.service.DataAccessException;
import io.konig.privacy.deidentification.service.DatasourceService;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class BasicAuthenticationPoint extends BasicAuthenticationEntryPoint {
	@Autowired
	DatasourceService dataSourceService;

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authEx)
			throws IOException, ServletException {
		validate(request, response);
	}

	private boolean validate(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		String authHeader = req.getHeader("Authorization");
		String dbPassword = null;
		if (authHeader != null) {
			StringTokenizer st = new StringTokenizer(authHeader);
			if (st.hasMoreTokens()) {
				String basic = st.nextToken();
				if (basic.equalsIgnoreCase("Basic")) {
					try {
						String credentials = new String(Base64.decodeBase64(st.nextToken()), "UTF-8");
						int p = credentials.indexOf(":");
						if (p != -1) {
							String _username = credentials.substring(0, p).trim();
							String _password = convertToSHA256String(credentials.substring(p + 1).trim());
							try {
								dbPassword = dataSourceService.getUserDetails(_username);
							} catch (DataAccessException e) {
								e.getMessage();
							}

							if (!dbPassword.equals(_password)) {
								unauthorized(resp, "Bad credentials");
							} else {
								return true;
							}
						} else {
							unauthorized(resp, "Invalid authentication token");
						}
					} catch (UnsupportedEncodingException e) {
						throw new Error("Couldn't retrieve authentication", e);
					}
				}
			}
			unauthorized(resp, "No Header Value");
		}
		return false;
	}

	private static String convertToSHA256String(String value) throws ServletException {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] bytes = md.digest(value.getBytes());
			StringBuffer stringBuffer = new StringBuffer();
			for (int i = 0; i < bytes.length; i++) {
				stringBuffer.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
			}
			return stringBuffer.toString();
		} catch (Throwable e) {
			throw new ServletException(e);
		}
	}

	private void unauthorized(HttpServletResponse response, String message) throws IOException {
		response.sendError(HttpServletResponse.SC_UNAUTHORIZED, message);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		setRealmName("KonigPrivacyDeIdentification");
		super.afterPropertiesSet();
	}

}
