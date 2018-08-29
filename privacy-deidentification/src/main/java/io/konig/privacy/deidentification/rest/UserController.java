package io.konig.privacy.deidentification.rest;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import io.konig.privacy.deidentification.service.UserService;

@RestController
@RequestMapping(value = { "/api" })
public class UserController {

	@Autowired
	UserService userService;

	@RequestMapping(value = "/privacy/credentials", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> uploadPrivacyCredentials(@RequestParam("file") MultipartFile file) throws Exception {
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
			userService.uploadDatasourceUsers(usersList);
		} catch (Exception ee) {
			throw new Exception();
		} finally {
			try {
				csvReader.close();
			} catch (Exception ee) {
				throw new Exception();
			}
		}

		HttpHeaders responseHeaders = new HttpHeaders();
		return new ResponseEntity<>(responseHeaders, HttpStatus.CREATED);
	}
}