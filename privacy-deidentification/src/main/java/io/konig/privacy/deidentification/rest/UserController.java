package io.konig.privacy.deidentification.rest;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.opencsv.CSVReader;
import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.CsvToBean;

import io.konig.privacy.deidentification.model.Users;
import io.konig.privacy.deidentification.repo.DatasourceTrustServiceImpl;
import io.konig.privacy.deidentification.service.DataAccessException;
import io.konig.privacy.deidentification.service.UserService;

@RestController
@RequestMapping(value = { "/api" })
public class UserController {
	private static Logger logger = LoggerFactory.getLogger(UserController.class);

	@Autowired
	UserService userService;

	@RequestMapping(value = "/privacy/credentials", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> uploadUserAccounts(@RequestParam("file") MultipartFile file) throws Exception {
		CSVReader csvReader = null;
		InputStream input = file.getInputStream();
		try {
			csvReader = new CSVReader(new InputStreamReader(input), ',', '"', 1);
			ColumnPositionMappingStrategy mappingStrategy = new ColumnPositionMappingStrategy();
			mappingStrategy.setType(Users.class);
			String[] columns = new String[] { "userName", "password", "permissions" };
			mappingStrategy.setColumnMapping(columns);
			CsvToBean ctb = new CsvToBean();
			List<Users> usersList = ctb.parse(mappingStrategy, csvReader);
			userService.uploadUserAccounts(usersList);
		} catch (Exception exp) {
			logger.info(exp.getMessage());
			throw new Exception();
		} finally {
			try {
				csvReader.close();
			} catch (Exception e) {
				logger.info(e.getMessage());
				throw new Exception();
			}
		}

		HttpHeaders responseHeaders = new HttpHeaders();
		return new ResponseEntity<>(responseHeaders, HttpStatus.CREATED);
	}
	
	@RequestMapping(value = "/privacy/{version}/credentials", method = RequestMethod.GET)
	@ResponseBody
	public void getUserAccounts(HttpServletResponse response) throws Exception {
		response.addHeader("Content-Type", "application/csv");
		response.addHeader("Content-Disposition", "attachment; filename=UserAccounts.csv");
		response.setCharacterEncoding("UTF-8");
		try{
			List<Users> userDetailsStream = userService.getUserAccounts();
			PrintWriter out = response.getWriter();
			out.write("\"userName\",\"permissions\"");
			out.write("\n");
			if(userDetailsStream != null){
			userDetailsStream.forEach(userDetail -> {
				out.write(userDetail.toString());
				out.write("\n");
			});
			}
			out.flush();
			out.close();
		}catch (IOException ix) {
			throw new Exception();
		}
	}

	@RequestMapping(value="/privacy/{version}/credentials" , method = RequestMethod.DELETE)
	public ResponseEntity<?> deleteUserAccounts(@RequestParam("file") MultipartFile file) throws Exception {
		CSVReader csvReader = null;
		InputStream input = file.getInputStream();
		try {
			csvReader = new CSVReader(new InputStreamReader(input), ',', '"', 1);
			ColumnPositionMappingStrategy mappingStrategy = new ColumnPositionMappingStrategy();
			mappingStrategy.setType(Users.class);
			String[] columns = new String[] { "userName" };
			mappingStrategy.setColumnMapping(columns);
			CsvToBean ctb = new CsvToBean();
			List<Users> usersList = ctb.parse(mappingStrategy, csvReader);
			userService.deleteUserAccounts(usersList);

		} catch (Exception exp) {
			logger.info(exp.getMessage());
			throw new Exception();
		} finally {
			try {
				csvReader.close();
			} catch (Exception e) {
				logger.info(e.getMessage());
				throw new Exception();
			}
		}

		HttpHeaders responseHeaders = new HttpHeaders();
		return new ResponseEntity<>(responseHeaders, HttpStatus.OK);
	}
}
