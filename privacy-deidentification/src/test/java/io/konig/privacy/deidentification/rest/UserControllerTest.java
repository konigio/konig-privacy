package io.konig.privacy.deidentification.rest;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.fileUpload;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;


@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private JdbcTemplate jdbcTemplate;
	
	@Test
	public void authenticationTest() throws Exception {
		mockMvc.perform(
				get("/api/privacy/{version}/credentials", "v1"))
				.andExpect(status().isUnauthorized()).andDo(print());
	}
	
	@Ignore
	public void uploadUserAccountTest() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "Users/UploadUserAccounts.csv", "multipart/form-data",
				"Username,Password,Permissions\nsofaIntegration,Cwm9tKXb8YkVALhGLWyCz8GUmPYzx2,PII".getBytes());
		mockMvc.perform(
				fileUpload("/api/privacy/credentials").file(file).header("authorization", "Basic dGVzdDp0ZXN0MTIz"))
				.andExpect(status().isCreated()).andDo(print());
	}

	@Ignore
	public void getUserAccounts() throws Exception {
		mockMvc.perform(
				get("/api/privacy/{version}/credentials", "v1").header("authorization", "Basic dGVzdDp0ZXN0MTIz"))
				.andExpect(status().isOk()).andDo(print());
	}
	
}
