package com.github.ahitm_2020_2025.blackonionbot.RestAPI;

import org.json.JSONObject;

import com.github.ahitm_2020_2025.blackonionbot.utils.BotUser;
import spark.Request;
import spark.Response;

public interface PostRequest {
	String handle(Request request, Response response, JSONObject body, BotUser user);
	
	default boolean requiresLogin() {
		return true;
	}
	
	default boolean requiresAdmin() {
		return true;
	}
	
	default boolean isJson() {
		return true;
	}
	
	String url();
	
	default String[] requiredParameters() {
		return new String[] {};
	}
}
