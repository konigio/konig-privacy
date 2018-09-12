package io.konig.privacy.deidentification.service;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonBuilder {
	
	private List<JsonNode> stack = new ArrayList<>();
	private ObjectMapper mapper = new ObjectMapper();
	
	public JsonBuilder begin() {
		stack.add(mapper.createObjectNode());
		return this;
	}
	
	public JsonBuilder beginObject(String fieldName) {
		ObjectNode obj = mapper.createObjectNode();
		ObjectNode parent = peekObject();
		parent.set(fieldName, obj);
		stack.add(obj);
		return this;
	}
	
	public JsonBuilder put(String fieldName, String value) {
		peekObject().put(fieldName, value);
		return this;
	}
	
	public JsonBuilder set(String fieldName, JsonNode node) {
		if (node != null) {
			peekObject().set(fieldName, node);
		}
		return this;
	}
	
	public JsonBuilder endObject(String fieldName) {
		stack.remove(stack.size()-1);
		return this;
	}

	private ObjectNode peekObject() {
		return (ObjectNode) stack.get(stack.size()-1);
	}
	
	public ObjectNode end() {
		return (ObjectNode) stack.remove(stack.size()-1);
		
	}

}
