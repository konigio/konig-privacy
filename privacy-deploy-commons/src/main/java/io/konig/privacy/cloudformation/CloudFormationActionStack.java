package io.konig.privacy.cloudformation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

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
	
	public void createTemplateForAWSDeployment(File configFile, File velocityTemplate, File outFile) throws IOException {
		Properties properties=System.getProperties();
		intialize(configFile);
		VelocityEngine engine = velocityEngine(velocityTemplate);
	
		
		Template template = engine.getTemplate(velocityTemplate.getName(), "UTF-8");
		VelocityContext context=new VelocityContext();
		context.put("beginVar", "${");
		context.put("endVar", "}");
		for(Object key:properties.keySet()){
			String k=(String)key;
			context.put(k, properties.getProperty(k));
		}
		File parentDir = outFile.getParentFile();
		parentDir.mkdirs();
		
		try (FileWriter writer = new FileWriter(outFile)) {

			template.merge(context, writer);
		}
		
	}

	private VelocityEngine velocityEngine(File velocityTemplate) {
		String templateDir = velocityTemplate.getParentFile().getAbsolutePath();
		Properties properties = new Properties();
		properties.put("resource.loader", "file");
		properties.put("file.resource.loader.path", templateDir);
		return new VelocityEngine(properties);
	}
   
}
