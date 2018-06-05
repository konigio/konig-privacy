package io.konig.privacy.cloudformation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.regex.Matcher;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

/**
 * Hello world!
 *
 */
public class CloudFormationActionStack 
{
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
	
	public String CreateTemplateForAWSDeployment(File file){
		String strResult=null;
		Properties properties=System.getProperties();
		StringWriter result = new StringWriter();
		intialize(file);
		VelocityEngine engine = new VelocityEngine(properties);
	
		String path= System.getProperty("user.dir");
		Path rs = Paths.get(path);
		path.replaceAll("\\\\","/");
		//TODO - get the cloudformation.yml from privacy-deploy/src/aws path
		path = path+"/privacy-deploy/src/aws/cloudformation.yml";
		Template template = engine.getTemplate(path, "UTF-8");
		VelocityContext context=new VelocityContext();
		context.put("beginVar", "${");
		context.put("endVar", "}");
		for(Object key:properties.keySet()){
			String k=(String)key;
			context.put(k, properties.getProperty(k));
		}
		template.merge(context, result);
		strResult=result.toString();
		return strResult;
	}
   
}
