package io.konig.privacy.deploy;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
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
 * Goal which touches a timestamp file.
 *
 * 
 */
@Mojo( name = "deploy" )
public class PrivacyDeploymentMojo
    extends AbstractMojo
{
	private final Logger slf4jLogger = LoggerFactory.getLogger(PrivacyDeploymentMojo.class);
   
    
    @Parameter(property="konig.privacy.deployment.cloudformationFile", defaultValue="${project.basedir}/target/deploy/aws/cloudformation.yaml")
    private File cloudformationFile;
    
    public void execute()
        throws MojoExecutionException
    {
    	slf4jLogger.info("===========================================");
		slf4jLogger.info("Getting Started with AWS CloudFormation");
		slf4jLogger.info("===========================================\n");
        String stackName           = "CloudFormationPrivacyStack";
        AmazonCloudFormation stackbuilder = null;
        try {
			stackbuilder = AmazonCloudFormationClientBuilder.standard()
			        .withCredentials(getCredential())
			        .withRegion(System.getProperty("aws.region"))
			        .build();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();			
		}
        try {
            // Create a stack
            CreateStackRequest createRequest = new CreateStackRequest();
            createRequest.setStackName(stackName);
            try {
            	String templateText = readText(cloudformationFile);
				createRequest.setTemplateBody(templateText);
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
    private String readText(File file) throws IOException {
    	return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
	}
	public static AWSStaticCredentialsProvider getCredential() throws Exception {
		String accessKeyId = System.getProperty("aws.accessKeyId");
		String secretKey = System.getProperty("aws.secretKey");
		if (accessKeyId == null || secretKey == null)
			throw new Exception();
		return new AWSStaticCredentialsProvider(
				new BasicAWSCredentials(System.getProperty("aws.accessKeyId"), System.getProperty("aws.secretKey")));

	}
}
