package io.konig.privacy.pushimage;

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Goal which touches a timestamp file.
 *
 * 
 */
@Mojo( name = "push" )
public class ImagePushToAWSMojo
    extends AbstractMojo
{
    
    @Parameter
	private String awsEcsRepositoryUrn;
	
	@Parameter
	private String imageName;
	
	@Parameter
	private String repositoryName;

    public void execute()
        throws MojoExecutionException
    {
    	try{
    		 Process process = Runtime.getRuntime().exec("aws ecr create-repository --repository-name "+repositoryName);
    		 BufferedReader buffer = new BufferedReader(new InputStreamReader(process.getInputStream()));
    		 String line;
    		 while((line=buffer.readLine())!=null)
    			{
    			 System.out.println(line);
    			}
    			process = Runtime.getRuntime().exec("aws ecr get-login");
    	 		 buffer = new BufferedReader(new InputStreamReader(process.getInputStream()));
    	 
    	 		while((line=buffer.readLine())!=null)
    	 		{
    	 			System.out.println("Docker login command from AWS"+line);
    	 			process = Runtime.getRuntime().exec(line);
    	 			buffer = new BufferedReader(new InputStreamReader(process.getInputStream()));
    	 				
    	 				while((line=buffer.readLine())!=null)
    	 				{
    	 					System.out.println(line);
    	 				}
    	 				String command1="docker tag "+imageName+" "+awsEcsRepositoryUrn+repositoryName+":latest";
    	 				System.out.println(command1);
    	 				process = Runtime.getRuntime().exec(command1);
    	 				buffer=new BufferedReader(new InputStreamReader(process.getInputStream()));
    					 while((line=buffer.readLine())!=null)
    						{
    							System.out.println(line);
    						}
    					 command1="docker push "+awsEcsRepositoryUrn+repositoryName+":latest";
    					 System.out.println(command1);
    	 				process = Runtime.getRuntime().exec(command1);
    	 				
    	 				 buffer=new BufferedReader(new InputStreamReader(process.getInputStream()));
    	 				 while((line=buffer.readLine())!=null)
    	 					{
    	 						System.out.println(line);
    	 					}
    	 		}
    			}catch(IOException e)
    			{
    				e.getMessage();
    			}
    }
    
    
}
