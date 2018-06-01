package io.konig.privacy.cloud;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Properties;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
/**
 * 
 * This class is used to create Cloud formation stack.
 *
 */
public class CreateCloudFormationStack {
	private final Logger slf4jLogger = LoggerFactory.getLogger(CreateCloudFormationStack.class);
	/**
	 * Method is used to intialize the property file by getting the location of file as argument
	 * @param file
	 */
	public  static void intialize(File file){
		InputStream in = null;
		try {
			Properties p = new Properties();
			in = new FileInputStream(file);
			p.load(in);

			for (String name : p.stringPropertyNames()) {
				String value = p.getProperty(name);
				System.setProperty(name, value);
			}
			in.close();
		} catch (IOException ex) {
			try {
				throw new IOException("Unable to configure system property ");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} 
	}
	/**
	 * This method creates Cloud Formation stack
	 * @param file
	 * @throws Exception
	 */
	public void createAmazonCloudFormationStack(File file) throws Exception{
		Properties properties=System.getProperties();
		StringWriter result = new StringWriter();
		intialize(file);
		
        AmazonCloudFormation stackbuilder = AmazonCloudFormationClientBuilder.standard()
            .withCredentials(getCredential())
            .withRegion(System.getProperty("aws.region"))
            .build();
		slf4jLogger.info("===========================================");
		slf4jLogger.info("Getting Started with AWS CloudFormation");
		slf4jLogger.info("===========================================\n");
        
        String stackName           = "CloudFormationPrivacyStack";
        
        VelocityEngine engine = new VelocityEngine(properties);
		Template template = engine.getTemplate("/src/aws/cloudformation.yml", "UTF-8");
		VelocityContext context=new VelocityContext();
		context.put("beginVar", "${");
		context.put("endVar", "}");
		for(Object key:properties.keySet()){
			String k=(String)key;
			context.put(k, properties.getProperty(k));
		}
		template.merge(context, result);
		String strResult=result.toString();

        try {
            // Create a stack
            CreateStackRequest createRequest = new CreateStackRequest();
            createRequest.setStackName(stackName);
            try {
				createRequest.setTemplateBody(strResult);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            slf4jLogger.info("Creating a stack called "+ createRequest.getStackName());
            stackbuilder.createStack(createRequest);

            // Wait for stack to be created
            // Note that you could use SNS notifications on the CreateStack call to track the progress of the stack creation
            slf4jLogger.info("Stack creation completed, the stack "+ stackName + " completed ");
	
	} catch (AmazonServiceException ase) {
		slf4jLogger.error("Caught an AmazonServiceException, which means your request made it ");
		slf4jLogger.error("to AWS CloudFormation, but was rejected with an error response for some reason.");
		slf4jLogger.error("Error Message:    " + ase.getMessage());        
		slf4jLogger.error("HTTP Status Code: " + ase.getStatusCode()); 
		slf4jLogger.error("AWS Error Code:   " + ase.getErrorCode()); 
		slf4jLogger.error("Error Type:       " + ase.getErrorType());
		slf4jLogger.error("Request ID:       " + ase.getRequestId());        
    } catch (AmazonClientException ace) {
			slf4jLogger.error(
					"Caught an AmazonServiceException, which means the client encountered a serious internal problem while trying to communicate with AWS CloudFormation");
			slf4jLogger.error("Error Message: " + ace.getMessage());        
    }
	}
		
	/**
	 * This method is used to build the credentials for AWS.
	 * @return
	 * @throws Exception
	 */
	public static AWSStaticCredentialsProvider getCredential() throws Exception {
		String accessKeyId = System.getProperty("aws.accessKeyId");
		String secretKey = System.getProperty("aws.secretKey");
		if (accessKeyId == null || secretKey == null)
			throw new Exception();
		return new AWSStaticCredentialsProvider(
				new BasicAWSCredentials(System.getProperty("aws.accessKeyId"), System.getProperty("aws.secretKey")));

	}
}
